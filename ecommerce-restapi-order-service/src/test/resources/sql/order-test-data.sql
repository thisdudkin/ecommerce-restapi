INSERT INTO items (id, name, price, archived, created_at, updated_at)
VALUES (100, 'Keyboard', 10.00, false, TIMESTAMP '2026-01-01 09:00:00', TIMESTAMP '2026-01-01 09:00:00'),
       (101, 'Mouse', 15.00, false, TIMESTAMP '2026-01-01 09:05:00', TIMESTAMP '2026-01-01 09:05:00'),
       (102, 'Monitor', 30.00, false, TIMESTAMP '2026-01-01 09:10:00', TIMESTAMP '2026-01-01 09:10:00');

INSERT INTO orders (id, user_id, status, total_price, deleted, version, created_at, updated_at)
VALUES (200, 1, 'NEW', 10.00, false, 0, TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-01 10:00:00'),
       (201, 1, 'NEW', 0.00, false, 0, TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-01 10:00:00'),
       (202, 1, 'PAID', 0.00, false, 0, TIMESTAMP '2026-01-01 11:00:00', TIMESTAMP '2026-01-01 11:00:00'),
       (203, 1, 'CANCELLED', 0.00, true, 0, TIMESTAMP '2026-01-01 12:00:00', TIMESTAMP '2026-01-01 12:00:00'),
       (204, 2, 'NEW', 0.00, false, 0, TIMESTAMP '2026-01-01 10:30:00', TIMESTAMP '2026-01-01 10:30:00');

INSERT INTO order_items (id, order_id, item_id, quantity, created_at, updated_at)
VALUES (300, 200, 100, 1, TIMESTAMP '2026-01-01 10:00:00', TIMESTAMP '2026-01-01 10:00:00');

SELECT setval('items_id_seq', 102, true);
SELECT setval('orders_id_seq', 204, true);
SELECT setval('order_items_id_seq', 300, true);
