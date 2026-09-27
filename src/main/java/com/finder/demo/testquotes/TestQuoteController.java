package com.finder.demo.testquotes;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/test-quotes")
public class TestQuoteController {
    private final JdbcTemplate jdbc;

    public TestQuoteController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> all() {
        return jdbc.queryForList("""
                SELECT tq.id, tq.name, tq.description, tq.status,
                       tq.project_id AS "projectId", tq.bench_id AS "benchId", tq.template_id AS "templateId",
                       p.project_number AS "projectNumber", p.name AS "projectName",
                       p.project_number AS "productNumber", p.name AS "productName", b.name AS "benchName",
                       tt.name AS "templateName",
                       (SELECT COUNT(*) FROM test_steps ts WHERE ts.quote_id = tq.id) AS "stepCount",
                       tq.updated_at AS "updatedAt"
                FROM test_quotes tq JOIN projects p ON p.id = tq.project_id
                LEFT JOIN test_benches b ON b.id = tq.bench_id
                LEFT JOIN test_templates tt ON tt.id = tq.template_id
                ORDER BY tq.updated_at DESC
                """);
    }

    @GetMapping("/{id}")
    public Map<String, Object> one(@PathVariable long id) {
        Map<String, Object> quote = jdbc.queryForMap("""
                SELECT tq.id, tq.name, tq.description, tq.status,
                       tq.project_id AS "projectId", tq.bench_id AS "benchId", tq.template_id AS "templateId",
                       p.project_number AS "projectNumber", p.name AS "projectName",
                       p.project_number AS "productNumber", p.name AS "productName", b.name AS "benchName",
                       tt.name AS "templateName", tt.content AS "templateContent"
                FROM test_quotes tq JOIN projects p ON p.id = tq.project_id
                LEFT JOIN test_benches b ON b.id = tq.bench_id
                LEFT JOIN test_templates tt ON tt.id = tq.template_id
                WHERE tq.id = ?
                """, id);
        quote.put("references", jdbc.queryForList("""
                SELECT id, reference_type AS "type", reference_value AS "value"
                FROM test_quote_references WHERE quote_id = ? ORDER BY id
                """, id));
        quote.put("steps", jdbc.queryForList("""
                SELECT ts.id, ts.step_number AS "stepNumber", ts.sub_step AS "subStep",
                       ts.verification_type AS "verificationType", ts.start_condition AS "startCondition",
                       ts.ground_reference AS "groundReference", ts.pin_to_test AS "pinToTest",
                       ts.measurement_type AS "measurementType", ts.unit, ts.expected_min AS "expectedMin",
                       ts.expected_max AS "expectedMax", ts.relay, ts.voltage, ts.notes,
                       ts.power_supply_name AS "powerSupplyName", ts.supply_voltage AS "supplyVoltage",
                       ts.supply_current AS "supplyCurrent", ts.relay_channels AS "relayChannels",
                       ts.relay_delay_ms AS "relayDelayMs", ts.test_method AS "testMethod",
                       ts.measurement_method AS "measurementMethod", ts.wait_ms AS "waitMs",
                       ts.required_instruments AS "requiredInstruments", ts.test_template_id AS "testTemplateId",
                       step_template.name AS "testTemplateName", step_template.content AS "testTemplateContent"
                FROM test_steps ts LEFT JOIN test_templates step_template ON step_template.id = ts.test_template_id
                WHERE ts.quote_id = ? ORDER BY ts.step_number, ts.sub_step
                """, id));
        return quote;
    }

