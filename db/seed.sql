-- foreign_keys is a per-connection pragma that defaults to OFF, so it must be enabled in *this*
-- session (separate from the one that ran schema.sql) for the inserts below to be FK-checked.
PRAGMA foreign_keys = ON;

INSERT INTO guest (name)
VALUES ('Alice'), -- guest id = 001
       ('Bob'), -- guest id = 002
       ('Eve'), -- guest id = 003
       ('Charlie'), -- guest id = 004
       ('Bobby'), -- guest id = 005
       ('Ali'), -- guest id = 006
       ('Charles'), -- guest id = 007
       ('Charlotte'), -- guest id = 008
       ('Chase'), -- guest id = 009
       ('Chad'), -- guest id = 010
       ('Chantelle'), -- guest id = 011
       ('Chanel'), -- guest id = 012
       ('Charlie'), -- guest id = 013
       ('Chandler'), -- guest id = 014
       ('Charlene'), -- guest id = 015
       ('Aaron'), -- guest id = 016
       ('Adam'), -- guest id = 017
       ('Alan'), -- guest id = 018
       ('Albert'), -- guest id = 019
       ('Amelia'), -- guest id = 020
       ('Barbara'), -- guest id = 021
       ('Benjamin'), -- guest id = 022
       ('Beth'), -- guest id = 023
       ('Blake'), -- guest id = 024
       ('Brian'), -- guest id = 025
       ('Caleb'), -- guest id = 026
       ('Cameron'), -- guest id = 027
       ('Carla'), -- guest id = 028
       ('Clara'), -- guest id = 029
       ('Colin'), -- guest id = 030
       ('Daniel'), -- guest id = 031
       ('David'), -- guest id = 032
       ('Dennis'), -- guest id = 033
       ('Diana'), -- guest id = 034
       ('Dylan'), -- guest id = 035
       ('Edward'), -- guest id = 036
       ('Eleanor'), -- guest id = 037
       ('Eli'), -- guest id = 038
       ('Emma'), -- guest id = 039
       ('Ethan'), -- guest id = 040
       ('Faith'), -- guest id = 041
       ('Felix'), -- guest id = 042
       ('Fiona'), -- guest id = 043
       ('Frank'), -- guest id = 044
       ('Frederick'), -- guest id = 045
       ('Gabriel'), -- guest id = 046
       ('Gary'), -- guest id = 047
       ('George'), -- guest id = 048
       ('Grace'), -- guest id = 049
       ('Gregory'), -- guest id = 050
       ('Hannah'), -- guest id = 051
       ('Harold'), -- guest id = 052
       ('Hazel'), -- guest id = 053
       ('Henry'), -- guest id = 054
       ('Holly'), -- guest id = 055
       ('Ian'), -- guest id = 056
       ('Imogen'), -- guest id = 057
       ('Irene'), -- guest id = 058
       ('Isaac'), -- guest id = 059
       ('Ivy'), -- guest id = 060
       ('Jack'), -- guest id = 061
       ('James'), -- guest id = 062
       ('Jasmine'), -- guest id = 063
       ('Jonathan'), -- guest id = 064
       ('Julia'), -- guest id = 065
       ('Karen'), -- guest id = 066
       ('Katherine'), -- guest id = 067
       ('Keith'), -- guest id = 068
       ('Kevin'), -- guest id = 069
       ('Kyle'), -- guest id = 070
       ('Laura'), -- guest id = 071
       ('Leo'), -- guest id = 072
       ('Liam'), -- guest id = 073
       ('Lucas'), -- guest id = 074
       ('Lucy'), -- guest id = 075
       ('Marcus'), -- guest id = 076
       ('Maria'), -- guest id = 077
       ('Martin'), -- guest id = 078
       ('Mason'), -- guest id = 079
       ('Megan'), -- guest id = 080
       ('Nadia'), -- guest id = 081
       ('Nathan'), -- guest id = 082
       ('Neil'), -- guest id = 083
       ('Nicholas'), -- guest id = 084
       ('Nora'), -- guest id = 085
       ('Olive'), -- guest id = 086
       ('Oliver'), -- guest id = 087
       ('Olivia'), -- guest id = 088
       ('Oscar'), -- guest id = 089
       ('Owen'), -- guest id = 090
       ('Patricia'), -- guest id = 091
       ('Paul'), -- guest id = 092
       ('Penelope'), -- guest id = 093
       ('Peter'), -- guest id = 094
       ('Philip'), -- guest id = 095
       ('Quentin'), -- guest id = 096
       ('Quincy'), -- guest id = 097
       ('Quinn'), -- guest id = 098
       ('Quinton'), -- guest id = 099
       ('Rachel'), -- guest id = 100
       ('Ralph'), -- guest id = 101
       ('Rebecca'), -- guest id = 102
       ('Richard'), -- guest id = 103
       ('Robert'), -- guest id = 104
       ('Samuel'), -- guest id = 105
       ('Sandra'), -- guest id = 106
       ('Scott'), -- guest id = 107
       ('Sophia'), -- guest id = 108
       ('Stephen'), -- guest id = 109
       ('Tara'), -- guest id = 110
       ('Theodore'), -- guest id = 111
       ('Thomas'), -- guest id = 112
       ('Tobias'), -- guest id = 113
       ('Tracy'), -- guest id = 114
       ('Ulrich'), -- guest id = 115
       ('Ulysses'), -- guest id = 116
       ('Uma'), -- guest id = 117
       ('Uriah'), -- guest id = 118
       ('Ursula'), -- guest id = 119
       ('Valerie'), -- guest id = 120
       ('Vanessa'), -- guest id = 121
       ('Vaughn'), -- guest id = 122
       ('Vera'), -- guest id = 123
       ('Victor'), -- guest id = 124
       ('Walter'), -- guest id = 125
       ('Wanda'), -- guest id = 126
       ('Warren'), -- guest id = 127
       ('Wendy'), -- guest id = 128
       ('William'), -- guest id = 129
       ('Xander'), -- guest id = 130
       ('Xavier'), -- guest id = 131
       ('Xena'), -- guest id = 132
       ('Ximena'), -- guest id = 133
       ('Xiomara'), -- guest id = 134
       ('Yasmin'), -- guest id = 135
       ('Yolanda'), -- guest id = 136
       ('York'), -- guest id = 137
       ('Yusuf'), -- guest id = 138
       ('Yves'), -- guest id = 139
       ('Zachary'), -- guest id = 140
       ('Zara'), -- guest id = 141
       ('Zoe'), -- guest id = 142
       ('Zoltan'), -- guest id = 143
       ('Zuri'); -- guest id = 144

