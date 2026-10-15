package com.example.powerxchange2;

public class EnergyPool {
    private long id, listingId, creatorId;
    private String creatorName;
    private double totalQty, joinedQty;
    private String unit;
    private double pricePerUnit;
    private String status, createdAt, expiresAt;
    private String energyType, sellerName, location;

    public EnergyPool() {}

    public EnergyPool(long id, long listingId, long creatorId, String creatorName,
                      double totalQty, double joinedQty, String unit, double pricePerUnit,
                      String status, String createdAt, String expiresAt,
                      String energyType, String sellerName, String location) {
        this.id = id; this.listingId = listingId; this.creatorId = creatorId;
        this.creatorName = creatorName; this.totalQty = totalQty; this.joinedQty = joinedQty;
        this.unit = unit; this.pricePerUnit = pricePerUnit; this.status = status;
        this.createdAt = createdAt; this.expiresAt = expiresAt;
        this.energyType = energyType; this.sellerName = sellerName; this.location = location;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getListingId() { return listingId; }
    public void setListingId(long id) { this.listingId = id; }
    public long getCreatorId() { return creatorId; }
    public void setCreatorId(long id) { this.creatorId = id; }
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String n) { this.creatorName = n; }
    public double getTotalQty() { return totalQty; }
    public void setTotalQty(double q) { this.totalQty = q; }
    public double getJoinedQty() { return joinedQty; }
    public void setJoinedQty(double q) { this.joinedQty = q; }
    public double getRemainingQty() { return Math.max(0, totalQty - joinedQty); }
    public String getUnit() { return unit; }
    public void setUnit(String u) { this.unit = u; }
    public double getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(double p) { this.pricePerUnit = p; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String c) { this.createdAt = c; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String e) { this.expiresAt = e; }
    public String getEnergyType() { return energyType; }
    public void setEnergyType(String t) { this.energyType = t; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String s) { this.sellerName = s; }
    public String getLocation() { return location; }
    public void setLocation(String l) { this.location = l; }

    public int getProgressPercentage() {
        if (totalQty <= 0) return 0;
        int pct = (int) Math.round((joinedQty / totalQty) * 100);
        return Math.min(100, Math.max(0, pct));
    }

    public boolean isOpen() { return "OPEN".equalsIgnoreCase(status); }
    public boolean isFull() { return "FULL".equalsIgnoreCase(status); }
    public boolean isClosed() { return "CLOSED".equalsIgnoreCase(status); }
    public boolean isExpired() { return "EXPIRED".equalsIgnoreCase(status); }
}