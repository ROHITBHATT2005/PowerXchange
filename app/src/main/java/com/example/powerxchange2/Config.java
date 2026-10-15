package com.example.powerxchange2;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Config {
    public static final double INITIAL_WALLET_BALANCE = 0.0;
    public static final double BASE_DELIVERY_FEE = 15.0;
    public static final double PER_KM_RATE = 3.0;
    public static final double PLATFORM_CUT_RATIO = 0.20;
    public static final double PARTNER_CUT_RATIO = 0.80;
    public static final double DEFAULT_DISTANCE_KM = 5.0;
    public static final double SAME_CITY_DISTANCE_KM  = 5.0;
    public static final double CROSS_CITY_DISTANCE_KM = 15.0;

    public static final double MARKET_SOLAR   = 2.34;
    public static final double MARKET_WIND    = 3.85;
    public static final double MARKET_BATTERY = 2.00;
    public static final double MARKET_BIOGAS  = 69.00;

    public static final double CO2_SOLAR_PER_KWH   = 0.65;
    public static final double CO2_WIND_PER_KWH    = 0.69;
    public static final double CO2_BATTERY_PER_KWH = 0.55;
    public static final double CO2_BIOGAS_PER_M3   = 1.00;

    public static final double MIN_ADD_CREDITS = 10.0;
    public static final double MIN_WITHDRAW    = 100.0;

    public static final long POOL_EXPIRY_MS = 24L * 60 * 60 * 1000;

    public static class CompanyInfo {
        public String name, location;
        public CompanyInfo(String name, String location) { this.name = name; this.location = location; }
        @Override public String toString() { return name + " (" + location + ")"; }
    }

    public static final List<CompanyInfo> INDIAN_COMPANIES = Arrays.asList(
            new CompanyInfo("Tata Power", "Mumbai"),
            new CompanyInfo("Adani Green Energy", "Ahmedabad"),
            new CompanyInfo("ReNew Power", "Gurugram"),
            new CompanyInfo("NTPC Renewable Energy", "Noida"),
            new CompanyInfo("JSW Energy", "Mumbai"),
            new CompanyInfo("Suzlon Energy", "Pune"),
            new CompanyInfo("Greenko", "Hyderabad"),
            new CompanyInfo("Azure Power", "New Delhi"),
            new CompanyInfo("Torrent Power", "Ahmedabad"),
            new CompanyInfo("Waaree Energies", "Surat")
    );

    public static String formatCurrency(double amount){
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        String formatted = nf.format(amount);
        formatted = formatted.replace("INR", "").replace("Rs.", "").trim();
        if (!formatted.startsWith("₹")) formatted = "₹" + formatted;
        return formatted;
    }

    public static String formatPricePerUnit(double price, String energyType) {
        String unit = getUnitForEnergyType(energyType);
        return String.format(Locale.US, "₹%.2f/%s", price, unit);
    }

    public static String hashPassword(String plainText) {
        if (plainText == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) { return plainText; }
    }

    public static String getUnitForEnergyType(String energyType) {
        if (energyType != null && energyType.equalsIgnoreCase("Biogas")) return "m³";
        return "kWh";
    }

    public static double getMarketPrice(String energyType) {
        if (energyType == null) return MARKET_SOLAR;
        switch (energyType) {
            case "Wind": return MARKET_WIND;
            case "Battery": return MARKET_BATTERY;
            case "Biogas": return MARKET_BIOGAS;
            default: return MARKET_SOLAR;
        }
    }
}