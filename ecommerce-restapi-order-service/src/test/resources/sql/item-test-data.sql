INSERT INTO items (id, name, price, archived, created_at, updated_at)
VALUES (100, 'Keyboard', 10.00, false, TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-01 10:00:00'),
       (101, 'Mouse', 15.00, false, TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-01 10:00:00'),
       (102, 'Monitor', 30.00, false, TIMESTAMP '2026-01-01 11:00:00', TIMESTAMP '2026-01-01 11:00:00'),
       (103, 'Archived', 99.00, true, TIMESTAMP '2026-01-01 12:00:00', TIMESTAMP '2026-01-01 12:00:00');

SELECT setval('items_id_seq', 103, true);
