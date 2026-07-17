// What's currently on screen, so an identical result doesn't re-render (and re-trigger the animations).
let renderedKey = null;

// Monotonic id for the in-flight guest search, so a slow earlier response can't overwrite a newer one.
let latestSearchId = 0;

// The full guest list, fetched once and then matched against in the browser (near-match search lives
// client-side). A memoised promise so concurrent searches share the single in-flight request.
let allGuestsPromise = null;

// The seating-map SVG, fetched and parsed once, then cloned for every selection.
let seatingMapSvg = null;

// DOM handles, assigned by the browser bootstrap at the bottom of the file. They stay undefined when
// this file is loaded outside a browser (the definitions below are pure and don't touch them).
let searchForm;
let searchInput;
let resultsContainer;

// Compute the history query string for a state change, or return null when the state is unchanged (so
// an identical entry isn't pushed). Pure: it takes the current query string rather than reading
// `window`, so the routing logic stands on its own. Returns "" to mean "clear the query".
function buildQuery(currentSearch, name, guestId) {
    const current = new URLSearchParams(currentSearch);
    const nextName = name.trim().length === 0 ? null : name;
    const nextGuest = guestId === null ? null : String(guestId);

    if (current.get("name") === nextName && current.get("guest") === nextGuest) {
        return null;
    }

    const params = new URLSearchParams();
    if (nextName !== null) {
        params.set("name", nextName);
    }
    if (nextGuest !== null) {
        params.set("guest", nextGuest);
    }

    const query = params.toString();
    return query ? `?${query}` : "";
}

// Parse a history query string into the view state it describes: a name, and an optional guest id.
// The read half of what buildQuery writes, and DOM-free in the same way.
function parseQuery(search) {
    const params = new URLSearchParams(search);
    return {
        name: params.get("name") ?? "",
        guestId: params.get("guest"),
    };
}

// The id every chair in the seating-map SVG carries, composed of a guest's table and seat.
function seatElementId(tableNumber, seatNumber) {
    return `table-${tableNumber}-seat-${seatNumber}`;
}

// The head table carries a real number in the database (so it fits the same seat/table schema as
// every other table), but it is only ever shown by name. This is that internal number.
const HEAD_TABLE_NUMBER = 17;

// How a table is named to guests: "Head Table" for the head table, otherwise "Table N". Kept
// DOM-free so the same wording drives the list, the map caption, and the close-up label.
function tableLabel(tableNumber) {
    return Number(tableNumber) === HEAD_TABLE_NUMBER ? "Head Table" : `Table ${tableNumber}`;
}

// Order a table's guests for the roster: the selected guest's own row first, then everyone else by
// seat number. Each row is tagged `isSelf` by id, not name, so two guests sharing a first name never
// both get highlighted. DOM-free so the ordering stands on its own and can be tested off-browser.
function prepareRoster(guests, selfId) {
    return guests
        .map((guest) => ({ ...guest, isSelf: guest.id === selfId }))
        .sort((a, b) => {
            if (a.isSelf !== b.isSelf) {
                return a.isSelf ? -1 : 1;
            }
            return a.seatNumber - b.seatNumber;
        });
}

// Everyone seated at a given table, picked out of the full guest list. Pure and DOM-free so the
// neighbor roster can be derived from the list already in memory, with no extra request.
function guestsAtTable(guests, tableNumber) {
    return guests.filter((candidate) => candidate.tableNumber === tableNumber);
}

// Where a seat's faint number sits on the close-up: pushed `distance` units outward from the table
// centre through the seat, so the number clears the chair whatever the table's shape.
function seatNumberPosition(seatX, seatY, centerX, centerY, distance) {
    const dx = seatX - centerX;
    const dy = seatY - centerY;
    const length = Math.hypot(dx, dy) || 1;
    return { x: seatX + (dx / length) * distance, y: seatY + (dy / length) * distance };
}

