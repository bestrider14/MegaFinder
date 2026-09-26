package com.finder.demo.testquotes;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test-templates")
public class TestTemplateController {
    private final JdbcTemplate jdbc;

    public TestTemplateController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> all() {
        return jdbc.queryForList("""
                SELECT id, template_key AS "templateKey", name, description, language, content,
                       is_system AS "system", created_at AS "createdAt", updated_at AS "updatedAt"
                FROM test_templates ORDER BY is_system DESC, name
                """);
    }

    @GetMapping("/{id}")
    public Map<String, Object> one(@PathVariable long id) {
        return jdbc.queryForMap("""
                SELECT id, template_key AS "templateKey", name, description, language, content,
                       is_system AS "system", created_at AS "createdAt", updated_at AS "updatedAt"
                FROM test_templates WHERE id = ?
                """, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody TestTemplateRequest request) {
        long id = jdbc.queryForObject("""
                INSERT INTO test_templates (template_key, name, description, language, content)
                VALUES (?, ?, ?, COALESCE(?, 'CPP'), ?) RETURNING id
                """, Long.class, key(request.name()), request.name(), request.description(), request.language(), request.content());
        return one(id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody TestTemplateRequest request) {
        jdbc.update("""
                UPDATE test_templates SET name = ?, description = ?, language = COALESCE(?, language),
                content = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, request.name(), request.description(), request.language(), request.content(), id);
        return one(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        jdbc.update("DELETE FROM test_templates WHERE id = ? AND is_system = FALSE", id);
    }

    private String key(String name) {
        String key = name.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
        return "CUSTOM_" + (key.isBlank() ? "TEMPLATE" : key);
    }
}
