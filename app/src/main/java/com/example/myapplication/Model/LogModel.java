package com.example.myapplication.Model;

public class LogModel {
    private String action;
    private String productName;
    private String userUID;
    private int quantity;
    private double price;
    private String timestamp;

    public LogModel() {
        // Default constructor required for calls to DataSnapshot.getValue(LogModel.class)
    }

    public LogModel(String action, String productName, String userUID, int quantity, double price, String timestamp) {
        this.action = action;
        this.productName = productName;
        this.userUID = userUID;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = timestamp;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getUserUID() {
        return userUID;
    }

    public void setUserUID(String userUID) {
        this.userUID = userUID;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
