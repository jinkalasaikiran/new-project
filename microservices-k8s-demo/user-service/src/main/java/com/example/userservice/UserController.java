package com.example.userservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class UserController {

    // In-memory "database" - swap for a real DB (Postgres, etc.) in production
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UserController() {
        users.put("u1", new User("u1", "Alice", "alice@example.com"));
        users.put("u2", new User("u2", "Bob", "bob@example.com"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "user-service"));
    }

    @GetMapping("/users")
    public ResponseEntity<Iterable<User>> listUsers() {
        return ResponseEntity.ok(users.values());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        User user = users.get(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "user not found"));
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String email = body.get("email");
        if (name == null || email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "name and email are required"));
        }
        String id = UUID.randomUUID().toString().substring(0, 8);
        User user = new User(id, name, email);
        users.put(id, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}
