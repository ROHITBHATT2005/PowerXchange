package com.example.powerxchange2;

public class BarterOffer {
    private long id;
    private long creatorId;
    private String creatorName;
    private String giveType;
    private double giveQty;
    private String giveUnit;
    private String wantType;
    private double wantQty;
    private String wantUnit;
    private String note;
    private String location;
    private long accepterId;
    private String accepterName;
    private String status;
    private String createdAt;
    private String acceptedAt;

    public BarterOffer() {}

    public BarterOffer(long id, long creatorId, String creatorName, String giveType, double giveQty,
                       String giveUnit, String wantType, double wantQty, String wantUnit,
                       String note, String location, long accepterId, String accepterName,
                       String status, String createdAt) {
        this.id = id;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.giveType = giveType;
        this.giveQty = giveQty;
        this.giveUnit = giveUnit;
        this.wantType = wantType;
        this.wantQty = wantQty;
        this.wantUnit = wantUnit;
        this.note = note;
        this.location = location;
        this.accepterId = accepterId;
        this.accepterName = accepterName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getCreatorId() { return creatorId; }
    public void setCreatorId(long creatorId) { this.creatorId = creatorId; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getGiveType() { return giveType; }
    public void setGiveType(String giveType) { this.giveType = giveType; }

    public double getGiveQty() { return giveQty; }
    public void setGiveQty(double giveQty) { this.giveQty = giveQty; }

    public String getGiveUnit() { return giveUnit; }
    public void setGiveUnit(String giveUnit) { this.giveUnit = giveUnit; }

    public String getWantType() { return wantType; }
    public void setWantType(String wantType) { this.wantType = wantType; }

    public double getWantQty() { return wantQty; }
    public void setWantQty(double wantQty) { this.wantQty = wantQty; }

    public String getWantUnit() { return wantUnit; }
    public void setWantUnit(String wantUnit) { this.wantUnit = wantUnit; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public long getAccepterId() { return accepterId; }
    public void setAccepterId(long accepterId) { this.accepterId = accepterId; }

    public String getAccepterName() { return accepterName; }
    public void setAccepterName(String accepterName) { this.accepterName = accepterName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(String acceptedAt) { this.acceptedAt = acceptedAt; }
}