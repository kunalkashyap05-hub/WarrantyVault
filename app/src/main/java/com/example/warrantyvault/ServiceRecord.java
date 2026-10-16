package com.example.warrantyvault;

import java.io.Serializable;

public class ServiceRecord implements Serializable {
    private String serviceId;
    private String userId;
    private String productId;
    private String productName;
    private String serviceDate;
    private String serviceCentre;
    private String description;
    private String cost;
    private String remarks;

    public ServiceRecord() {}

    public ServiceRecord(String serviceId, String userId, String productId, String productName,
                         String serviceDate, String serviceCentre, String description, String cost, String remarks) {
        this.serviceId = serviceId;
        this.userId = userId;
        this.productId = productId;
        this.productName = productName;
        this.serviceDate = serviceDate;
        this.serviceCentre = serviceCentre;
        this.description = description;
        this.cost = cost;
        this.remarks = remarks;
    }

    public String getServiceId() { return serviceId; }
    public String getUserId() { return userId; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getServiceDate() { return serviceDate; }
    public String getServiceCentre() { return serviceCentre; }
    public String getDescription() { return description; }
    public String getCost() { return cost; }
    public String getRemarks() { return remarks; }
}
