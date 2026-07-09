-- foreign_keys is a per-connection pragma that defaults to OFF, so it must be enabled in *this*
-- session (separate from the one that ran schema.sql) for the inserts below to be FK-checked.
PRAGMA foreign_keys = ON;

INSERT INTO guest (name)
VALUES
       ('Ada'), -- guest id = 001
       ('Aaron'), -- guest id = 002
       ('Adam'), -- guest id = 003
       ('Alan'), -- guest id = 004
       ('Albert'), -- guest id = 005
       ('Ali'), -- guest id = 006
       ('Alice'), -- guest id = 007
       ('Amelia'), -- guest id = 008
       ('Barbara'), -- guest id = 009
       ('Benjamin'), -- guest id = 010
       ('Beth'), -- guest id = 011
       ('Blake'), -- guest id = 012
       ('Bob'), -- guest id = 013
       ('Bobby'), -- guest id = 014
       ('Brian'), -- guest id = 015
       ('Bruno'), -- guest id = 016
       ('Caleb'), -- guest id = 017
       ('Cameron'), -- guest id = 018
       ('Carla'), -- guest id = 019
       ('Chad'), -- guest id = 020
       ('Chandler'), -- guest id = 021
       ('Chanel'), -- guest id = 022
       ('Chantelle'), -- guest id = 023
       ('Charlene'), -- guest id = 024
       ('Charles'), -- guest id = 025
       ('Charlie'), -- guest id = 026
       ('Charlie'), -- guest id = 027
       ('Charlotte'), -- guest id = 028
       ('Chase'), -- guest id = 029
       ('Clara'), -- guest id = 030
       ('Colin'), -- guest id = 031
       ('Daniel'), -- guest id = 032
       ('David'), -- guest id = 033
       ('Dennis'), -- guest id = 034
       ('Diana'), -- guest id = 035
       ('Dylan'), -- guest id = 036
       ('Edward'), -- guest id = 037
       ('Eleanor'), -- guest id = 038
       ('Eli'), -- guest id = 039
       ('Emma'), -- guest id = 040
       ('Ethan'), -- guest id = 041
       ('Eve'), -- guest id = 042
       ('Faith'), -- guest id = 043
       ('Felix'), -- guest id = 044
       ('Fiona'), -- guest id = 045
       ('Frank'), -- guest id = 046
       ('Frederick'), -- guest id = 047
       ('Gabriel'), -- guest id = 048
       ('Gary'), -- guest id = 049
       ('George'), -- guest id = 050
       ('Grace'), -- guest id = 051
       ('Gregory'), -- guest id = 052
       ('Hannah'), -- guest id = 053
       ('Harold'), -- guest id = 054
       ('Hazel'), -- guest id = 055
       ('Henry'), -- guest id = 056
       ('Holly'), -- guest id = 057
       ('Ian'), -- guest id = 058
       ('Imogen'), -- guest id = 059
       ('Irene'), -- guest id = 060
       ('Isaac'), -- guest id = 061
       ('Ivy'), -- guest id = 062
       ('Jack'), -- guest id = 063
       ('James'), -- guest id = 064
       ('Jasmine'), -- guest id = 065
       ('Jonathan'), -- guest id = 066
       ('Julia'), -- guest id = 067
       ('Karen'), -- guest id = 068
       ('Katherine'), -- guest id = 069
       ('Keith'), -- guest id = 070
       ('Kevin'), -- guest id = 071
       ('Kyle'), -- guest id = 072
       ('Laura'), -- guest id = 073
       ('Leo'), -- guest id = 074
       ('Liam'), -- guest id = 075
       ('Lucas'), -- guest id = 076
       ('Lucy'), -- guest id = 077
       ('Marcus'), -- guest id = 078
       ('Maria'), -- guest id = 079
       ('Martin'), -- guest id = 080
       ('Mason'), -- guest id = 081
       ('Megan'), -- guest id = 082
       ('Nadia'), -- guest id = 083
       ('Nathan'), -- guest id = 084
       ('Neil'), -- guest id = 085
       ('Nicholas'), -- guest id = 086
       ('Nora'), -- guest id = 087
       ('Olive'), -- guest id = 088
       ('Oliver'), -- guest id = 089
       ('Olivia'), -- guest id = 090
       ('Oscar'), -- guest id = 091
       ('Owen'), -- guest id = 092
       ('Patricia'), -- guest id = 093
       ('Paul'), -- guest id = 094
       ('Penelope'), -- guest id = 095
       ('Peter'), -- guest id = 096
       ('Philip'), -- guest id = 097
       ('Quentin'), -- guest id = 098
       ('Quincy'), -- guest id = 099
       ('Quinn'), -- guest id = 100
       ('Quinton'), -- guest id = 101
       ('Rachel'), -- guest id = 102
       ('Ralph'), -- guest id = 103
       ('Rebecca'), -- guest id = 104
       ('Richard'), -- guest id = 105
       ('Robert'), -- guest id = 106
       ('Samuel'), -- guest id = 107
       ('Sandra'), -- guest id = 108
       ('Scott'), -- guest id = 109
       ('Sophia'), -- guest id = 110
       ('Stephen'), -- guest id = 111
       ('Tara'), -- guest id = 112
       ('Theodore'), -- guest id = 113
       ('Thomas'), -- guest id = 114
       ('Tobias'), -- guest id = 115
       ('Tracy'), -- guest id = 116
       ('Ulrich'), -- guest id = 117
       ('Ulysses'), -- guest id = 118
       ('Uma'), -- guest id = 119
       ('Uriah'), -- guest id = 120
       ('Ursula'), -- guest id = 121
       ('Valerie'), -- guest id = 122
       ('Vanessa'), -- guest id = 123
       ('Vaughn'), -- guest id = 124
       ('Vera'), -- guest id = 125
       ('Victor'), -- guest id = 126
       ('Walter'), -- guest id = 127
       ('Wanda'), -- guest id = 128
       ('Warren'), -- guest id = 129
       ('Wendy'), -- guest id = 130
       ('William'), -- guest id = 131
       ('Xander'), -- guest id = 132
       ('Xavier'), -- guest id = 133
       ('Xena'), -- guest id = 134
       ('Ximena'), -- guest id = 135
       ('Xiomara'), -- guest id = 136
       ('Yasmin'), -- guest id = 137
       ('Yolanda'), -- guest id = 138
       ('York'), -- guest id = 139
       ('Yusuf'), -- guest id = 140
       ('Yves'), -- guest id = 141
       ('Zachary'), -- guest id = 142
       ('Zara'), -- guest id = 143
       ('Zoe'), -- guest id = 144
       ('Zoltan'), -- guest id = 145
       ('Zuri'); -- guest id = 146

