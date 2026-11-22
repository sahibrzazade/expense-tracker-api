CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(254) NOT NULL UNIQUE,  -- stored trimmed and lower-case by the application
    name          VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,         -- Argon2id hash with its parameters, never the raw password
    currency      VARCHAR(3)   NOT NULL CHECK (currency IN ('AZN', 'USD')),
    created_at    TIMESTAMPTZ  NOT NULL
);
