package com.example.powerxchange2;

public class DeliveryTask {
    private long id, tradeId;
    private String energyType, unit, sellerName, pickupAddress, buyerName, dropAddress;
    private double quantity, distanceKm, deliveryFeeTotal, platformCut, partnerCut;
    private String status, deliveryCode, createdAt, acceptedAt, pickedUpAt, deliveredAt;
    private long partnerId;

    // R2-11 additions
    private long barterId;
    private int legNo;
    private String orderType = "PURCHASE";
    private long senderId;
    private long receiverId;

    public DeliveryTask() {}

    public DeliveryTask(long id, long tradeId, String energyType, double quantity, String unit,
                        String sellerName, String pickupAddress, String buyerName, String dropAddress,
                        double distanceKm, double deliveryFeeTotal, double platformCut, double partnerCut,
                        String status, long partnerId, String deliveryCode,
                        String createdAt, String acceptedAt, String pickedUpAt, String deliveredAt) {
        this.id = id; this.tradeId = tradeId; this.energyType = energyType; this.quantity = quantity;
        this.unit = unit; this.sellerName = sellerName; this.pickupAddress = pickupAddress;
        this.buyerName = buyerName; this.dropAddress = dropAddress; this.distanceKm = distanceKm;
        this.deliveryFeeTotal = deliveryFeeTotal; this.platformCut = platformCut;
        this.partnerCut = partnerCut; this.status = status; this.partnerId = partnerId;
        this.deliveryCode = deliveryCode; this.createdAt = createdAt;
        this.acceptedAt = acceptedAt; this.pickedUpAt = pickedUpAt; this.deliveredAt = deliveredAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getTradeId() { return tradeId; }
    public void setTradeId(long t) { this.tradeId = t; }
    public String getEnergyType() { return energyType; }
    public void setEnergyType(String e) { this.energyType = e; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double q) { this.quantity = q; }
    public String getUnit() { return unit; }
    public void setUnit(String u) { this.unit = u; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String s) { this.sellerName = s; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String a) { this.pickupAddress = a; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String b) { this.buyerName = b; }
    public String getDropAddress() { return dropAddress; }
    public void setDropAddress(String d) { this.dropAddress = d; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double d) { this.distanceKm = d; }
    public double getDeliveryFeeTotal() { return deliveryFeeTotal; }
    public void setDeliveryFeeTotal(double d) { this.deliveryFeeTotal = d; }
    public double getPlatformCut() { return platformCut; }
    public void setPlatformCut(double p) { this.platformCut = p; }
    public double getPartnerCut() { return partnerCut; }
    public void setPartnerCut(double p) { this.partnerCut = p; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public long getPartnerId() { return partnerId; }
    public void setPartnerId(long p) { this.partnerId = p; }
    public String getDeliveryCode() { return deliveryCode; }
    public void setDeliveryCode(String c) { this.deliveryCode = c; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String c) { this.createdAt = c; }
    public String getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(String a) { this.acceptedAt = a; }
    public String getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(String p) { this.pickedUpAt = p; }
    public String getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(String d) { this.deliveredAt = d; }

    public long getBarterId() { return barterId; }
    public void setBarterId(long v) { this.barterId = v; }
    public int getLegNo() { return legNo; }
    public void setLegNo(int v) { this.legNo = v; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String v) { this.orderType = v; }
    public boolean isBarter() { return "BARTER".equalsIgnoreCase(orderType); }
    public long getSenderId() { return senderId; }
    public void setSenderId(long v) { this.senderId = v; }
    public long getReceiverId() { return receiverId; }
    public void setReceiverId(long v) { this.receiverId = v; }
}