// Where a seat's faint number sits on the rectangular head table's close-up: directly above the top
// row and directly below the bottom row, keeping the number aligned over its chair. The radial rule
// above suits a circle, but on a two-sided table it fans the corner numbers out at an angle; here the
// side is chosen purely by whether the seat sits above or below the table centre.
function stackedSeatNumberPosition(seatX, seatY, centerY, distance) {
    const offset = seatY < centerY ? -distance : distance;
    return { x: seatX, y: seatY + offset };
}

// Fold a name for comparison: strip accents and lowercase, so "José" and "jose" compare equal. Uses
// the browser's own full-Unicode normalizer, no dependency. Pure, like the helpers above.
function foldName(value) {
    return value
        .normalize("NFD")
        .replace(/\p{Diacritic}/gu, "")
        .toLowerCase();
}

// Fewest single-character edits (insert/delete/substitute) to turn `query` into any *prefix* of
// `text`: trailing characters of `text` are free, so "jon" is distance 1 from "john(...)". This is
// how a misspelled query is scored against a full name.
function fuzzyPrefixDistance(query, text) {
    let previous = Array.from({ length: text.length + 1 }, (_, index) => index);

    for (let i = 1; i <= query.length; i++) {
        const current = [i];
        for (let j = 1; j <= text.length; j++) {
            const substitution = query[i - 1] === text[j - 1] ? 0 : 1;
            current[j] = Math.min(
                previous[j] + 1,
                current[j - 1] + 1,
                previous[j - 1] + substitution,
            );
        }
        previous = current;
    }

    return Math.min(...previous);
}

// Rank one guest against an already-folded query. Lower is better; -1 means "no match". An exact
// prefix beats a word-start match, which beats a fuzzy near-miss. Fuzzy only kicks in once the query
// is long enough to be meaningful, since a one- or two-character query is within an edit of almost
// every name and would flood the list.
function guestMatchScore(guest, foldedQuery) {
    const name = foldName(guest.name);
    if (name.startsWith(foldedQuery)) {
        return 0;
    }

    const words = name.split(/\s+/);
    if (words.some((word) => word.startsWith(foldedQuery))) {
        return 1;
    }

    if (foldedQuery.length < 4) {
        return -1;
    }

    const distance = Math.min(
        ...[name, ...words].map((text) => fuzzyPrefixDistance(foldedQuery, text)),
    );
    const threshold = foldedQuery.length <= 5 ? 1 : 2;
    return distance <= threshold ? 10 + distance : -1;
}

// Filter and rank the full guest list against a raw query, best matches first. The list arrives
// already name-sorted from the server, so the alphabetical tiebreak just keeps equal-score rows tidy.
function matchGuests(query, guests) {
    const foldedQuery = foldName(query.trim());
    if (foldedQuery.length === 0) {
        return [];
    }

    return guests
        .map((guest) => ({ guest, score: guestMatchScore(guest, foldedQuery) }))
        .filter((entry) => entry.score >= 0)
        .sort((a, b) => a.score - b.score || a.guest.name.localeCompare(b.guest.name))
        .map((entry) => entry.guest);
}

const SVG_NAMESPACE = "http://www.w3.org/2000/svg";

// How far (in SVG user units) a faint seat number sits outside its chair on the table close-up.
const SEAT_LABEL_DISTANCE = 10;

// Reflect the URL on screen: the list for a `?name=` search, or the map for a `?name=&guest=`
// selection. Used on first load and on Back/Forward, where all we have is the query string.
function renderFromUrl() {
    const { name, guestId } = parseQuery(window.location.search);
    searchInput.value = name;

    if (guestId) {
        showSeatingMapById(name, Number(guestId));
    } else {
        searchGuests(name);
    }
}

// Run a search and record it as a new history entry, so Back/Forward step between searches.
function commitSearch(value) {
    pushState(value, null);
    searchGuests(value);
}

