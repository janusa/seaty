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
       ('Charlotte'),
       ('Chase'),
       ('Chad'),
       ('Chantelle'),
       ('Chanel'),
       ('Charlie'),
       ('Chandler'),
       ('Charlene');

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
       (5, 2),
       (6, 2),
       (7, 3),
       (8, 3),
       (9, 4),
       (10, 4),
       (11, 4),
       (12, 4),
       (13, 5),
       (14, 5),
       (15, 5);

INSERT INTO seating_assignment(guest_id, seat_id)
VALUES (1, 3),
       (2, 1),
       (3, 2),
       (4, 8),
       (5, 5),
       (6, 6),
       (7, 7),
       (8, 4),
       (9, 14),
       (10, 10),
       (11, 15),
       (12, 9),
       (13, 11),
       (14, 13),
       (15, 12);
