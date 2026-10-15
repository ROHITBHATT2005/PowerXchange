package com.example.powerxchange2;

public class AutoTradeRule {
    private long id, userId;
    private String energyType;
    private double maxPrice, maxQty;
    private boolean active;
    private String createdAt;

    public AutoTradeRule() {}
    public AutoTradeRule(long id, long userId, String energyType, double maxPrice,double maxQty, boolean active, String createdAt) {
        this.id = id; this.userId = userId; this.energyType = energyType;
        this.maxPrice = maxPrice; this.maxQty = maxQty;
        this.active = active; this.createdAt = createdAt;
    }
    public long getId(){
        return id;
    }
    public void setId(long id){
        this.id = id;
    }
    public long getUserId(){
        return userId;
    }
    public void setUserId(long userId){
        this.userId = userId;
    }
    public String getEnergyType(){
        return energyType;
    }
    public void setEnergyType(String t){
        this.energyType = t;
    }
    public double getMaxPrice(){
        return maxPrice;
    }
    public void setMaxPrice(double p){
        this.maxPrice = p;
    }
    public double getMaxQty(){
        return maxQty;
    }
    public void setMaxQty(double q){
        this.maxQty = q;
    }
    public boolean isActive(){
        return active;
    }
    public void setActive(boolean a){
        this.active = a;
    }
    public String getCreatedAt(){
        return createdAt;
    }
    public void setCreatedAt(String c){
        this.createdAt = c;
    }
}