// Push the current view onto the browser history: a `?name=` search, or a `?name=&guest=` map
// selection. Skips duplicates so an unchanged state doesn't create a dead entry; keeping it in the
// URL also lets a search, or a selected seat, survive a reload and be shared.
function pushState(name, guestId) {
    const query = buildQuery(window.location.search, name, guestId);
    if (query === null) {
        return;
    }
    window.history.pushState(null, "", query || window.location.pathname);
}

// Fetch the whole guest list once (same-origin, so it rides the existing session cookie) and cache
// the promise, so every search matches against an in-memory list instead of a round trip. On failure
// the cache is cleared so the next search retries, and callers get an empty list (rendered as "no
// matches") rather than a rejection.
function loadAllGuests() {
    if (allGuestsPromise === null) {
        allGuestsPromise = fetch("/api/guests")
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`Failed to load guests (HTTP ${response.status}).`);
                }
                return response.json();
            })
            .catch((error) => {
                console.error(error);
                allGuestsPromise = null;
                return [];
            });
    }

    return allGuestsPromise;
}

async function searchGuests(value) {
    const query = value.trim();

    if (query.length === 0) {
        showMessage("Start typing to find your seat!");
        return;
    }

    const requestId = ++latestSearchId;
    const guests = await loadAllGuests();

    // A newer search started while the list was loading; drop this now-stale render.
    if (requestId !== latestSearchId) {
        return;
    }

    renderGuests(matchGuests(query, guests));
}

function renderGuests(guests) {
    if (guests.length === 0) {
        showMessage("No guests found with this name.");
        return;
    }

    renderList(guests);
}

function renderList(guests) {
    if (isAlreadyRendered(`list:${guests.map((guest) => guest.id).join(",")}`)) {
        return;
    }

    resultsContainer.classList.remove("map-view");

    const list = document.createElement("ul");
    list.className = "search-results-list";

    for (const guest of guests) {
        const item = document.createElement("li");
        item.className = "search-result";
        item.tabIndex = 0;
        item.setAttribute("role", "button");

        const name = document.createElement("h2");
        name.textContent = guest.name;

        const seat = document.createElement("p");
        seat.textContent = `${tableLabel(guest.tableNumber)}, Seat ${guest.seatNumber}`;

        item.append(name, seat);

        const select = () => selectGuest(guest);
        item.addEventListener("click", select);
        item.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                select();
            }
        });

        list.append(item);
    }

    resultsContainer.replaceChildren(list);
}

// Picking one result fills the search bar with that name and shows the seating map with their seat lit up.
function selectGuest(guest) {
    searchInput.value = guest.name;
    pushState(guest.name, guest.id);
    renderSeatingMap(guest);
}

// Restore the seating map from the URL (reload or Back/Forward): all we have is the guest's name and
// id, so re-run the name search and pick the matching guest out of the results to render their map.
async function showSeatingMapById(name, guestId) {
    const guests = await loadAllGuests();
    const guest = guests.find((candidate) => candidate.id === guestId);

    if (guest) {
        renderSeatingMap(guest);
    } else {
        // The guest is gone (e.g. a stale or shared link) - fall back to the plain search results.
        renderGuests(matchGuests(name, guests));
    }
}

// Fetch and parse the seating-map SVG once, then reuse it (same-origin, so it rides the existing
// session cookie). Throws on a failed fetch or missing <svg> so a bad response is never cached.
async function loadSeatingMap() {
    if (seatingMapSvg === null) {
        const response = await fetch("/images/seating-map.svg");
        if (!response.ok) {
            throw new Error(`Failed to load the seating map (HTTP ${response.status}).`);
        }

        const holder = document.createElement("div");
        holder.innerHTML = await response.text();
        const svg = holder.querySelector("svg");
        if (!svg) {
            throw new Error("Seating-map markup contained no <svg> element.");
        }

        seatingMapSvg = svg;
    }

    return seatingMapSvg;
}

