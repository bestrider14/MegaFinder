ALTER TABLE projects
    ADD COLUMN presentation TEXT,
    ADD COLUMN assembly_elements TEXT,
    ADD COLUMN assembly_steps TEXT,
    ADD COLUMN presentation_images TEXT,
    ADD COLUMN enclosure_images TEXT,
    ADD COLUMN enclosure_pinout TEXT,
    ADD COLUMN pcb_images TEXT,
    ADD COLUMN pcb_specifications TEXT;

-- A project is the product record from this migration onward. When a database
-- already contains the old product table, keep its product information on the
-- related project before removing the duplicate relation.
UPDATE projects p
SET presentation = source.presentation,
    assembly_elements = source.assembly_elements,
    assembly_steps = source.assembly_steps,
    presentation_images = source.presentation_images,
    enclosure_images = source.enclosure_images,
    enclosure_pinout = source.enclosure_pinout,
    pcb_images = source.pcb_images,
    pcb_specifications = source.pcb_specifications,
    updated_at = GREATEST(p.updated_at, source.updated_at)
FROM (
    SELECT DISTINCT ON (project_id)
           project_id, presentation, assembly_elements, assembly_steps,
           presentation_images, enclosure_images, enclosure_pinout,
           pcb_images, pcb_specifications, updated_at
    FROM products
    ORDER BY project_id, updated_at DESC, id DESC
) source
WHERE p.id = source.project_id;

CREATE TABLE project_contacts (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    role VARCHAR(120),
    email VARCHAR(200),
    phone VARCHAR(80)
);

INSERT INTO project_contacts (project_id, name, role, email, phone)
SELECT pr.project_id, pc.name, pc.role, pc.email, pc.phone
FROM product_contacts pc
JOIN products pr ON pr.id = pc.product_id;

ALTER TABLE test_quotes DROP CONSTRAINT fk_test_quotes_product;
ALTER TABLE test_quotes DROP COLUMN product_id;

DROP TABLE product_contacts;
DROP TABLE products;
