package com.finder.demo.users;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Array;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final JdbcTemplate jdbc;

    public UserController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> all() {
        return jdbc.query("SELECT id, display_name AS \"displayName\", email, role, extra_permissions AS \"extraPermissions\", active, created_at AS \"createdAt\" FROM app_users ORDER BY display_name",
                (rs, row) -> Map.of("id", rs.getLong("id"), "displayName", rs.getString("displayName"),
                        "email", rs.getString("email"), "role", rs.getString("role"),
                        "extraPermissions", arrayValues(rs.getArray("extraPermissions")), "active", rs.getBoolean("active"),
                        "createdAt", rs.getTimestamp("createdAt")));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody UserRequest request) {
        Long id = jdbc.queryForObject("INSERT INTO app_users (display_name, email, role, extra_permissions, active) VALUES (?, ?, ?, ?, COALESCE(?, true)) RETURNING id",
                Long.class, request.displayName(), request.email(), request.role(), request.extraPermissions() == null ? new String[0] : request.extraPermissions().toArray(String[]::new), request.active());
        return jdbc.queryForMap("SELECT id, display_name AS \"displayName\", email, role, extra_permissions AS \"extraPermissions\", active FROM app_users WHERE id = ?", id);
    }

    private List<String> arrayValues(Array array) {
        try { return array == null ? List.of() : List.of((String[]) array.getArray()); }
        catch (SQLException exception) { return List.of(); }
    }
}
