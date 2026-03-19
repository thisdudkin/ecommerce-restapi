BEGIN;

TRUNCATE TABLE users RESTART IDENTITY CASCADE;

INSERT INTO users (name, surname, birth_date, email, created_at, updated_at) VALUES
    ('Alexander', 'Dudkin', '2006-09-01', 'raddanprofile@gmail.com', now(), now()),
    ('Ilya', 'Inchakov', '2005-06-17', 'ilyainchakov@gmail.com', now(), now()),
    ('Ksenia', 'Nechay-Nicevich', '2006-02-09', 'ksenianechai@yandex.by', now(), now()),
    ('Irina', 'Kolesneva', '2006-04-19', 'irinakolira@mail.ru', now(), now()),
    ('Eugene', 'Nabok', '2005-07-04', 'nabokeugene@mail.ru', now(), now());

INSERT INTO payments_cards (user_id, number, holder, expiration_date, created_at, updated_at) VALUES
    ((SELECT id FROM users WHERE surname = 'Dudkin'), '4112-8453-7741-9021', 'ALEXANDER DUDKIN', '2028-10-10', now(), now()),
    ((SELECT id FROM users WHERE surname = 'Dudkin'), '5314-6621-1943-7752', 'ALEXANDER DUDKIN', '2027-06-01', now(), now()),
    ((SELECT id FROM users WHERE surname = 'Dudkin'), '2202-1445-8893-0021', 'ALEXANDER DUDKIN', '2029-03-15', now(), now()),

    ((SELECT id FROM users WHERE surname = 'Inchakov'), '4556-9012-7743-5521', 'ILYA INCHAKOV', '2028-08-20', now(), now()),

    ((SELECT id FROM users WHERE surname = 'Nechay-Nicevich'), '2200-5561-8821-3345', 'KSENIA NECHAY', '2027-11-01', now(), now()),
    ((SELECT id FROM users WHERE surname = 'Nechay-Nicevich'), '5214-9032-6674-1200', 'KSENIA NECHAY', '2029-04-12', now(), now()),
    ((SELECT id FROM users WHERE surname = 'Nechay-Nicevich'), '4123-8821-7740-6622', 'KSENIA NECHAY', '2028-01-25', now(), now()),
    ((SELECT id FROM users WHERE surname = 'Nechay-Nicevich'), '2204-7712-9941-5528', 'KSENIA NECHAY', '2030-07-30', now(), now()),

    ((SELECT id FROM users WHERE surname = 'Kolesneva'), '5488-2109-6642-7731', 'IRINA KOLESNEVA', '2027-09-09', now(), now()),
    ((SELECT id FROM users WHERE surname = 'Kolesneva'), '2201-8842-5531-9900', 'IRINA KOLESNEVA', '2028-12-01', now(), now()),

    ((SELECT id FROM users WHERE surname = 'Nabok'), '4012-9931-7754-6641', 'EUGENE NABOK', '2027-05-14', now(), now()),
    ((SELECT id FROM users WHERE surname = 'Nabok'), '5521-7734-9920-6612', 'EUGENE NABOK', '2029-06-18', now(), now()),
    ((SELECT id FROM users WHERE surname = 'Nabok'), '2203-4412-8899-1204', 'EUGENE NABOK', '2028-02-02', now(), now()),
    ((SELECT id FROM users WHERE surname = 'Nabok'), '2221-9033-6611-8420', 'EUGENE NABOK', '2030-10-10', now(), now()),
    ((SELECT id FROM users WHERE surname = 'Nabok'), '4539-7741-0021-6648', 'EUGENE NABOK', '2027-03-03', now(), now());

COMMIT;
