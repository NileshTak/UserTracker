package com.usertracker.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.usertracker.model.ContactModel;

/**
 * Created by paragbhuse on 22/01/18.
 */
public class ContactsDAO extends CardDBDAO {

    public ContactsDAO(Context context) {
        super(context);
    }

    public long save(ContactModel model) {
        ContentValues values = new ContentValues();
        // values.put(DataBaseHelper.ID_COLUMN, model.getId());
        values.put(DataBaseHelper.CONTACT_NAME, model.getName());
        values.put(DataBaseHelper.CONTACT_NUMBER, model.getNumber());
        values.put(DataBaseHelper.TRIP_ID_CONTACT_FK, model.getTripId());
        return database
                .insert(DataBaseHelper.TRIP_CONTACT_TABLE, null, values);
    }

    public String getContactsForTrip(String tripID) {
        String contacts  = "";


        String selection = DataBaseHelper.TRIP_ID_CONTACT_FK + "=?";
        String[] selectionArgs = { tripID };

        Cursor cursor = database.query(DataBaseHelper.TRIP_CONTACT_TABLE,
                new String[] { DataBaseHelper.CONTACT_NAME, DataBaseHelper.CONTACT_NUMBER}, selection,
                selectionArgs, null, null, null);

        while (cursor.moveToNext()) {
            contacts = cursor.getString(1) +",";
        }
        return  contacts;
    }

}
