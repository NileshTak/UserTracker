package com.usertracker.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;

import com.usertracker.model.TripDetailsModel;

/**
 * Created by paragbhuse on 17/01/18.
 */
public class TripDetailsDAO extends CardDBDAO {
    public TripDetailsDAO(Context context) {
        super(context);
    }

    public long save(TripDetailsModel model) {
        ContentValues values = new ContentValues();
        //values.put(DataBaseHelper.ID_COLUMN_TRIP_DETAILS_ID, model.getId());
        values.put(DataBaseHelper.TRIP_ID_FK, model.getTripId());
        values.put(DataBaseHelper.PLACE_NAME, model.getPlaceName());
        values.put(DataBaseHelper.LAT, model.getLat());
        values.put(DataBaseHelper.LON, model.getLon());
        values.put(DataBaseHelper.TIME, model.getTime());
        return database
                .insert(DataBaseHelper.TRIP_DETAILS_TABLE, null, values);
    }

    public ArrayList<TripDetailsModel> getTripDetails() {
        ArrayList<TripDetailsModel> tripDetailsList = new ArrayList<TripDetailsModel>();
        Cursor cursor = database.query(DataBaseHelper.TRIP_DETAILS_TABLE,
                new String[]{DataBaseHelper.ID_COLUMN_TRIP_DETAILS_ID, DataBaseHelper.TRIP_ID_FK, DataBaseHelper.PLACE_NAME, DataBaseHelper.LAT, DataBaseHelper.LON, DataBaseHelper.TIME}, null, null, null,
                null, null);

        if (cursor == null) {
            return null;
        }

        while (cursor.moveToNext()) {
            TripDetailsModel tripDetailsModel = new TripDetailsModel();
            tripDetailsModel.setId(cursor.getString(0));
            tripDetailsModel.setTripId(cursor.getString(1));
            tripDetailsModel.setPlaceName(cursor.getString(2));
            tripDetailsModel.setLat(cursor.getString(3));
            tripDetailsModel.setLon(cursor.getString(4));
            tripDetailsModel.setTime(cursor.getString(5));
            tripDetailsList.add(tripDetailsModel);
        }
        return tripDetailsList;
    }

    public ArrayList<TripDetailsModel> getTripDetails(String tripId) {
        ArrayList<TripDetailsModel> tripDetailsList = new ArrayList<TripDetailsModel>();
        String sql = "select *  from " + DataBaseHelper.TRIP_DETAILS_TABLE + " where " + DataBaseHelper.TRIP_ID_FK + " like '" + tripId + "'";
        Cursor cursor = database.rawQuery(sql, null);

        if (cursor == null) {
            return null;
        }

        while (cursor.moveToNext()) {
            TripDetailsModel tripDetailsModel = new TripDetailsModel();
            tripDetailsModel.setId(cursor.getString(0));
            tripDetailsModel.setTripId(cursor.getString(1));
            tripDetailsModel.setPlaceName(cursor.getString(2));
            tripDetailsModel.setLat(cursor.getString(3));
            tripDetailsModel.setLon(cursor.getString(4));
            tripDetailsModel.setTime(cursor.getString(5));
            tripDetailsList.add(tripDetailsModel);
        }
        return tripDetailsList;
    }
}
