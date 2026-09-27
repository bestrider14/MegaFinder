CREATE TABLE test_benches (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    location VARCHAR(160),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bench_equipment (
    id BIGSERIAL PRIMARY KEY,
    bench_id BIGINT NOT NULL REFERENCES test_benches(id) ON DELETE CASCADE,
    equipment_type VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    channel_count INTEGER,
    voltage_min NUMERIC(12, 4),
    voltage_max NUMERIC(12, 4),
    current_max NUMERIC(12, 4),
    notes TEXT
);

CREATE TABLE bench_terminals (
    id BIGSERIAL PRIMARY KEY,
    bench_id BIGINT NOT NULL REFERENCES test_benches(id) ON DELETE CASCADE,
    terminal_label VARCHAR(40) NOT NULL,
    pin_name VARCHAR(80),
    signal_type VARCHAR(80),
    description TEXT,
    relay_reference VARCHAR(120),
    notes TEXT,
    UNIQUE (bench_id, terminal_label)
);

ALTER TABLE test_quotes ADD COLUMN bench_id BIGINT REFERENCES test_benches(id) ON DELETE SET NULL;
ALTER TABLE test_steps ADD COLUMN power_supply_name VARCHAR(120);
ALTER TABLE test_steps ADD COLUMN supply_voltage NUMERIC(12, 4);
ALTER TABLE test_steps ADD COLUMN supply_current NUMERIC(12, 4);
ALTER TABLE test_steps ADD COLUMN relay_channels VARCHAR(240);
ALTER TABLE test_steps ADD COLUMN relay_delay_ms INTEGER;

INSERT INTO test_benches (name, description, location)
VALUES ('Banc ECU principal', 'Banc de validation avec alimentations programmables, oscilloscope et cartes relais.', 'Laboratoire A · Poste 01');

INSERT INTO bench_equipment (bench_id, equipment_type, name, quantity, channel_count, voltage_min, voltage_max, current_max, notes)
SELECT id, 'POWER_SUPPLY', 'Power supply 0-25 V / 50 A', 1, 1, 0, 25, 50, 'Sortie principale haute puissance'
FROM test_benches WHERE name = 'Banc ECU principal';
INSERT INTO bench_equipment (bench_id, equipment_type, name, quantity, channel_count, voltage_min, voltage_max, current_max, notes)
SELECT id, 'POWER_SUPPLY', 'Power supply 0-240 V / 5 A', 1, 1, 0, 240, 5, 'Sortie haute tension'
FROM test_benches WHERE name = 'Banc ECU principal';
INSERT INTO bench_equipment (bench_id, equipment_type, name, quantity, channel_count, voltage_min, voltage_max, current_max, notes)
SELECT id, 'OSCILLOSCOPE', 'Oscilloscope 4 voies', 1, 4, NULL, NULL, NULL, 'Mesure temporelle et signaux rapides'
FROM test_benches WHERE name = 'Banc ECU principal';
INSERT INTO bench_equipment (bench_id, equipment_type, name, quantity, channel_count, voltage_min, voltage_max, current_max, notes)
SELECT id, 'RELAY_CARD', 'Carte relais 20 voies', 4, 20, NULL, NULL, NULL, '80 relais disponibles · K1 à K80'
FROM test_benches WHERE name = 'Banc ECU principal';

INSERT INTO bench_terminals (bench_id, terminal_label, pin_name, signal_type, description, relay_reference)
SELECT id, 'X1-01', 'VBAT+', 'Alimentation', 'Entrée alimentation du module', 'K1'
FROM test_benches WHERE name = 'Banc ECU principal';
INSERT INTO bench_terminals (bench_id, terminal_label, pin_name, signal_type, description, relay_reference)
SELECT id, 'X1-02', 'GND', 'Retour', 'Retour commun', 'K2'
FROM test_benches WHERE name = 'Banc ECU principal';
INSERT INTO bench_terminals (bench_id, terminal_label, pin_name, signal_type, description, relay_reference)
SELECT id, 'X1-03', 'PIN_SENSOR_1', 'Analogique', 'Entrée capteur 1', 'K3'
FROM test_benches WHERE name = 'Banc ECU principal';
INSERT INTO bench_terminals (bench_id, terminal_label, pin_name, signal_type, description, relay_reference)
SELECT id, 'X1-04', 'PIN_SENSOR_2', 'Analogique', 'Entrée capteur 2', 'K4'
FROM test_benches WHERE name = 'Banc ECU principal';
INSERT INTO bench_terminals (bench_id, terminal_label, pin_name, signal_type, description, relay_reference)
SELECT id, 'X1-05', 'RELAY_RETURN', 'Relais', 'Sortie commandée', 'K5'
FROM test_benches WHERE name = 'Banc ECU principal';

UPDATE test_quotes
SET bench_id = (SELECT id FROM test_benches WHERE name = 'Banc ECU principal')
WHERE bench_id IS NULL;
