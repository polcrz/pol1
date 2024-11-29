package com.example.myapplication.Model;

public class InventoryModel {

    String Product, Image;
    double Price;

    int Quantity;

    public InventoryModel() {
    }

    public InventoryModel(String product, String image, double price, int quantity) {
        Product = product;
        Image = image;
        Price = price;
        Quantity = quantity;
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
}