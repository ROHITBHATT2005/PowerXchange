package com.example.powerxchange2;

public class Trade {
    private long id;
    private long listingId;
    private long buyerId;
    private String buyerName;
    private long sellerId;
    private String sellerName;
    private String energyType;
    private String unit;
    private double quantity;
    private double energyPrice;
    private double deliveryFee;
    private double platformCut;
    private double partnerCut;
    private double totalPrice;
    private String deliveryCode;
    private String timestamp;
    private String orderType = "PURCHASE";

    public Trade() {}

    public Trade(long id, long listingId, long buyerId, String buyerName, long sellerId, String sellerName,
                 String energyType, String unit, double quantity, double energyPrice, double deliveryFee,
                 double platformCut, double partnerCut, double totalPrice, String deliveryCode, String timestamp) {
        this.id = id;
        this.listingId = listingId;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.energyType = energyType;
        this.unit = unit;
        this.quantity = quantity;
        this.energyPrice = energyPrice;
        this.deliveryFee = deliveryFee;
        this.platformCut = platformCut;
        this.partnerCut = partnerCut;
        this.totalPrice = totalPrice;
        this.deliveryCode = deliveryCode;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getListingId() { return listingId; }
    public void setListingId(long listingId) { this.listingId = listingId; }

    public long getBuyerId() { return buyerId; }
    public void setBuyerId(long buyerId) { this.buyerId = buyerId; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public long getSellerId() { return sellerId; }
    public void setSellerId(long sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getEnergyType() { return energyType; }
    public void setEnergyType(String energyType) { this.energyType = energyType; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public double getEnergyPrice() { return energyPrice; }
    public void setEnergyPrice(double energyPrice) { this.energyPrice = energyPrice; }

    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }

    public double getPlatformCut() { return platformCut; }
    public void setPlatformCut(double platformCut) { this.platformCut = platformCut; }

    public double getPartnerCut() { return partnerCut; }
    public void setPartnerCut(double partnerCut) { this.partnerCut = partnerCut; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getDeliveryCode() { return deliveryCode; }
    public void setDeliveryCode(String deliveryCode) { this.deliveryCode = deliveryCode; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public boolean isBarter() { return "BARTER".equalsIgnoreCase(orderType); }
}