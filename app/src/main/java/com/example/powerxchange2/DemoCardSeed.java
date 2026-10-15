package com.example.powerxchange2;

import java.util.Arrays;
import java.util.List;

public class DemoCardSeed {

    public static class Seed {
        public final String cardNumber, holderName, expiry, cvv, cardType, network, bankName;
        public final double balance;

        public Seed(String cardNumber, String holderName, String expiry, String cvv,
                    String cardType, String network, String bankName, double balance) {
            this.cardNumber = cardNumber; this.holderName = holderName; this.expiry = expiry;
            this.cvv = cvv; this.cardType = cardType; this.network = network;
            this.bankName = bankName; this.balance = balance;
        }
    }

    public static List<Seed> cards() {
        double b = 1000000.00;
        return Arrays.asList(
                new Seed("4111111111111111", "Rohit Bhatt",  "12/30", "123", "CREDIT", "VISA",       "HDFC Bank",  b),
                new Seed("5555555555554444", "Sayan Biswas", "11/29", "456", "CREDIT", "MASTERCARD", "ICICI Bank", b),
                new Seed("4012888888881881", "Ria Rana",     "10/31", "789", "DEBIT",  "VISA",       "SBI",        b),
                new Seed("5200828282828210", "Rahul Sharma", "09/30", "321", "DEBIT",  "MASTERCARD", "Axis Bank",  b)
        );
    }
}