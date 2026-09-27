package com.finder.demo.products;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final JdbcTemplate jdbc;

    public ProductController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> all() {
        return jdbc.queryForList("""
                SELECT pr.id, pr.project_id AS "projectId", pr.product_number AS "productNumber", pr.name,
                       pr.description, pr.updated_at AS "updatedAt", p.project_number AS "projectNumber",
                       p.name AS "projectName",
                       (SELECT COUNT(*) FROM product_contacts pc WHERE pc.product_id = pr.id) AS "contactCount",
                       (SELECT COUNT(*) FROM test_quotes tq WHERE tq.product_id = pr.id) AS "quoteCount"
                FROM products pr JOIN projects p ON p.id = pr.project_id
                ORDER BY pr.updated_at DESC, pr.name
                """);
    }

    @GetMapping("/{id}")
    public Map<String, Object> one(@PathVariable long id) {
        Map<String, Object> product = jdbc.queryForMap("""
                SELECT pr.id, pr.project_id AS "projectId", pr.product_number AS "productNumber", pr.name,
                       pr.description, pr.presentation, pr.assembly_elements AS "assemblyElements",
                       pr.assembly_steps AS "assemblySteps", pr.presentation_images, pr.enclosure_images,
                       pr.enclosure_pinout, pr.pcb_images, pr.pcb_specifications,
                       pr.created_at AS "createdAt", pr.updated_at AS "updatedAt",
                       p.project_number AS "projectNumber", p.name AS "projectName"
                FROM products pr JOIN projects p ON p.id = pr.project_id WHERE pr.id = ?
                """, id);
        product.put("presentationImages", lines(product.remove("presentation_images")));
        product.put("enclosureImages", lines(product.remove("enclosure_images")));
        product.put("pcbImages", lines(product.remove("pcb_images")));
        product.put("contacts", jdbc.queryForList("""
                SELECT id, name, role, email, phone FROM product_contacts
                WHERE product_id = ? ORDER BY id
                """, id));
        product.put("quotes", jdbc.queryForList("""
                SELECT tq.id, tq.name, tq.status, tq.description, tq.updated_at AS "updatedAt",
                       (SELECT COUNT(*) FROM test_steps ts WHERE ts.quote_id = tq.id) AS "stepCount"
                FROM test_quotes tq WHERE tq.product_id = ? ORDER BY tq.updated_at DESC
                """, id));
        return product;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> create(@Valid @RequestBody ProductRequest request) {
        long projectId = requireProject(request.projectId());
        long id = jdbc.queryForObject("""
                INSERT INTO products (project_id, product_number, name, description, presentation,
                assembly_elements, assembly_steps, presentation_images, enclosure_images, enclosure_pinout,
                pcb_images, pcb_specifications)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, projectId, request.productNumber(), request.name(), request.description(),
                request.presentation(), request.assemblyElements(), request.assemblySteps(),
                join(request.presentationImages()), join(request.enclosureImages()), request.enclosurePinout(),
                join(request.pcbImages()), request.pcbSpecifications());
        replaceContacts(id, request.contacts());
        return one(id);
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody ProductRequest request) {
        jdbc.update("""
                UPDATE products SET project_id = COALESCE(?, project_id), product_number = ?, name = ?,
                description = ?, presentation = ?, assembly_elements = ?, assembly_steps = ?, presentation_images = ?, enclosure_images = ?,
                enclosure_pinout = ?, pcb_images = ?, pcb_specifications = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, request.projectId(), request.productNumber(), request.name(), request.description(),
                request.presentation(), request.assemblyElements(), request.assemblySteps(), join(request.presentationImages()), join(request.enclosureImages()),
                request.enclosurePinout(), join(request.pcbImages()), request.pcbSpecifications(), id);
        replaceContacts(id, request.contacts());
        return one(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { jdbc.update("DELETE FROM products WHERE id = ?", id); }

    private void replaceContacts(long productId, List<ProductContactRequest> contacts) {
        jdbc.update("DELETE FROM product_contacts WHERE product_id = ?", productId);
        if (contacts == null) return;
        for (ProductContactRequest contact : contacts) {
            if (contact == null || contact.name() == null || contact.name().isBlank()) continue;
            jdbc.update("""
                    INSERT INTO product_contacts (product_id, name, role, email, phone)
                    VALUES (?, ?, ?, ?, ?)
                    """, productId, contact.name(), contact.role(), contact.email(), contact.phone());
        }
    }

    private long requireProject(Long projectId) {
        if (projectId == null) throw new IllegalArgumentException("Un projet est requis pour le produit");
        return jdbc.queryForObject("SELECT id FROM projects WHERE id = ?", Long.class, projectId);
    }

    private String join(List<String> values) {
        return values == null ? null : values.stream().filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private List<String> lines(Object value) {
        if (value == null) return List.of();
        return Arrays.stream(String.valueOf(value).replace("\\n", "\n").split("\\R"))
                .map(String::trim).filter(valueLine -> !valueLine.isBlank()).toList();
    }
}
