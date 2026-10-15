package com.example.powerxchange2;

public class AutoBuyHistory {
    public long id, userId, listingId;
    public String sellerName, energyType, unit, status, createdAt;
    public double quantity, pricePaid;

    public AutoBuyHistory(long id, long userId, long listingId, String sellerName, String energyType,
                          double quantity, String unit, double pricePaid, String status, String createdAt) {
        this.id = id; this.userId = userId; this.listingId = listingId; this.sellerName = sellerName;
        this.energyType = energyType; this.quantity = quantity; this.unit = unit;
        this.pricePaid = pricePaid; this.status = status; this.createdAt = createdAt;
    }

    public boolean isBought() { return "BOUGHT".equalsIgnoreCase(status); }
}