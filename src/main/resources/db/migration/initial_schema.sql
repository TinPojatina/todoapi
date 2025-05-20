CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(100) NOT NULL,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TYPE task_status AS ENUM ('TO_DO', 'IN_PROGRESS', 'DONE');
CREATE TYPE task_priority AS ENUM ('LOW', 'MED', 'HIGH');

CREATE TABLE tasks (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(100) NOT NULL,
                       description TEXT,
                       status task_status NOT NULL DEFAULT 'TO_DO',
                       priority task_priority NOT NULL DEFAULT 'MED',
                       created_by BIGINT REFERENCES users(id),
                       assigned_to BIGINT REFERENCES users(id),
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO users (username, password, email)
VALUES ('admin', '$2a$10$KPxU0M.6JvpvEQQHevnJpOzdoRo5yfSF0iJrMTUwF0tMQpxgdnYJW', 'admin@kanban.com');
-- admin - password