CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    product_number VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    presentation TEXT,
    assembly_elements TEXT,
    assembly_steps TEXT,
    presentation_images TEXT,
    enclosure_images TEXT,
    enclosure_pinout TEXT,
    pcb_images TEXT,
    pcb_specifications TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_contacts (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    role VARCHAR(120),
    email VARCHAR(200),
    phone VARCHAR(80)
);

INSERT INTO products (project_id, product_number, name, description, presentation, presentation_images,
                      enclosure_images, enclosure_pinout, pcb_images, pcb_specifications)
SELECT id, project_number || '-P1', name,
       description,
       'Produit de démonstration MegaFinder pour présenter la structure du dossier de test.',
       'images/produit-vue-avant.png\nimages/produit-vue-arriere.png',
       'images/boitier-face-avant.png\nimages/boitier-face-arriere.png',
       'TP5 = alimentation / mesure\nTP9 = GND\nTP12 = signal de réveil',
       'images/pcb-top.png\nimages/pcb-bottom.png',
       '4 couches · FR-4 · 12 V nominal · connecteur X1'
FROM projects;

INSERT INTO product_contacts (product_id, name, role, email)
SELECT pr.id, p.contact_person, 'Contact principal', NULL
FROM products pr JOIN projects p ON p.id = pr.project_id
WHERE p.contact_person IS NOT NULL;

ALTER TABLE test_quotes ADD COLUMN product_id BIGINT;
UPDATE test_quotes tq
SET product_id = (SELECT pr.id FROM products pr WHERE pr.project_id = tq.project_id ORDER BY pr.id LIMIT 1)
WHERE product_id IS NULL;
ALTER TABLE test_quotes ALTER COLUMN product_id SET NOT NULL;
ALTER TABLE test_quotes ADD CONSTRAINT fk_test_quotes_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;

CREATE TABLE test_quote_references (
    id BIGSERIAL PRIMARY KEY,
    quote_id BIGINT NOT NULL REFERENCES test_quotes(id) ON DELETE CASCADE,
    reference_type VARCHAR(80) NOT NULL,
    reference_value VARCHAR(160) NOT NULL
);

INSERT INTO test_quote_references (quote_id, reference_type, reference_value)
SELECT tq.id, 'Megatech', 'MT-2401-001' FROM test_quotes tq LIMIT 1;
INSERT INTO test_quote_references (quote_id, reference_type, reference_value)
SELECT tq.id, 'BRP', 'BRP-ECU-2026-01' FROM test_quotes tq LIMIT 1;

ALTER TABLE test_steps ADD COLUMN verification_type VARCHAR(40);
ALTER TABLE test_steps ADD COLUMN test_method TEXT;
ALTER TABLE test_steps ADD COLUMN measurement_method TEXT;
ALTER TABLE test_steps ADD COLUMN wait_ms INTEGER;
ALTER TABLE test_steps ADD COLUMN ground_reference VARCHAR(120);
ALTER TABLE test_steps ADD COLUMN required_instruments TEXT;
ALTER TABLE test_steps ADD COLUMN test_template_id BIGINT REFERENCES test_templates(id) ON DELETE SET NULL;

UPDATE test_steps
SET verification_type = 'B',
    test_method = 'Appliquer la condition de départ',
    measurement_method = 'Mesurer la valeur sur le pin à tester',
    ground_reference = 'GND sur TP9'
WHERE verification_type IS NULL;
