const searchForm = document.querySelector("form[role='search']");
const searchInput = document.querySelector("#guest-search");
const resultsContainer = document.querySelector("#search-result-container");

let timeoutId = null;

// What's currently on screen, so an identical result doesn't re-render (and re-trigger the animations).
let renderedKey = null;

const MIN_SEARCH_LENGTH = 3;
const MAX_SEARCH_LENGTH = 80;

searchInput.addEventListener("input", () => {
    clearTimeout(timeoutId);
    persistQuery(searchInput.value);

    timeoutId = setTimeout(() => {
        searchGuests(searchInput.value);
    }, 200);
});

// Pressing Enter would otherwise submit the form and reload the page; search in place instead.
searchForm.addEventListener("submit", (event) => {
    event.preventDefault();
    clearTimeout(timeoutId);
    persistQuery(searchInput.value);
    searchGuests(searchInput.value);
});

// Restore the previous search after a reload by reading it back from the URL.
const initialQuery = new URLSearchParams(window.location.search).get("name");
if (initialQuery) {
    searchInput.value = initialQuery;
    searchGuests(initialQuery);
}

// Keep the current search in the URL so it survives a page reload (and can be shared).
function persistQuery(value) {
    const params = new URLSearchParams(window.location.search);

    if (value.trim().length === 0) {
        params.delete("name");
    } else {
        params.set("name", value);
    }

    const query = params.toString();
    window.history.replaceState(null, "", query ? `?${query}` : window.location.pathname);
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

    // A single match takes over the whole screen; several matches stay a list.
    if (guests.length === 1) {
        renderSingleGuest(guests[0]);
        return;
    }

    renderList(guests);
}

function renderList(guests) {
    if (isAlreadyRendered(`list:${guests.map((guest) => guest.id).join(",")}`)) {
        return;
    }

    resultsContainer.classList.remove("single-view");

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

// Picking one result fills the search bar with that name and blows the result up full-screen.
function selectGuest(guest) {
    searchInput.value = guest.name;
    persistQuery(guest.name);
    renderSingleGuest(guest);
}

function renderSingleGuest(guest) {
    if (isAlreadyRendered(`single:${guest.id}`)) {
        return;
    }

    resultsContainer.classList.add("single-view");

    const card = document.createElement("div");
    card.className = "single-guest";

    const flowers = [
        {src: "/images/orchid.png", cls: "single-guest-flower--top-left"},
        {src: "/images/magnolia.png", cls: "single-guest-flower--bottom-right"}
    ];

    for (const flower of flowers) {
        const img = document.createElement("img");
        img.className = `single-guest-flower ${flower.cls}`;
        img.src = flower.src;
        img.alt = "";
        img.setAttribute("aria-hidden", "true");
        card.append(img);
    }

    const content = document.createElement("div");
    content.className = "single-guest-content";

    const name = document.createElement("h2");
    name.className = "single-guest-name";
    name.textContent = guest.name;

    const seat = document.createElement("p");
    seat.className = "single-guest-seat";
    seat.textContent = `Table ${guest.tableNumber}, Seat ${guest.seatNumber}`;

    content.append(name, seat);
    card.append(content);

    resultsContainer.replaceChildren(card);
}

function showMessage(text) {
    if (isAlreadyRendered(`message:${text}`)) {
        return;
    }

    resultsContainer.classList.remove("single-view");

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
