let timeoutId = null;

// What's currently on screen, so an identical result doesn't re-render (and re-trigger the animations).
let renderedKey = null;

// The seating-map SVG markup, fetched once and reused for every selection.
let seatingMapMarkup = null;

const MIN_SEARCH_LENGTH = 3;
const MAX_SEARCH_LENGTH = 80;

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
// URL also lets a search — or a selected seat — survive a reload and be shared.
function pushState(name, guestId) {
    const query = buildQuery(window.location.search, name, guestId);
    if (query === null) {
        return;
    }
    window.history.pushState(null, "", query || window.location.pathname);
}

async function searchGuests(value) {
    const query = value.trim();

    if (query.length < MIN_SEARCH_LENGTH) {
        showMessage("Start typing to find your seat!");
        return;
    }

    try {
        const response = await fetch(`/api/guests?name=${encodeURIComponent(query)}`, {
            method: "GET"
        });

        const guests = await response.json();

        renderGuests(guests);

    } catch (error) {
        console.error(error);
    }
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
        seat.textContent = `Table ${guest.tableNumber}, Seat ${guest.seatNumber}`;

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
    try {
        const response = await fetch(`/api/guests?name=${encodeURIComponent(name)}`, {
            method: "GET"
        });

        const guests = await response.json();
        const guest = guests.find((candidate) => candidate.id === guestId);

        if (guest) {
            renderSeatingMap(guest);
        } else {
            // The guest is gone (e.g. a stale or shared link) - fall back to the plain search results.
            renderGuests(guests);
        }
    } catch (error) {
        console.error(error);
    }
}

// Fetch the seating-map SVG once and cache it (same-origin, so it rides the existing session cookie).
async function loadSeatingMap() {
    if (seatingMapMarkup === null) {
        const response = await fetch("/images/seating-map.svg");
        seatingMapMarkup = await response.text();
    }

    return seatingMapMarkup;
}

// Show the whole room, spotlighting the guest's table and chair. Every chair carries an id of the
// form `table-{tableNumber}-seat-{seatNumber}`, so the guest's own seat is found by composing that id.
async function renderSeatingMap(guest) {
    if (isAlreadyRendered(`map:${guest.id}`)) {
        return;
    }

    resultsContainer.classList.add("map-view");

    const markup = await loadSeatingMap();

    // The name/seat caption is the accessible source of truth (and the fallback if the seat isn't on the map).
    const caption = document.createElement("div");
    caption.className = "seating-map-caption";

    const name = document.createElement("h2");
    name.className = "seating-map-name";
    name.textContent = guest.name;

    const seat = document.createElement("p");
    seat.className = "seating-map-seat";
    seat.textContent = `Table ${guest.tableNumber}, Seat ${guest.seatNumber}`;

    caption.append(name, seat);

    const map = document.createElement("div");
    map.className = "seating-map";
    map.innerHTML = markup;

    const svg = map.querySelector("svg");
    // Drop the intrinsic size so CSS controls how the map scales (and rotates on phones).
    svg.removeAttribute("width");
    svg.removeAttribute("height");
    svg.setAttribute("aria-hidden", "true");

    const chair = svg.querySelector(`#${seatElementId(guest.tableNumber, guest.seatNumber)}`);
    if (chair) {
        svg.classList.add("is-focused");
        chair.classList.add("seat-highlight");
        chair.closest("g")?.classList.add("table-highlight");
    }

    resultsContainer.replaceChildren(caption, map);

    // Scroll the highlighted seat into view: on a phone the (rotated) map is taller than the
    // screen, so the seat can start off-screen. Wait a frame so the map has been laid out first.
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

    searchInput.addEventListener("input", () => {
        clearTimeout(timeoutId);

        timeoutId = setTimeout(() => {
            commitSearch(searchInput.value);
        }, 200);
    });

    // Pressing Enter would otherwise submit the form and reload the page; search in place instead.
    searchForm.addEventListener("submit", (event) => {
        event.preventDefault();
        clearTimeout(timeoutId);
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
