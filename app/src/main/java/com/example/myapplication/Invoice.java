package com.example.myapplication;

import java.io.Serializable;

public class Invoice implements Serializable {
    private String invoiceNumber;
    private String vendorName;
    private String dateAndTime;
    private String orderId;
    private String orderDetails;
    private String itemPrices;
    private String vatDetails;
    private double totalPrice;
    private double discount;
    private double finalPrice;
    private double cashPayment;
    private double change;
    private String pwdName;
    private String pwdId;
    private String idIssued;
    private String expirationDate;
    private boolean isSenior;

    // Constructor
    public Invoice(String invoiceNumber, String vendorName, String dateAndTime, String orderId, String orderDetails,
                   String itemPrices, String vatDetails, double totalPrice, double discount, double finalPrice,
                   double cashPayment, double change, String pwdName, String pwdId, String idIssued,
                   String expirationDate, boolean isSenior) {
        this.invoiceNumber = invoiceNumber;
        this.vendorName = vendorName;
        this.dateAndTime = dateAndTime;
        this.orderId = orderId;
        this.orderDetails = orderDetails;
        this.itemPrices = itemPrices;
        this.vatDetails = vatDetails;
        this.totalPrice = totalPrice;
        this.discount = discount;
        this.finalPrice = finalPrice;
        this.cashPayment = cashPayment;
        this.change = change;
        this.pwdName = pwdName;
        this.pwdId = pwdId;
        this.idIssued = idIssued;
        this.expirationDate = expirationDate;
        this.isSenior = isSenior;
    }

    // Getters
    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getDateAndTime() {
        return dateAndTime;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getOrderDetails() {
        return orderDetails;
    }

    public String getItemPrices() {
        return itemPrices;
    }

    public String getVatDetails() {
        return vatDetails;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public double getDiscount() {
        return discount;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public double getCashPayment() {
        return cashPayment;
    }

    public double getChange() {
        return change;
    }

    public String getPwdName() {
        return pwdName;
    }

    public String getPwdId() {
        return pwdId;
    }

    public String getIdIssued() {
        return idIssued;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public boolean isSenior() {
        return isSenior;
    }

    // Optional: toString method for debugging
    @Override
    public String toString() {
        return "Invoice{" +
                "invoiceNumber='" + invoiceNumber + '\'' +
                ", vendorName='" + vendorName + '\'' +
                ", dateAndTime='" + dateAndTime + '\'' +
                ", orderId='" + orderId + '\'' +
                ", orderDetails='" + orderDetails + '\'' +
                ", itemPrices='" + itemPrices + '\'' +
                ", vatDetails='" + vatDetails + '\'' +
                ", totalPrice=" + totalPrice +
                ", discount=" + discount +
                ", finalPrice=" + finalPrice +
                ", cashPayment=" + cashPayment +
                ", change=" + change +
                ", pwdName='" + pwdName + '\'' +
                ", pwdId='" + pwdId + '\'' +
                ", idIssued='" + idIssued + '\'' +
                ", expirationDate='" + expirationDate + '\'' +
                ", isSenior=" + isSenior +
                '}';
    }
}