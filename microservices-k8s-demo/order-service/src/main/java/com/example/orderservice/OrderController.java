package com.example.orderservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class OrderController {

    private final RestTemplate restTemplate;

    // In-memory "database" of orders
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    public OrderController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "order-service"));
    }

    @GetMapping("/orders")
    public ResponseEntity<Iterable<Order>> listOrders() {
        return ResponseEntity.ok(orders.values());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        Order order = orders.get(id);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "order not found"));
        }
        return ResponseEntity.ok(order);
    }

    @PostMapping("/orders")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body) {
        String userId = (String) body.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }

        // 1. Validate the user exists via user-service
        try {
            restTemplate.getForObject(userServiceUrl + "/users/" + userId, Map.class);
        } catch (HttpClientErrorException.NotFound ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "user not found"));
        }

        // 2. Fetch the user's cart via cart-service
        Map<String, Object> cart = restTemplate.getForObject(cartServiceUrl + "/cart/" + userId, Map.class);
        List<Map<String, Object>> items = cart != null
                ? (List<Map<String, Object>>) cart.getOrDefault("items", List.of())
                : List.of();
        if (items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "cart is empty, cannot place order"));
        }
        double total = ((Number) cart.getOrDefault("total", 0.0)).doubleValue();

        // 3. Create the order record
        String orderId = UUID.randomUUID().toString().substring(0, 8);
        Order order = new Order(orderId, userId, items, total, "CONFIRMED");
        orders.put(orderId, order);

        // 4. Clear the cart now that the order is placed
        restTemplate.delete(cartServiceUrl + "/cart/" + userId);

        // 5. Fire-and-forget notification (best-effort; order still succeeds if this fails)
        try {
            Map<String, String> notification = Map.of(
                    "userId", userId,
                    "message", "Your order " + orderId + " has been confirmed. Total: $" + total
            );
            restTemplate.postForObject(notificationServiceUrl + "/notify", notification, Map.class);
        } catch (Exception ex) {
            // Notification failures should not block order creation
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
