package com.example.warrantyvault;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "WarrantyVault.db";
    private static final int DATABASE_VERSION = 2;

    // Table Names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_PRODUCTS = "products";
    public static final String TABLE_SERVICE_RECORDS = "service_records";
    public static final String TABLE_DOCUMENTS = "documents";

    // Users Columns
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USER_NAME = "name";
    public static final String COLUMN_USER_EMAIL = "email";
    public static final String COLUMN_USER_PASSWORD = "password";

    // Products Columns
    public static final String COLUMN_PRODUCT_ID = "id";
    public static final String COLUMN_PRODUCT_USER_ID = "user_id";
    public static final String COLUMN_PRODUCT_NAME = "product_name";
    public static final String COLUMN_PRODUCT_BRAND = "brand";
    public static final String COLUMN_PRODUCT_CATEGORY = "category";
    public static final String COLUMN_PRODUCT_MODEL_NUMBER = "model_number";
    public static final String COLUMN_PRODUCT_SERIAL_NUMBER = "serial_number";
    public static final String COLUMN_PRODUCT_PURCHASE_DATE = "purchase_date";
    public static final String COLUMN_PRODUCT_PURCHASE_PRICE = "purchase_price";
    public static final String COLUMN_PRODUCT_WARRANTY_DURATION = "warranty_duration";
    public static final String COLUMN_PRODUCT_WARRANTY_EXPIRY = "warranty_expiry";
    public static final String COLUMN_PRODUCT_WARRANTY_STATUS = "warranty_status";
    public static final String COLUMN_PRODUCT_IMAGE_PATH = "product_image_path";
    public static final String COLUMN_PRODUCT_BILL_IMAGE_PATH = "bill_image_path";

    // Service Records Columns
    public static final String COLUMN_SERVICE_ID = "id";
    public static final String COLUMN_SERVICE_USER_ID = "user_id";
    public static final String COLUMN_SERVICE_PRODUCT_ID = "product_id";
    public static final String COLUMN_SERVICE_PRODUCT_NAME = "product_name";
    public static final String COLUMN_SERVICE_DATE = "service_date";
    public static final String COLUMN_SERVICE_CENTRE = "service_centre";
    public static final String COLUMN_SERVICE_DESCRIPTION = "description";
    public static final String COLUMN_SERVICE_COST = "cost";
    public static final String COLUMN_SERVICE_REMARKS = "remarks";

    // Documents Columns
    public static final String COLUMN_DOC_ID = "id";
    public static final String COLUMN_DOC_USER_ID = "user_id";
    public static final String COLUMN_DOC_PRODUCT_ID = "product_id";
    public static final String COLUMN_DOC_NAME = "display_name";
    public static final String COLUMN_DOC_PATH = "file_path";
    public static final String COLUMN_DOC_MIME = "mime_type";
    public static final String COLUMN_DOC_SIZE = "size_bytes";
    public static final String COLUMN_DOC_ADDED_AT = "added_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USER_NAME + " TEXT NOT NULL, "
                + COLUMN_USER_EMAIL + " TEXT UNIQUE NOT NULL, "
                + COLUMN_USER_PASSWORD + " TEXT NOT NULL" + ")";

        String CREATE_PRODUCTS_TABLE = "CREATE TABLE " + TABLE_PRODUCTS + "("
                + COLUMN_PRODUCT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_PRODUCT_USER_ID + " INTEGER NOT NULL, "
                + COLUMN_PRODUCT_NAME + " TEXT, "
                + COLUMN_PRODUCT_BRAND + " TEXT, "
                + COLUMN_PRODUCT_CATEGORY + " TEXT, "
                + COLUMN_PRODUCT_MODEL_NUMBER + " TEXT, "
                + COLUMN_PRODUCT_SERIAL_NUMBER + " TEXT, "
                + COLUMN_PRODUCT_PURCHASE_DATE + " TEXT, "
                + COLUMN_PRODUCT_PURCHASE_PRICE + " REAL, "
                + COLUMN_PRODUCT_WARRANTY_DURATION + " INTEGER, "
                + COLUMN_PRODUCT_WARRANTY_EXPIRY + " TEXT, "
                + COLUMN_PRODUCT_WARRANTY_STATUS + " TEXT, "
                + COLUMN_PRODUCT_IMAGE_PATH + " TEXT, "
                + COLUMN_PRODUCT_BILL_IMAGE_PATH + " TEXT, "
                + "FOREIGN KEY(" + COLUMN_PRODUCT_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ") ON DELETE CASCADE" + ")";

        String CREATE_SERVICE_RECORDS_TABLE = "CREATE TABLE " + TABLE_SERVICE_RECORDS + "("
                + COLUMN_SERVICE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_SERVICE_USER_ID + " INTEGER NOT NULL, "
                + COLUMN_SERVICE_PRODUCT_ID + " INTEGER NOT NULL, "
                + COLUMN_SERVICE_PRODUCT_NAME + " TEXT, "
                + COLUMN_SERVICE_DATE + " TEXT, "
                + COLUMN_SERVICE_CENTRE + " TEXT, "
                + COLUMN_SERVICE_DESCRIPTION + " TEXT, "
                + COLUMN_SERVICE_COST + " REAL, "
                + COLUMN_SERVICE_REMARKS + " TEXT, "
                + "FOREIGN KEY(" + COLUMN_SERVICE_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY(" + COLUMN_SERVICE_PRODUCT_ID + ") REFERENCES " + TABLE_PRODUCTS + "(" + COLUMN_PRODUCT_ID + ") ON DELETE CASCADE" + ")";

        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_PRODUCTS_TABLE);
        db.execSQL(CREATE_SERVICE_RECORDS_TABLE);
        createDocumentsTable(db);
    }

    private void createDocumentsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DOCUMENTS + "("
                + COLUMN_DOC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_DOC_USER_ID + " INTEGER NOT NULL, "
                + COLUMN_DOC_PRODUCT_ID + " INTEGER NOT NULL, "
                + COLUMN_DOC_NAME + " TEXT, "
                + COLUMN_DOC_PATH + " TEXT, "
                + COLUMN_DOC_MIME + " TEXT, "
                + COLUMN_DOC_SIZE + " INTEGER, "
                + COLUMN_DOC_ADDED_AT + " INTEGER, "
                + "FOREIGN KEY(" + COLUMN_DOC_PRODUCT_ID + ") REFERENCES " + TABLE_PRODUCTS + "(" + COLUMN_PRODUCT_ID + ") ON DELETE CASCADE" + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // v2: warranty claims removed, documents added. User data is kept.
            db.execSQL("DROP TABLE IF EXISTS warranty_claims");
            createDocumentsTable(db);
        }
    }

    // ==========================================
    // SECURITY: SHA-256 Password Hashing Helper
    // ==========================================
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password; // Fallback
        }
    }

    // ==========================================
    // USERS METHODS
    // ==========================================
    public long insertUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_NAME, name);
        values.put(COLUMN_USER_EMAIL, email);
        values.put(COLUMN_USER_PASSWORD, hashPassword(password));
        long id = db.insert(TABLE_USERS, null, values);
        return id;
    }

    public Cursor getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_USERS, null, COLUMN_USER_EMAIL + "=?", new String[]{email}, null, null, null);
    }

    public Cursor getUserById(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_USERS, null, COLUMN_USER_ID + "=?", new String[]{String.valueOf(userId)}, null, null, null);
    }

    public boolean validateUserLogin(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String hashedPassword = hashPassword(password);
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_ID},
                COLUMN_USER_EMAIL + "=? AND " + COLUMN_USER_PASSWORD + "=?",
                new String[]{email, hashedPassword}, null, null, null);
        boolean isValid = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        return isValid;
    }

    public int updateUserName(long userId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_NAME, newName);
        int rows = db.update(TABLE_USERS, values, COLUMN_USER_ID + "=?", new String[]{String.valueOf(userId)});
        return rows;
    }

    // ==========================================
    // PRODUCTS METHODS
    // ==========================================
    public long insertProduct(long userId, String productName, String brand, String category,
                              String modelNumber, String serialNumber, String purchaseDate,
                              double purchasePrice, int warrantyDuration, String warrantyExpiry,
                              String warrantyStatus, String productImagePath, String billImagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PRODUCT_USER_ID, userId);
        values.put(COLUMN_PRODUCT_NAME, productName);
        values.put(COLUMN_PRODUCT_BRAND, brand);
        values.put(COLUMN_PRODUCT_CATEGORY, category);
        values.put(COLUMN_PRODUCT_MODEL_NUMBER, modelNumber);
        values.put(COLUMN_PRODUCT_SERIAL_NUMBER, serialNumber);
        values.put(COLUMN_PRODUCT_PURCHASE_DATE, purchaseDate);
        values.put(COLUMN_PRODUCT_PURCHASE_PRICE, purchasePrice);
        values.put(COLUMN_PRODUCT_WARRANTY_DURATION, warrantyDuration);
        values.put(COLUMN_PRODUCT_WARRANTY_EXPIRY, warrantyExpiry);
        values.put(COLUMN_PRODUCT_WARRANTY_STATUS, warrantyStatus);
        values.put(COLUMN_PRODUCT_IMAGE_PATH, productImagePath);
        values.put(COLUMN_PRODUCT_BILL_IMAGE_PATH, billImagePath);
        long id = db.insert(TABLE_PRODUCTS, null, values);
        return id;
    }

    public Cursor getProductsByUser(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_PRODUCTS, null, COLUMN_PRODUCT_USER_ID + "=?", new String[]{String.valueOf(userId)}, null, null, null);
    }

    public Cursor getProductById(long productId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_PRODUCTS, null, COLUMN_PRODUCT_ID + "=?", new String[]{String.valueOf(productId)}, null, null, null);
    }

    public int deleteProduct(long productId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_PRODUCTS, COLUMN_PRODUCT_ID + "=?", new String[]{String.valueOf(productId)});
        return rows;
    }

    public int updateProduct(long productId, String productName, String brand, String category,
                             String modelNumber, String serialNumber, String purchaseDate,
                             double purchasePrice, int warrantyDuration, String warrantyExpiry,
                             String warrantyStatus, String productImagePath, String billImagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PRODUCT_NAME, productName);
        values.put(COLUMN_PRODUCT_BRAND, brand);
        values.put(COLUMN_PRODUCT_CATEGORY, category);
        values.put(COLUMN_PRODUCT_MODEL_NUMBER, modelNumber);
        values.put(COLUMN_PRODUCT_SERIAL_NUMBER, serialNumber);
        values.put(COLUMN_PRODUCT_PURCHASE_DATE, purchaseDate);
        values.put(COLUMN_PRODUCT_PURCHASE_PRICE, purchasePrice);
        values.put(COLUMN_PRODUCT_WARRANTY_DURATION, warrantyDuration);
        values.put(COLUMN_PRODUCT_WARRANTY_EXPIRY, warrantyExpiry);
        values.put(COLUMN_PRODUCT_WARRANTY_STATUS, warrantyStatus);
        values.put(COLUMN_PRODUCT_IMAGE_PATH, productImagePath);
        values.put(COLUMN_PRODUCT_BILL_IMAGE_PATH, billImagePath);
        int rows = db.update(TABLE_PRODUCTS, values, COLUMN_PRODUCT_ID + "=?", new String[]{String.valueOf(productId)});
        return rows;
    }

    // ==========================================
    // SERVICE RECORDS METHODS
    // ==========================================
    public long insertServiceRecord(long userId, long productId, String productName,
                                   String serviceDate, String serviceCentre, String description,
                                   double cost, String remarks) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SERVICE_USER_ID, userId);
        values.put(COLUMN_SERVICE_PRODUCT_ID, productId);
        values.put(COLUMN_SERVICE_PRODUCT_NAME, productName);
        values.put(COLUMN_SERVICE_DATE, serviceDate);
        values.put(COLUMN_SERVICE_CENTRE, serviceCentre);
        values.put(COLUMN_SERVICE_DESCRIPTION, description);
        values.put(COLUMN_SERVICE_COST, cost);
        values.put(COLUMN_SERVICE_REMARKS, remarks);
        long id = db.insert(TABLE_SERVICE_RECORDS, null, values);
        return id;
    }

    public Cursor getServiceRecordsByUser(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_SERVICE_RECORDS, null, COLUMN_SERVICE_USER_ID + "=?", new String[]{String.valueOf(userId)}, null, null, null);
    }

    public Cursor getServiceRecordsByProduct(long productId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_SERVICE_RECORDS, null, COLUMN_SERVICE_PRODUCT_ID + "=?", new String[]{String.valueOf(productId)}, null, null, null);
    }

    public int deleteServiceRecord(long serviceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_SERVICE_RECORDS, COLUMN_SERVICE_ID + "=?", new String[]{String.valueOf(serviceId)});
        return rows;
    }

    // ==========================================
    // DOCUMENTS METHODS
    // ==========================================
    public long insertDocument(long userId, long productId, String name, String path, String mime, long size) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DOC_USER_ID, userId);
        values.put(COLUMN_DOC_PRODUCT_ID, productId);
        values.put(COLUMN_DOC_NAME, name);
        values.put(COLUMN_DOC_PATH, path);
        values.put(COLUMN_DOC_MIME, mime);
        values.put(COLUMN_DOC_SIZE, size);
        values.put(COLUMN_DOC_ADDED_AT, System.currentTimeMillis());
        return db.insert(TABLE_DOCUMENTS, null, values);
    }

    public List<ProductDocument> getDocumentsByProduct(long productId) {
        List<ProductDocument> out = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_DOCUMENTS, null, COLUMN_DOC_PRODUCT_ID + "=?",
                new String[]{String.valueOf(productId)}, null, null, COLUMN_DOC_ADDED_AT + " DESC");
        try {
            while (c.moveToNext()) {
                ProductDocument d = new ProductDocument();
                d.id = c.getLong(c.getColumnIndexOrThrow(COLUMN_DOC_ID));
                d.productId = c.getLong(c.getColumnIndexOrThrow(COLUMN_DOC_PRODUCT_ID));
                d.name = c.getString(c.getColumnIndexOrThrow(COLUMN_DOC_NAME));
                d.path = c.getString(c.getColumnIndexOrThrow(COLUMN_DOC_PATH));
                d.mime = c.getString(c.getColumnIndexOrThrow(COLUMN_DOC_MIME));
                d.size = c.getLong(c.getColumnIndexOrThrow(COLUMN_DOC_SIZE));
                d.addedAt = c.getLong(c.getColumnIndexOrThrow(COLUMN_DOC_ADDED_AT));
                out.add(d);
            }
        } finally {
            c.close();
        }
        return out;
    }

    public int deleteDocument(long docId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_DOCUMENTS, COLUMN_DOC_ID + "=?", new String[]{String.valueOf(docId)});
    }

    public int updateProductBill(long productId, String billPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PRODUCT_BILL_IMAGE_PATH, billPath);
        return db.update(TABLE_PRODUCTS, values, COLUMN_PRODUCT_ID + "=?", new String[]{String.valueOf(productId)});
    }

    // ==========================================
    // PRODUCT LIST (single place that builds Product objects, status always fresh)
    // ==========================================
    public List<Product> getProductsList(long userId) {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_PRODUCTS, null, COLUMN_PRODUCT_USER_ID + "=?",
                new String[]{String.valueOf(userId)}, null, null, COLUMN_PRODUCT_ID + " DESC");
        try {
            while (c.moveToNext()) {
                list.add(productFromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public Product getProductObject(long productId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_PRODUCTS, null, COLUMN_PRODUCT_ID + "=?",
                new String[]{String.valueOf(productId)}, null, null, null);
        try {
            return c.moveToFirst() ? productFromCursor(c) : null;
        } finally {
            c.close();
        }
    }

    private static String str(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i == -1 || c.isNull(i)) return "";
        return c.getString(i);
    }

    private Product productFromCursor(Cursor c) {
        Product p = new Product();
        p.setProductId(str(c, COLUMN_PRODUCT_ID));
        p.setUserId(str(c, COLUMN_PRODUCT_USER_ID));
        p.setProductName(str(c, COLUMN_PRODUCT_NAME));
        p.setBrand(str(c, COLUMN_PRODUCT_BRAND));
        p.setCategory(str(c, COLUMN_PRODUCT_CATEGORY));
        p.setModelNumber(str(c, COLUMN_PRODUCT_MODEL_NUMBER));
        p.setSerialNumber(str(c, COLUMN_PRODUCT_SERIAL_NUMBER));
        p.setPurchaseDate(str(c, COLUMN_PRODUCT_PURCHASE_DATE));
        String price = str(c, COLUMN_PRODUCT_PURCHASE_PRICE);
        p.setPurchasePrice(price.isEmpty() ? "0" : price);
        String dur = str(c, COLUMN_PRODUCT_WARRANTY_DURATION);
        p.setWarrantyDuration(dur.isEmpty() ? "12" : dur);
        String expiry = str(c, COLUMN_PRODUCT_WARRANTY_EXPIRY);
        p.setWarrantyExpiry(expiry);
        // Status is recomputed from the expiry date so it never goes stale.
        p.setWarrantyStatus(WarrantyUtils.computeStatus(expiry));
        p.setProductImageUrl(str(c, COLUMN_PRODUCT_IMAGE_PATH));
        p.setBillImageUrl(str(c, COLUMN_PRODUCT_BILL_IMAGE_PATH));
        return p;
    }

    // ==========================================
    // STATISTICS / COUNTS METHODS
    // ==========================================
    public int getProductCount(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COLUMN_PRODUCT_ID},
                COLUMN_PRODUCT_USER_ID + "=?", new String[]{String.valueOf(userId)},
                null, null, null);
        int count = cursor != null ? cursor.getCount() : 0;
        if (cursor != null) {
            cursor.close();
        }
        return count;
    }

    public int getActiveWarrantyCount(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COLUMN_PRODUCT_ID},
                COLUMN_PRODUCT_USER_ID + "=? AND " + COLUMN_PRODUCT_WARRANTY_STATUS + "=?",
                new String[]{String.valueOf(userId), "ACTIVE"},
                null, null, null);
        int count = cursor != null ? cursor.getCount() : 0;
        if (cursor != null) {
            cursor.close();
        }
        return count;
    }
}
