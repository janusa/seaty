PRAGMA foreign_keys = ON;

CREATE TABLE guest
(
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL
);

CREATE TABLE dining_table
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    table_number INTEGER NOT NULL UNIQUE CHECK ( table_number > 0 ),
    seat_count   INTEGER NOT NULL CHECK ( seat_count > 0)
);

CREATE TABLE seat
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    seat_number INTEGER NOT NULL CHECK ( seat_number > 0 ),
    table_id    INTEGER NOT NULL REFERENCES dining_table (id) ON DELETE CASCADE,

    UNIQUE (table_id, seat_number)
);

CREATE TABLE seating_assignment
(
    guest_id INTEGER UNIQUE REFERENCES guest (id) ON DELETE CASCADE,
    seat_id  INTEGER UNIQUE REFERENCES seat (id) ON DELETE CASCADE
);
