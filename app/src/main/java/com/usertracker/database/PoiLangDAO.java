package com.usertracker.database;

import android.content.Context;

public class PoiLangDAO extends CardDBDAO {

    private static final String WHERE_ID_EQUALS = DataBaseHelper.ID_COLUMN
            + " =?";

    public PoiLangDAO(Context context) {
        super(context);
    }

    /*public long save(PLang plang_data) {

        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.POI_ID, plang_data.getPoi_id());
        values.put(DataBaseHelper.POILANGS_COLUMN, plang_data.getLangarr());

        return database
                .insert(DataBaseHelper.POILANGS_TABLE, null, values);
    }

    public long update(PLang plang_data) {
        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.POI_ID, plang_data.getPoi_id());
        values.put(DataBaseHelper.POILANGS_COLUMN, plang_data.getLangarr());

        long result = database.update(DataBaseHelper.POILANGS_TABLE,
                values, WHERE_ID_EQUALS,
                new String[] { String.valueOf(plang_data.getId()) });
        Log.d("Update Result:", "=" + result);
        return result;

    }

    public int deleteDept(PLang plang_data) {
        return database.delete(DataBaseHelper.POILANGS_TABLE,
                WHERE_ID_EQUALS, new String[] { plang_data.getId() + "" });
    }

    public List<PLang> getPLangs1() {
        List<PLang> plang_list = new ArrayList<PLang>();
        Cursor cursor = database.query(DataBaseHelper.POILANGS_TABLE,
                new String[] { DataBaseHelper.ID_COLUMN, DataBaseHelper.POI_ID,
                        DataBaseHelper.POILANGS_COLUMN }, null, null, null,
                null, null);

        while (cursor.moveToNext()) {
            PLang plang_bin = new PLang();
            plang_bin.setId(cursor.getInt(0));
            plang_bin.setPoi_id(cursor.getString(1));
            plang_bin.setLangarr(cursor.getString(2));
            plang_list.add(plang_bin);
        }
        return plang_list;
    }

    public List<PLang> getPLangs(String pid) {
        List<PLang> plang_list = new ArrayList<PLang>();

        String selection = DataBaseHelper.POI_ID + "=?";
        String[] selectionArgs = { pid };

        Cursor cursor = database.query(DataBaseHelper.POILANGS_TABLE,
                new String[] { DataBaseHelper.ID_COLUMN, DataBaseHelper.POI_ID,
                        DataBaseHelper.POILANGS_COLUMN }, selection,
                selectionArgs, null, null, null);

        while (cursor.moveToNext()) {
            PLang plang_bin = new PLang();
            plang_bin.setId(cursor.getInt(0));
            plang_bin.setPoi_id(cursor.getString(1));
            plang_bin.setLangarr(cursor.getString(2));
            plang_list.add(plang_bin);
        }
        return plang_list;
    }

    public void loadPLangs(String poi_id, String langarrs) {
        PLang plangbin = new PLang(poi_id, langarrs);

        List<PLang> plang_arr = new ArrayList<PLang>();
        plang_arr.add(plangbin);

        for (PLang dept : plang_arr) {
            ContentValues values = new ContentValues();
            values.put(DataBaseHelper.POI_ID, dept.getPoi_id());
            values.put(DataBaseHelper.POILANGS_COLUMN, dept.getLangarr());
            database.insert(DataBaseHelper.POILANGS_TABLE, null, values);
        }
    }*/

}
