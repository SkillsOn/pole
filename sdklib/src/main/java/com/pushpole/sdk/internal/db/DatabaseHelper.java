package com.pushpole.sdk.internal.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created on 16-03-29, 3:57 PM.
 *
 * @author Akram Shokri
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Table Names
    static final String TABLE_TASK = "_task_table";
    static final String TABLE_UPSTREAM_MSG_DOWNSTREAM_NOTIF = "_upstream_n_notif_table";
    static final String TABLE_COLLECTION = "_collection_data";

    // Common column names
    static final String COLUMN_ID = "_id";
    static final String COLUMN_CREATED_AT = "created_at";

    // TASK Table - column names
    static final String COLUMN_TAG = "tag";
    static final String COLUMN_IS_NETWORK_REQUIRED = "network_required";
    static final String COLUMN_NEXT_TRY_TIME = "next_try_time";
    static final String COLUMN_BACK_OFF_COUNT = "back_off_count";
    static final String COLUMN_TASK_DELAY = "task_delay";
    static final String COLUMN_IS_PERIODIC = "is_periodic";
    static final String COLUMN_TASK_TYPE = "task_type";

    // NOTIF Table - column names
    static final String COLUMN_GCM_MSG_ID = "gcm_msg_id";
    static final String COLUMN_GCM_MSG_CREATED_DATE = "msg_create_date";
    static final String COLUMN_GCM_MSG_CREATED_TIME = "msg_create_time";
    static final String COLUMN_GCM_MSG_DATA = "msg_data";
    static final String COLUMN_GCM_MSG_TYPE = "msg_type";

    // Collection Table - column names
    static final String COLUMN_COLL_JSON = "col_json";
    static final String COLUMN_COLL_TYPE = "col_type";


    // Database Version
    private static final int DATABASE_VERSION = 8;

    // Database Name
    private static final String DATABASE_NAME = "__pushpole_base_lib_db";

    // Table Create Statements
    private static final String CREATE_TABLE_TASK = "CREATE TABLE "
            + TABLE_TASK + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY," +
            COLUMN_TAG + " TEXT," +
            COLUMN_IS_NETWORK_REQUIRED + " INTEGER," +
            COLUMN_NEXT_TRY_TIME + " INTEGER," +
            COLUMN_BACK_OFF_COUNT + " INTEGER," +
            COLUMN_TASK_DELAY + " INTEGER," +
            COLUMN_IS_PERIODIC + " INTEGER," +
            COLUMN_TASK_TYPE + " INTEGER," +
            COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP )";

    // Table Create Statements
    private static final String CREATE_TABLE_RECEIVED_NOTIF = "CREATE TABLE "
            + TABLE_UPSTREAM_MSG_DOWNSTREAM_NOTIF + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY," +
            COLUMN_GCM_MSG_ID + " TEXT," +
            COLUMN_GCM_MSG_DATA + " TEXT," +
            COLUMN_GCM_MSG_TYPE + " INTEGER," +
            COLUMN_GCM_MSG_CREATED_TIME + " INTEGER," + //date in millisecond, the number of milliseconds since Jan. 1, 1970, midnight GMT
            COLUMN_GCM_MSG_CREATED_DATE + " DATE )";

    private static final String CREATE_TABLE_COLLECTION = "CREATE TABLE "
            + TABLE_COLLECTION + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY," +
            COLUMN_COLL_JSON + " TEXT," +
            COLUMN_COLL_TYPE + " TEXT," +
            COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP )";

    private volatile static DatabaseHelper mInstance;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static DatabaseHelper getInstance(Context context) {
        if (mInstance == null) {
            synchronized (DatabaseHelper.class) {
                if (mInstance == null) {
                    mInstance = new DatabaseHelper(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // creating required tables
        db.execSQL(CREATE_TABLE_TASK);
        db.execSQL(CREATE_TABLE_RECEIVED_NOTIF);
        db.execSQL(CREATE_TABLE_COLLECTION);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASK);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_UPSTREAM_MSG_DOWNSTREAM_NOTIF);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COLLECTION);
        onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASK);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_UPSTREAM_MSG_DOWNSTREAM_NOTIF);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COLLECTION);

        // create new tables
        onCreate(db);
    }

    public static enum DBTaskType {
        DB_TASK_TYPE_NONE_NETWORK(100),
        DB_TASK_TYPE_NETWORK(103),
        DB_TASK_TYPE_PENDING(107);

        private int code;

        DBTaskType(int c) {
            code = c;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }
    }

}
