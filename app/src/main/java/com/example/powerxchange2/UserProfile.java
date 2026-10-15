package com.example.powerxchange2;

public class UserProfile {
    private long id;
    private String name;
    private String email;
    private String phone;
    private String passwordHash;
    private String role;
    private String userType;
    private String companyName;
    private String cityLocation;
    private double walletBalance;
    private double tradedKwh;
    private double co2Offset;
    private double moneySaved;
    private String createdAt;

    public UserProfile() {}

    public UserProfile(long id, String name, String email, String phone, String passwordHash, String role,
                       String userType, String companyName, String cityLocation, double walletBalance,
                       double tradedKwh, double co2Offset, double moneySaved, String createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.userType = userType;
        this.companyName = companyName;
        this.cityLocation = cityLocation;
        this.walletBalance = walletBalance;
        this.tradedKwh = tradedKwh;
        this.co2Offset = co2Offset;
        this.moneySaved = moneySaved;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCityLocation() { return cityLocation; }
    public void setCityLocation(String cityLocation) { this.cityLocation = cityLocation; }

    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double walletBalance) { this.walletBalance = walletBalance; }

    public double getTradedKwh() { return tradedKwh; }
    public void setTradedKwh(double tradedKwh) { this.tradedKwh = tradedKwh; }

    public double getCo2Offset() { return co2Offset; }
    public void setCo2Offset(double co2Offset) { this.co2Offset = co2Offset; }

    public double getMoneySaved() { return moneySaved; }
    public void setMoneySaved(double moneySaved) { this.moneySaved = moneySaved; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isProsumer() {
        return "PROSUMER".equalsIgnoreCase(role);
    }

    public boolean isConsumer() {
        return "CONSUMER".equalsIgnoreCase(role);
    }

    public boolean isDelivery() {
        return "DELIVERY".equalsIgnoreCase(role);
    }

    public boolean isCorporate() {
        return "CORPORATE".equalsIgnoreCase(userType);
    }
}