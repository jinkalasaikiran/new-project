package com.example.productservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class ProductController {

    // In-memory "database" - swap for a real DB in production
    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public ProductController() {
        products.put("p1", new Product("p1", "Wireless Mouse", 19.99, 150));
        products.put("p2", new Product("p2", "Mechanical Keyboard", 79.99, 80));
        products.put("p3", new Product("p3", "USB-C Hub", 34.50, 200));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "product-service"));
    }

    @GetMapping("/products")
    public ResponseEntity<Iterable<Product>> listProducts() {
        return ResponseEntity.ok(products.values());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable String id) {
        Product product = products.get(id);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "product not found"));
        }
        return ResponseEntity.ok(product);
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "name is required"));
        }
        double price = body.get("price") != null ? Double.parseDouble(body.get("price").toString()) : 0.0;
        int stock = body.get("stock") != null ? Integer.parseInt(body.get("stock").toString()) : 0;

        String id = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product(id, name, price, stock);
        products.put(id, product);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
}
