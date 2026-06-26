INSERT INTO guest (name)
VALUES ('Alice'),
       ('Bob'),
       ('Eve'),
       ('Charlie'),
       ('Bobby'),
       ('Ali'),
       ('Charles'),
       ('Charlotte');

INSERT INTO dining_table (table_number, seat_count)
VALUES ('1', 6),
       ('2', 8),
       ('3', 6),
       ('4', 4),
       ('5', 6);

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
