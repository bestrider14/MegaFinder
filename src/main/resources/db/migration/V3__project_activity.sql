CREATE TABLE project_activity (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    action VARCHAR(40) NOT NULL,
    details TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_activity_project_created
    ON project_activity(project_id, created_at DESC);

INSERT INTO project_activity (project_id, user_id, action, details, created_at)
SELECT p.id, u.id, 'CREATED', 'Projet créé lors de l’initialisation de la maquette.', p.updated_at
FROM projects p
LEFT JOIN app_users u ON u.email = 'alex.martin@megafinder.local';
