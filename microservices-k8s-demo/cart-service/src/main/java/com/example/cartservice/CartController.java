package com.example.cartservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class CartController {

    private final RestTemplate restTemplate;

    // userId -> list of cart items. In-memory store for demo purposes.
    private final Map<String, List<CartItem>> carts = new ConcurrentHashMap<>();

    @Value("${product.service.url}")
    private String productServiceUrl;

    public CartController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "cart-service"));
    }

    @GetMapping("/cart/{userId}")
    public ResponseEntity<?> getCart(@PathVariable String userId) {
        List<CartItem> items = carts.getOrDefault(userId, new ArrayList<>());
        List<Map<String, Object>> enriched = new ArrayList<>();
        double total = 0.0;

        for (CartItem item : items) {
            try {
                Map productResponse = restTemplate.getForObject(
                        productServiceUrl + "/products/" + item.getProductId(), Map.class);
                if (productResponse != null) {
                    double price = ((Number) productResponse.get("price")).doubleValue();
                    double lineTotal = price * item.getQuantity();
                    total += lineTotal;

                    Map<String, Object> line = new HashMap<>();
                    line.put("productId", item.getProductId());
                    line.put("name", productResponse.get("name"));
                    line.put("price", price);
                    line.put("quantity", item.getQuantity());
                    line.put("lineTotal", lineTotal);
                    enriched.add(line);
                }
            } catch (HttpClientErrorException.NotFound ex) {
                // Product no longer exists; skip it silently for this demo
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("items", enriched);
        response.put("total", total);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cart/{userId}/items")
    public ResponseEntity<?> addItem(@PathVariable String userId, @RequestBody Map<String, Object> body) {
        String productId = (String) body.get("productId");
        if (productId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "productId is required"));
        }
        int quantity = body.get("quantity") != null ? Integer.parseInt(body.get("quantity").toString()) : 1;

        // Validate product exists via product-service
        try {
            restTemplate.getForObject(productServiceUrl + "/products/" + productId, Map.class);
        } catch (HttpClientErrorException.NotFound ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "product not found"));
        }

        carts.computeIfAbsent(userId, k -> new ArrayList<>()).add(new CartItem(productId, quantity));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "item added", "userId", userId));
    }

    @DeleteMapping("/cart/{userId}")
    public ResponseEntity<?> clearCart(@PathVariable String userId) {
        carts.remove(userId);
        return ResponseEntity.ok(Map.of("message", "cart cleared", "userId", userId));
    }
}
