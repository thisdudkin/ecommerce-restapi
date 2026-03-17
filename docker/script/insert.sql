BEGIN;

CREATE TEMP TABLE tmp_first_names
(
    id      INT PRIMARY KEY,
    ru_name VARCHAR(100) NOT NULL,
    en_name VARCHAR(100) NOT NULL
) ON COMMIT DROP;

CREATE TEMP TABLE tmp_last_names
(
    id         INT PRIMARY KEY,
    ru_surname VARCHAR(100) NOT NULL,
    en_surname VARCHAR(100) NOT NULL
) ON COMMIT DROP;

CREATE TEMP TABLE tmp_email_domains
(
    id     INT PRIMARY KEY,
    domain VARCHAR(50) NOT NULL
) ON COMMIT DROP;

INSERT INTO tmp_first_names (id, ru_name, en_name)
VALUES (1, 'Александр', 'alexander'),
       (2, 'Алексей', 'alexey'),
       (3, 'Андрей', 'andrey'),
       (4, 'Антон', 'anton'),
       (5, 'Артём', 'artem'),
       (6, 'Борис', 'boris'),
       (7, 'Вадим', 'vadim'),
       (8, 'Виктор', 'viktor'),
       (9, 'Виталий', 'vitaliy'),
       (10, 'Владимир', 'vladimir'),
       (11, 'Даниил', 'daniil'),
       (12, 'Денис', 'denis'),
       (13, 'Дмитрий', 'dmitriy'),
       (14, 'Евгений', 'evgeniy'),
       (15, 'Егор', 'egor'),
       (16, 'Иван', 'ivan'),
       (17, 'Игорь', 'igor'),
       (18, 'Кирилл', 'kirill'),
       (19, 'Константин', 'konstantin'),
       (20, 'Максим', 'maksim'),
       (21, 'Матвей', 'matvey'),
       (22, 'Михаил', 'mihail'),
       (23, 'Никита', 'nikita'),
       (24, 'Николай', 'nikolay'),
       (25, 'Олег', 'oleg'),
       (26, 'Павел', 'pavel'),
       (27, 'Роман', 'roman'),
       (28, 'Сергей', 'sergey'),
       (29, 'Тимофей', 'timofey'),
       (30, 'Юрий', 'yuriy');

INSERT INTO tmp_last_names (id, ru_surname, en_surname)
VALUES (1, 'Иванов', 'ivanov'),
       (2, 'Петров', 'petrov'),
       (3, 'Сидоров', 'sidorov'),
       (4, 'Смирнов', 'smirnov'),
       (5, 'Кузнецов', 'kuznetsov'),
       (6, 'Попов', 'popov'),
       (7, 'Васильев', 'vasiliev'),
       (8, 'Соколов', 'sokolov'),
       (9, 'Михайлов', 'mikhailov'),
       (10, 'Новиков', 'novikov'),
       (11, 'Фёдоров', 'fedorov'),
       (12, 'Морозов', 'morozov'),
       (13, 'Волков', 'volkov'),
       (14, 'Алексеев', 'alekseev'),
       (15, 'Лебедев', 'lebedev'),
       (16, 'Семёнов', 'semenov'),
       (17, 'Егоров', 'egorov'),
       (18, 'Павлов', 'pavlov'),
       (19, 'Козлов', 'kozlov'),
       (20, 'Степанов', 'stepanov'),
       (21, 'Николаев', 'nikolaev'),
       (22, 'Орлов', 'orlov'),
       (23, 'Андреев', 'andreev'),
       (24, 'Макаров', 'makarov'),
       (25, 'Никитин', 'nikitin'),
       (26, 'Захаров', 'zakharov'),
       (27, 'Зайцев', 'zaytsev'),
       (28, 'Соловьёв', 'soloviev'),
       (29, 'Борисов', 'borisov'),
       (30, 'Яковлев', 'yakovlev'),
       (31, 'Григорьев', 'grigoriev'),
       (32, 'Романов', 'romanov'),
       (33, 'Воробьёв', 'vorobiev'),
       (34, 'Сергеев', 'sergeev'),
       (35, 'Кузьмин', 'kuzmin'),
       (36, 'Фролов', 'frolov'),
       (37, 'Александров', 'alexandrov'),
       (38, 'Дмитриев', 'dmitriev'),
       (39, 'Королёв', 'korolev'),
       (40, 'Гусев', 'gusev'),
       (41, 'Киселёв', 'kiselev'),
       (42, 'Ильин', 'ilin'),
       (43, 'Максимов', 'maksimov'),
       (44, 'Поляков', 'polyakov'),
       (45, 'Сорокин', 'sorokin'),
       (46, 'Виноградов', 'vinogradov'),
       (47, 'Белов', 'belov'),
       (48, 'Комаров', 'komarov'),
       (49, 'Богданов', 'bogdanov'),
       (50, 'Медведев', 'medvedev');

