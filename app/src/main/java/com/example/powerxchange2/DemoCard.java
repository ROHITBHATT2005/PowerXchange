package com.example.powerxchange2;

public class DemoCard {
    public long id;
    public String cardNumber, holderName, expiry, cvv, cardType, network, bankName;
    public double balance;

    public DemoCard() {}

    public DemoCard(long id, String cardNumber, String holderName, String expiry, String cvv,
                    String cardType, String network, String bankName, double balance) {
        this.id = id; this.cardNumber = cardNumber; this.holderName = holderName;
        this.expiry = expiry; this.cvv = cvv; this.cardType = cardType;
        this.network = network; this.bankName = bankName; this.balance = balance;
    }

    public String last4() {
        if (cardNumber == null || cardNumber.length() < 4) return cardNumber == null ? "" : cardNumber;
        return cardNumber.substring(cardNumber.length() - 4);
    }

    public String masked() { return "•••• •••• •••• " + last4(); }

    public String displayLabel() {
        return network + " " + cardType + " • " + masked() + " — " + holderName + " (" + bankName + ")";
    }
}