-- 18 tables, matching the seating map. Table 18 is the head table (shown as "Head Table" in the UI).
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
       (17),
       (18);

-- seat_number restarts at 1 for each table, matching the map's per-table chair ids
-- (table-{table_number}-seat-{seat_number}). Chair counts match the map: 8 for a small
-- round table, 10 for a large round table, 6 for the rectangular head table (one side only).
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
    -- table 2 (large round)
    (1, 2), -- seat id = 009
    (2, 2), -- seat id = 010
    (3, 2), -- seat id = 011
    (4, 2), -- seat id = 012
    (5, 2), -- seat id = 013
    (6, 2), -- seat id = 014
    (7, 2), -- seat id = 015
    (8, 2), -- seat id = 016
    (9, 2), -- seat id = 017
    (10, 2), -- seat id = 018
    -- table 3
    (1, 3), -- seat id = 019
    (2, 3), -- seat id = 020
    (3, 3), -- seat id = 021
    (4, 3), -- seat id = 022
    (5, 3), -- seat id = 023
    (6, 3), -- seat id = 024
    (7, 3), -- seat id = 025
    (8, 3), -- seat id = 026
    -- table 4
    (1, 4), -- seat id = 027
    (2, 4), -- seat id = 028
    (3, 4), -- seat id = 029
    (4, 4), -- seat id = 030
    (5, 4), -- seat id = 031
    (6, 4), -- seat id = 032
    (7, 4), -- seat id = 033
    (8, 4), -- seat id = 034
    -- table 5
    (1, 5), -- seat id = 035
    (2, 5), -- seat id = 036
    (3, 5), -- seat id = 037
    (4, 5), -- seat id = 038
    (5, 5), -- seat id = 039
    (6, 5), -- seat id = 040
    (7, 5), -- seat id = 041
    (8, 5), -- seat id = 042
    -- table 6
    (1, 6), -- seat id = 043
    (2, 6), -- seat id = 044
    (3, 6), -- seat id = 045
    (4, 6), -- seat id = 046
    (5, 6), -- seat id = 047
    (6, 6), -- seat id = 048
    (7, 6), -- seat id = 049
    (8, 6), -- seat id = 050
    -- table 7
    (1, 7), -- seat id = 051
    (2, 7), -- seat id = 052
    (3, 7), -- seat id = 053
    (4, 7), -- seat id = 054
    (5, 7), -- seat id = 055
    (6, 7), -- seat id = 056
    (7, 7), -- seat id = 057
    (8, 7), -- seat id = 058
    -- table 8
    (1, 8), -- seat id = 059
    (2, 8), -- seat id = 060
    (3, 8), -- seat id = 061
    (4, 8), -- seat id = 062
    (5, 8), -- seat id = 063
    (6, 8), -- seat id = 064
    (7, 8), -- seat id = 065
    (8, 8), -- seat id = 066
    -- table 9
    (1, 9), -- seat id = 067
    (2, 9), -- seat id = 068
    (3, 9), -- seat id = 069
    (4, 9), -- seat id = 070
    (5, 9), -- seat id = 071
    (6, 9), -- seat id = 072
    (7, 9), -- seat id = 073
    (8, 9), -- seat id = 074
    -- table 10
    (1, 10), -- seat id = 075
    (2, 10), -- seat id = 076
    (3, 10), -- seat id = 077
    (4, 10), -- seat id = 078
    (5, 10), -- seat id = 079
    (6, 10), -- seat id = 080
    (7, 10), -- seat id = 081
    (8, 10), -- seat id = 082
    -- table 11 (large round)
    (1, 11), -- seat id = 083
    (2, 11), -- seat id = 084
    (3, 11), -- seat id = 085
    (4, 11), -- seat id = 086
    (5, 11), -- seat id = 087
    (6, 11), -- seat id = 088
    (7, 11), -- seat id = 089
    (8, 11), -- seat id = 090
    (9, 11), -- seat id = 091
    (10, 11), -- seat id = 092
    -- table 12
    (1, 12), -- seat id = 093
    (2, 12), -- seat id = 094
    (3, 12), -- seat id = 095
    (4, 12), -- seat id = 096
    (5, 12), -- seat id = 097
    (6, 12), -- seat id = 098
    (7, 12), -- seat id = 099
    (8, 12), -- seat id = 100
    -- table 13
    (1, 13), -- seat id = 101
    (2, 13), -- seat id = 102
    (3, 13), -- seat id = 103
    (4, 13), -- seat id = 104
    (5, 13), -- seat id = 105
    (6, 13), -- seat id = 106
    (7, 13), -- seat id = 107
    (8, 13), -- seat id = 108
    -- table 14
    (1, 14), -- seat id = 109
    (2, 14), -- seat id = 110
    (3, 14), -- seat id = 111
    (4, 14), -- seat id = 112
    (5, 14), -- seat id = 113
    (6, 14), -- seat id = 114
    (7, 14), -- seat id = 115
    (8, 14), -- seat id = 116
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
    -- table 18 (rectangular head table)
    (1, 18), -- seat id = 141
    (2, 18), -- seat id = 142
    (3, 18), -- seat id = 143
    (4, 18), -- seat id = 144
    (5, 18), -- seat id = 145
    (6, 18); -- seat id = 146

-- Seat each guest in order: the k-th guest (by id) takes the k-th seat, walking the tables in order
-- and the seats within each table. Pairing by row position rather than transcribing 146 literal
-- (guest_id, seat_id) rows keeps the data correct even if the guest or seat rows above are
-- reordered or renumbered.
INSERT INTO seating_assignment (guest_id, seat_id)
SELECT g.id, s.id
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn FROM guest) g
JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY table_id, seat_number) AS rn FROM seat) s
    ON g.rn = s.rn;
