package com.finder.demo.testquotes;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test-quotes")
public class TestQuoteController {
    private final JdbcTemplate jdbc;

    public TestQuoteController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> all() {
        return jdbc.queryForList("""
                SELECT tq.id, tq.name, tq.description, tq.status, tq.project_id AS \"projectId\", tq.template_id AS \"templateId\",
                       p.project_number AS \"projectNumber\", p.name AS \"projectName\",
                       tt.name AS \"templateName\",
                       (SELECT COUNT(*) FROM test_steps ts WHERE ts.quote_id = tq.id) AS \"stepCount\",
                       tq.updated_at AS \"updatedAt\"
                FROM test_quotes tq JOIN projects p ON p.id = tq.project_id
                LEFT JOIN test_templates tt ON tt.id = tq.template_id ORDER BY tq.updated_at DESC
                """);
    }

    @GetMapping("/{id}")
    public Map<String, Object> one(@PathVariable long id) {
        Map<String, Object> quote = jdbc.queryForMap("""
                SELECT tq.id, tq.name, tq.description, tq.status, tq.project_id AS \"projectId\", tq.template_id AS \"templateId\",
                       p.project_number AS \"projectNumber\", p.name AS \"projectName\", tt.name AS \"templateName\",
                       tt.content AS \"templateContent\"
                FROM test_quotes tq JOIN projects p ON p.id = tq.project_id
                LEFT JOIN test_templates tt ON tt.id = tq.template_id WHERE tq.id = ?
                """, id);
        quote.put("steps", jdbc.queryForList("""
                SELECT id, step_number AS \"stepNumber\", sub_step AS \"subStep\", start_condition AS \"startCondition\",
                       pin_to_test AS \"pinToTest\", measurement_type AS \"measurementType\", unit,
                       expected_min AS \"expectedMin\", expected_max AS \"expectedMax\", relay, voltage, notes
                FROM test_steps WHERE quote_id = ? ORDER BY step_number, sub_step
                """, id));
        return quote;
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody TestQuoteRequest request) {
        long id = jdbc.queryForObject("""
                INSERT INTO test_quotes (project_id, template_id, name, description, status)
                VALUES (?, COALESCE(?, (SELECT id FROM test_templates WHERE template_key = 'DEFAULT')), ?, ?, COALESCE(?, 'DRAFT')) RETURNING id
                """, Long.class, request.projectId(), request.templateId(), request.name(), request.description(), request.status());
        replaceSteps(id, request.steps());
        return one(id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody TestQuoteRequest request) {
        jdbc.update("""
                UPDATE test_quotes SET project_id = ?,
                template_id = COALESCE(?, template_id, (SELECT id FROM test_templates WHERE template_key = 'DEFAULT')),
                name = ?, description = ?, status = COALESCE(?, status), updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, request.projectId(), request.templateId(), request.name(), request.description(), request.status(), id);
        replaceSteps(id, request.steps());
        return one(id);
    }

    @PostMapping("/{id}/generate-cpp")
    public ResponseEntity<byte[]> generateCpp(@PathVariable long id) {
        Map<String, Object> quote = one(id);
        String content = cppTemplate(quote);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("test_quote_" + id + ".cpp").build());
        return ResponseEntity.ok().headers(headers).body(content.getBytes(StandardCharsets.UTF_8));
    }

    private void replaceSteps(long quoteId, List<TestStepRequest> steps) {
        jdbc.update("DELETE FROM test_steps WHERE quote_id = ?", quoteId);
        if (steps == null) return;
        for (TestStepRequest step : steps) {
            jdbc.update("""
                    INSERT INTO test_steps (quote_id, step_number, sub_step, start_condition, pin_to_test, measurement_type, unit,
                    expected_min, expected_max, relay, voltage, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, quoteId, value(step.stepNumber(), 1), value(step.subStep(), 1), step.startCondition(), step.pinToTest(),
                    value(step.measurementType(), "VOLTAGE"), value(step.unit(), "V"), step.expectedMin(), step.expectedMax(),
                    step.relay(), step.voltage(), step.notes());
        }
    }

    private String cppTemplate(Map<String, Object> quote) {
        String source = quote.get("templateContent") == null ? defaultTemplate() : String.valueOf(quote.get("templateContent"));
        @SuppressWarnings("unchecked") List<Map<String, Object>> steps = (List<Map<String, Object>>) quote.get("steps");
        StringBuilder stepOutput = new StringBuilder();
        for (Map<String, Object> step : steps) {
            stepOutput.append("TEST_F(").append(cppName(String.valueOf(quote.get("name")))).append("Test, Step")
                    .append(step.get("stepNumber")).append("_").append(step.get("subStep")).append(") {\n");
            stepOutput.append("    // Start condition: ").append(step.get("startCondition")).append("\n");
            stepOutput.append("    // Measure ").append(step.get("measurementType")).append(" on ").append(step.get("pinToTest"))
                    .append(" [").append(step.get("expectedMin")).append(", ").append(step.get("expectedMax")).append(" ").append(step.get("unit")).append("]\n");
            stepOutput.append("    // Relay: ").append(step.get("relay")).append(" | Supply: ").append(step.get("voltage")).append(" V\n");
            stepOutput.append("    GTEST_SUCCEED();\n}\n\n");
        }
        return source.replace("{{QUOTE_ID}}", String.valueOf(quote.get("id")))
                .replace("{{QUOTE_NAME}}", String.valueOf(quote.get("name")))
                .replace("{{PROJECT_NUMBER}}", String.valueOf(quote.get("projectNumber")))
                .replace("{{PROJECT_NAME}}", String.valueOf(quote.get("projectName")))
                .replace("{{TEST_CLASS}}", cppName(String.valueOf(quote.get("name"))) + "Test")
                .replace("{{STEPS}}", stepOutput.toString());
    }

    private String cppName(String value) { return value.replaceAll("[^A-Za-z0-9]", ""); }
    private <T> T value(T value, T fallback) { return value == null ? fallback : value; }

    private String defaultTemplate() {
        return "// Generated by MegaFinder - Test quote #{{QUOTE_ID}}\n"
                + "#include <gtest/gtest.h>\n\n"
                + "class {{TEST_CLASS}} : public ::testing::Test {};\n\n{{STEPS}}";
    }
}
