const searchForm = document.querySelector("form[role='search']");
const searchInput = document.querySelector("#guest-search");
const resultsContainer = document.querySelector("#search-result-container");

let timeoutId = null;

const MIN_SEARCH_LENGTH = 3;
const MAX_SEARCH_LENGTH = 80;

searchInput.addEventListener("input", () => {
    clearTimeout(timeoutId);

    timeoutId = setTimeout(() => {
        searchGuests(searchInput.value);
    }, 200);
});

// Pressing Enter would otherwise submit the form and reload the page; search in place instead.
searchForm.addEventListener("submit", (event) => {
    event.preventDefault();
    clearTimeout(timeoutId);
    searchGuests(searchInput.value);
});

async function searchGuests(value) {
    const query = value.trim();

    if (query.length < MIN_SEARCH_LENGTH) {
        resultsContainer.innerHTML = "<p>Start typing to find your seat!</p>";
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

    function renderGuests(guests) {
        if (guests.length === 0) {
            resultsContainer.innerHTML = "<p>No guests found with this name.</p>";
            return;
        }

        const list = document.createElement("ul");
        list.className = "search-results-list";

        for (const guest of guests) {
            const item = document.createElement("li");
            item.className = "search-result";

            const name = document.createElement("h2");
            name.textContent = guest.name;

            const seat = document.createElement("p");
            seat.textContent = `Table ${guest.tableNumber}, seat ${guest.seatNumber}`;

            item.append(name, seat);
            list.append(item);
        }

        resultsContainer.replaceChildren(list);
    }
}
