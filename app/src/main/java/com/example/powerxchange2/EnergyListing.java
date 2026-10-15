package com.example.powerxchange2;

public class EnergyListing {
    private long id;
    private long sellerId;
    private String sellerName;
    private String sellerType;
    private String companyName;
    private String energyType;
    private String unit;
    private double quantity;
    private double pricePerUnit;
    private String location;
    private String status;
    private String createdAt;

    public EnergyListing() {}

    public EnergyListing(long id, long sellerId, String sellerName, String sellerType, String companyName,
                         String energyType, String unit, double quantity, double pricePerUnit,
                         String location, String status, String createdAt) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.sellerType = sellerType;
        this.companyName = companyName;
        this.energyType = energyType;
        this.unit = unit;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.location = location;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSellerId() { return sellerId; }
    public void setSellerId(long sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getSellerType() { return sellerType; }
    public void setSellerType(String sellerType) { this.sellerType = sellerType; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getEnergyType() { return energyType; }
    public void setEnergyType(String energyType) { this.energyType = energyType; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public double getKwh() { return quantity; }

    public double getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(double pricePerUnit) { this.pricePerUnit = pricePerUnit; }

    public double getPricePerKwh() { return pricePerUnit; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public double getTotalPrice() {
        return quantity * pricePerUnit;
    }

    public boolean isCorporate() {
        return "CORPORATE".equalsIgnoreCase(sellerType);
    }
}