package com.example.powerxchange2;

public class WalletTransaction {
    public long id, userId, referenceId;
    public double amount;
    public String type, description, method, referenceLast4, createdAt;

    public WalletTransaction(long id, long userId, double amount, String type, String description,
                             String method, String referenceLast4, long referenceId, String createdAt) {
        this.id = id; this.userId = userId; this.amount = amount; this.type = type;
        this.description = description; this.method = method;
        this.referenceLast4 = referenceLast4; this.referenceId = referenceId;
        this.createdAt = createdAt;
    }
}