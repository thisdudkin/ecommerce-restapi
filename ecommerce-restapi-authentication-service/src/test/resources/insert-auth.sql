BEGIN;

TRUNCATE TABLE registration_compensation_outbox RESTART IDENTITY CASCADE;
TRUNCATE TABLE refresh_tokens RESTART IDENTITY CASCADE;
TRUNCATE TABLE user_credentials RESTART IDENTITY CASCADE;

INSERT INTO user_credentials (user_id,
                              login,
                              password_hash,
                              role,
                              active,
                              created_at,
                              updated_at)
VALUES (101,
        'alex.user',
        '$2y$10$Atyh0b3T0B5viF70aV8xsOeVSM3rtQEdGPVG4HGM7r.eHitRlF1h2',
        'USER',
        true,
        now(),
        now()),
       (102,
        'inactive.user',
        '$2y$10$PuW1yWUtwenTwexTiedtn.j.1H6L.IPSy/qLmWoHykh2nG4aC2jQS',
        'USER',
        false,
        now(),
        now()),
       (103,
        'admin.user',
        '$2y$10$eb5XvkSOIDXj5nG6xcUMLO8AzN/kMSWMtOtNwYqjma736wfnufjUu',
        'ADMIN',
        true,
        now(),
        now());

COMMIT;
