package com.finder.demo.projects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;

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

    public Project create(ProjectRequest request) {
        long id = jdbc.queryForObject("""
                INSERT INTO projects (project_number, name, contact_person, description, status, documentation_path)
                VALUES (?, ?, ?, ?, COALESCE(?, 'ACTIVE'), ?) RETURNING id
                """, Long.class, request.projectNumber(), request.name(), request.contactPerson(),
                request.description(), request.status(), folderPath(request.projectNumber()));
        Project project = findById(id);
        ensureFolder(project.documentationPath());
        return project;
    }

    public Project update(long id, ProjectRequest request) {
        jdbc.update("""
                UPDATE projects SET project_number = ?, name = ?, contact_person = ?, description = ?,
                status = COALESCE(?, status), documentation_path = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, request.projectNumber(), request.name(), request.contactPerson(), request.description(),
                request.status(), folderPath(request.projectNumber()), id);
        Project project = findById(id);
        ensureFolder(project.documentationPath());
        return project;
    }

    public void delete(long id) { jdbc.update("DELETE FROM projects WHERE id = ?", id); }

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
