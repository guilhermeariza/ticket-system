-- auth-service database schema

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    CONSTRAINT check_username_length CHECK (char_length(username) >= 3)
);

-- Create index on username for faster lookups
CREATE INDEX idx_users_username ON users(username);
