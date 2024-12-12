package com.example.myapplication.Model;

public class SalesModel {

    String Product, Image, productId;
    double Price;
    int Quantity;
    long Timestamp;

    public SalesModel() {
    }

    public SalesModel(String productId, String product, String image, double price, int quantity, long timestamp) {
        this.productId = productId;
        Product = product;
        Image = image;
        Price = price;
        Quantity = quantity;
        Timestamp = timestamp;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProduct() {
        return Product;
    }

    public void setProduct(String product) {
        Product = product;
    }

    public String getImage() {
        return Image;
    }

    public void setImage(String image) {
        Image = image;
    }

    public double getPrice() {
        return Price;
    }

    public void setPrice(double price) {
        Price = price;
    }

    public int getQuantity() {
        return Quantity;
    }

    public void setQuantity(int quantity) {
        Quantity = quantity;
    }

    public long getTimestamp() {
        return Timestamp;
    }

    public void setTimestamp(long timestamp) {
        Timestamp = timestamp;
    }
}