    @GetMapping("/{id}/equipment-summary")
    public Map<String, Object> equipmentSummary(@PathVariable long id) {
        Map<String, Object> quote = jdbc.queryForMap("""
                SELECT tq.id, tq.bench_id AS "benchId", b.name AS "benchName"
                FROM test_quotes tq LEFT JOIN test_benches b ON b.id = tq.bench_id
                WHERE tq.id = ?
                """, id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("quoteId", quote.get("id"));
        result.put("benchId", quote.get("benchId"));
        result.put("benchName", quote.get("benchName"));

        List<Map<String, Object>> inventory = quote.get("benchId") == null ? List.of() : jdbc.queryForList("""
                SELECT equipment_type AS "equipmentType", name, quantity, channel_count AS "channelCount"
                FROM bench_equipment WHERE bench_id = ? ORDER BY id
                """, quote.get("benchId"));
        List<Map<String, Object>> steps = jdbc.queryForList("""
                SELECT step_number AS "stepNumber", sub_step AS "subStep", power_supply_name AS "powerSupplyName",
                       required_instruments AS "requiredInstruments", relay_channels AS "relayChannels"
                FROM test_steps WHERE quote_id = ? ORDER BY step_number, sub_step
                """, id);

        Map<String, Integer> requested = new LinkedHashMap<>();
        Map<String, Set<String>> requestedDetails = new LinkedHashMap<>();
        Set<String> powerSupplies = new LinkedHashSet<>();
        Set<String> relayChannels = new LinkedHashSet<>();
        for (Map<String, Object> step : steps) {
            String powerSupply = text(step.get("powerSupplyName"));
            if (!powerSupply.isBlank()) powerSupplies.add(powerSupply);
            addInstrument(requested, requestedDetails, powerSupply, "POWER_SUPPLY");
            for (String instrument : splitValues(step.get("requiredInstruments"))) {
                addInstrument(requested, requestedDetails, instrument, classify(instrument));
            }
            for (String channel : splitValues(step.get("relayChannels"))) relayChannels.add(channel);
        }
        if (!powerSupplies.isEmpty()) {
            requested.put("POWER_SUPPLY", powerSupplies.size());
            requestedDetails.computeIfAbsent("POWER_SUPPLY", ignored -> new LinkedHashSet<>()).addAll(powerSupplies);
        }
        if (!relayChannels.isEmpty()) {
            requested.put("RELAY_CARD", Math.max(1, relayChannels.size()));
            requestedDetails.computeIfAbsent("RELAY_CARD", ignored -> new LinkedHashSet<>())
                    .add("Canaux : " + String.join(", ", relayChannels));
        }

        Map<String, Integer> available = new LinkedHashMap<>();
        Map<String, List<String>> availableDetails = new LinkedHashMap<>();
        for (Map<String, Object> item : inventory) {
            String type = text(item.get("equipmentType"));
            int quantity = number(item.get("quantity"), 1);
            int capacity = "RELAY_CARD".equals(type) ? quantity * number(item.get("channelCount"), 1) : quantity;
            available.merge(type, capacity, Integer::sum);
            availableDetails.computeIfAbsent(type, ignored -> new ArrayList<>())
                    .add(text(item.get("name")) + " (x" + quantity + ")");
        }

        List<Map<String, Object>> requirements = new ArrayList<>();
        for (String type : requested.keySet()) {
            int required = requested.get(type);
            int availableCount = available.getOrDefault(type, 0);
            Map<String, Object> requirement = new LinkedHashMap<>();
            requirement.put("equipmentType", type);
            requirement.put("label", label(type));
            requirement.put("required", required);
            requirement.put("available", availableCount);
            requirement.put("unit", "RELAY_CARD".equals(type) ? "canaux" : "unités");
            requirement.put("status", availableCount >= required ? "OK" : "MISSING");
            requirement.put("requestedDetails", new ArrayList<>(requestedDetails.getOrDefault(type, Set.of())));
            requirement.put("availableDetails", availableDetails.getOrDefault(type, List.of()));
            requirements.add(requirement);
        }
        result.put("stepCount", steps.size());
        result.put("requirements", requirements);
        result.put("relayChannels", relayChannels);
        return result;
    }

    @PostMapping
    @Transactional
    public Map<String, Object> create(@Valid @RequestBody TestQuoteRequest request) {
        long id = jdbc.queryForObject("""
                INSERT INTO test_quotes (project_id, bench_id, template_id, name, description, status)
                VALUES (?, ?,
                        COALESCE(?, (SELECT id FROM test_templates WHERE template_key = 'DEFAULT')),
                        ?, ?, COALESCE(?, 'DRAFT')) RETURNING id
                """, Long.class, request.projectId(), request.benchId(), request.templateId(),
                request.name(), request.description(), request.status());
        replaceReferences(id, request.references());
        replaceSteps(id, request.steps());
        return one(id);
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody TestQuoteRequest request) {
        jdbc.update("""
                UPDATE test_quotes SET project_id = ?, bench_id = ?, template_id = COALESCE(?, template_id),
                name = ?, description = ?,
                status = COALESCE(?, status), updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, request.projectId(), request.benchId(), request.templateId(), request.name(),
                request.description(), request.status(), id);
        replaceReferences(id, request.references());
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

    @PostMapping("/{id}/generate-pdf")
    public ResponseEntity<byte[]> generatePdf(@PathVariable long id) {
        try {
            Map<String, Object> quote = one(id);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(document, output);
            document.open();
            Font title = new Font(Font.HELVETICA, 20, Font.BOLD);
            Font heading = new Font(Font.HELVETICA, 13, Font.BOLD);
            document.add(new Paragraph("Devis de vérification", title));
            document.add(new Paragraph(text(quote.get("name"))));
            document.add(new Paragraph("Produit : " + text(quote.get("productNumber")) + " · " + text(quote.get("productName"))));
            document.add(new Paragraph("Banc de test : " + text(quote.get("benchName"))));
            document.add(new Paragraph("Références : " + referencesText(quote)));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Présentation du produit", heading));
            document.add(new Paragraph("La présentation, les images du produit, le boîtier et le PCB sont disponibles dans la fiche produit."));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Vérification", heading));
            @SuppressWarnings("unchecked") List<Map<String, Object>> steps = (List<Map<String, Object>>) quote.get("steps");
            PdfPTable table = new PdfPTable(new float[]{.55f, .75f, 1.4f, 1.35f, 1.2f});
            table.setWidthPercentage(100);
            addCell(table, "Étape", true); addCell(table, "Type", true); addCell(table, "Conditions", true);
            addCell(table, "Méthode", true); addCell(table, "Résultat attendu", true);
            for (Map<String, Object> step : steps) {
                addCell(table, text(step.get("stepNumber")) + "." + text(step.get("subStep")), false);
                addCell(table, text(step.get("verificationType")), false);
                addCell(table, text(step.get("startCondition")) + "\n" + text(step.get("groundReference")), false);
                addCell(table, text(step.get("testMethod")) + "\nMesure : " + text(step.get("measurementMethod")), false);
                addCell(table, text(step.get("expectedMin")) + " à " + text(step.get("expectedMax")) + " " + text(step.get("unit")), false);
            }
            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Généré par MegaFinder", new Font(Font.HELVETICA, 9, Font.ITALIC)));
            document.close();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename("devis_verification_" + id + ".pdf").build());
            return ResponseEntity.ok().headers(headers).body(output.toByteArray());
        } catch (DocumentException exception) {
            throw new IllegalStateException("Impossible de générer le PDF", exception);
        }
    }

    private void replaceReferences(long quoteId, List<TestQuoteReferenceRequest> references) {
        jdbc.update("DELETE FROM test_quote_references WHERE quote_id = ?", quoteId);
        if (references == null) return;
        for (TestQuoteReferenceRequest reference : references) {
            if (reference == null || blank(reference.type()) || blank(reference.value())) continue;
            jdbc.update("INSERT INTO test_quote_references (quote_id, reference_type, reference_value) VALUES (?, ?, ?)",
                    quoteId, reference.type(), reference.value());
        }
    }

    private void replaceSteps(long quoteId, List<TestStepRequest> steps) {
        jdbc.update("DELETE FROM test_steps WHERE quote_id = ?", quoteId);
        if (steps == null) return;
        for (TestStepRequest step : steps) {
            jdbc.update("""
                    INSERT INTO test_steps (quote_id, step_number, sub_step, verification_type, start_condition,
                    ground_reference, pin_to_test, measurement_type, unit, expected_min, expected_max, relay, voltage,
                    notes, power_supply_name, supply_voltage, supply_current, relay_channels, relay_delay_ms,
                    test_method, measurement_method, wait_ms, required_instruments, test_template_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, quoteId, value(step.stepNumber(), 1), value(step.subStep(), 1), value(step.verificationType(), "B"),
                    step.startCondition(), step.groundReference(), step.pinToTest(), value(step.measurementType(), "VOLTAGE"),
                    value(step.unit(), "V"), step.expectedMin(), step.expectedMax(), step.relay(), step.voltage(), step.notes(),
                    step.powerSupplyName(), step.supplyVoltage(), step.supplyCurrent(), step.relayChannels(), step.relayDelayMs(),
                    step.testMethod(), step.measurementMethod(), step.waitMs(), step.requiredInstruments(), step.testTemplateId());
        }
    }

    private String cppTemplate(Map<String, Object> quote) {
        String source = quote.get("templateContent") == null ? defaultTemplate() : String.valueOf(quote.get("templateContent"));
        @SuppressWarnings("unchecked") List<Map<String, Object>> steps = (List<Map<String, Object>>) quote.get("steps");
        StringBuilder stepOutput = new StringBuilder();
        for (Map<String, Object> step : steps) {
            String stepTemplate = text(step.get("testTemplateContent"));
            if (!stepTemplate.isBlank() && !"null".equals(stepTemplate)) {
                stepOutput.append(applyStepTemplate(stepTemplate, quote, step)).append("\n\n");
            } else {
                stepOutput.append("TEST_F(").append(cppName(text(quote.get("name")))).append("Test, Step")
                        .append(text(step.get("stepNumber"))).append("_").append(text(step.get("subStep"))).append(") {\n");
                stepOutput.append("    // Verification type: ").append(text(step.get("verificationType"))).append("\n");
                stepOutput.append("    // Start condition: ").append(text(step.get("startCondition"))).append(" | Ground: ").append(text(step.get("groundReference"))).append("\n");
                stepOutput.append("    // Test method: ").append(text(step.get("testMethod"))).append(" | Wait: ").append(text(step.get("waitMs"))).append(" ms\n");
                stepOutput.append("    // Measure: ").append(text(step.get("measurementMethod"))).append(" on ").append(text(step.get("pinToTest"))).append("\n");
                stepOutput.append("    // Expected: [").append(text(step.get("expectedMin"))).append(", ").append(text(step.get("expectedMax"))).append(" ").append(text(step.get("unit"))).append("]\n");
                stepOutput.append("    // Supply: ").append(text(step.get("powerSupplyName"))).append(" | ").append(text(step.get("supplyVoltage"))).append(" V / ").append(text(step.get("supplyCurrent"))).append(" A\n");
                stepOutput.append("    // Relay channels: ").append(text(step.get("relayChannels"))).append(" | Activation delay: ").append(text(step.get("relayDelayMs"))).append(" ms\n");
                stepOutput.append("    GTEST_SUCCEED();\n}\n");
            }
        }
        return source.replace("{{QUOTE_ID}}", text(quote.get("id")))
                .replace("{{QUOTE_NAME}}", text(quote.get("name")))
                .replace("{{PRODUCT_NUMBER}}", text(quote.get("productNumber")))
                .replace("{{PRODUCT_NAME}}", text(quote.get("productName")))
                .replace("{{PROJECT_NUMBER}}", text(quote.get("projectNumber")))
                .replace("{{PROJECT_NAME}}", text(quote.get("projectName")))
                .replace("{{TEST_CLASS}}", cppName(text(quote.get("name"))) + "Test")
                .replace("{{STEPS}}", stepOutput.toString());
    }

    private String applyStepTemplate(String template, Map<String, Object> quote, Map<String, Object> step) {
        return template.replace("{{QUOTE_ID}}", text(quote.get("id")))
                .replace("{{PRODUCT_NUMBER}}", text(quote.get("productNumber")))
                .replace("{{PRODUCT_NAME}}", text(quote.get("productName")))
                .replace("{{STEP_NUMBER}}", text(step.get("stepNumber")))
                .replace("{{SUB_STEP}}", text(step.get("subStep")))
                .replace("{{VERIFICATION_TYPE}}", text(step.get("verificationType")))
                .replace("{{START_CONDITION}}", text(step.get("startCondition")))
                .replace("{{GROUND_REFERENCE}}", text(step.get("groundReference")))
                .replace("{{PIN_TO_TEST}}", text(step.get("pinToTest")))
                .replace("{{TEST_METHOD}}", text(step.get("testMethod")))
                .replace("{{MEASUREMENT_METHOD}}", text(step.get("measurementMethod")))
                .replace("{{WAIT_MS}}", text(step.get("waitMs")))
                .replace("{{EXPECTED_MIN}}", text(step.get("expectedMin")))
                .replace("{{EXPECTED_MAX}}", text(step.get("expectedMax")))
                .replace("{{UNIT}}", text(step.get("unit")))
                .replace("{{POWER_SUPPLY}}", text(step.get("powerSupplyName")))
                .replace("{{SUPPLY_VOLTAGE}}", text(step.get("supplyVoltage")))
                .replace("{{SUPPLY_CURRENT}}", text(step.get("supplyCurrent")))
                .replace("{{RELAY_CHANNELS}}", text(step.get("relayChannels")))
                .replace("{{RELAY_DELAY_MS}}", text(step.get("relayDelayMs")));
    }

    private void addCell(PdfPTable table, String value, boolean header) {
        PdfPCell cell = new PdfPCell(new Phrase(value));
        cell.setPadding(5);
        if (header) cell.setBackgroundColor(new java.awt.Color(225, 225, 235));
        table.addCell(cell);
    }

    private String referencesText(Map<String, Object> quote) {
        @SuppressWarnings("unchecked") List<Map<String, Object>> references = (List<Map<String, Object>>) quote.get("references");
        return references.stream().map(reference -> text(reference.get("type")) + " : " + text(reference.get("value")))
                .reduce((left, right) -> left + " · " + right).orElse("Aucune");
    }

    private void addInstrument(Map<String, Integer> requested, Map<String, Set<String>> details,
                               String value, String type) {
        if (value == null || value.isBlank() || "OTHER".equals(type)) {
            if (value != null && !value.isBlank()) details.computeIfAbsent("OTHER", ignored -> new LinkedHashSet<>()).add(value);
            return;
        }
        requested.merge(type, 1, Integer::sum);
        details.computeIfAbsent(type, ignored -> new LinkedHashSet<>()).add(value);
    }

    private String classify(String value) {
        String normalized = value.toLowerCase();
        if (normalized.contains("scope") || normalized.contains("oscillo")) return "OSCILLOSCOPE";
        if (normalized.contains("alim") || normalized.contains("power supply") || normalized.contains("supply")) return "POWER_SUPPLY";
        if (normalized.contains("relais") || normalized.contains("relay")) return "RELAY_CARD";
        return "OTHER";
    }

    private List<String> splitValues(Object value) {
        if (value == null) return List.of();
        return java.util.Arrays.stream(String.valueOf(value).split("[,;\\n]"))
                .map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private String label(String type) {
        return switch (type) {
            case "POWER_SUPPLY" -> "Power supply";
            case "OSCILLOSCOPE" -> "Oscilloscope";
            case "RELAY_CARD" -> "Carte relais";
            default -> "Autre instrument";
        };
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private String cppName(String value) { return value.replaceAll("[^A-Za-z0-9]", ""); }
    private <T> T value(T value, T fallback) { return value == null ? fallback : value; }
    private String text(Object value) { return value == null ? "" : String.valueOf(value); }
    private boolean blank(String value) { return value == null || value.isBlank(); }

    private String defaultTemplate() {
        return "// Generated by MegaFinder - Verification quote #{{QUOTE_ID}}\\n"
                + "#include <gtest/gtest.h>\\n\\n"
                + "class {{TEST_CLASS}} : public ::testing::Test {};\\n\\n{{STEPS}}";
    }
}
