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
VALUES ('admin', 'password', 'admin@kanban.com');

INSERT INTO users (username, password, email)
VALUES
    ('john_dev', 'password', 'john@example.com'),
    ('sarah_pm', 'password', 'sarah@example.com'),
    ('mike_qa', 'password', 'mike@example.com'),
    ('lisa_design', 'password', 'lisa@example.com');

-- Note: These passwords would be:
-- john_dev: password123
-- sarah_pm: manager2023
-- mike_qa: testing456
-- lisa_design: design789

INSERT INTO tasks (title, description, status, priority, created_by, assigned_to, version)
VALUES
-- TO_DO tasks
('Setup project structure', 'Create initial directory structure and configuration files', 'TO_DO', 'HIGH', 1, 2, 0),
('Design database schema', 'Create ER diagram and define table relationships', 'TO_DO', 'HIGH', 1, 4, 0),
('Research API frameworks', 'Evaluate different API frameworks for the project', 'TO_DO', 'MED', 2, 2, 0),

-- IN_PROGRESS tasks
('Implement user authentication', 'Create login, registration, and token validation', 'IN_PROGRESS', 'HIGH', 1, 2, 0),
('Design UI mockups', 'Create initial mockups for the main dashboard', 'IN_PROGRESS', 'MED', 2, 4, 0),
('Write unit tests', 'Create test cases for core functionalities', 'IN_PROGRESS', 'MED', 3, 3, 0),

-- DONE tasks
('Project kickoff meeting', 'Initial team meeting to discuss project scope', 'DONE', 'HIGH', 2, null, 0),
('Requirements gathering', 'Document functional and non-functional requirements', 'DONE', 'HIGH', 2, 2, 1),
('Setup CI/CD pipeline', 'Configure Jenkins for continuous integration', 'DONE', 'MED', 2, 3, 2);