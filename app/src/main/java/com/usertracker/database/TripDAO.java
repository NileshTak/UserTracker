package com.usertracker.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.usertracker.model.TripModel;

/**
 * Created by paragbhuse on 17/01/18.
 */
public class TripDAO extends CardDBDAO {
    private static final String WHERE_ID_EQUALS = DataBaseHelper.ID_COLUMN
            + " =?";

    public TripDAO(Context context) {
        super(context);
    }

    public long save(TripModel model) {
        ContentValues values = new ContentValues();
        // values.put(DataBaseHelper.ID_COLUMN, model.getId());
        values.put(DataBaseHelper.TRIP_NAME, model.getName());
        values.put(DataBaseHelper.USER_ID, model.getUserId());
        values.put(DataBaseHelper.START_TIME, model.getStartTime());
        values.put(DataBaseHelper.START_LAT, model.getStartLat());
        values.put(DataBaseHelper.START_LON, model.getStartLon());
        values.put(DataBaseHelper.END_LAT, model.getEndLat());
        values.put(DataBaseHelper.END_LON, model.getEndLon());
        //values.put(DataBaseHelper.TRIP_ID, model.getId());
        return database
                .insert(DataBaseHelper.TRIP_TABLE, null, values);
    }

    public int getLatestID() {

        String sql = "select " + DataBaseHelper.ID_COLUMN + " from " + DataBaseHelper.TRIP_TABLE;
        Cursor cursor = database.rawQuery(sql, null);

        if (cursor != null) {
            while (cursor.moveToLast()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    public String getLatestTripContacts() {
        String contacts = "";

        int tripId = getLatestID();
        Cursor cursor = database.rawQuery("select " + DataBaseHelper.CONTACT_NUMBER + " from " + DataBaseHelper.TRIP_CONTACT_TABLE + " where " + DataBaseHelper.TRIP_ID_CONTACT_FK + " like '" + tripId + "'", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                contacts = cursor.getString(0) + ",";
            }
        }
        return contacts;
    }

    public TripModel getLatestTrip() {
        TripModel model = new TripModel();
        Cursor cursor = database.query(DataBaseHelper.TRIP_TABLE,
                new String[]{DataBaseHelper.ID_COLUMN, DataBaseHelper.TRIP_NAME, DataBaseHelper.USER_ID, DataBaseHelper.START_TIME,
                        DataBaseHelper.START_LAT, DataBaseHelper.START_LON, DataBaseHelper.END_LAT, DataBaseHelper.END_LON}, null, null, null,
                null, null);

        if (cursor != null) {
            cursor.moveToLast();
            model.setId(String.valueOf(cursor.getInt(0)));
            model.setName(cursor.getString(1));
            model.setUserId(cursor.getString(2));
            model.setStartTime(cursor.getString(3));
            model.setStartLat(cursor.getString(4));
            model.setStartLon(cursor.getString(5));
            model.setEndLat(cursor.getString(6));
            model.setEndLon(cursor.getString(7));
        }
        return model;
    }
}