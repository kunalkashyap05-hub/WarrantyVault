package com.example.warrantyvault;

import java.io.Serializable;

public class Product implements Serializable {
    private String productId;
    private String userId;
    private String productName;
    private String brand;
    private String category;
    private String modelNumber;
    private String serialNumber;
    private String purchaseDate;
    private String purchasePrice;
    private String warrantyDuration;
    private String warrantyExpiry;
    private String warrantyStatus;
    private String productImageUrl;
    private String billImageUrl;

    public Product() {
        // Required for Firestore
    }

    public Product(String productId, String userId, String productName, String brand, String category,
                   String modelNumber, String serialNumber, String purchaseDate, String purchasePrice,
                   String warrantyDuration, String warrantyExpiry, String warrantyStatus,
                   String productImageUrl, String billImageUrl) {
        this.productId = productId;
        this.userId = userId;
        this.productName = productName;
        this.brand = brand;
        this.category = category;
        this.modelNumber = modelNumber;
        this.serialNumber = serialNumber;
        this.purchaseDate = purchaseDate;
        this.purchasePrice = purchasePrice;
        this.warrantyDuration = warrantyDuration;
        this.warrantyExpiry = warrantyExpiry;
        this.warrantyStatus = warrantyStatus;
        this.productImageUrl = productImageUrl;
        this.billImageUrl = billImageUrl;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getModelNumber() { return modelNumber; }
    public void setModelNumber(String modelNumber) { this.modelNumber = modelNumber; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(String purchasePrice) { this.purchasePrice = purchasePrice; }

    public String getWarrantyDuration() { return warrantyDuration; }
    public void setWarrantyDuration(String warrantyDuration) { this.warrantyDuration = warrantyDuration; }

    public String getWarrantyExpiry() { return warrantyExpiry; }
    public void setWarrantyExpiry(String warrantyExpiry) { this.warrantyExpiry = warrantyExpiry; }

    public String getWarrantyStatus() { return warrantyStatus; }
    public void setWarrantyStatus(String warrantyStatus) { this.warrantyStatus = warrantyStatus; }

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }

    public String getBillImageUrl() { return billImageUrl; }
    public void setBillImageUrl(String billImageUrl) { this.billImageUrl = billImageUrl; }
}
