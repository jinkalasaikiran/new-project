package com.example.orderservice;

import java.util.List;
import java.util.Map;

public class Order {
    private String id;
    private String userId;
    private List<Map<String, Object>> items;
    private double total;
    private String status;

    public Order() {
    }

    public Order(String id, String userId, List<Map<String, Object>> items, double total, String status) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.total = total;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<Map<String, Object>> getItems() {
        return items;
    }

    public void setItems(List<Map<String, Object>> items) {
        this.items = items;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
