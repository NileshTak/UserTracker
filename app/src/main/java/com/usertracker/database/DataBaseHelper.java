package com.usertracker.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "UserTracker";
    private static final int DATABASE_VERSION = 1;

    /***
     * Trip Table
     */
    public static final String TRIP_TABLE = "TripMaster";

    public static final String ID_COLUMN = "id";
    public static final String TRIP_NAME = "trip_name";
    //public static final String TRIP_ID = "trip_id";
    public static final String USER_ID = "user_id";
    public static final String START_TIME = "start_time";
    public static final String START_LAT = "start_lat";
    public static final String START_LON = "start_lon";
    public static final String END_LAT = "end_lat";
    public static final String END_LON = "end_lon";

    public static final String CREATE_TRIP_TABLE = "CREATE TABLE "
            + TRIP_TABLE + "(" + ID_COLUMN + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +TRIP_NAME + " TEXT ," + USER_ID + " TEXT, " + START_TIME + " TEXT, " + START_LAT + " TEXT, " + START_LON + " TEXT, " + END_LAT + " TEXT, " + END_LON + " TEXT)";
    /**
     * End of Trip Table
     */


    /**
     * Trip Details Table
     */

    public static final String TRIP_DETAILS_TABLE = "TripDetailsTable";

    public static final String ID_COLUMN_TRIP_DETAILS_ID = "trip_details_id";
    public static final String TRIP_ID_FK = "trip_id";
    public static final String PLACE_NAME = "place_name";
    public static final String LAT = "lat";
    public static final String LON = "lon";
    public static final String TIME = "time";

    public static final String CREATE_TRIP_DETAILS_TABLE = "CREATE TABLE "
            + TRIP_DETAILS_TABLE + "(" + ID_COLUMN_TRIP_DETAILS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," + TRIP_ID_FK
            + " INTEGER, " + PLACE_NAME + " TEXT, " + LAT + " TEXT, " + LON + " TEXT, "+ TIME +" TEXT)";
    /**
     * End of Trip Details Table
     */


    /**
     * Trip Contacts Table
     */

    public static final String TRIP_CONTACT_TABLE = "TripContactTable";

    public static final String ID  = "contact_id";
    public static final String CONTACT_NAME = "contact_name";
    public static final String CONTACT_NUMBER = "contact_number";
    public static final String TRIP_ID_CONTACT_FK = "trip_id";

    public static final String CREATE_CONTACT_TRIP_TABLE = "CREATE TABLE "
            + TRIP_CONTACT_TABLE +"(" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
            + CONTACT_NAME + " TEXT, "
            + CONTACT_NUMBER + " TEXT, "
            + TRIP_ID_CONTACT_FK +" INTEGER )";

    /**
     * End of Trip Contacts
     */

    private static DataBaseHelper instance;

    public static synchronized DataBaseHelper getHelper(Context context) {
        if (instance == null)
            instance = new DataBaseHelper(context);
        return instance;
    }

    private DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TRIP_TABLE);
        db.execSQL(CREATE_TRIP_DETAILS_TABLE);
        db.execSQL(CREATE_CONTACT_TRIP_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
