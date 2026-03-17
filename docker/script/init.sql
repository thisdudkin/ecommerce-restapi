-- Users Database

CREATE ROLE user_service
  WITH LOGIN
  PASSWORD '?'
  NOSUPERUSER
  NOCREATEDB
  NOCREATEROLE
  NOINHERIT;

CREATE DATABASE users_db
  OWNER user_service;

GRANT CONNECT ON DATABASE users_db TO user_service;

\connect users_db

GRANT USAGE, CREATE ON SCHEMA public TO user_service;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO user_service;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT USAGE, SELECT ON SEQUENCES TO user_service;

-- Identity Database

CREATE ROLE authentication_service
    WITH LOGIN
    PASSWORD '?'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT;

CREATE DATABASE identity_db
    OWNER authentication_service;

GRANT CONNECT ON DATABASE identity_db TO authentication_service;

\connect identity_db

GRANT USAGE, CREATE ON SCHEMA public TO authentication_service;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO authentication_service;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO authentication_service;

-- Orders Database

CREATE ROLE order_service
    WITH LOGIN
    PASSWORD '?'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT;

CREATE DATABASE orders_db
    OWNER order_service;

GRANT CONNECT ON DATABASE orders_db TO order_service;

\connect orders_db

GRANT USAGE, CREATE ON SCHEMA public TO order_service;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO order_service;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO order_service;
