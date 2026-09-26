package com.finder.demo.projects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
public class ProjectRepository {
    private final JdbcTemplate jdbc;
    private final Path documentationRoot;
    private final RowMapper<Project> mapper = (rs, rowNum) -> new Project(
            rs.getLong("id"), rs.getString("project_number"), rs.getString("name"),
            rs.getString("contact_person"), rs.getString("description"), rs.getString("status"),
            rs.getString("documentation_path"), rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());

    public ProjectRepository(JdbcTemplate jdbc,
                             @Value("${app.documentation-root:./documentation}") String documentationRoot) {
        this.jdbc = jdbc;
        this.documentationRoot = Path.of(documentationRoot).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void createExistingProjectFolders() {
        jdbc.queryForList("SELECT documentation_path FROM projects").forEach(row -> ensureFolder((String) row.get("documentation_path")));
    }

    public List<Project> findAll() {
        return jdbc.query("SELECT * FROM projects ORDER BY updated_at DESC", mapper);
    }

    public Project findById(long id) {
        return jdbc.queryForObject("SELECT * FROM projects WHERE id = ?", mapper, id);
    }

    @Transactional
    public Project create(ProjectRequest request, Long userId) {
        long id = jdbc.queryForObject("""
                INSERT INTO projects (project_number, name, contact_person, description, status, documentation_path)
                VALUES (?, ?, ?, ?, COALESCE(?, 'ACTIVE'), ?) RETURNING id
                """, Long.class, request.projectNumber(), request.name(), request.contactPerson(),
                request.description(), request.status(), folderPath(request.projectNumber()));
        Project project = findById(id);
        ensureFolder(project.documentationPath());
        recordActivity(id, userId, "CREATED", "Projet créé : " + project.projectNumber() + " · " + project.name());
        return project;
    }

    @Transactional
    public Project update(long id, ProjectRequest request, Long userId) {
        Project before = findById(id);
        jdbc.update("""
                UPDATE projects SET project_number = ?, name = ?, contact_person = ?, description = ?,
                status = COALESCE(?, status), documentation_path = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, request.projectNumber(), request.name(), request.contactPerson(), request.description(),
                request.status(), folderPath(request.projectNumber()), id);
        Project project = findById(id);
        ensureFolder(project.documentationPath());
        recordActivity(id, userId, "UPDATED", changedDetails(before, request));
        return project;
    }

    public List<Map<String, Object>> findActivity(long projectId) {
        return jdbc.queryForList("""
                SELECT pa.id, pa.action, pa.details, pa.created_at AS "createdAt",
                       au.id AS "actorUserId", au.display_name AS "actorName", au.email AS "actorEmail"
                FROM project_activity pa
                LEFT JOIN app_users au ON au.id = pa.user_id
                WHERE pa.project_id = ?
                ORDER BY pa.created_at DESC, pa.id DESC
                """, projectId);
    }

    public void delete(long id) { jdbc.update("DELETE FROM projects WHERE id = ?", id); }

    private void recordActivity(long projectId, Long userId, String action, String details) {
        jdbc.update("""
                INSERT INTO project_activity (project_id, user_id, action, details)
                VALUES (?, ?, ?, ?)
                """, projectId, resolveUserId(userId), action, details);
    }

    private Long resolveUserId(Long userId) {
        if (userId != null) {
            List<Long> activeUser = jdbc.query("SELECT id FROM app_users WHERE id = ? AND active = TRUE",
                    (rs, rowNum) -> rs.getLong("id"), userId);
            if (!activeUser.isEmpty()) return activeUser.get(0);
        }
        return jdbc.queryForObject("SELECT id FROM app_users WHERE active = TRUE ORDER BY id LIMIT 1", Long.class);
    }

    private String changedDetails(Project before, ProjectRequest request) {
        List<String> fields = new ArrayList<>();
        if (!Objects.equals(before.projectNumber(), request.projectNumber())) fields.add("numéro du projet");
        if (!Objects.equals(before.name(), request.name())) fields.add("nom");
        if (!Objects.equals(before.contactPerson(), request.contactPerson())) fields.add("personne à contacter");
        if (!Objects.equals(before.description(), request.description())) fields.add("description");
        if (request.status() != null && !Objects.equals(before.status(), request.status())) fields.add("statut");
        return fields.isEmpty() ? "Informations du projet enregistrées." :
                "Champs modifiés : " + fields.stream().collect(Collectors.joining(", ")) + ".";
    }

    private String folderPath(String projectNumber) {
        String safeNumber = projectNumber.trim().replaceAll("[^A-Za-z0-9_-]+", "-");
        return "projets/" + (safeNumber.isBlank() ? "project" : safeNumber);
    }

    private void ensureFolder(String relativePath) {
        try {
            Path folder = documentationRoot.resolve(relativePath).normalize();
            if (!folder.startsWith(documentationRoot)) {
                throw new IllegalStateException("Dossier de projet invalide");
            }
            Files.createDirectories(folder);
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de créer le dossier du projet", exception);
        }
    }
}
