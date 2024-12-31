package com.example.myapplication.Model;


public class LogEntry {
    private String actionType;
    private String productName;
    private String user;
    private long timestamp;
    private String details;

    // Constructor, getters, and setters
    public LogEntry() {
    }

    public LogEntry(String actionType, String productName, String user, long timestamp, String details) {
        this.actionType = actionType;
        this.productName = productName;
        this.user = user;
        this.timestamp = timestamp;
        this.details = details;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
