package com.example.powerxchange2;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SQLiteRepository {
    private static final String PREF_NAME = "powerxchange_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";

    private final DatabaseHelper dbHelper;
    private final SharedPreferences prefs;
    private final WalletRepository walletRepo;

    public SQLiteRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context.getApplicationContext());
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.walletRepo = new WalletRepository(this.dbHelper);
    }

    public WalletRepository getWalletRepository() { return walletRepo; }
    public DatabaseHelper getDbHelper() { return dbHelper; }

    // ============================================================
    // SESSION
    // ============================================================
    public void saveUserSession(UserProfile user) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true)
                .putLong(KEY_USER_ID, user.getId()).apply();
    }
    public boolean isLoggedIn() {
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) return false;
        long userId = prefs.getLong(KEY_USER_ID, -1);
        return getUserById(userId) != null;
    }
    public long getLoggedInUserId() { return prefs.getLong(KEY_USER_ID, -1); }
    public UserProfile getLoggedInUser() {
        long id = getLoggedInUserId();
        return id != -1 ? getUserById(id) : null;
    }
    public void logout() { prefs.edit().clear().apply(); }

    // ============================================================
    // USERS
    // ============================================================
    public boolean registerUser(String name, String email, String phone, String password,
                                String role, String userType, String companyName, String cityLocation) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM " + DatabaseHelper.TABLE_USERS +
                " WHERE LOWER(email) = ?", new String[]{email.toLowerCase().trim()});
        if (c.moveToFirst()) { c.close(); return false; }
        c.close();

        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("email", email.toLowerCase().trim());
        cv.put("phone", phone);
        cv.put("password_hash", Config.hashPassword(password));
        cv.put("role", role);
        cv.put("user_type", userType != null ? userType : "INDIVIDUAL");
        cv.put("company_name", companyName != null ? companyName : "");
        cv.put("city_location", cityLocation != null ? cityLocation : "Mumbai");
        cv.put("wallet_balance", Config.INITIAL_WALLET_BALANCE);
        cv.put("traded_kwh", 0.0);
        cv.put("co2_offset", 0.0);
        cv.put("money_saved", 0.0);
        cv.put("created_at", today());
        return db.insert(DatabaseHelper.TABLE_USERS, null, cv) != -1;
    }

    public UserProfile loginUser(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_USERS +
                        " WHERE LOWER(email) = ? AND password_hash = ?",
                new String[]{email.toLowerCase().trim(), Config.hashPassword(password)});
        UserProfile u = null;
        if (c.moveToFirst()) u = cursorToUser(c);
        c.close();
        return u;
    }

    public UserProfile getUserById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_USERS + " WHERE id = ?",
                new String[]{String.valueOf(id)});
        UserProfile u = null;
        if (c.moveToFirst()) u = cursorToUser(c);
        c.close();
        return u;
    }

    public void updateWalletBalance(long userId, double newBalance) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("wallet_balance", newBalance);
        db.update(DatabaseHelper.TABLE_USERS, cv, "id = ?", new String[]{String.valueOf(userId)});
    }

    private UserProfile cursorToUser(Cursor c) {
        return new UserProfile(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getString(c.getColumnIndexOrThrow("email")),
                c.getString(c.getColumnIndexOrThrow("phone")),
                c.getString(c.getColumnIndexOrThrow("password_hash")),
                c.getString(c.getColumnIndexOrThrow("role")),
                c.getString(c.getColumnIndexOrThrow("user_type")),
                c.getString(c.getColumnIndexOrThrow("company_name")),
                c.getString(c.getColumnIndexOrThrow("city_location")),
                c.getDouble(c.getColumnIndexOrThrow("wallet_balance")),
                c.getDouble(c.getColumnIndexOrThrow("traded_kwh")),
                c.getDouble(c.getColumnIndexOrThrow("co2_offset")),
                0.0,
                c.getString(c.getColumnIndexOrThrow("created_at"))
        );
    }

    // ============================================================
    // LISTINGS
    // ============================================================
    public List<EnergyListing> getAllListings(String energyTypeFilter, String sellerTypeFilter) {
        List<EnergyListing> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder q = new StringBuilder("SELECT * FROM " + DatabaseHelper.TABLE_LISTINGS +
                " WHERE status='AVAILABLE' AND quantity > 0");
        List<String> args = new ArrayList<>();
        if (energyTypeFilter != null && !energyTypeFilter.equalsIgnoreCase("ALL")) {
            q.append(" AND LOWER(energy_type) = ?");
            args.add(energyTypeFilter.toLowerCase());
        }
        if (sellerTypeFilter != null && !sellerTypeFilter.equalsIgnoreCase("ALL")) {
            q.append(" AND LOWER(seller_type) = ?");
            args.add(sellerTypeFilter.toLowerCase());
        }
        q.append(" ORDER BY id DESC");
        Cursor c = db.rawQuery(q.toString(), args.isEmpty() ? null : args.toArray(new String[0]));
        while (c.moveToNext()) list.add(cursorToListing(c));
        c.close();
        return list;
    }

    public EnergyListing getListingById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_LISTINGS + " WHERE id = ?",
                new String[]{String.valueOf(id)});
        EnergyListing l = null;
        if (c.moveToFirst()) l = cursorToListing(c);
        c.close();
        return l;
    }

    public boolean addListing(UserProfile seller, String energyType, double quantity,
                              double pricePerUnit, String location) {
        if (seller == null || !seller.isProsumer()) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String unit = Config.getUnitForEnergyType(energyType);
        String createdAt = today();
        String sellerName = seller.isCorporate() && !TextUtils.isEmpty(seller.getCompanyName())
                ? seller.getCompanyName() : seller.getName();

        ContentValues cv = new ContentValues();
        cv.put("seller_id", seller.getId());
        cv.put("seller_name", sellerName);
        cv.put("seller_type", seller.getUserType());
        cv.put("company_name", seller.getCompanyName());
        cv.put("energy_type", energyType);
        cv.put("unit", unit);
        cv.put("quantity", quantity);
        cv.put("price_per_unit", pricePerUnit);
        cv.put("location", location);
        cv.put("status", "AVAILABLE");
        cv.put("created_at", createdAt);
        long id = db.insert(DatabaseHelper.TABLE_LISTINGS, null, cv);
        if (id == -1) return false;

        EnergyListing listing = new EnergyListing(id, seller.getId(), sellerName, seller.getUserType(),
                seller.getCompanyName(), energyType, unit, quantity, pricePerUnit, location,
                "AVAILABLE", createdAt);
        evaluateAutoRulesForNewListing(listing);
        return true;
    }

    public void reduceListingQuantity(long listingId, double amount) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT quantity FROM " + DatabaseHelper.TABLE_LISTINGS +
                " WHERE id=?", new String[]{String.valueOf(listingId)});
        double q = 0;
        if (c.moveToFirst()) q = c.getDouble(0);
        c.close();
        double newQ = q - amount;
        ContentValues cv = new ContentValues();
        if (newQ <= 0.0001) {
            cv.put("quantity", 0.0);
            cv.put("status", "TRADED");
        } else {
            cv.put("quantity", newQ);
        }
        db.update(DatabaseHelper.TABLE_LISTINGS, cv, "id=?", new String[]{String.valueOf(listingId)});
    }

    public void markListingTraded(long listingId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", "TRADED");
        cv.put("quantity", 0.0);
        db.update(DatabaseHelper.TABLE_LISTINGS, cv, "id=?", new String[]{String.valueOf(listingId)});
    }

    private EnergyListing cursorToListing(Cursor c) {
        return new EnergyListing(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getLong(c.getColumnIndexOrThrow("seller_id")),
                c.getString(c.getColumnIndexOrThrow("seller_name")),
                c.getString(c.getColumnIndexOrThrow("seller_type")),
                c.getString(c.getColumnIndexOrThrow("company_name")),
                c.getString(c.getColumnIndexOrThrow("energy_type")),
                c.getString(c.getColumnIndexOrThrow("unit")),
                c.getDouble(c.getColumnIndexOrThrow("quantity")),
                c.getDouble(c.getColumnIndexOrThrow("price_per_unit")),
                c.getString(c.getColumnIndexOrThrow("location")),
                c.getString(c.getColumnIndexOrThrow("status")),
                c.getString(c.getColumnIndexOrThrow("created_at"))
        );
    }

    // ============================================================
    // PURCHASE
    // ============================================================
    public static class TradeResult {
        public boolean success;
        public String errorMessage;
        public String deliveryCode;
        public Trade trade;
        public TradeResult(boolean success, String errorMessage, String deliveryCode, Trade trade) {
            this.success = success; this.errorMessage = errorMessage;
            this.deliveryCode = deliveryCode; this.trade = trade;
        }
    }

    public TradeResult executePurchase(UserProfile buyer, EnergyListing listing, double distanceKm) {
        return executePartialPurchase(buyer, listing, listing.getQuantity(), distanceKm, false);
    }

    public TradeResult executePartialPurchase(UserProfile buyer, EnergyListing listing,
                                              double qtyToBuy, double distanceKm, boolean isAutoBuy) {
        if (buyer == null || listing == null || qtyToBuy <= 0)
            return new TradeResult(false, "Invalid data", null, null);
        if (buyer.getId() == listing.getSellerId())
            return new TradeResult(false, "Cannot buy your own listing.", null, null);
        if (qtyToBuy > listing.getQuantity() + 0.0001)
            return new TradeResult(false, "Not enough quantity available.", null, null);

        double energyPrice = qtyToBuy * listing.getPricePerUnit();
        double deliveryFeeTotal = Config.BASE_DELIVERY_FEE + (Config.PER_KM_RATE * distanceKm);
        double platformCut = deliveryFeeTotal * Config.PLATFORM_CUT_RATIO;
        double partnerCut = deliveryFeeTotal * Config.PARTNER_CUT_RATIO;
        double totalPayable = energyPrice + deliveryFeeTotal;

        double bal = walletRepo.getBalance(buyer.getId());
        if (bal < totalPayable) {
            return new TradeResult(false, String.format(Locale.US,
                    "Insufficient wallet balance! Required: %s, Available: %s",
                    Config.formatCurrency(totalPayable), Config.formatCurrency(bal)), null, null);
        }

        boolean ok = walletRepo.transfer(buyer.getId(), listing.getSellerId(), energyPrice,
                "Bought " + qtyToBuy + " " + listing.getUnit() + " " + listing.getEnergyType(),
                "Sold " + qtyToBuy + " " + listing.getUnit() + " " + listing.getEnergyType(),
                listing.getId());
        if (!ok) return new TradeResult(false, "Wallet transfer failed.", null, null);

        walletRepo.adjustBalance(buyer.getId(), -deliveryFeeTotal,
                WalletRepository.TYPE_PURCHASE, "Delivery fee", listing.getId());

        reduceListingQuantity(listing.getId(), qtyToBuy);

        String deliveryCode = String.valueOf(100000 + new Random().nextInt(900000));
        String ts = now();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cvT = new ContentValues();
        cvT.put("listing_id", listing.getId());
        cvT.put("buyer_id", buyer.getId());
        cvT.put("buyer_name", buyer.getName());
        cvT.put("seller_id", listing.getSellerId());
        cvT.put("seller_name", listing.getSellerName());
        cvT.put("energy_type", listing.getEnergyType());
        cvT.put("unit", listing.getUnit());
        cvT.put("quantity", qtyToBuy);
        cvT.put("energy_price", energyPrice);
        cvT.put("delivery_fee", deliveryFeeTotal);
        cvT.put("platform_cut", platformCut);
        cvT.put("partner_cut", partnerCut);
        cvT.put("total_price", totalPayable);
        cvT.put("delivery_code", deliveryCode);
        cvT.put("timestamp", ts);
        cvT.put("order_type", "PURCHASE");
        long tradeId = db.insert(DatabaseHelper.TABLE_TRADES, null, cvT);

        ContentValues cvD = new ContentValues();
        cvD.put("trade_id", tradeId);
        cvD.put("order_type", "PURCHASE");
        cvD.put("sender_id", listing.getSellerId());
        cvD.put("receiver_id", buyer.getId());
        cvD.put("pickup_address", listing.getLocation());
        cvD.put("drop_address", buyer.getCityLocation() != null ? buyer.getCityLocation() : "Buyer Location");
        cvD.put("distance_km", distanceKm);
        cvD.put("delivery_fee_total", deliveryFeeTotal);
        cvD.put("platform_cut", platformCut);
        cvD.put("partner_cut", partnerCut);
        cvD.put("status", "AVAILABLE");
        cvD.put("partner_id", 0);
        cvD.put("delivery_code", deliveryCode);
        cvD.put("created_at", ts);
        cvD.put("delivered_at", "");
        db.insert(DatabaseHelper.TABLE_DELIVERIES, null, cvD);

        if (isAutoBuy) {
            insertAutoBuyHistory(buyer.getId(), listing, qtyToBuy, totalPayable, "BOUGHT");
        }

        Trade t = new Trade(tradeId, listing.getId(), buyer.getId(), buyer.getName(),
                listing.getSellerId(), listing.getSellerName(), listing.getEnergyType(),
                listing.getUnit(), qtyToBuy, energyPrice, deliveryFeeTotal,
                platformCut, partnerCut, totalPayable, deliveryCode, ts);
        t.setOrderType("PURCHASE");
        return new TradeResult(true, "Purchase successful!", deliveryCode, t);
    }

    public List<Trade> getTradesByBuyer(long buyerId) {
        return queryTrades("buyer_id = ?", new String[]{String.valueOf(buyerId)});
    }

    public List<Trade> getTradesBySeller(long sellerId) {
        return queryTrades("seller_id = ?", new String[]{String.valueOf(sellerId)});
    }

    private List<Trade> queryTrades(String where, String[] args) {
        List<Trade> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_TRADES +
                " WHERE " + where + " ORDER BY id DESC", args);
        while (c.moveToNext()) list.add(cursorToTrade(c));
        c.close();
        return list;
    }

    public List<Trade> getActiveTradesByBuyer(long buyerId) {
        return queryTrades(
                "buyer_id = ? AND id NOT IN (SELECT trade_id FROM " + DatabaseHelper.TABLE_DELIVERIES +
                        " WHERE status = 'DELIVERED')",
                new String[]{String.valueOf(buyerId)});
    }

    public List<Trade> getActiveTradesBySeller(long sellerId) {
        return queryTrades(
                "seller_id = ? AND id NOT IN (SELECT trade_id FROM " + DatabaseHelper.TABLE_DELIVERIES +
                        " WHERE status = 'DELIVERED')",
                new String[]{String.valueOf(sellerId)});
    }

    /** R2-11.5: active barter trades where the user is either buyer (receiver) or seller (sender). */
    public List<Trade> getActiveBarterTradesForUser(long userId) {
        List<Trade> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String q = "SELECT * FROM " + DatabaseHelper.TABLE_TRADES +
                " WHERE order_type='BARTER' AND (buyer_id = ? OR seller_id = ?) " +
                "AND id NOT IN (SELECT trade_id FROM " + DatabaseHelper.TABLE_DELIVERIES +
                " WHERE status='DELIVERED') ORDER BY id DESC";
        Cursor c = db.rawQuery(q, new String[]{String.valueOf(userId), String.valueOf(userId)});
        while (c.moveToNext()) list.add(cursorToTrade(c));
        c.close();
        return list;
    }

    private Trade cursorToTrade(Cursor c) {
        Trade t = new Trade(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getLong(c.getColumnIndexOrThrow("listing_id")),
                c.getLong(c.getColumnIndexOrThrow("buyer_id")),
                c.getString(c.getColumnIndexOrThrow("buyer_name")),
                c.getLong(c.getColumnIndexOrThrow("seller_id")),
                c.getString(c.getColumnIndexOrThrow("seller_name")),
                c.getString(c.getColumnIndexOrThrow("energy_type")),
                c.getString(c.getColumnIndexOrThrow("unit")),
                c.getDouble(c.getColumnIndexOrThrow("quantity")),
                c.getDouble(c.getColumnIndexOrThrow("energy_price")),
                c.getDouble(c.getColumnIndexOrThrow("delivery_fee")),
                c.getDouble(c.getColumnIndexOrThrow("platform_cut")),
                c.getDouble(c.getColumnIndexOrThrow("partner_cut")),
                c.getDouble(c.getColumnIndexOrThrow("total_price")),
                c.getString(c.getColumnIndexOrThrow("delivery_code")),
                c.getString(c.getColumnIndexOrThrow("timestamp"))
        );
        try {
            String ot = c.getString(c.getColumnIndexOrThrow("order_type"));
            if (ot != null) t.setOrderType(ot);
        } catch (Exception ignored) {}
        return t;
    }

    public String getDeliveryStatusForTrade(long tradeId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT status FROM " + DatabaseHelper.TABLE_DELIVERIES +
                " WHERE trade_id=? ORDER BY id DESC LIMIT 1", new String[]{String.valueOf(tradeId)});
        String s = "PLACED";
        if (c.moveToFirst()) {
            String raw = c.getString(0);
            if (raw == null || "AVAILABLE".equalsIgnoreCase(raw)) s = "PLACED";
            else if ("ACCEPTED".equalsIgnoreCase(raw)) s = "ACCEPTED";
            else if ("PICKED_UP".equalsIgnoreCase(raw)) s = "PICKED UP";
            else if ("DELIVERED".equalsIgnoreCase(raw)) s = "DELIVERED";
            else s = raw;
        }
        c.close();
        return s;
    }

    public String getPartnerNameForTrade(long tradeId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT u.name FROM " + DatabaseHelper.TABLE_DELIVERIES + " d JOIN " +
                DatabaseHelper.TABLE_USERS + " u ON d.partner_id = u.id " +
                " WHERE d.trade_id=? ORDER BY d.id DESC LIMIT 1", new String[]{String.valueOf(tradeId)});
        String name = null;
        if (c.moveToFirst()) name = c.getString(0);
        c.close();
        return name;
    }

    // ============================================================
    // DELIVERY PARTNER
    // ============================================================
    public List<DeliveryTask> getDeliveryTasksByStatus(String status, long partnerId) {
        List<DeliveryTask> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String q;
        String[] args;
        String base = "SELECT d.*, t.energy_type, t.quantity, t.unit, t.seller_name, t.buyer_name FROM " +
                DatabaseHelper.TABLE_DELIVERIES + " d JOIN " + DatabaseHelper.TABLE_TRADES +
                " t ON d.trade_id = t.id WHERE ";
        if ("AVAILABLE".equalsIgnoreCase(status)) {
            q = base + "d.status = 'AVAILABLE' ORDER BY d.id DESC";
            args = null;
        } else {
            q = base + "d.partner_id = ? AND d.status = ? ORDER BY d.id DESC";
            args = new String[]{String.valueOf(partnerId), status};
        }
        Cursor c = db.rawQuery(q, args);
        while (c.moveToNext()) {
            DeliveryTask t = new DeliveryTask(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    c.getLong(c.getColumnIndexOrThrow("trade_id")),
                    c.getString(c.getColumnIndexOrThrow("energy_type")),
                    c.getDouble(c.getColumnIndexOrThrow("quantity")),
                    c.getString(c.getColumnIndexOrThrow("unit")),
                    c.getString(c.getColumnIndexOrThrow("seller_name")),
                    c.getString(c.getColumnIndexOrThrow("pickup_address")),
                    c.getString(c.getColumnIndexOrThrow("buyer_name")),
                    c.getString(c.getColumnIndexOrThrow("drop_address")),
                    c.getDouble(c.getColumnIndexOrThrow("distance_km")),
                    c.getDouble(c.getColumnIndexOrThrow("delivery_fee_total")),
                    c.getDouble(c.getColumnIndexOrThrow("platform_cut")),
                    c.getDouble(c.getColumnIndexOrThrow("partner_cut")),
                    c.getString(c.getColumnIndexOrThrow("status")),
                    c.getLong(c.getColumnIndexOrThrow("partner_id")),
                    c.getString(c.getColumnIndexOrThrow("delivery_code")),
                    c.getString(c.getColumnIndexOrThrow("created_at")),
                    safeString(c, "accepted_at"),
                    safeString(c, "picked_up_at"),
                    c.getString(c.getColumnIndexOrThrow("delivered_at"))
            );
            try {
                t.setBarterId(c.getLong(c.getColumnIndexOrThrow("barter_id")));
                t.setLegNo(c.getInt(c.getColumnIndexOrThrow("leg_no")));
                String ot = c.getString(c.getColumnIndexOrThrow("order_type"));
                if (ot != null) t.setOrderType(ot);
                t.setSenderId(c.getLong(c.getColumnIndexOrThrow("sender_id")));
                t.setReceiverId(c.getLong(c.getColumnIndexOrThrow("receiver_id")));
            } catch (Exception ignored) {}
            list.add(t);
        }
        c.close();
        return list;
    }

    public boolean acceptDeliveryTask(long deliveryId, long partnerId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", "ACCEPTED");
        cv.put("partner_id", partnerId);
        cv.put("accepted_at", now());
        return db.update(DatabaseHelper.TABLE_DELIVERIES, cv,
                "id = ? AND status = 'AVAILABLE'", new String[]{String.valueOf(deliveryId)}) > 0;
    }

    public boolean markPickedUp(long deliveryId, long partnerId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", "PICKED_UP");
        cv.put("picked_up_at", now());
        return db.update(DatabaseHelper.TABLE_DELIVERIES, cv,
                "id = ? AND partner_id = ? AND status = 'ACCEPTED'",
                new String[]{String.valueOf(deliveryId), String.valueOf(partnerId)}) > 0;
    }

    public boolean completeDeliveryTask(long deliveryId, long partnerId, String inputCode) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_DELIVERIES +
                        " WHERE id = ? AND partner_id = ?",
                new String[]{String.valueOf(deliveryId), String.valueOf(partnerId)});
        if (!c.moveToFirst()) { c.close(); return false; }
        String actual = c.getString(c.getColumnIndexOrThrow("delivery_code"));
        double partnerCut = c.getDouble(c.getColumnIndexOrThrow("partner_cut"));
        String status = c.getString(c.getColumnIndexOrThrow("status"));
        long barterId = 0;
        try { barterId = c.getLong(c.getColumnIndexOrThrow("barter_id")); } catch (Exception ignored) {}
        c.close();

        if (!"PICKED_UP".equalsIgnoreCase(status) && !"ACCEPTED".equalsIgnoreCase(status))
            return false;

        if (actual != null && actual.trim().equals(inputCode.trim())) {
            SQLiteDatabase wdb = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("status", "DELIVERED");
            cv.put("delivered_at", now());
            wdb.update(DatabaseHelper.TABLE_DELIVERIES, cv, "id = ?",
                    new String[]{String.valueOf(deliveryId)});

            walletRepo.adjustBalance(partnerId, partnerCut,
                    WalletRepository.TYPE_DELIVERY_EARNING, "Delivery earning", deliveryId);

            if (barterId > 0 && getBarterDeliveredLegCount(barterId) >= 2) {
                ContentValues bc = new ContentValues();
                bc.put("status", "COMPLETED");
                wdb.update(DatabaseHelper.TABLE_BARTERS, bc, "id=?",
                        new String[]{String.valueOf(barterId)});
            }
            return true;
        }
        return false;
    }

    public double getPartnerTotalEarnings(long partnerId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(partner_cut) FROM " + DatabaseHelper.TABLE_DELIVERIES +
                " WHERE partner_id = ? AND status = 'DELIVERED'", new String[]{String.valueOf(partnerId)});
        double sum = 0;
        if (c.moveToFirst()) sum = c.getDouble(0);
        c.close();
        return sum;
    }

    // ============================================================
    // POOLS (R2-2)
    // ============================================================
    public boolean createPool(UserProfile creator, EnergyListing listing, double creatorQty) {
        if (creator == null || listing == null) return false;
        if (creatorQty <= 0 || creatorQty > listing.getQuantity()) return false;

        double energyCost = creatorQty * listing.getPricePerUnit();
        double deliveryFee = Config.BASE_DELIVERY_FEE + (Config.PER_KM_RATE * Config.DEFAULT_DISTANCE_KM);
        double totalDebit = energyCost + deliveryFee;
        if (walletRepo.getBalance(creator.getId()) < totalDebit) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        long poolId = -1;
        try {
            boolean ok = walletRepo.transfer(creator.getId(), listing.getSellerId(), energyCost,
                    "Pool share (" + creatorQty + " " + listing.getUnit() + ")", "Pool sale", listing.getId());
            if (!ok) { return false; }
            walletRepo.adjustBalance(creator.getId(), -deliveryFee,
                    WalletRepository.TYPE_PURCHASE, "Pool delivery fee", listing.getId());

            double poolSize = listing.getQuantity();
            String nowStr = now();
            String expiresAt = String.valueOf(System.currentTimeMillis() + Config.POOL_EXPIRY_MS);

            ContentValues cvPool = new ContentValues();
            cvPool.put("listing_id", listing.getId());
            cvPool.put("creator_id", creator.getId());
            cvPool.put("creator_name", creator.getName());
            cvPool.put("total_qty", poolSize);
            cvPool.put("joined_qty", creatorQty);
            cvPool.put("unit", listing.getUnit());
            cvPool.put("price_per_unit", listing.getPricePerUnit());
            cvPool.put("status", "OPEN");
            cvPool.put("created_at", nowStr);
            cvPool.put("expires_at", expiresAt);
            poolId = db.insert(DatabaseHelper.TABLE_POOLS, null, cvPool);
            if (poolId == -1) { return false; }

            String code = String.valueOf(100000 + new Random().nextInt(900000));
            ContentValues cvM = new ContentValues();
            cvM.put("pool_id", poolId);
            cvM.put("user_id", creator.getId());
            cvM.put("user_name", creator.getName());
            cvM.put("qty", creatorQty);
            cvM.put("amount_paid", energyCost);
            cvM.put("delivery_fee", deliveryFee);
            cvM.put("delivery_code", code);
            cvM.put("status", "JOINED");
            db.insert(DatabaseHelper.TABLE_POOL_MEMBERS, null, cvM);

            double reserved = poolSize - creatorQty;
            reduceListingQuantity(listing.getId(), reserved);

            if (reserved <= 0.0001) {
                ContentValues up = new ContentValues();
                up.put("status", "FULL");
                db.update(DatabaseHelper.TABLE_POOLS, up, "id=?", new String[]{String.valueOf(poolId)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        EnergyPool p = getPoolById(poolId);
        if (p != null && "FULL".equalsIgnoreCase(p.getStatus())) finalizePool(poolId);
        return true;
    }

    public List<EnergyPool> getAllOpenPools() {
        return queryPools("p.status='OPEN'", null);
    }

    public List<EnergyPool> getMyPools(long userId) {
        return queryPools(
                "(p.creator_id = ? OR p.id IN (SELECT pool_id FROM " + DatabaseHelper.TABLE_POOL_MEMBERS +
                        " WHERE user_id = ?))",
                new String[]{String.valueOf(userId), String.valueOf(userId)});
    }

    private List<EnergyPool> queryPools(String where, String[] args) {
        List<EnergyPool> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String q = "SELECT p.*, l.energy_type, l.seller_name, l.location FROM " +
                DatabaseHelper.TABLE_POOLS + " p LEFT JOIN " + DatabaseHelper.TABLE_LISTINGS +
                " l ON p.listing_id = l.id WHERE " + where + " ORDER BY p.id DESC";
        Cursor c = db.rawQuery(q, args);
        while (c.moveToNext()) list.add(cursorToPool(c));
        c.close();
        return list;
    }

    public EnergyPool getPoolById(long poolId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String q = "SELECT p.*, l.energy_type, l.seller_name, l.location FROM " +
                DatabaseHelper.TABLE_POOLS + " p LEFT JOIN " + DatabaseHelper.TABLE_LISTINGS +
                " l ON p.listing_id = l.id WHERE p.id=?";
        Cursor c = db.rawQuery(q, new String[]{String.valueOf(poolId)});
        EnergyPool p = null;
        if (c.moveToFirst()) p = cursorToPool(c);
        c.close();
        return p;
    }

    public boolean hasUserJoinedPool(long poolId, long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM " + DatabaseHelper.TABLE_POOL_MEMBERS +
                        " WHERE pool_id=? AND user_id=?",
                new String[]{String.valueOf(poolId), String.valueOf(userId)});
        boolean joined = c.moveToFirst();
        c.close();
        return joined;
    }

    public boolean joinPool(UserProfile user, long poolId, double qty) {
        if (user == null || qty <= 0) return false;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor pc = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_POOLS +
                " WHERE id=? AND status='OPEN'", new String[]{String.valueOf(poolId)});
        if (!pc.moveToFirst()) { pc.close(); return false; }

        long creatorId = pc.getLong(pc.getColumnIndexOrThrow("creator_id"));
        long listingId = pc.getLong(pc.getColumnIndexOrThrow("listing_id"));
        double total = pc.getDouble(pc.getColumnIndexOrThrow("total_qty"));
        double joined = pc.getDouble(pc.getColumnIndexOrThrow("joined_qty"));
        double price = pc.getDouble(pc.getColumnIndexOrThrow("price_per_unit"));
        String unit = pc.getString(pc.getColumnIndexOrThrow("unit"));
        pc.close();

        if (creatorId == user.getId()) return false;
        if (hasUserJoinedPool(poolId, user.getId())) return false;
        if (qty > (total - joined) + 0.0001) return false;

        EnergyListing listing = getListingById(listingId);
        if (listing == null) return false;

        double energyCost = qty * price;
        double deliveryFee = Config.BASE_DELIVERY_FEE + (Config.PER_KM_RATE * Config.DEFAULT_DISTANCE_KM);
        double totalDebit = energyCost + deliveryFee;
        if (walletRepo.getBalance(user.getId()) < totalDebit) return false;

        SQLiteDatabase wdb = dbHelper.getWritableDatabase();
        wdb.beginTransaction();
        try {
            boolean ok = walletRepo.transfer(user.getId(), listing.getSellerId(), energyCost,
                    "Pool join (" + qty + " " + unit + ")", "Pool sale", poolId);
            if (!ok) { return false; }
            walletRepo.adjustBalance(user.getId(), -deliveryFee,
                    WalletRepository.TYPE_PURCHASE, "Pool delivery fee", poolId);

            String code = String.valueOf(100000 + new Random().nextInt(900000));
            ContentValues cv = new ContentValues();
            cv.put("pool_id", poolId);
            cv.put("user_id", user.getId());
            cv.put("user_name", user.getName());
            cv.put("qty", qty);
            cv.put("amount_paid", energyCost);
            cv.put("delivery_fee", deliveryFee);
            cv.put("delivery_code", code);
            cv.put("status", "JOINED");
            wdb.insert(DatabaseHelper.TABLE_POOL_MEMBERS, null, cv);

            double newJoined = joined + qty;
            ContentValues up = new ContentValues();
            up.put("joined_qty", newJoined);
            if (newJoined >= total - 0.0001) up.put("status", "FULL");
            wdb.update(DatabaseHelper.TABLE_POOLS, up, "id=?", new String[]{String.valueOf(poolId)});
            wdb.setTransactionSuccessful();
        } finally {
            wdb.endTransaction();
        }

        EnergyPool updated = getPoolById(poolId);
        if (updated != null && "FULL".equalsIgnoreCase(updated.getStatus())) finalizePool(poolId);
        return true;
    }

    public boolean closePoolEarly(UserProfile creator, long poolId) {
        if (creator == null) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT creator_id, listing_id, total_qty, joined_qty FROM " +
                        DatabaseHelper.TABLE_POOLS + " WHERE id=? AND status='OPEN'",
                new String[]{String.valueOf(poolId)});
        if (!c.moveToFirst()) { c.close(); return false; }
        long creatorId = c.getLong(0);
        long listingId = c.getLong(1);
        double totalQty = c.getDouble(2);
        double joinedQty = c.getDouble(3);
        c.close();
        if (creatorId != creator.getId()) return false;

        SQLiteDatabase wdb = dbHelper.getWritableDatabase();
        wdb.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("status", "CLOSED");
            cv.put("total_qty", joinedQty);
            wdb.update(DatabaseHelper.TABLE_POOLS, cv, "id=?", new String[]{String.valueOf(poolId)});

            double unjoined = totalQty - joinedQty;
            if (unjoined > 0.0001) {
                Cursor lc = wdb.rawQuery("SELECT quantity FROM " + DatabaseHelper.TABLE_LISTINGS +
                        " WHERE id=?", new String[]{String.valueOf(listingId)});
                if (lc.moveToFirst()) {
                    double cur = lc.getDouble(0);
                    ContentValues lu = new ContentValues();
                    lu.put("quantity", cur + unjoined);
                    lu.put("status", "AVAILABLE");
                    wdb.update(DatabaseHelper.TABLE_LISTINGS, lu, "id=?",
                            new String[]{String.valueOf(listingId)});
                }
                lc.close();
            }
            wdb.setTransactionSuccessful();
        } finally {
            wdb.endTransaction();
        }
        finalizePool(poolId);
        return true;
    }

    public List<PoolMember> getPoolMembers(long poolId) {
        List<PoolMember> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_POOL_MEMBERS +
                " WHERE pool_id=?", new String[]{String.valueOf(poolId)});
        while (c.moveToNext()) {
            list.add(new PoolMember(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    c.getLong(c.getColumnIndexOrThrow("pool_id")),
                    c.getLong(c.getColumnIndexOrThrow("user_id")),
                    c.getString(c.getColumnIndexOrThrow("user_name")),
                    c.getDouble(c.getColumnIndexOrThrow("qty")),
                    c.getDouble(c.getColumnIndexOrThrow("amount_paid")),
                    c.getDouble(c.getColumnIndexOrThrow("delivery_fee")),
                    c.getString(c.getColumnIndexOrThrow("delivery_code")),
                    c.getString(c.getColumnIndexOrThrow("status"))
            ));
        }
        c.close();
        return list;
    }

    private void finalizePool(long poolId) {
        EnergyPool pool = getPoolById(poolId);
        if (pool == null) return;

        EnergyListing listing = getListingById(pool.getListingId());
        if (listing == null) return;

        markListingTraded(listing.getId());

        List<PoolMember> members = getPoolMembers(poolId);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String ts = now();

        for (PoolMember m : members) {
            if ("DELIVERED".equalsIgnoreCase(m.getStatus())) continue;

            UserProfile member = getUserById(m.getUserId());
            String drop = member != null && member.getCityLocation() != null
                    ? member.getCityLocation() : "Member Address";
            double distance = Config.DEFAULT_DISTANCE_KM;
            double deliveryFee = Config.BASE_DELIVERY_FEE + (Config.PER_KM_RATE * distance);
            double platformCut = deliveryFee * Config.PLATFORM_CUT_RATIO;
            double partnerCut = deliveryFee * Config.PARTNER_CUT_RATIO;

            ContentValues cvT = new ContentValues();
            cvT.put("listing_id", listing.getId());
            cvT.put("buyer_id", m.getUserId());
            cvT.put("buyer_name", m.getUserName());
            cvT.put("seller_id", listing.getSellerId());
            cvT.put("seller_name", listing.getSellerName());
            cvT.put("energy_type", listing.getEnergyType());
            cvT.put("unit", listing.getUnit());
            cvT.put("quantity", m.getQty());
            cvT.put("energy_price", m.getAmountPaid());
            cvT.put("delivery_fee", deliveryFee);
            cvT.put("platform_cut", platformCut);
            cvT.put("partner_cut", partnerCut);
            cvT.put("total_price", m.getAmountPaid() + deliveryFee);
            cvT.put("delivery_code", m.getDeliveryCode());
            cvT.put("timestamp", ts);
            cvT.put("order_type", "POOL");
            long tradeId = db.insert(DatabaseHelper.TABLE_TRADES, null, cvT);

            ContentValues cvD = new ContentValues();
            cvD.put("trade_id", tradeId);
            cvD.put("order_type", "POOL");
            cvD.put("sender_id", listing.getSellerId());
            cvD.put("receiver_id", m.getUserId());
            cvD.put("pickup_address", listing.getLocation());
            cvD.put("drop_address", drop);
            cvD.put("distance_km", distance);
            cvD.put("delivery_fee_total", deliveryFee);
            cvD.put("platform_cut", platformCut);
            cvD.put("partner_cut", partnerCut);
            cvD.put("status", "AVAILABLE");
            cvD.put("partner_id", 0);
            cvD.put("delivery_code", m.getDeliveryCode());
            cvD.put("created_at", ts);
            cvD.put("delivered_at", "");
            db.insert(DatabaseHelper.TABLE_DELIVERIES, null, cvD);

            ContentValues cvM = new ContentValues();
            cvM.put("status", "DELIVERED");
            db.update(DatabaseHelper.TABLE_POOL_MEMBERS, cvM, "id=?",
                    new String[]{String.valueOf(m.getId())});
        }
    }

    public void expireStalePools() {
        long nowMs = System.currentTimeMillis();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM " + DatabaseHelper.TABLE_POOLS +
                        " WHERE status='OPEN' AND expires_at IS NOT NULL AND CAST(expires_at AS INTEGER) < ?",
                new String[]{String.valueOf(nowMs)});
        List<Long> toExpire = new ArrayList<>();
        while (c.moveToNext()) toExpire.add(c.getLong(0));
        c.close();
        for (long poolId : toExpire) refundPool(poolId);
    }

    private void refundPool(long poolId) {
        EnergyPool pool = getPoolById(poolId);
        if (pool == null) return;
        List<PoolMember> members = getPoolMembers(poolId);

        SQLiteDatabase wdb = dbHelper.getWritableDatabase();
        wdb.beginTransaction();
        try {
            for (PoolMember m : members) {
                double refund = m.getAmountPaid() + m.getDeliveryFee();
                walletRepo.adjustBalance(m.getUserId(), refund,
                        WalletRepository.TYPE_POOL_REFUND, "Pool expired refund", poolId);
            }
            ContentValues cv = new ContentValues();
            cv.put("status", "EXPIRED");
            wdb.update(DatabaseHelper.TABLE_POOLS, cv, "id=?", new String[]{String.valueOf(poolId)});

            double unjoined = pool.getTotalQty() - pool.getJoinedQty();
            if (unjoined > 0.0001 && pool.getListingId() > 0) {
                Cursor lc = wdb.rawQuery("SELECT quantity FROM " + DatabaseHelper.TABLE_LISTINGS +
                        " WHERE id=?", new String[]{String.valueOf(pool.getListingId())});
                if (lc.moveToFirst()) {
                    double cur = lc.getDouble(0);
                    ContentValues lu = new ContentValues();
                    lu.put("quantity", cur + unjoined);
                    lu.put("status", "AVAILABLE");
                    wdb.update(DatabaseHelper.TABLE_LISTINGS, lu, "id=?",
                            new String[]{String.valueOf(pool.getListingId())});
                }
                lc.close();
            }
            wdb.setTransactionSuccessful();
        } finally {
            wdb.endTransaction();
        }
    }

    private EnergyPool cursorToPool(Cursor c) {
        return new EnergyPool(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getLong(c.getColumnIndexOrThrow("listing_id")),
                c.getLong(c.getColumnIndexOrThrow("creator_id")),
                c.getString(c.getColumnIndexOrThrow("creator_name")),
                c.getDouble(c.getColumnIndexOrThrow("total_qty")),
                c.getDouble(c.getColumnIndexOrThrow("joined_qty")),
                c.getString(c.getColumnIndexOrThrow("unit")),
                c.getDouble(c.getColumnIndexOrThrow("price_per_unit")),
                c.getString(c.getColumnIndexOrThrow("status")),
                c.getString(c.getColumnIndexOrThrow("created_at")),
                c.getString(c.getColumnIndexOrThrow("expires_at")),
                safeString(c, "energy_type"),
                safeString(c, "seller_name"),
                safeString(c, "location")
        );
    }

    // ============================================================
    // BARTER (R2-11)
    // ============================================================
    public static class BarterAcceptResult {
        public boolean success;
        public String errorMessage;
        public long leg1DeliveryId, leg2DeliveryId;
        public BarterAcceptResult(boolean s, String e, long l1, long l2) {
            success = s; errorMessage = e; leg1DeliveryId = l1; leg2DeliveryId = l2;
        }
    }

    public boolean createBarterOffer(UserProfile creator, String giveType, double giveQty,
                                     String wantType, double wantQty, String note, String location) {
        if (creator == null || !creator.isProsumer()) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("creator_id", creator.getId());
        cv.put("creator_name", creator.getName());
        cv.put("give_type", giveType);
        cv.put("give_qty", giveQty);
        cv.put("give_unit", Config.getUnitForEnergyType(giveType));
        cv.put("want_type", wantType);
        cv.put("want_qty", wantQty);
        cv.put("want_unit", Config.getUnitForEnergyType(wantType));
        cv.put("note", note != null ? note : "");
        cv.put("location", location != null ? location : creator.getCityLocation());
        cv.put("accepter_id", 0);
        cv.put("accepter_name", "");
        cv.put("status", "OPEN");
        cv.put("created_at", today());
        return db.insert(DatabaseHelper.TABLE_BARTERS, null, cv) != -1;
    }

    public List<BarterOffer> getAllOpenBarters() {
        List<BarterOffer> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_BARTERS +
                " WHERE status='OPEN' ORDER BY id DESC", null);
        while (c.moveToNext()) list.add(cursorToBarter(c));
        c.close();
        return list;
    }

    public List<BarterOffer> getMyBarters(long userId) {
        List<BarterOffer> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_BARTERS +
                        " WHERE creator_id = ? OR accepter_id = ? ORDER BY id DESC",
                new String[]{String.valueOf(userId), String.valueOf(userId)});
        while (c.moveToNext()) list.add(cursorToBarter(c));
        c.close();
        return list;
    }

    /** R2-11.1/11.2/11.3: accept barter, create two delivery legs in one transaction. */
    public BarterAcceptResult acceptBarterAndCreateLegs(UserProfile accepter, long barterId) {
        if (accepter == null) return new BarterAcceptResult(false, "No session", 0, 0);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_BARTERS +
                    " WHERE id=? AND status='OPEN'", new String[]{String.valueOf(barterId)});
            if (!c.moveToFirst()) {
                c.close();
                return new BarterAcceptResult(false, "Barter no longer available", 0, 0);
            }
            long creatorId = c.getLong(c.getColumnIndexOrThrow("creator_id"));
            String creatorName = c.getString(c.getColumnIndexOrThrow("creator_name"));
            String giveType = c.getString(c.getColumnIndexOrThrow("give_type"));
            double giveQty = c.getDouble(c.getColumnIndexOrThrow("give_qty"));
            String giveUnit = c.getString(c.getColumnIndexOrThrow("give_unit"));
            String wantType = c.getString(c.getColumnIndexOrThrow("want_type"));
            double wantQty = c.getDouble(c.getColumnIndexOrThrow("want_qty"));
            String wantUnit = c.getString(c.getColumnIndexOrThrow("want_unit"));
            String creatorLoc = c.getString(c.getColumnIndexOrThrow("location"));
            c.close();

            if (creatorId == accepter.getId()) {
                return new BarterAcceptResult(false, "You cannot accept your own barter", 0, 0);
            }

            UserProfile creator = getUserById(creatorId);
            if (creator == null) {
                return new BarterAcceptResult(false, "Creator not found", 0, 0);
            }
            if (creatorLoc == null || creatorLoc.isEmpty()) creatorLoc = creator.getCityLocation();
            String accepterLoc = accepter.getCityLocation();
            if (accepterLoc == null || accepterLoc.isEmpty()) accepterLoc = "Buyer Location";

            double distance = estimateDistanceKm(creator, accepter);
            double legFee = Config.BASE_DELIVERY_FEE + (Config.PER_KM_RATE * distance);
            double platformCut = legFee * Config.PLATFORM_CUT_RATIO;
            double partnerCut  = legFee * Config.PARTNER_CUT_RATIO;

            double accepterBal = walletBalanceFromDb(db, accepter.getId());
            double creatorBal  = walletBalanceFromDb(db, creatorId);
            if (accepterBal < legFee) {
                return new BarterAcceptResult(false, "Insufficient wallet balance for delivery fee", 0, 0);
            }
            if (creatorBal < legFee) {
                return new BarterAcceptResult(false, "Creator has insufficient wallet balance for delivery fee", 0, 0);
            }

            String ts = now();

            ContentValues bc = new ContentValues();
            bc.put("status", "ACCEPTED");
            bc.put("accepter_id", accepter.getId());
            bc.put("accepter_name", accepter.getName());
            bc.put("accepted_at", ts);
            db.update(DatabaseHelper.TABLE_BARTERS, bc, "id=?", new String[]{String.valueOf(barterId)});

            long leg1Delivery = createBarterLeg(db, barterId, 1,
                    creator.getId(), creatorName, creatorLoc,
                    accepter.getId(), accepter.getName(), accepterLoc,
                    giveType, giveQty, giveUnit, distance, legFee, platformCut, partnerCut, ts);

            long leg2Delivery = createBarterLeg(db, barterId, 2,
                    accepter.getId(), accepter.getName(), accepterLoc,
                    creator.getId(), creatorName, creatorLoc,
                    wantType, wantQty, wantUnit, distance, legFee, platformCut, partnerCut, ts);

            applyFeeDebit(db, accepter.getId(), legFee, leg1Delivery);
            applyFeeDebit(db, creatorId,       legFee, leg2Delivery);

            db.setTransactionSuccessful();
            return new BarterAcceptResult(true, "Barter accepted", leg1Delivery, leg2Delivery);
        } catch (Exception e) {
            return new BarterAcceptResult(false, "Failed to accept barter", 0, 0);
        } finally {
            db.endTransaction();
        }
    }

    private long createBarterLeg(SQLiteDatabase db, long barterId, int legNo,
                                 long senderId, String senderName, String senderLoc,
                                 long receiverId, String receiverName, String receiverLoc,
                                 String energyType, double qty, String unit,
                                 double distance, double fee, double platformCut, double partnerCut,
                                 String ts) {
        String code = String.valueOf(100000 + new Random().nextInt(900000));

        ContentValues cvT = new ContentValues();
        cvT.put("listing_id", 0);
        cvT.put("buyer_id", receiverId);
        cvT.put("buyer_name", receiverName);
        cvT.put("seller_id", senderId);
        cvT.put("seller_name", senderName);
        cvT.put("energy_type", energyType);
        cvT.put("unit", unit);
        cvT.put("quantity", qty);
        cvT.put("energy_price", 0.0);
        cvT.put("delivery_fee", fee);
        cvT.put("platform_cut", platformCut);
        cvT.put("partner_cut", partnerCut);
        cvT.put("total_price", fee);
        cvT.put("delivery_code", code);
        cvT.put("timestamp", ts);
        cvT.put("order_type", "BARTER");
        long tradeId = db.insert(DatabaseHelper.TABLE_TRADES, null, cvT);

        ContentValues cvD = new ContentValues();
        cvD.put("trade_id", tradeId);
        cvD.put("barter_id", barterId);
        cvD.put("leg_no", legNo);
        cvD.put("order_type", "BARTER");
        cvD.put("sender_id", senderId);
        cvD.put("receiver_id", receiverId);
        cvD.put("pickup_address", senderLoc);
        cvD.put("drop_address", receiverLoc);
        cvD.put("distance_km", distance);
        cvD.put("delivery_fee_total", fee);
        cvD.put("platform_cut", platformCut);
        cvD.put("partner_cut", partnerCut);
        cvD.put("status", "AVAILABLE");
        cvD.put("partner_id", 0);
        cvD.put("delivery_code", code);
        cvD.put("created_at", ts);
        cvD.put("delivered_at", "");
        return db.insert(DatabaseHelper.TABLE_DELIVERIES, null, cvD);
    }

    private double walletBalanceFromDb(SQLiteDatabase db, long userId) {
        Cursor c = db.rawQuery("SELECT wallet_balance FROM " + DatabaseHelper.TABLE_USERS +
                " WHERE id=?", new String[]{String.valueOf(userId)});
        double b = 0;
        if (c.moveToFirst()) b = c.getDouble(0);
        c.close();
        return b;
    }

    private void applyFeeDebit(SQLiteDatabase db, long userId, double fee, long deliveryId) {
        double cur = walletBalanceFromDb(db, userId);
        ContentValues cv = new ContentValues();
        cv.put("wallet_balance", cur - fee);
        db.update(DatabaseHelper.TABLE_USERS, cv, "id=?", new String[]{String.valueOf(userId)});

        ContentValues tx = new ContentValues();
        tx.put("user_id", userId);
        tx.put("amount", -fee);
        tx.put("type", WalletRepository.TYPE_PURCHASE);
        tx.put("description", "Barter delivery fee");
        tx.put("method", WalletRepository.METHOD_INTERNAL);
        tx.put("reference_id", deliveryId);
        tx.put("created_at", now());
        db.insert(DatabaseHelper.TABLE_WALLET_TX, null, tx);
    }

    private double estimateDistanceKm(UserProfile a, UserProfile b) {
        if (a == null || b == null) return Config.DEFAULT_DISTANCE_KM;
        String ca = a.getCityLocation() == null ? "" : a.getCityLocation().trim().toLowerCase(Locale.US);
        String cb = b.getCityLocation() == null ? "" : b.getCityLocation().trim().toLowerCase(Locale.US);
        if (!ca.isEmpty() && ca.equals(cb)) return 5.0;
        return 15.0;
    }

    /** R2-11.6: delivered legs of a barter. */
    public int getBarterDeliveredLegCount(long barterId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_DELIVERIES +
                " WHERE barter_id=? AND status='DELIVERED'", new String[]{String.valueOf(barterId)});
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    public int getBarterTotalLegCount(long barterId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_DELIVERIES +
                " WHERE barter_id=?", new String[]{String.valueOf(barterId)});
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    private BarterOffer cursorToBarter(Cursor c) {
        BarterOffer b = new BarterOffer(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getLong(c.getColumnIndexOrThrow("creator_id")),
                c.getString(c.getColumnIndexOrThrow("creator_name")),
                c.getString(c.getColumnIndexOrThrow("give_type")),
                c.getDouble(c.getColumnIndexOrThrow("give_qty")),
                c.getString(c.getColumnIndexOrThrow("give_unit")),
                c.getString(c.getColumnIndexOrThrow("want_type")),
                c.getDouble(c.getColumnIndexOrThrow("want_qty")),
                c.getString(c.getColumnIndexOrThrow("want_unit")),
                c.getString(c.getColumnIndexOrThrow("note")),
                c.getString(c.getColumnIndexOrThrow("location")),
                c.getLong(c.getColumnIndexOrThrow("accepter_id")),
                c.getString(c.getColumnIndexOrThrow("accepter_name")),
                c.getString(c.getColumnIndexOrThrow("status")),
                c.getString(c.getColumnIndexOrThrow("created_at"))
        );
        try { b.setAcceptedAt(c.getString(c.getColumnIndexOrThrow("accepted_at"))); } catch (Exception ignored) {}
        return b;
    }

    // ============================================================
    // AUTO-TRADE (R2-8)
    // ============================================================
    public List<AutoTradeRule> getAutoRules(long userId) {
        List<AutoTradeRule> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_AUTO_RULES +
                " WHERE user_id=? ORDER BY id ASC", new String[]{String.valueOf(userId)});
        while (c.moveToNext()) list.add(cursorToRule(c));
        c.close();
        return list;
    }

    public long saveAutoRule(AutoTradeRule r) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_id", r.getUserId());
        cv.put("energy_type", r.getEnergyType());
        cv.put("max_price", r.getMaxPrice());
        cv.put("max_qty", r.getMaxQty());
        cv.put("active", r.isActive() ? 1 : 0);
        if (r.getId() > 0) {
            db.update(DatabaseHelper.TABLE_AUTO_RULES, cv, "id=?",
                    new String[]{String.valueOf(r.getId())});
            return r.getId();
        } else {
            cv.put("created_at", now());
            return db.insert(DatabaseHelper.TABLE_AUTO_RULES, null, cv);
        }
    }

    public void deleteAutoRule(long ruleId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_AUTO_RULES, "id=?", new String[]{String.valueOf(ruleId)});
    }

    private AutoTradeRule cursorToRule(Cursor c) {
        return new AutoTradeRule(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getLong(c.getColumnIndexOrThrow("user_id")),
                c.getString(c.getColumnIndexOrThrow("energy_type")),
                c.getDouble(c.getColumnIndexOrThrow("max_price")),
                c.getDouble(c.getColumnIndexOrThrow("max_qty")),
                c.getInt(c.getColumnIndexOrThrow("active")) == 1,
                c.getString(c.getColumnIndexOrThrow("created_at"))
        );
    }

    public void evaluateAutoRulesForNewListing(EnergyListing listing) {
        if (listing == null || !"AVAILABLE".equalsIgnoreCase(listing.getStatus())) return;
        if (listing.getQuantity() <= 0) return;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + DatabaseHelper.TABLE_AUTO_RULES +
                        " WHERE active=1 AND LOWER(energy_type)=LOWER(?) AND max_price >= ? " +
                        " AND user_id != ? ORDER BY max_price ASC, created_at ASC",
                new String[]{listing.getEnergyType(), String.valueOf(listing.getPricePerUnit()),
                        String.valueOf(listing.getSellerId())});
        List<AutoTradeRule> matches = new ArrayList<>();
        while (c.moveToNext()) matches.add(cursorToRule(c));
        c.close();

        for (AutoTradeRule rule : matches) {
            EnergyListing current = getListingById(listing.getId());
            if (current == null || current.getQuantity() <= 0) break;

            double qty = Math.min(rule.getMaxQty(), current.getQuantity());
            if (qty <= 0) continue;

            UserProfile buyer = getUserById(rule.getUserId());
            if (buyer == null) continue;
            if (buyer.getId() == current.getSellerId()) continue;

            TradeResult res = executePartialPurchase(buyer, current, qty,
                    Config.DEFAULT_DISTANCE_KM, true);
            if (!res.success) {
                insertAutoBuyHistory(buyer.getId(), current, qty, 0.0, "SKIPPED_LOW_BALANCE");
            }
        }
    }

    private void insertAutoBuyHistory(long userId, EnergyListing listing, double qty,
                                      double pricePaid, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_id", userId);
        cv.put("listing_id", listing.getId());
        cv.put("seller_name", listing.getSellerName());
        cv.put("energy_type", listing.getEnergyType());
        cv.put("quantity", qty);
        cv.put("unit", listing.getUnit());
        cv.put("price_paid", pricePaid);
        cv.put("status", status);
        cv.put("created_at", now());
        db.insert(DatabaseHelper.TABLE_AUTO_BUY_HISTORY, null, cv);
    }

    public List<AutoBuyHistory> getAutoBuyHistory(long userId) {
        List<AutoBuyHistory> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_AUTO_BUY_HISTORY +
                " WHERE user_id=? ORDER BY id DESC LIMIT 100", new String[]{String.valueOf(userId)});
        while (c.moveToNext()) {
            list.add(new AutoBuyHistory(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    c.getLong(c.getColumnIndexOrThrow("user_id")),
                    c.getLong(c.getColumnIndexOrThrow("listing_id")),
                    c.getString(c.getColumnIndexOrThrow("seller_name")),
                    c.getString(c.getColumnIndexOrThrow("energy_type")),
                    c.getDouble(c.getColumnIndexOrThrow("quantity")),
                    c.getString(c.getColumnIndexOrThrow("unit")),
                    c.getDouble(c.getColumnIndexOrThrow("price_paid")),
                    c.getString(c.getColumnIndexOrThrow("status")),
                    c.getString(c.getColumnIndexOrThrow("created_at"))
            ));
        }
        c.close();
        return list;
    }

    public void runAutoTradeForAllUsers() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT DISTINCT energy_type FROM " + DatabaseHelper.TABLE_AUTO_RULES +
                " WHERE active=1", null);
        List<String> types = new ArrayList<>();
        while (c.moveToNext()) types.add(c.getString(0));
        c.close();

        for (String type : types) {
            List<EnergyListing> listings = getAllListings(type, "ALL");
            for (EnergyListing l : listings) evaluateAutoRulesForNewListing(l);
        }
    }
    public static class EnergyTotals {
        public double solarKwh, windKwh, batteryKwh, biogasM3, co2Kg;
        public double totalKwh() { return solarKwh + windKwh + batteryKwh; }
    }

    public EnergyTotals getBuyerEnergyTotals(long userId) {
        return getEnergyTotals("t.buyer_id = ?", new String[]{String.valueOf(userId)}, true);
    }

    public EnergyTotals getSellerEnergyTotals(long userId) {
        return getEnergyTotals("t.seller_id = ?", new String[]{String.valueOf(userId)}, false);
    }

    private EnergyTotals getEnergyTotals(String where, String[] args, boolean computeCo2) {
        EnergyTotals t = new EnergyTotals();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String q = "SELECT t.energy_type, t.quantity FROM " + DatabaseHelper.TABLE_TRADES + " t " +
                "JOIN " + DatabaseHelper.TABLE_DELIVERIES + " d ON d.trade_id = t.id " +
                "WHERE " + where + " AND d.status='DELIVERED'";
        Cursor c = db.rawQuery(q, args);
        while (c.moveToNext()) {
            String type = c.getString(0);
            double qty = c.getDouble(1);
            if (type == null) continue;
            switch (type) {
                case "Solar":   t.solarKwh += qty;   break;
                case "Wind":    t.windKwh += qty;    break;
                case "Battery": t.batteryKwh += qty; break;
                case "Biogas":  t.biogasM3 += qty;   break;
            }
        }
        c.close();
        if (computeCo2) {
            t.co2Kg = t.windKwh * Config.CO2_WIND_PER_KWH
                    + t.solarKwh * Config.CO2_SOLAR_PER_KWH
                    + t.batteryKwh * Config.CO2_BATTERY_PER_KWH
                    + t.biogasM3 * Config.CO2_BIOGAS_PER_M3;
        }
        return t;
    }

    public int getCompletedBuyerTradeCount(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TRADES + " t " +
                "JOIN " + DatabaseHelper.TABLE_DELIVERIES + " d ON d.trade_id = t.id " +
                "WHERE t.buyer_id=? AND d.status='DELIVERED'", new String[]{String.valueOf(userId)});
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    public int getJoinedPoolCount(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_POOL_MEMBERS +
                " WHERE user_id=?", new String[]{String.valueOf(userId)});
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    public static class MonthlyPoint {
        public String label;
        public double energyKwh;
        public double co2Kg;
    }

    public List<MonthlyPoint> getBuyerMonthlyImpact(long userId, int monthsBack) {
        List<MonthlyPoint> pts = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -(monthsBack - 1));
        for (int i = 0; i < monthsBack; i++) {
            MonthlyPoint p = new MonthlyPoint();
            p.label = monthLabel(cal.get(Calendar.MONTH));
            pts.add(p);
            cal.add(Calendar.MONTH, 1);
        }

        Calendar start = Calendar.getInstance();
        start.add(Calendar.MONTH, -(monthsBack - 1));
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT t.energy_type, t.quantity, t.timestamp FROM " + DatabaseHelper.TABLE_TRADES + " t " +
                        "JOIN " + DatabaseHelper.TABLE_DELIVERIES + " d ON d.trade_id = t.id " +
                        "WHERE t.buyer_id=? AND d.status='DELIVERED' AND t.timestamp >= ?",
                new String[]{String.valueOf(userId),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(start.getTime())});
        while (c.moveToNext()) {
            String type = c.getString(0);
            double qty = c.getDouble(1);
            int idx = monthIndexFromTs(c.getString(2), monthsBack);
            if (idx < 0 || idx >= pts.size()) continue;
            MonthlyPoint p = pts.get(idx);
            if ("Solar".equalsIgnoreCase(type))      { p.energyKwh += qty; p.co2Kg += qty * Config.CO2_SOLAR_PER_KWH; }
            else if ("Wind".equalsIgnoreCase(type))  { p.energyKwh += qty; p.co2Kg += qty * Config.CO2_WIND_PER_KWH; }
            else if ("Battery".equalsIgnoreCase(type)) { p.energyKwh += qty; p.co2Kg += qty * Config.CO2_BATTERY_PER_KWH; }
            else if ("Biogas".equalsIgnoreCase(type)){ p.co2Kg += qty * Config.CO2_BIOGAS_PER_M3; }
        }
        c.close();
        return pts;
    }

    private int monthIndexFromTs(String ts, int monthsBack) {
        if (ts == null) return -1;
        try {
            Date d;
            try {
                d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(ts);
            } catch (Exception e) {
                d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(ts);
            }
            if (d == null) return -1;
            Calendar now = Calendar.getInstance();
            Calendar then = Calendar.getInstance();
            then.setTime(d);
            int diff = (now.get(Calendar.YEAR) - then.get(Calendar.YEAR)) * 12
                    + (now.get(Calendar.MONTH) - then.get(Calendar.MONTH));
            return monthsBack - 1 - diff;
        } catch (Exception e) { return -1; }
    }

    private String monthLabel(int month0) {
        String[] m = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        return m[month0 % 12];
    }
    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }
    private static String safeString(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 ? c.getString(idx) : null;
    }
}