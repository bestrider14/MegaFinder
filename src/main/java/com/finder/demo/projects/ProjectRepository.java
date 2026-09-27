package com.finder.demo.projects;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
            rs.getTimestamp("updated_at").toLocalDateTime(), rs.getString("presentation"),
            rs.getString("assembly_elements"), rs.getString("assembly_steps"),
            lines(rs.getString("presentation_images")), lines(rs.getString("enclosure_images")),
            rs.getString("enclosure_pinout"), lines(rs.getString("pcb_images")),
            rs.getString("pcb_specifications"), List.of(), List.of());

    public ProjectRepository(JdbcTemplate jdbc,
                             @Value("${app.documentation-root:./documentation}") String documentationRoot) {
        this.jdbc = jdbc;
        this.documentationRoot = Path.of(documentationRoot).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void createExistingProjectFolders() {
        jdbc.queryForList("SELECT documentation_path FROM projects")
                .forEach(row -> ensureFolder((String) row.get("documentation_path")));
    }

    public List<Project> findAll() {
        return jdbc.query("SELECT * FROM projects ORDER BY updated_at DESC", mapper);
    }

    public Project findById(long id) {
        return withDetails(jdbc.queryForObject("SELECT * FROM projects WHERE id = ?", mapper, id));
    }

    @Transactional
    public Project create(ProjectRequest request, Long userId) {
        long id = jdbc.queryForObject("""
                INSERT INTO projects (project_number, name, contact_person, description, status, documentation_path,
                presentation, assembly_elements, assembly_steps, presentation_images, enclosure_images,
                enclosure_pinout, pcb_images, pcb_specifications)
                VALUES (?, ?, ?, ?, COALESCE(?, 'ACTIVE'), ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, request.projectNumber(), request.name(), request.contactPerson(),
                request.description(), request.status(), folderPath(request.projectNumber()), request.presentation(),
                request.assemblyElements(), request.assemblySteps(), join(request.presentationImages()),
                join(request.enclosureImages()), request.enclosurePinout(), join(request.pcbImages()),
                request.pcbSpecifications());
        replaceContacts(id, request.contacts());
        Project project = findById(id);
        ensureFolder(project.documentationPath());
        recordActivity(id, userId, "CREATED", "Projet cree : " + project.projectNumber() + " · " + project.name());
        return project;
    }

    @Transactional
    public Project update(long id, ProjectRequest request, Long userId) {
        Project before = findById(id);
        jdbc.update("""
                UPDATE projects SET project_number = ?, name = ?, contact_person = ?, description = ?,
                status = COALESCE(?, status), documentation_path = ?, presentation = ?, assembly_elements = ?,
                assembly_steps = ?, presentation_images = ?, enclosure_images = ?, enclosure_pinout = ?,
                pcb_images = ?, pcb_specifications = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, request.projectNumber(), request.name(), request.contactPerson(), request.description(),
                request.status(), folderPath(request.projectNumber()), request.presentation(), request.assemblyElements(),
                request.assemblySteps(), join(request.presentationImages()), join(request.enclosureImages()),
                request.enclosurePinout(), join(request.pcbImages()), request.pcbSpecifications(), id);
        replaceContacts(id, request.contacts());
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

    private Project withDetails(Project project) {
        return new Project(project.id(), project.projectNumber(), project.name(), project.contactPerson(),
                project.description(), project.status(), project.documentationPath(), project.createdAt(),
                project.updatedAt(), project.presentation(), project.assemblyElements(), project.assemblySteps(),
                project.presentationImages(), project.enclosureImages(), project.enclosurePinout(), project.pcbImages(),
                project.pcbSpecifications(), jdbc.queryForList("""
                SELECT id, name, role, email, phone FROM project_contacts
                WHERE project_id = ? ORDER BY id
                """, project.id()), jdbc.queryForList("""
                SELECT tq.id, tq.name, tq.status, tq.description, tq.updated_at AS "updatedAt",
                       (SELECT COUNT(*) FROM test_steps ts WHERE ts.quote_id = tq.id) AS "stepCount"
                FROM test_quotes tq WHERE tq.project_id = ? ORDER BY tq.updated_at DESC
                """, project.id()));
    }

    private void replaceContacts(long projectId, List<ProjectContactRequest> contacts) {
        jdbc.update("DELETE FROM project_contacts WHERE project_id = ?", projectId);
        if (contacts == null) return;
        for (ProjectContactRequest contact : contacts) {
            if (contact == null || contact.name() == null || contact.name().isBlank()) continue;
            jdbc.update("""
                    INSERT INTO project_contacts (project_id, name, role, email, phone)
                    VALUES (?, ?, ?, ?, ?)
                    """, projectId, contact.name(), contact.role(), contact.email(), contact.phone());
        }
    }

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
        if (!Objects.equals(before.projectNumber(), request.projectNumber())) fields.add("numero du projet");
        if (!Objects.equals(before.name(), request.name())) fields.add("nom");
        if (!Objects.equals(before.contactPerson(), request.contactPerson())) fields.add("contact principal");
        if (!Objects.equals(before.description(), request.description())) fields.add("description");
        if (!Objects.equals(before.presentation(), request.presentation())) fields.add("presentation");
        if (!Objects.equals(before.assemblyElements(), request.assemblyElements()) ||
                !Objects.equals(before.assemblySteps(), request.assemblySteps())) fields.add("assemblage");
        if (!Objects.equals(before.enclosurePinout(), request.enclosurePinout())) fields.add("boitier et pinout");
        if (!Objects.equals(before.pcbSpecifications(), request.pcbSpecifications())) fields.add("PCB");
        if (request.status() != null && !Objects.equals(before.status(), request.status())) fields.add("statut");
        return fields.isEmpty() ? "Informations du projet enregistrees." :
                "Champs modifies : " + fields.stream().collect(Collectors.joining(", ")) + ".";
    }

    private String folderPath(String projectNumber) {
        String safeNumber = projectNumber.trim().replaceAll("[^A-Za-z0-9_-]+", "-");
        return "projets/" + (safeNumber.isBlank() ? "project" : safeNumber);
    }

    private void ensureFolder(String relativePath) {
        try {
            Path folder = documentationRoot.resolve(relativePath).normalize();
            if (!folder.startsWith(documentationRoot)) throw new IllegalStateException("Dossier de projet invalide");
            Files.createDirectories(folder);
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de creer le dossier du projet", exception);
        }
    }

    private String join(List<String> values) {
        return values == null ? null : values.stream().filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private static List<String> lines(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.replace("\\n", "\n").split("\\R"))
                .map(String::trim).filter(item -> !item.isBlank()).toList();
    }
}
