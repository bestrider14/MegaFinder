package com.finder.demo.projects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProjectRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Project> mapper = (rs, rowNum) -> new Project(
            rs.getLong("id"), rs.getString("project_number"), rs.getString("name"),
            rs.getString("contact_person"), rs.getString("description"), rs.getString("status"),
            rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime());

    public ProjectRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Project> findAll() {
        return jdbc.query("SELECT * FROM projects ORDER BY updated_at DESC", mapper);
    }

    public Project findById(long id) {
        return jdbc.queryForObject("SELECT * FROM projects WHERE id = ?", mapper, id);
    }

    public Project create(ProjectRequest request) {
        long id = jdbc.queryForObject("""
                INSERT INTO projects (project_number, name, contact_person, description, status)
                VALUES (?, ?, ?, ?, COALESCE(?, 'ACTIVE')) RETURNING id
                """, Long.class, request.projectNumber(), request.name(), request.contactPerson(),
                request.description(), request.status());
        return findById(id);
    }

    public Project update(long id, ProjectRequest request) {
        jdbc.update("""
                UPDATE projects SET project_number = ?, name = ?, contact_person = ?, description = ?,
                status = COALESCE(?, status), updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, request.projectNumber(), request.name(), request.contactPerson(), request.description(),
                request.status(), id);
        return findById(id);
    }

    public void delete(long id) { jdbc.update("DELETE FROM projects WHERE id = ?", id); }
}
