CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    project_number VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    contact_person VARCHAR(160),
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE test_quotes (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE test_steps (
    id BIGSERIAL PRIMARY KEY,
    quote_id BIGINT NOT NULL REFERENCES test_quotes(id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    sub_step INTEGER NOT NULL DEFAULT 1,
    start_condition VARCHAR(240),
    pin_to_test VARCHAR(80),
    measurement_type VARCHAR(40) NOT NULL DEFAULT 'VOLTAGE',
    unit VARCHAR(20) NOT NULL DEFAULT 'V',
    expected_min NUMERIC(12, 4),
    expected_max NUMERIC(12, 4),
    relay VARCHAR(80),
    voltage NUMERIC(12, 4),
    notes TEXT
);

CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    email VARCHAR(200) NOT NULL UNIQUE,
    role VARCHAR(40) NOT NULL DEFAULT 'ENGINEER',
    extra_permissions TEXT[] NOT NULL DEFAULT '{}',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO projects (project_number, name, contact_person, description, status)
VALUES
    ('MF-2401', 'Banc de test ECU', 'Sophie Tremblay', 'Validation du contrôleur moteur et automatisation des scénarios.', 'ACTIVE'),
    ('MF-2398', 'Module de puissance', 'Marc Gagnon', 'Documentation et tests de caractérisation du module.', 'ACTIVE'),
    ('MF-2387', 'Interface capteurs', 'Nadia Roy', 'Suite de tests pour les entrées analogiques.', 'ARCHIVED');

INSERT INTO test_quotes (project_id, name, description, status)
SELECT id, 'Validation des entrées analogiques', 'Scénario nominal 12 V avec vérification des seuils.', 'IN_PROGRESS'
FROM projects WHERE project_number = 'MF-2401';

INSERT INTO test_steps (quote_id, step_number, sub_step, start_condition, pin_to_test, measurement_type, unit, expected_min, expected_max, relay, voltage)
SELECT tq.id, 3, 1, '5 V sur pin 1', 'Pin 2', 'VOLTAGE', 'V', 3, 4, 'Relais 1', 12
FROM test_quotes tq WHERE tq.name = 'Validation des entrées analogiques';

INSERT INTO app_users (display_name, email, role, extra_permissions)
VALUES ('Alex Martin', 'alex.martin@megafinder.local', 'ADMIN', ARRAY['GENERATE_CPP', 'MANAGE_USERS']),
       ('Sophie Tremblay', 'sophie.tremblay@megafinder.local', 'ENGINEER', ARRAY['GENERATE_CPP']);