-- 17 tables, matching the seating map.
INSERT INTO dining_table (table_number)
VALUES (1),
       (2),
       (3),
       (4),
       (5),
       (6),
       (7),
       (8),
       (9),
       (10),
       (11),
       (12),
       (13),
       (14),
       (15),
       (16),
       (17);

-- seat_number restarts at 1 for each table, matching the map's per-table chair ids
-- (table-{table_number}-seat-{seat_number}). Chair counts match the map: 8 for a small
-- round table, 10 for a large round table, 12 for the rectangular head table.
INSERT INTO seat(seat_number, table_id)
VALUES
    -- table 1
    (1, 1), -- seat id = 001
    (2, 1), -- seat id = 002
    (3, 1), -- seat id = 003
    (4, 1), -- seat id = 004
    (5, 1), -- seat id = 005
    (6, 1), -- seat id = 006
    (7, 1), -- seat id = 007
    (8, 1), -- seat id = 008
    -- table 2
    (1, 2), -- seat id = 009
    (2, 2), -- seat id = 010
    (3, 2), -- seat id = 011
    (4, 2), -- seat id = 012
    (5, 2), -- seat id = 013
    (6, 2), -- seat id = 014
    (7, 2), -- seat id = 015
    (8, 2), -- seat id = 016
    -- table 3
    (1, 3), -- seat id = 017
    (2, 3), -- seat id = 018
    (3, 3), -- seat id = 019
    (4, 3), -- seat id = 020
    (5, 3), -- seat id = 021
    (6, 3), -- seat id = 022
    (7, 3), -- seat id = 023
    (8, 3), -- seat id = 024
    -- table 4
    (1, 4), -- seat id = 025
    (2, 4), -- seat id = 026
    (3, 4), -- seat id = 027
    (4, 4), -- seat id = 028
    (5, 4), -- seat id = 029
    (6, 4), -- seat id = 030
    (7, 4), -- seat id = 031
    (8, 4), -- seat id = 032
    -- table 5
    (1, 5), -- seat id = 033
    (2, 5), -- seat id = 034
    (3, 5), -- seat id = 035
    (4, 5), -- seat id = 036
    (5, 5), -- seat id = 037
    (6, 5), -- seat id = 038
    (7, 5), -- seat id = 039
    (8, 5), -- seat id = 040
    -- table 6
    (1, 6), -- seat id = 041
    (2, 6), -- seat id = 042
    (3, 6), -- seat id = 043
    (4, 6), -- seat id = 044
    (5, 6), -- seat id = 045
    (6, 6), -- seat id = 046
    (7, 6), -- seat id = 047
    (8, 6), -- seat id = 048
    -- table 7
    (1, 7), -- seat id = 049
    (2, 7), -- seat id = 050
    (3, 7), -- seat id = 051
    (4, 7), -- seat id = 052
    (5, 7), -- seat id = 053
    (6, 7), -- seat id = 054
    (7, 7), -- seat id = 055
    (8, 7), -- seat id = 056
    -- table 8
    (1, 8), -- seat id = 057
    (2, 8), -- seat id = 058
    (3, 8), -- seat id = 059
    (4, 8), -- seat id = 060
    (5, 8), -- seat id = 061
    (6, 8), -- seat id = 062
    (7, 8), -- seat id = 063
    (8, 8), -- seat id = 064
    -- table 9
    (1, 9), -- seat id = 065
    (2, 9), -- seat id = 066
    (3, 9), -- seat id = 067
    (4, 9), -- seat id = 068
    (5, 9), -- seat id = 069
    (6, 9), -- seat id = 070
    (7, 9), -- seat id = 071
    (8, 9), -- seat id = 072
    -- table 10
    (1, 10), -- seat id = 073
    (2, 10), -- seat id = 074
    (3, 10), -- seat id = 075
    (4, 10), -- seat id = 076
    (5, 10), -- seat id = 077
    (6, 10), -- seat id = 078
    (7, 10), -- seat id = 079
    (8, 10), -- seat id = 080
    (9, 10), -- seat id = 081
    (10, 10), -- seat id = 082
    -- table 11
    (1, 11), -- seat id = 083
    (2, 11), -- seat id = 084
    (3, 11), -- seat id = 085
    (4, 11), -- seat id = 086
    (5, 11), -- seat id = 087
    (6, 11), -- seat id = 088
    (7, 11), -- seat id = 089
    (8, 11), -- seat id = 090
    -- table 12
    (1, 12), -- seat id = 091
    (2, 12), -- seat id = 092
    (3, 12), -- seat id = 093
    (4, 12), -- seat id = 094
    (5, 12), -- seat id = 095
    (6, 12), -- seat id = 096
    (7, 12), -- seat id = 097
    (8, 12), -- seat id = 098
    -- table 13
    (1, 13), -- seat id = 099
    (2, 13), -- seat id = 100
    (3, 13), -- seat id = 101
    (4, 13), -- seat id = 102
    (5, 13), -- seat id = 103
    (6, 13), -- seat id = 104
    (7, 13), -- seat id = 105
    (8, 13), -- seat id = 106
    -- table 14
    (1, 14), -- seat id = 107
    (2, 14), -- seat id = 108
    (3, 14), -- seat id = 109
    (4, 14), -- seat id = 110
    (5, 14), -- seat id = 111
    (6, 14), -- seat id = 112
    (7, 14), -- seat id = 113
    (8, 14), -- seat id = 114
    (9, 14), -- seat id = 115
    (10, 14), -- seat id = 116
    -- table 15
    (1, 15), -- seat id = 117
    (2, 15), -- seat id = 118
    (3, 15), -- seat id = 119
    (4, 15), -- seat id = 120
    (5, 15), -- seat id = 121
    (6, 15), -- seat id = 122
    (7, 15), -- seat id = 123
    (8, 15), -- seat id = 124
    -- table 16
    (1, 16), -- seat id = 125
    (2, 16), -- seat id = 126
    (3, 16), -- seat id = 127
    (4, 16), -- seat id = 128
    (5, 16), -- seat id = 129
    (6, 16), -- seat id = 130
    (7, 16), -- seat id = 131
    (8, 16), -- seat id = 132
    -- table 17
    (1, 17), -- seat id = 133
    (2, 17), -- seat id = 134
    (3, 17), -- seat id = 135
    (4, 17), -- seat id = 136
    (5, 17), -- seat id = 137
    (6, 17), -- seat id = 138
    (7, 17), -- seat id = 139
    (8, 17), -- seat id = 140
    (9, 17), -- seat id = 141
    (10, 17), -- seat id = 142
    (11, 17), -- seat id = 143
    (12, 17); -- seat id = 144

-- Seat each guest in order: the k-th guest (by id) takes the k-th seat, walking the tables in order
-- and the seats within each table. Pairing by row position rather than transcribing 144 literal
-- (guest_id, seat_id) rows keeps the data correct even if the guest or seat rows above are
-- reordered or renumbered.
INSERT INTO seating_assignment (guest_id, seat_id)
SELECT g.id, s.id
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn FROM guest) g
JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY table_id, seat_number) AS rn FROM seat) s
    ON g.rn = s.rn;
