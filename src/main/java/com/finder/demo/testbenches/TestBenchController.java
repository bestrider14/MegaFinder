package com.finder.demo.testbenches;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test-benches")
public class TestBenchController {
    private final JdbcTemplate jdbc;

    public TestBenchController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> all() {
        return jdbc.queryForList("""
                SELECT b.id, b.name, b.description, b.location, b.created_at AS "createdAt", b.updated_at AS "updatedAt",
                       (SELECT COUNT(*) FROM bench_equipment e WHERE e.bench_id = b.id) AS "equipmentCount",
                       (SELECT COUNT(*) FROM bench_terminals t WHERE t.bench_id = b.id) AS "terminalCount",
                       (SELECT COALESCE(SUM(e.quantity * COALESCE(e.channel_count, 1)), 0) FROM bench_equipment e
                        WHERE e.bench_id = b.id AND e.equipment_type = 'RELAY_CARD') AS "relayCount"
                FROM test_benches b ORDER BY b.updated_at DESC, b.name
                """);
    }

    @GetMapping("/{id}")
    public Map<String, Object> one(@PathVariable long id) {
        Map<String, Object> bench = jdbc.queryForMap("""
                SELECT id, name, description, location, created_at AS "createdAt", updated_at AS "updatedAt"
                FROM test_benches WHERE id = ?
                """, id);
        bench.put("equipment", jdbc.queryForList("""
                SELECT id, equipment_type AS "equipmentType", name, quantity, channel_count AS "channelCount",
                       voltage_min AS "voltageMin", voltage_max AS "voltageMax", current_max AS "currentMax", notes
                FROM bench_equipment WHERE bench_id = ? ORDER BY id
                """, id));
        bench.put("terminals", jdbc.queryForList("""
                SELECT id, terminal_label AS "terminalLabel", pin_name AS "pinName", signal_type AS "signalType",
                       description, relay_reference AS "relayReference", notes
                FROM bench_terminals WHERE bench_id = ? ORDER BY terminal_label, id
                """, id));
        return bench;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> create(@Valid @RequestBody TestBenchRequest request) {
        long id = jdbc.queryForObject("""
                INSERT INTO test_benches (name, description, location) VALUES (?, ?, ?) RETURNING id
                """, Long.class, request.name(), request.description(), request.location());
        replaceEquipment(id, request.equipment());
        replaceTerminals(id, request.terminals());
        return one(id);
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody TestBenchRequest request) {
        jdbc.update("""
                UPDATE test_benches SET name = ?, description = ?, location = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, request.name(), request.description(), request.location(), id);
        replaceEquipment(id, request.equipment());
        replaceTerminals(id, request.terminals());
        return one(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { jdbc.update("DELETE FROM test_benches WHERE id = ?", id); }

    private void replaceEquipment(long benchId, List<BenchEquipmentRequest> equipment) {
        jdbc.update("DELETE FROM bench_equipment WHERE bench_id = ?", benchId);
        if (equipment == null) return;
        for (BenchEquipmentRequest item : equipment) {
            jdbc.update("""
                    INSERT INTO bench_equipment (bench_id, equipment_type, name, quantity, channel_count,
                    voltage_min, voltage_max, current_max, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, benchId, value(item.equipmentType(), "OTHER"), item.name(), value(item.quantity(), 1),
                    item.channelCount(), item.voltageMin(), item.voltageMax(), item.currentMax(), item.notes());
        }
    }

    private void replaceTerminals(long benchId, List<BenchTerminalRequest> terminals) {
        jdbc.update("DELETE FROM bench_terminals WHERE bench_id = ?", benchId);
        if (terminals == null) return;
        for (BenchTerminalRequest item : terminals) {
            jdbc.update("""
                    INSERT INTO bench_terminals (bench_id, terminal_label, pin_name, signal_type, description, relay_reference, notes)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, benchId, item.terminalLabel(), item.pinName(), item.signalType(), item.description(),
                    item.relayReference(), item.notes());
        }
    }

    private <T> T value(T value, T fallback) { return value == null ? fallback : value; }
}