// Build the read-only roster of everyone at the guest's table: the selected guest's own row first
// (highlighted), then the rest in seat order. Returns null for a solo table so no empty roster is
// shown. Real text (not aria-hidden), so it reads alongside the caption for assistive technology.
function renderRoster(guests, guest) {
    const rows = prepareRoster(guests, guest.id);
    if (rows.length <= 1) {
        return null;
    }

    const section = document.createElement("div");
    section.className = "roster";

    const heading = document.createElement("h3");
    heading.className = "roster-heading";
    heading.textContent = "At your table";

    const list = document.createElement("ul");
    list.className = "search-results-list roster-list";

    for (const row of rows) {
        const item = document.createElement("li");
        item.className = row.isSelf ? "roster-row roster-self" : "roster-row";

        const name = document.createElement("p");
        name.className = "roster-name";
        name.textContent = row.name;

        const seat = document.createElement("p");
        seat.className = "roster-seat";
        seat.textContent = `Seat ${row.seatNumber}`;

        item.append(name, seat);
        list.append(item);
    }

    section.append(heading, list);
    return section;
}

// Show the whole room, spotlighting the guest's table and chair. Every chair carries an id of the
// form `table-{tableNumber}-seat-{seatNumber}`, so the guest's own seat is found by composing that id.
async function renderSeatingMap(guest) {
    const key = `map:${guest.id}`;
    if (isAlreadyRendered(key)) {
        return;
    }

    // The roster comes from the full guest list already in memory, filtered to this table, so it
    // needs no extra request. loadAllGuests never rejects (it resolves to an empty list on failure),
    // in which case renderRoster simply shows the map without a roster.
    const rosterPromise = loadAllGuests().then((guests) =>
        guestsAtTable(guests, guest.tableNumber),
    );

    let template;
    try {
        template = await loadSeatingMap();
    } catch (error) {
        console.error(error);
        // Clear the dedup key so re-selecting this guest tries the fetch again.
        renderedKey = null;
        return;
    }

    // A newer view (Back/Forward or a fresh search) was requested while we awaited the map; don't
    // clobber it with this now-stale render.
    if (renderedKey !== key) {
        return;
    }

    // Wait for the roster here, so everything below is built and attached in one synchronous pass.
    // renderTableDetail measures its bounds on the next animation frame, which needs the close-up
    // already in the document - so nothing may await between building it and replaceChildren.
    const rosterGuests = await rosterPromise;
    if (renderedKey !== key) {
        return;
    }

    resultsContainer.classList.add("map-view");

    // The name/seat caption is the accessible source of truth (and the fallback if the seat isn't on the map).
    const caption = document.createElement("div");
    caption.className = "seating-map-caption";

    const name = document.createElement("h2");
    name.className = "seating-map-name";
    name.textContent = guest.name;

    const seat = document.createElement("p");
    seat.className = "seating-map-seat";
    seat.textContent = `${tableLabel(guest.tableNumber)}, Seat ${guest.seatNumber}`;

    caption.append(name, seat);

    const map = document.createElement("div");
    map.className = "seating-map";

    // Clone the parsed map once per render so each view mutates its own copy.
    const svg = template.cloneNode(true);
    // Drop the intrinsic size so CSS controls how the map scales; it always stays landscape.
    svg.removeAttribute("width");
    svg.removeAttribute("height");
    svg.setAttribute("aria-hidden", "true");
    map.append(svg);

    const chair = svg.querySelector(`#${seatElementId(guest.tableNumber, guest.seatNumber)}`);
    const table = chair?.closest("g");
    if (chair) {
        svg.classList.add("is-focused");
        chair.classList.add("seat-highlight");
        table?.classList.add("table-highlight");
    } else {
        // The seat has no chair on the map: the caption still names it, but surface the data/map
        // mismatch instead of silently showing an unlit room.
        console.warn(
            `No chair for table ${guest.tableNumber}, seat ${guest.seatNumber} on the seating map.`,
        );
    }

    const detail = table ? renderTableDetail(table, guest) : null;
    const roster = rosterGuests ? renderRoster(rosterGuests, guest) : null;

    resultsContainer.replaceChildren(...[caption, map, detail, roster].filter(Boolean));

    // Scroll the highlighted seat into view: the full room can be wider than a small screen, so the
    // seat may start off-screen. Wait a frame so the map has been laid out first.
    if (chair) {
        const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
        requestAnimationFrame(() => {
            chair.scrollIntoView({
                behavior: prefersReducedMotion ? "auto" : "smooth",
                block: "center",
                inline: "center",
            });
        });
    }
}

