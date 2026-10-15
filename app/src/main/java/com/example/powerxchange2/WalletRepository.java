package com.example.powerxchange2;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class WalletRepository {
    public static final String TYPE_ADD_CREDITS = "ADD_CREDITS";
    public static final String TYPE_WITHDRAW    = "WITHDRAW";
    public static final String TYPE_PURCHASE    = "PURCHASE";
    public static final String TYPE_SALE        = "SALE";
    public static final String TYPE_POOL_REFUND = "POOL_REFUND";
    public static final String TYPE_DELIVERY_EARNING = "DELIVERY_EARNING";
    public static final String METHOD_CARD     = "CARD";
    public static final String METHOD_INTERNAL = "INTERNAL";
    private final DatabaseHelper dbHelper;
    public WalletRepository(Context c) {
        this(new DatabaseHelper(c.getApplicationContext()));
    }
    public WalletRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }
    public double getBalance(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT wallet_balance FROM " + DatabaseHelper.TABLE_USERS + " WHERE id=?", new String[]{String.valueOf(userId)});
        double bal = 0;
        if (c.moveToFirst()) bal = c.getDouble(0);
        c.close();
        return bal;
    }
    public synchronized boolean adjustBalance(long userId, double delta, String type, String description, long referenceId) {
        return adjustBalance(userId, delta, type, description, METHOD_INTERNAL, null, referenceId);
    }
    public synchronized boolean adjustBalance(long userId, double delta, String type, String description, String method, String referenceLast4, long referenceId) {
        if (userId <= 0 || delta == 0) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try{
            Cursor c = db.rawQuery("SELECT wallet_balance FROM " + DatabaseHelper.TABLE_USERS +
                    " WHERE id=?", new String[]{String.valueOf(userId)});
            if (!c.moveToFirst()) { c.close(); return false; }
            double cur = c.getDouble(0);
            c.close();
            double next = cur + delta;
            if (next < 0) return false;
            ContentValues cv = new ContentValues();
            cv.put("wallet_balance", next);
            db.update(DatabaseHelper.TABLE_USERS, cv, "id=?", new String[]{String.valueOf(userId)});
            insertTx(db, userId, delta, type, description, method, referenceLast4, referenceId);
            db.setTransactionSuccessful();
            return true;
        } finally { db.endTransaction(); }
    }
    public synchronized boolean transfer(long fromId, long toId, double amount,
                                         String fromDesc, String toDesc, long refId) {
        if (amount <= 0) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("SELECT wallet_balance FROM " + DatabaseHelper.TABLE_USERS +
                    " WHERE id=?", new String[]{String.valueOf(fromId)});
            if (!c.moveToFirst()) { c.close(); return false; }
            double fromBal = c.getDouble(0);
            c.close();
            if (fromBal < amount) return false;

            ContentValues cv1 = new ContentValues();
            cv1.put("wallet_balance", fromBal - amount);
            db.update(DatabaseHelper.TABLE_USERS, cv1, "id=?", new String[]{String.valueOf(fromId)});

            Cursor c2 = db.rawQuery("SELECT wallet_balance FROM " + DatabaseHelper.TABLE_USERS +
                    " WHERE id=?", new String[]{String.valueOf(toId)});
            double toBal = 0;
            if (c2.moveToFirst()) toBal = c2.getDouble(0);
            c2.close();
            ContentValues cv2 = new ContentValues();
            cv2.put("wallet_balance", toBal + amount);
            db.update(DatabaseHelper.TABLE_USERS, cv2, "id=?", new String[]{String.valueOf(toId)});

            insertTx(db, fromId, -amount, TYPE_PURCHASE, fromDesc, METHOD_INTERNAL, null, refId);
            insertTx(db, toId,    amount, TYPE_SALE,     toDesc,   METHOD_INTERNAL, null, refId);

            db.setTransactionSuccessful();
            return true;
        } finally { db.endTransaction(); }
    }
    public synchronized boolean addCreditsFromCard(long userId, DemoCard card, double amount) {
        if (amount <= 0 || card == null) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("SELECT balance FROM " + DatabaseHelper.TABLE_DEMO_CARDS +
                    " WHERE id=?", new String[]{String.valueOf(card.id)});
            if (!c.moveToFirst()) { c.close(); return false; }
            double cardBal = c.getDouble(0);
            c.close();
            if (cardBal < amount) return false;

            ContentValues cv1 = new ContentValues();
            cv1.put("balance", cardBal - amount);
            db.update(DatabaseHelper.TABLE_DEMO_CARDS, cv1, "id=?", new String[]{String.valueOf(card.id)});

            Cursor c2 = db.rawQuery("SELECT wallet_balance FROM " + DatabaseHelper.TABLE_USERS +
                    " WHERE id=?", new String[]{String.valueOf(userId)});
            if (!c2.moveToFirst()) { c2.close(); return false; }
            double walletBal = c2.getDouble(0);
            c2.close();
            ContentValues cv2 = new ContentValues();
            cv2.put("wallet_balance", walletBal + amount);
            db.update(DatabaseHelper.TABLE_USERS, cv2, "id=?", new String[]{String.valueOf(userId)});

            insertTx(db, userId, amount, TYPE_ADD_CREDITS, "Added credits from card ending " + card.last4(),
                    METHOD_CARD, card.last4(), card.id);

            db.setTransactionSuccessful();
            return true;
        } finally { db.endTransaction(); }
    }

    /** R2-5.4: withdraw credits back to demo card. */
    public synchronized boolean withdrawToCard(long userId, DemoCard card, double amount) {
        if (amount <= 0 || card == null) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("SELECT wallet_balance FROM " + DatabaseHelper.TABLE_USERS +
                    " WHERE id=?", new String[]{String.valueOf(userId)});
            if (!c.moveToFirst()) { c.close(); return false; }
            double walletBal = c.getDouble(0);
            c.close();
            if (walletBal < amount) return false;

            ContentValues cv1 = new ContentValues();
            cv1.put("wallet_balance", walletBal - amount);
            db.update(DatabaseHelper.TABLE_USERS, cv1, "id=?", new String[]{String.valueOf(userId)});

            Cursor c2 = db.rawQuery("SELECT balance FROM " + DatabaseHelper.TABLE_DEMO_CARDS +
                    " WHERE id=?", new String[]{String.valueOf(card.id)});
            double cardBal = 0;
            if (c2.moveToFirst()) cardBal = c2.getDouble(0);
            c2.close();
            ContentValues cv2 = new ContentValues();
            cv2.put("balance", cardBal + amount);
            db.update(DatabaseHelper.TABLE_DEMO_CARDS, cv2, "id=?", new String[]{String.valueOf(card.id)});

            insertTx(db, userId, -amount, TYPE_WITHDRAW, "Withdrew credits to card ending " + card.last4(),
                    METHOD_CARD, card.last4(), card.id);

            db.setTransactionSuccessful();
            return true;
        } finally { db.endTransaction(); }
    }

    // ---------------- Demo cards ----------------

    public List<DemoCard> getAllDemoCards() {
        List<DemoCard> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_DEMO_CARDS + " ORDER BY id ASC", null);
        while (c.moveToNext()) list.add(cursorToCard(c));
        c.close();
        return list;
    }

    public DemoCard findCardByNumber(String rawNumber) {
        String digits = normalizeDigits(rawNumber);
        if (digits.isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_DEMO_CARDS +
                " WHERE card_number=?", new String[]{digits});
        DemoCard card = null;
        if (c.moveToFirst()) card = cursorToCard(c);
        c.close();
        return card;
    }

    public DemoCard getCardById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_DEMO_CARDS + " WHERE id=?",
                new String[]{String.valueOf(id)});
        DemoCard card = null;
        if (c.moveToFirst()) card = cursorToCard(c);
        c.close();
        return card;
    }

    // ---------------- Helpers ----------------

    public static String normalizeDigits(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\D", "");
    }

    private void insertTx(SQLiteDatabase db, long userId, double amount, String type,
                          String description, String method, String last4, long refId) {
        ContentValues tx = new ContentValues();
        tx.put("user_id", userId);
        tx.put("amount", amount);
        tx.put("type", type);
        tx.put("description", description);
        tx.put("method", method);
        tx.put("reference_last4", last4);
        tx.put("reference_id", refId);
        tx.put("created_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        db.insert(DatabaseHelper.TABLE_WALLET_TX, null, tx);
    }

    private DemoCard cursorToCard(Cursor c) {
        return new DemoCard(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("card_number")),
                c.getString(c.getColumnIndexOrThrow("holder_name")),
                c.getString(c.getColumnIndexOrThrow("expiry")),
                c.getString(c.getColumnIndexOrThrow("cvv")),
                c.getString(c.getColumnIndexOrThrow("card_type")),
                c.getString(c.getColumnIndexOrThrow("network")),
                c.getString(c.getColumnIndexOrThrow("bank_name")),
                c.getDouble(c.getColumnIndexOrThrow("balance"))
        );
    }
}