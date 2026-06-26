-- foreign_keys is a per-connection pragma that defaults to OFF, so it must be enabled in *this*
-- session (separate from the one that ran schema.sql) for the inserts below to be FK-checked.
PRAGMA foreign_keys = ON;

INSERT INTO guest (name)
VALUES ('Alice'),
       ('Bob'),
       ('Eve'),
       ('Charlie'),
       ('Bobby'),
       ('Ali'),
       ('Charles'),
       ('Charlotte');

INSERT INTO dining_table (table_number)
VALUES (1),
       (2),
       (3),
       (4),
       (5);

INSERT INTO seat(seat_number, table_id)
VALUES (1, 1),
       (2, 1),
       (3, 2),
       (4, 2),
       (5, 5),
       (6, 2),
       (7, 5),
       (8, 2);

INSERT INTO seating_assignment(guest_id, seat_id)
VALUES (1, 3),
       (2, 1),
       (3, 2),
       (4, 8),
       (5, 5),
       (6, 6),
       (7, 7),
       (8, 4);