// Build a close-up beneath the full map showing only the guest's own table, with their seat lit up.
// The table `<g>` is cloned out of the full map (so the shapes match exactly), dropped into a fresh
// SVG, and cropped to the table by measuring its bounds once the browser has laid it out. The clone's
// ids are stripped so they don't collide with the identical ids already on the full map.
function renderTableDetail(table, guest) {
    const detailSvg = document.createElementNS(SVG_NAMESPACE, "svg");
    detailSvg.setAttribute("xmlns", SVG_NAMESPACE);
    detailSvg.setAttribute("aria-hidden", "true");

    // The clone is taken after the chair was lit on the full map, so it already carries
    // seat-highlight; the table-highlight it also inherits is inert outside `.is-focused`.
    const tableClone = table.cloneNode(true);

    labelTable(tableClone, guest.tableNumber);

    // Faint seat numbers key each chair to a roster row. Drawn while the clone still carries its
    // chair ids (`table-{t}-seat-{s}`), since the id-strip below reads the number from each id.
    labelSeatNumbers(tableClone);

    tableClone.removeAttribute("id");
    for (const node of tableClone.querySelectorAll("[id]")) {
        node.removeAttribute("id");
    }

    detailSvg.append(tableClone);

    const wrapper = document.createElement("div");
    wrapper.className = "seating-map-detail";
    wrapper.append(detailSvg);

    // getBBox needs the element laid out, so crop to the table's bounds on the next frame.
    requestAnimationFrame(() => {
        const bounds = tableClone.getBBox();
        // A touch more room than the table alone, so the outward seat numbers aren't clipped.
        const padding = 14;
        detailSvg.setAttribute(
            "viewBox",
            `${bounds.x - padding} ${bounds.y - padding} ${bounds.width + padding * 2} ${
                bounds.height + padding * 2
            }`,
        );
    });

    return wrapper;
}

// Write the table's name across the middle of the close-up table: its number, or "Head Table" for the
// rectangular head table. The centre is taken from the table body (the id-less circle/rect the seats
// sit around), so the label lands dead centre whatever the table's shape.
function labelTable(tableClone, tableNumber) {
    // The map's only rectangular table is the head table; every round table is labelled with its
    // number. The data carries no table role, so the shape class is the signal we have.
    const isHeadTable = tableClone.classList.contains("rectangular-table");
    const body = tableClone.querySelector(":scope > circle:not([id]), :scope > rect:not([id])");
    if (!body) {
        return;
    }

    let centerX;
    let centerY;
    if (body.hasAttribute("cx")) {
        centerX = Number(body.getAttribute("cx"));
        centerY = Number(body.getAttribute("cy"));
    } else {
        const bodyWidth = Number(body.getAttribute("width"));
        centerX = Number(body.getAttribute("x")) + bodyWidth / 2;
        centerY = Number(body.getAttribute("y")) + Number(body.getAttribute("height")) / 2;
    }

    const label = document.createElementNS(SVG_NAMESPACE, "text");
    label.setAttribute("x", centerX);
    label.setAttribute("y", centerY);
    label.setAttribute("text-anchor", "middle");
    // `central` aligns the text's centre on the y coordinate, so the label sits at the table's centre.
    label.setAttribute("dominant-baseline", "central");
    label.classList.add("table-label");
    label.textContent = isHeadTable ? "Head Table" : String(tableNumber);

    if (isHeadTable) {
        label.classList.add("head-table-label");
    }

    tableClone.append(label);
}

