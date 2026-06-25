INSERT INTO guest (name)
VALUES ('Alice'),
       ('Bob');

INSERT INTO dining_table (table_number, seat_count)
VALUES ('1', 6),
       ('2', 8),
       ('3', 6),
       ('4', 4),
       ('5', 6);

INSERT INTO seat(seat_number, table_id)
VALUES (1, 1),
       (2, 1),
       (1, 2);

INSERT INTO seating_assignment(guest_id, seat_id)
VALUES (1, 3),
       (2, 1),
       (3, 2)
