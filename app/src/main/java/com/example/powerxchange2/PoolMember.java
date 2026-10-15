package com.example.powerxchange2;

public class PoolMember {
    private long id, poolId, userId;
    private String userName;
    private double qty, amountPaid, deliveryFee;
    private String deliveryCode, status;

    public PoolMember() {}

    public PoolMember(long id, long poolId, long userId, String userName, double qty,
                      double amountPaid, double deliveryFee, String deliveryCode, String status) {
        this.id = id; this.poolId = poolId; this.userId = userId; this.userName = userName;
        this.qty = qty; this.amountPaid = amountPaid; this.deliveryFee = deliveryFee;
        this.deliveryCode = deliveryCode; this.status = status;
    }
    public long getId() { return id; }
    public long getPoolId() { return poolId; }
    public long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public double getQty() { return qty; }
    public double getAmountPaid() { return amountPaid; }
    public double getDeliveryFee() { return deliveryFee; }
    public String getDeliveryCode() { return deliveryCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}