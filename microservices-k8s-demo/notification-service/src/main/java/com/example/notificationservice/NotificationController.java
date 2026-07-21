package com.example.notificationservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
public class NotificationController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotificationController.class);

    // In-memory history of sent notifications, useful for demo/debugging
    private final List<Map<String, String>> history = new CopyOnWriteArrayList<>();

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "notification-service"));
    }

    @PostMapping("/notify")
    public ResponseEntity<?> notify(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String message = body.get("message");
        if (userId == null || message == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId and message are required"));
        }

        // Simulate sending an email/SMS/push notification. Swap this for a real
        // provider integration (SES, Twilio, FCM, etc.) in production.
        Map<String, String> record = new LinkedHashMap<>();
        record.put("userId", userId);
        record.put("message", message);
        record.put("sentAt", Instant.now().toString());
        history.add(record);

        log.info("Notification sent to user {}: {}", userId, message);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "sent", "userId", userId));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Map<String, String>>> listNotifications() {
        return ResponseEntity.ok(history);
    }
}