INSERT INTO tmp_email_domains (id, domain)
VALUES (1, '@yandex.by'),
       (2, '@yandex.ru'),
       (3, '@gmail.com'),
       (4, '@outlook.com'),
       (5, '@icloud.com');

CREATE TEMP TABLE tmp_inserted_users
(
    id      BIGINT PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    surname VARCHAR(100) NOT NULL
) ON COMMIT DROP;

WITH inserted_users AS (
    INSERT INTO users (
                       name,
                       surname,
                       birth_date,
                       email,
                       active,
                       created_at,
                       updated_at
        )
        SELECT fn.ru_name                                                    AS name,
               ln.ru_surname                                                 AS surname,
               (DATE '1950-01-01' + ((gs.i * 13) % 20454))::date             AS birth_date,
               LOWER(fn.en_name || '.' || ln.en_surname || gs.i || d.domain) AS email,
               (gs.i % 10 <> 0)                                              AS active,
               (TIMESTAMP '2018-01-01 00:00:00'
                   + (((gs.i * 97) % 220000000)::text || ' seconds')::interval
                   )                                                         AS created_at,
               (TIMESTAMP '2018-01-01 00:00:00'
                   + (((gs.i * 97) % 220000000)::text || ' seconds')::interval
                   + (((gs.i * 17) % 2592000)::text || ' seconds')::interval
                   )                                                         AS updated_at
        FROM generate_series(1, 350000) AS gs(i)
                 JOIN tmp_first_names fn ON fn.id = ((gs.i * 17) % 30) + 1
                 JOIN tmp_last_names ln ON ln.id = ((gs.i * 37) % 50) + 1
                 JOIN tmp_email_domains d ON d.id = ((gs.i * 13) % 5) + 1
        RETURNING id, name, surname)
INSERT
INTO tmp_inserted_users (id, name, surname)
SELECT id, name, surname
FROM inserted_users;

CREATE TEMP TABLE tmp_user_pool
(
    rn      INT PRIMARY KEY,
    user_id BIGINT       NOT NULL,
    holder  VARCHAR(200) NOT NULL
) ON COMMIT DROP;

INSERT INTO tmp_user_pool (rn, user_id, holder)
SELECT ROW_NUMBER() OVER (ORDER BY id) AS rn,
       id                              AS user_id,
       UPPER(name || ' ' || surname)   AS holder
FROM tmp_inserted_users;

INSERT INTO payments_cards (user_id,
                            number,
                            holder,
                            expiration_date,
                            active,
                            created_at,
                            updated_at)
SELECT up.user_id,
       SUBSTRING(card_num FROM 1 FOR 4) || '-' ||
       SUBSTRING(card_num FROM 5 FOR 4) || '-' ||
       SUBSTRING(card_num FROM 9 FOR 4) || '-' ||
       SUBSTRING(card_num FROM 13 FOR 4)               AS number,
       up.holder,
       (CURRENT_DATE + 1 + ((gs.i * 29) % 1825))::date AS expiration_date,
       (gs.i % 12 <> 0)                                AS active,
       (TIMESTAMP '2019-01-01 00:00:00'
           + (((gs.i * 83) % 189000000)::text || ' seconds')::interval
           )                                           AS created_at,
       (TIMESTAMP '2019-01-01 00:00:00'
           + (((gs.i * 83) % 189000000)::text || ' seconds')::interval
           + (((gs.i * 11) % 1209600)::text || ' seconds')::interval
           )                                           AS updated_at
FROM (SELECT i,
             LPAD((4000000000000000 + i)::text, 16, '0') AS card_num
      FROM generate_series(1, 650000) AS s(i)) AS gs
         JOIN tmp_user_pool up
              ON up.rn = ((gs.i - 1) % 350000) + 1;

COMMIT;
