package com.finder.demo.dashboard;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final JdbcTemplate jdbc;

    public DashboardController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return Map.of(
                "projects", jdbc.queryForObject("SELECT COUNT(*) FROM projects WHERE status <> 'ARCHIVED'", Integer.class),
                "activeQuotes", jdbc.queryForObject("SELECT COUNT(*) FROM test_quotes WHERE status = 'IN_PROGRESS'", Integer.class),
                "documents", 24,
                "teamMembers", jdbc.queryForObject("SELECT COUNT(*) FROM app_users WHERE active = true", Integer.class),
                "recentProjects", jdbc.queryForList("SELECT id, project_number AS \"projectNumber\", name, status, updated_at AS \"updatedAt\" FROM projects ORDER BY updated_at DESC LIMIT 4")
        );
    }
}
