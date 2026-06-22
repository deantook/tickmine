ALTER TABLE users ADD COLUMN email VARCHAR(255);
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);

-- Legacy rows created before auth may have null email; new users require email.
CREATE UNIQUE INDEX idx_users_email ON users (email) WHERE email IS NOT NULL;