// Draw a faint number beside every chair on the close-up, matching the seat numbers in the roster.
// Must run before the clone's ids are stripped: the chairs are found by their `table-{t}-seat-{s}`
// id, the number is read from that id, and it's placed just outside the chair via seatNumberPosition.
// The centre is taken from the id-less table body, the same node labelTable measures.
function labelSeatNumbers(tableClone) {
    const body = tableClone.querySelector(":scope > circle:not([id]), :scope > rect:not([id])");
    if (!body) {
        return;
    }

    let centerX;
    let centerY;
    if (body.hasAttribute("cx")) {
        centerX = Number(body.getAttribute("cx"));
        centerY = Number(body.getAttribute("cy"));
    } else {
        centerX = Number(body.getAttribute("x")) + Number(body.getAttribute("width")) / 2;
        centerY = Number(body.getAttribute("y")) + Number(body.getAttribute("height")) / 2;
    }

    // The round tables seat guests all the way around, so their numbers read best pushed straight out
    // along each radial. The head table is two straight rows, where a radial offset skews the corner
    // numbers; stack them vertically instead (above the top row, below the bottom row). The shape
    // class is the same signal labelTable keys off, since the data carries no table role.
    const isHeadTable = tableClone.classList.contains("rectangular-table");

    for (const chair of tableClone.querySelectorAll('[id^="table-"]')) {
        const seatNumber = chair.id.split("-").pop();
        const seatX = Number(chair.getAttribute("cx"));
        const seatY = Number(chair.getAttribute("cy"));
        const position = isHeadTable
            ? stackedSeatNumberPosition(seatX, seatY, centerY, SEAT_LABEL_DISTANCE)
            : seatNumberPosition(seatX, seatY, centerX, centerY, SEAT_LABEL_DISTANCE);

        const label = document.createElementNS(SVG_NAMESPACE, "text");
        label.setAttribute("x", position.x);
        label.setAttribute("y", position.y);
        label.setAttribute("text-anchor", "middle");
        label.setAttribute("dominant-baseline", "central");
        label.classList.add("seat-number-label");
        label.textContent = seatNumber;

        tableClone.append(label);
    }
}

function showMessage(text) {
    if (isAlreadyRendered(`message:${text}`)) {
        return;
    }

    resultsContainer.classList.remove("map-view");

    const message = document.createElement("p");
    message.textContent = text;

    resultsContainer.replaceChildren(message);
}

// Returns true when the requested content is already on screen; otherwise records it as the new state.
function isAlreadyRendered(key) {
    if (renderedKey === key) {
        return true;
    }

    renderedKey = key;
    return false;
}

// Page bootstrap. Guarded by a document check and kept apart from the definitions above so those pure
// functions can also be loaded outside a browser; in a browser this branch always runs. It grabs the
// DOM handles, wires the search box, and restores whatever state the URL already describes.
if (typeof document !== "undefined") {
    searchForm = document.querySelector("form[role='search']");
    searchInput = document.querySelector("#guest-search");
    resultsContainer = document.querySelector("#search-result-container");

    // Warm the in-memory guest list right away so the first keystroke matches with no network wait.
    loadAllGuests();

    // Search on every keystroke. Matching is in-memory now (no round trip), so there's nothing to
    // debounce; each keystroke is its own history entry, so Back/Forward step through the search.
    searchInput.addEventListener("input", () => {
        commitSearch(searchInput.value);
    });

    // Pressing Enter would otherwise submit the form and reload the page; search in place instead.
    searchForm.addEventListener("submit", (event) => {
        event.preventDefault();
        commitSearch(searchInput.value);
    });

    // Back/forward navigates between past states: re-sync the input and results from the URL,
    // without pushing a new entry (this navigation *is* the history change).
    window.addEventListener("popstate", () => {
        renderFromUrl();
    });

    // Restore whatever the URL describes after a reload: a past search, or a guest's seating map.
    if (window.location.search) {
        renderFromUrl();
    }
}
