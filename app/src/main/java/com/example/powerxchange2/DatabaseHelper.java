package com.example.powerxchange2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "powerxchange.db";
    private static final int DB_VERSION = 6;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_LISTINGS = "listings";
    public static final String TABLE_TRADES = "trades";
    public static final String TABLE_DELIVERIES = "deliveries";
    public static final String TABLE_POOLS = "pools";
    public static final String TABLE_POOL_MEMBERS = "pool_members";
    public static final String TABLE_BARTERS = "barters";
    public static final String TABLE_AUTO_RULES = "auto_rules";
    public static final String TABLE_AUTO_BUY_HISTORY = "auto_buy_history";
    public static final String TABLE_WALLET_TX = "wallet_transactions";
    public static final String TABLE_DEMO_CARDS = "demo_cards";

    public DatabaseHelper(Context c) { super(c, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT UNIQUE, phone TEXT, " +
                "password_hash TEXT, role TEXT, user_type TEXT, company_name TEXT, city_location TEXT, " +
                "wallet_balance REAL DEFAULT 0.0, traded_kwh REAL DEFAULT 0.0, " +
                "co2_offset REAL DEFAULT 0.0, money_saved REAL DEFAULT 0.0, created_at TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_LISTINGS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, seller_id INTEGER, seller_name TEXT, seller_type TEXT, " +
                "company_name TEXT, energy_type TEXT, unit TEXT, quantity REAL, price_per_unit REAL, " +
                "location TEXT, status TEXT, created_at TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_TRADES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, listing_id INTEGER, buyer_id INTEGER, buyer_name TEXT, " +
                "seller_id INTEGER, seller_name TEXT, energy_type TEXT, unit TEXT, quantity REAL, " +
                "energy_price REAL, delivery_fee REAL, platform_cut REAL, partner_cut REAL, " +
                "total_price REAL, delivery_code TEXT, timestamp TEXT, order_type TEXT DEFAULT 'PURCHASE')");

        db.execSQL("CREATE TABLE " + TABLE_DELIVERIES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, trade_id INTEGER, " +
                "barter_id INTEGER DEFAULT 0, leg_no INTEGER DEFAULT 0, order_type TEXT DEFAULT 'PURCHASE', " +
                "sender_id INTEGER DEFAULT 0, receiver_id INTEGER DEFAULT 0, " +
                "pickup_address TEXT, drop_address TEXT, distance_km REAL, " +
                "delivery_fee_total REAL, platform_cut REAL, partner_cut REAL, " +
                "status TEXT, partner_id INTEGER DEFAULT 0, delivery_code TEXT, " +
                "created_at TEXT, accepted_at TEXT, picked_up_at TEXT, delivered_at TEXT)");

        createPoolTables(db);
        createAutoRuleTables(db);

        db.execSQL("CREATE TABLE " + TABLE_BARTERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, creator_id INTEGER, creator_name TEXT, " +
                "give_type TEXT, give_qty REAL, give_unit TEXT, want_type TEXT, want_qty REAL, want_unit TEXT, " +
                "note TEXT, location TEXT, accepter_id INTEGER DEFAULT 0, accepter_name TEXT, " +
                "status TEXT, created_at TEXT, accepted_at TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_WALLET_TX + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, amount REAL, " +
                "type TEXT, description TEXT, method TEXT, reference_last4 TEXT, " +
                "reference_id INTEGER DEFAULT 0, created_at TEXT)");

        createDemoCards(db);
        seedInitialData(db);
    }

    private void createPoolTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_POOLS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, listing_id INTEGER, creator_id INTEGER, " +
                "creator_name TEXT, total_qty REAL, joined_qty REAL, unit TEXT, price_per_unit REAL, " +
                "status TEXT, created_at TEXT, expires_at TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_POOL_MEMBERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, pool_id INTEGER, user_id INTEGER, user_name TEXT, " +
                "qty REAL, amount_paid REAL, delivery_fee REAL, delivery_code TEXT, status TEXT)");
    }

    private void createAutoRuleTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_AUTO_RULES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, energy_type TEXT, " +
                "max_price REAL, max_qty REAL, active INTEGER DEFAULT 1, created_at TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_AUTO_BUY_HISTORY + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, listing_id INTEGER, " +
                "seller_name TEXT, energy_type TEXT, quantity REAL, unit TEXT, price_paid REAL, " +
                "status TEXT, created_at TEXT)");
    }

    private void createDemoCards(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_DEMO_CARDS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, card_number TEXT UNIQUE, holder_name TEXT, " +
                "expiry TEXT, cvv TEXT, card_type TEXT, network TEXT, bank_name TEXT, " +
                "balance REAL DEFAULT 1000000.0, created_at TEXT)");

        for (DemoCardSeed.Seed s : DemoCardSeed.cards()) {
            db.execSQL("INSERT INTO " + TABLE_DEMO_CARDS +
                            " (card_number, holder_name, expiry, cvv, card_type, network, bank_name, balance, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{s.cardNumber, s.holderName, s.expiry, s.cvv,
                            s.cardType, s.network, s.bankName, s.balance, "2025-01-01"});
        }
    }

    private void seedInitialData(SQLiteDatabase db) {
        String pass = Config.hashPassword("password123");

        db.execSQL("INSERT INTO users (name,email,phone,password_hash,role,user_type,company_name,city_location,wallet_balance,traded_kwh,co2_offset,money_saved,created_at) VALUES " +
                "('Tata Power Solar Microgrid','prosumer@powerx.com','+91 9876543210','" + pass + "','PROSUMER','CORPORATE','Tata Power','Mumbai',100000.0,0.0,0.0,0.0,'2025-01-01')");
        db.execSQL("INSERT INTO users (name,email,phone,password_hash,role,user_type,company_name,city_location,wallet_balance,traded_kwh,co2_offset,money_saved,created_at) VALUES " +
                "('Rahul Sharma','consumer@powerx.com','+91 9812345678','" + pass + "','CONSUMER','INDIVIDUAL','','Mumbai',100000.0,0.0,0.0,0.0,'2025-01-01')");
        db.execSQL("INSERT INTO users (name,email,phone,password_hash,role,user_type,company_name,city_location,wallet_balance,traded_kwh,co2_offset,money_saved,created_at) VALUES " +
                "('Vikram Singh (Express Courier)','delivery@powerx.com','+91 9765432109','" + pass + "','DELIVERY','INDIVIDUAL','','Mumbai',100000.0,0.0,0.0,0.0,'2025-01-01')");

        db.execSQL("INSERT INTO listings (seller_id,seller_name,seller_type,company_name,energy_type,unit,quantity,price_per_unit,location,status,created_at) VALUES " +
                "(1,'Tata Power','CORPORATE','Tata Power','Solar','kWh',120.0,2.34,'Mumbai','AVAILABLE','2025-01-02')," +
                "(1,'Adani Green Energy','CORPORATE','Adani Green Energy','Wind','kWh',200.0,3.85,'Ahmedabad','AVAILABLE','2025-01-02')," +
                "(1,'Alex Green Microgrid','INDIVIDUAL','','Battery','kWh',40.0,2.00,'Pune','AVAILABLE','2025-01-02')," +
                "(1,'BioClean Agri Farm','INDIVIDUAL','','Biogas','m³',75.0,69.00,'Nashik','AVAILABLE','2025-01-02')," +
                "(1,'ReNew Power Solar','CORPORATE','ReNew Power','Solar','kWh',90.0,2.55,'Gurugram','AVAILABLE','2025-01-02')," +
                "(1,'Greenko Wind Farms','CORPORATE','Greenko','Wind','kWh',150.0,3.50,'Hyderabad','AVAILABLE','2025-01-02')," +
                "(1,'Suzlon Energy','CORPORATE','Suzlon Energy','Wind','kWh',80.0,4.10,'Pune','AVAILABLE','2025-01-02')," +
                "(1,'Sunrise Biogas','INDIVIDUAL','','Biogas','m³',60.0,63.00,'Nashik','AVAILABLE','2025-01-02')," +
                "(1,'Waaree Solar Rooftop','CORPORATE','Waaree Energies','Solar','kWh',100.0,2.15,'Surat','AVAILABLE','2025-01-02')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 4) {
            safeAddColumn(db, TABLE_DELIVERIES, "accepted_at", "TEXT");
            safeAddColumn(db, TABLE_DELIVERIES, "picked_up_at", "TEXT");

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_POOLS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_POOL_MEMBERS);
            createPoolTables(db);

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUTO_RULES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUTO_BUY_HISTORY);
            createAutoRuleTables(db);

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_WALLET_TX);
            db.execSQL("CREATE TABLE " + TABLE_WALLET_TX + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, amount REAL, " +
                    "type TEXT, description TEXT, method TEXT, reference_last4 TEXT, " +
                    "reference_id INTEGER DEFAULT 0, created_at TEXT)");

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEMO_CARDS);
            createDemoCards(db);

            db.execSQL("UPDATE listings SET unit='m³' WHERE LOWER(energy_type)='biogas'");
            db.execSQL("UPDATE trades SET unit='m³' WHERE LOWER(energy_type)='biogas'");
            db.execSQL("UPDATE barters SET give_unit='m³' WHERE LOWER(give_type)='biogas'");
            db.execSQL("UPDATE barters SET want_unit='m³' WHERE LOWER(want_type)='biogas'");
        }
        if (oldV < 5) {
            // R2-11 additions
            safeAddColumn(db, TABLE_DELIVERIES, "barter_id",    "INTEGER DEFAULT 0");
            safeAddColumn(db, TABLE_DELIVERIES, "leg_no",       "INTEGER DEFAULT 0");
            safeAddColumn(db, TABLE_DELIVERIES, "order_type",   "TEXT DEFAULT 'PURCHASE'");
            safeAddColumn(db, TABLE_DELIVERIES, "sender_id",    "INTEGER DEFAULT 0");
            safeAddColumn(db, TABLE_DELIVERIES, "receiver_id",  "INTEGER DEFAULT 0");
            safeAddColumn(db, TABLE_TRADES,     "order_type",   "TEXT DEFAULT 'PURCHASE'");
            safeAddColumn(db, TABLE_BARTERS,    "accepted_at",  "TEXT");
        }
        if (oldV < 6) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEMO_CARDS);
            createDemoCards(db);
        }
    }

    private void safeAddColumn(SQLiteDatabase db, String t, String col, String type) {
        Cursor c = db.rawQuery("PRAGMA table_info(" + t + ")", null);
        boolean exists = false;
        while (c.moveToNext()) if (col.equalsIgnoreCase(c.getString(c.getColumnIndexOrThrow("name")))) { exists = true; break; }
        c.close();
        if (!exists) db.execSQL("ALTER TABLE " + t + " ADD COLUMN " + col + " " + type);
    }
}