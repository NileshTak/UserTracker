package com.usertracker;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.usertracker.database.ContactsDAO;
import com.usertracker.database.TripDAO;
import com.usertracker.database.TripDetailsDAO;
import com.usertracker.model.ContactModel;
import com.usertracker.model.TripDetailsModel;
import com.usertracker.model.TripModel;
import com.usertracker.service.LocationTracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class UserTrackerActivity extends AbstractActivity
        implements NavigationView.OnNavigationItemSelectedListener, GoogleMap.OnInfoWindowClickListener,
        OnMapReadyCallback {
    public final static int REQUEST_ID_LOCATION = 1215;
    public final static int REQUEST_ID_CONTACTS = 1216;
    public final static int REQUEST_ID_INSTANT_MESSAGE_CONTACTS = 1217;
    public final static int REQ_CODE_SPEECH_INPUT = 1218;
    boolean bStartLocation = false;
    public ArrayList<ContactModel> contacts = new ArrayList<>();
    public ArrayList<ContactModel> instaMessageContacts = new ArrayList<>();
    TripDAO tripDAO;
    TripDetailsDAO tripDetailsDAO;
    Handler mHandler;
    ContactsDAO contactsDAO;
    LocationReceiver locationReceiver;
    GoogleMap mMap;
    SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_tracker);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        locationReceiver = new LocationReceiver();
        tripDAO = new TripDAO(UserTrackerActivity.this);
        tripDetailsDAO = new TripDetailsDAO(UserTrackerActivity.this);
        contactsDAO = new ContactsDAO(UserTrackerActivity.this);

        fabVoiceInput = (FloatingActionButton) findViewById(R.id.fab);
        setVoiceListener();

        startLocationService();

        if (pref.getBoolean(AppConstants.REGISTER_MOBILE, true)) {
            showMobileNumberDialog();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        BatteryManager bm = (BatteryManager) this.getSystemService(BATTERY_SERVICE);

        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);


        if (batLevel == 20) {
            sendBatterySMS();
        }



        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                locationEnabled();
            }
        }, 0, 100000);//put here time 1000 milliseconds=1 second

    }

    private void sendBatterySMS() {
        SharedPreferences sharedpreferences = getSharedPreferences("MyPREFERENCES", Context.MODE_PRIVATE);
        String Num = sharedpreferences.getString("Number","");

        if (!Num.isEmpty()) {
            SmsManager sms= SmsManager.getDefault();
            sms.sendTextMessage(Num, null,"Users Battery is draining...! User may diconnect after some time.", null,null);

        }
    }

    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }

    private void startLocationService() {
        try {
            Intent locationIntent = new Intent(UserTrackerActivity.this, LocationTracker.class);
            startService(locationIntent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void showMobileNumberDialog() {
        RegisterMobileNumberDialog registerMobileNumberDialog = new RegisterMobileNumberDialog(UserTrackerActivity.this, editor);
        registerMobileNumberDialog.show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_start_trip) {
            PlacePickerDialog placePickerDialog = new PlacePickerDialog(UserTrackerActivity.this);
            placePickerDialog.show();
        } else if (id == R.id.nav_end_trip) {
            stopTrip();
        } else if (id == R.id.nav_send_msg) {
            instaMessageContacts.clear();
            pickContactsForInstantMessage();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void pickContactsForInstantMessage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        startActivityForResult(intent, UserTrackerActivity.REQUEST_ID_INSTANT_MESSAGE_CONTACTS);
    }

    public void showPlacePicker(boolean bStart) {
        try {
            PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
            Intent intent = intentBuilder.build(UserTrackerActivity.this);
            bStartLocation = bStart;
            startActivityForResult(intent, REQUEST_ID_LOCATION);
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ID_LOCATION) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                if (bStartLocation) {
                    tripModel.setStartLat(String.valueOf(place.getLatLng().latitude));
                    tripModel.setStartLon(String.valueOf(place.getLatLng().longitude));
                } else {
                   tripModel.setEndLat(String.valueOf(place.getLatLng().latitude));
                    tripModel.setEndLon(String.valueOf(place.getLatLng().longitude));
                }
                //String toastMsg = String.format("Place: %s", place.getName());
                //Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == REQUEST_ID_CONTACTS) {
            if (resultCode == RESULT_OK) {
                Uri contactData = data.getData();
                Cursor cursor = managedQuery(contactData, null, null, null, null);
                cursor.moveToFirst();
                String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                if (!TextUtils.isEmpty(number)) {
                    number = number.replaceAll(" ", "");
                    number = number.replaceAll("-", "");
                }
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                ContactModel contactModel = new ContactModel();
                contactModel.setNumber(number);
                contactModel.setName(name);
                Log.d("Contact Added ", name + " " + number);

                SharedPreferences sharedpreferences = getSharedPreferences("MyPREFERENCES", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("Number", number);
                editor.commit();

                contacts.add(contactModel);
            }
        }

        if (requestCode == REQUEST_ID_INSTANT_MESSAGE_CONTACTS) {
            if (resultCode == RESULT_OK) {
                Uri contactData = data.getData();
                Cursor cursor = managedQuery(contactData, null, null, null, null);
                cursor.moveToFirst();
                String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                if (!TextUtils.isEmpty(number)) {
                    number = number.replaceAll(" ", "");
                    number = number.replaceAll("-", "");
                }
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                ContactModel contactModel = new ContactModel();
                contactModel.setNumber(number);
                contactModel.setName(name);
                Log.d("Contact Added ", name + " " + number);
                instaMessageContacts.add(contactModel);
                sendInstaMessage();
            }
        }

        if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && null != data) {
                String instaContacts = "";
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                //Toast.makeText(this, "Voice Input -" + result.get(0), Toast.LENGTH_SHORT).show();
                if (result != null && result.get(0) != null) {
                    if (instaMessageContacts != null) {
                        int size = instaMessageContacts.size();
                        for (int messageIndex = 0; messageIndex < size; messageIndex++) {
                            instaContacts = instaContacts + instaMessageContacts.get(messageIndex).getNumber() + ",";
                        }
                    }
                    sendNotification(5, null, instaContacts, result.get(0));
                }
            }
        }
    }

    private void sendInstaMessage() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, Record you message.");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            a.printStackTrace();
            Toast.makeText(this, getString(R.string.voice_recording_not_supported), Toast.LENGTH_SHORT).show();
        }
    }

    public void startTrip() {

        String allContacts = "";
        if (contacts != null && contacts.size() > 0) {
            tripModel.setStartTime(String.valueOf(System.currentTimeMillis()));
            tripModel.setUserId(pref.getString(AppConstants.PHONE_NUMBER, ""));
            tripDAO.save(tripModel);
            int tripId = tripDAO.getLatestID();
            if (editor != null) {
                editor.putString(AppConstants.TRIP_ID, String.valueOf(tripId));
                editor.commit();
            }
            for (int i = 0; i < contacts.size(); i++) {
                contacts.get(i).setTripId(tripId);
                allContacts = allContacts + contacts.get(i).getNumber() + ",";
                contactsDAO.save(contacts.get(i));
            }
            if (editor != null) {
                editor.putBoolean(AppConstants.TRIP_STARTED, true);
                editor.commit();
            }

            plotGraphStartAndEnd(tripModel);
        }


        sendSMS(pref.getString(AppConstants.USER_NAME, "")+ " had started the Trip. You can open User Tracker App to Track users path/location on Map. ");

        sendNotification(1, tripModel, allContacts, "Trip Started by " + pref.getString(AppConstants.USER_NAME, ""));
    }



    public void startReturnTrip() {

        setVolumeMax();
//        tripModel.setEndLat(  pref.getString(AppConstants.CURRENT_LAT,""));
//        tripModel.setEndLon(  pref.getString(AppConstants.CURRENT_LON,""));

        editor.putBoolean(AppConstants.RETURN_TRIP_STARTED_END, true);

        tripModel.setEndLat(tripModel.getStartLat());
        tripModel.setEndLon(tripModel.getStartLon());


        String allContacts = "";
        if (contacts != null && contacts.size() > 0) {
            tripModel.setStartTime(String.valueOf(System.currentTimeMillis()));
            tripModel.setUserId(pref.getString(AppConstants.PHONE_NUMBER, ""));


            tripDAO.save(tripModel);
            int tripId = tripDAO.getLatestID();
            if (editor != null) {
                editor.putString(AppConstants.TRIP_ID, String.valueOf(tripId));
                editor.commit();
            }
            for (int i = 0; i < contacts.size(); i++) {
                contacts.get(i).setTripId(tripId);
                allContacts = allContacts + contacts.get(i).getNumber() + ",";
                contactsDAO.save(contacts.get(i));
            }
            if (editor != null) {
                editor.putBoolean(AppConstants.TRIP_STARTED, true);
                editor.commit();
            }

            plotReturnGraphStartAndEnd(tripModel);
            editor.putBoolean(AppConstants.RETURN_TRIP_STARTED, true);
            editor.commit();

            tripDAO.save(tripModel);
        }


        sendSMS(  pref.getString(AppConstants.USER_NAME, "") + " is reached to destination now returning to source");

        sendNotification(1, tripModel, allContacts, "Return Trip Started by " + pref.getString(AppConstants.USER_NAME, ""));
    }



    private void locationEnabled () {
        LocationManager lm = (LocationManager)
                getSystemService(Context. LOCATION_SERVICE ) ;
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager. GPS_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager. NETWORK_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        if (!gps_enabled && !network_enabled) {

            if (pref.getBoolean(AppConstants.TRIP_STARTED, false)) {

                try {

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            sendSMS(pref.getString(AppConstants.USER_NAME, "") + " Lost GPS Signal");

                        }
                    }, 2000);
                } catch(Exception e) {

                }


             }


        }
    }

    public void sendSMS(String str) {

        SharedPreferences sharedpreferences = getSharedPreferences("MyPREFERENCES", Context.MODE_PRIVATE);
        String Num = sharedpreferences.getString("Number","");

        SmsManager sms= SmsManager.getDefault();
        sms.sendTextMessage(Num, null,str, null,null);

    }

    private void plotGraphStartAndEnd(TripModel tripModel) {
        LatLng origin = new LatLng(Double.parseDouble(tripModel.getStartLat()), Double.parseDouble(tripModel.getStartLon()));
        LatLng dest = new LatLng(Double.parseDouble(tripModel.getEndLat()), Double.parseDouble(tripModel.getEndLon()));
        // Getting URL to the Google Directions API
        String url = getUrl(origin, dest);
        FetchUrl FetchUrl = new FetchUrl("new");

        // Start downloading json data from Google Directions API
        FetchUrl.execute(url);
        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(origin));
    }


    private void plotReturnGraphStartAndEnd(TripModel tripModel) {
        LatLng dest = new LatLng(Double.parseDouble(tripModel.getStartLat()), Double.parseDouble(tripModel.getStartLon()));
        LatLng origin = new LatLng(Double.parseDouble(tripModel.getEndLat()), Double.parseDouble(tripModel.getEndLon()));
        // Getting URL to the Google Directions API
        String url = getUrl(origin, dest);
        FetchUrl FetchUrl = new FetchUrl("return");

        // Start downloading json data from Google Directions API
        FetchUrl.execute(url);
        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(origin));
    }

    private void stopTrip() {

        Log.d("aaaaaaaaaaaaaaaaaaa","stop");


        if (editor != null) {

            Log.d("aaaaaaaaaaaaaaaaaaa","setting flae");

            editor.putBoolean(AppConstants.RETURN_TRIP_STARTED, true);
            editor.commit();
            setProfile();

            Log.d("aaaaaaaaaaaaaaaaaaa","inserted "+pref.getBoolean(AppConstants.TRIP_STARTED,false) +" ");


            locationReceiver = new LocationReceiver();
            onResume();
            Toast.makeText(this, "Return Trip Started", Toast.LENGTH_SHORT).show();

        }
    }

    private void setProfile() {

        if (pref.getBoolean(AppConstants.RETURN_TRIP_STARTED_END,false)) {
            setVolumeMax();
            sendSMS(pref.getString(AppConstants.USER_NAME, "")+ " your return Trip is overed. You can open User Tracker App to Track users path/location on Map. ");

        } else {
            int profile = pref.getInt(AppConstants.END_TRIP_PROFILE, -1);
            switch (profile) {
                case 1:
                    setVolumeMax();
                    break;
                case 2:
                    setVolumeSilent();
                    break;
                case 3:
                    setVibrate();
                    break;
                case 4:
                    setDND();
                    break;
            }
            if (editor != null) {
                editor.putInt(AppConstants.END_TRIP_PROFILE, -1);
                editor.commit();
            }
        }


    }

    private void setVolumeMax() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        /*am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamMaxVolume(AudioManager.STREAM_RING), AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.STREAM_ALARM, 20, AudioManager.FLAG_SHOW_UI);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, AudioManager.FLAG_SHOW_UI);*/
        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }

    private void setVibrate() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        /*am.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.RINGER_MODE_VIBRATE, 20, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.STREAM_ALARM, 0, AudioManager.FLAG_SHOW_UI);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);*/
        am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
    }

    private void setDND() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        /*am.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.RINGER_MODE_VIBRATE, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.STREAM_ALARM, 0, AudioManager.FLAG_SHOW_UI);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);*/
        am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        am.setStreamVolume(AudioManager.RINGER_MODE_VIBRATE, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);

    }

    private void setVolumeSilent() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        /*am.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.RINGER_MODE_VIBRATE, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.STREAM_ALARM, 10, AudioManager.FLAG_SHOW_UI);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);*/
        am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationReceiver != null) {
            unregisterReceiver(locationReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerBroadCastReceiver();
    }

    private void registerBroadCastReceiver() {
        IntentFilter newServiceIntentFilter = new IntentFilter(AppConstants.NEW_LOCATION);
        newServiceIntentFilter.addAction(AppConstants.TRIP_STARTED);
        newServiceIntentFilter.addAction(AppConstants.END_TRIP);
        newServiceIntentFilter.addAction(AppConstants.SOS_SITUATION);
        newServiceIntentFilter.addAction(AppConstants.TRIP_DETAILS);
        this.registerReceiver(locationReceiver, newServiceIntentFilter);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap == null) {
            return;
        }
        mMap.setIndoorEnabled(true);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setBuildingsEnabled(true);
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setAllGesturesEnabled(true);

        if (mMap != null) {
            if (pref.getString(AppConstants.CURRENT_LAT, null) == null) {
                return;
            }
            Double lat = Double.parseDouble(pref.getString(AppConstants.CURRENT_LAT, ""));
            Double lon = Double.parseDouble(pref.getString(AppConstants.CURRENT_LON, ""));
            ;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 13));
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat, lon))      // Sets the center of the map to location user
                    .zoom(17)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    private class LocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("aaaaaaaaaaaaaaaaaaa","receidv");


            if (intent != null && !TextUtils.isEmpty(intent.getAction())) {

                Log.d("aaaaaaaaaaaaaaaaaaa","empty");


                if (intent.getAction().equalsIgnoreCase(AppConstants.NEW_LOCATION)) {

                    Log.d("aaaaaaaaaaaaaaaaaaa","intent");


                    if (pref != null) {

                        Log.d("aaaaaaaaaaaaaaaaaaa","notnull");


                        Toast.makeText(context, "Location Changed received", Toast.LENGTH_SHORT).show();
                        TripModel tripModel = tripDAO.getLatestTrip();
                        tripModel.setCurrentLat(pref.getString(AppConstants.CURRENT_LAT, ""));
                        tripModel.setCurrentLon(pref.getString(AppConstants.CURRENT_LON, ""));
                        String contacts = contactsDAO.getContactsForTrip(tripModel.getId());
                        sendNotification(2, tripModel, contacts, "");

                        Log.d("aaaaaaaaaaaaaaaaaaa",pref.getBoolean(AppConstants.TRIP_STARTED,false) +" ");

                        if (pref.getBoolean(AppConstants.RETURN_TRIP_STARTED,false)) {

                            double lat = Double.parseDouble(pref.getString(AppConstants.CURRENT_LAT, ""));
                            double lon = Double.parseDouble(pref.getString(AppConstants.CURRENT_LON, ""));

                            Log.d("aaaaaaaaaaaaaaaaaaa",lat+"  "+lon);

                            double endLat = Double.parseDouble(tripModel.getEndLat());
                            double endLon = Double.parseDouble(tripModel.getEndLon());

                            Log.d("aaaaaaaaaaaaaaaaaaa",endLat+"  "+endLon);


                            float[] dist = new float[1];

                            Location.distanceBetween(endLat, endLon, lat, lon, dist);

                            Log.d("aaaaaaaaaaaaaaaaaaa",dist[0]+"  "+dist[0] / 1000);


                            if ((dist[0] / 1000) > 0.2) {

                                startReturnTrip();
                            }
                        }

                            checkForEndTrip(pref.getString(AppConstants.CURRENT_LAT, ""), pref.getString(AppConstants.CURRENT_LON, ""));


                        }
                } else if (intent.getAction().equalsIgnoreCase(AppConstants.TRIP_DETAILS)) {
                    String id = intent.getStringExtra(AppConstants.TRIP_ID);
                    plotUserRouteOnMap(id);
                } else if (intent.getAction().equalsIgnoreCase(AppConstants.TRIP_STARTED)) {
                    if (mMap != null) {
                        mMap.clear();
                    }
                    plotStartAndEndPointOnMap("");
                } else if (intent.getAction().equalsIgnoreCase(AppConstants.END_TRIP)) {
                    stopTrip();
                } else if (intent.getAction().equalsIgnoreCase(AppConstants.SOS_SITUATION)) {

                }
            }
        }
    }

    private void checkForEndTrip(String currentLat, String currentLon) {

        Log.d("aaaaaaaaaaaaaaaaaaa","CalleeEnd");


        TripModel tripModel = tripDAO.getLatestTrip();

        double lat = Double.parseDouble(currentLat);
        double lon = Double.parseDouble(currentLon);

        double endLat = Double.parseDouble(tripModel.getEndLat());
        double endLon = Double.parseDouble(tripModel.getEndLon());

        float[] dist = new float[1];

        Location.distanceBetween(endLat, endLon, lat, lon, dist);

        if ((dist[0] / 1000) < 0.2) {
            sendEndTripNotification(tripModel);
            stopTrip();
        }
    }

    private void sendEndTripNotification(TripModel tripModel) {
        String contacts = contactsDAO.getContactsForTrip(tripModel.getId());
        sendNotification(3, this.tripModel, contacts, getString(R.string.trip_terminated));
    }

    private void plotStartAndEndPointOnMap(String tripType) {
        TripModel tripModel = new TripModel();
        if (tripDAO != null) {
            tripModel = tripDAO.getLatestTrip();
        }
        if (tripModel != null) {
            //mMap.clear();

            String plus = "\\+";
            double startLat = Double.parseDouble(tripModel.getStartLat().replaceAll(plus, ""));
            double startLon = Double.parseDouble(tripModel.getStartLon().replaceAll(plus, ""));

            double endLat = Double.parseDouble(tripModel.getEndLat().replaceAll(plus, ""));
            double endLon = Double.parseDouble(tripModel.getEndLon().replaceAll(plus, ""));



            MarkerOptions startMarker = new MarkerOptions().position(new LatLng(startLat, startLon)).title(getString(R.string.source));
            MarkerOptions endMarker = new MarkerOptions().position(new LatLng(endLat, endLon)).title(getString(R.string.destination));

            if (tripType == "new") {
                mMap.addMarker(startMarker).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.start_marker));
                mMap.addMarker(endMarker).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.end_marker));

            } else if(tripType == "return") {
                mMap.addMarker(startMarker).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.start_marker));
                mMap.addMarker(endMarker).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.end_marker));
            }


            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            //the include method will calculate the min and max bound.
            builder.include(startMarker.getPosition());
            builder.include(endMarker.getPosition());

            LatLngBounds bounds = builder.build();

            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.10); // offset from edges of the map 10% of screen

            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);

            mMap.animateCamera(cu);
        }
    }

    private class FetchUrl extends AsyncTask<String, Void, String> {

        String type = "";

        public FetchUrl(String s) {
             type = s;
        }

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

                ParserTask parserTask = new ParserTask(type);




            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        String tripType = "";

        public ParserTask(String type) {
            tripType = type;
        }

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask", jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask", "Executing routes");
                Log.d("ParserTask", routes.toString());
            } catch (Exception e) {
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(20);
                lineOptions.color(Color.BLUE);

                Log.d("onPostExecute", "onPostExecute lineoptions decoded");
            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                mMap.addPolyline(lineOptions);

                mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

                plotStartAndEndPointOnMap(tripType);
            } else {
                Log.d("onPostExecute", "without Polylines drawn");
            }
        }
    }

    public class DataParser {
        /**
         * Receives a JSONObject and returns a list of lists containing latitude and longitude
         */
        public List<List<HashMap<String, String>>> parse(JSONObject jObject) {

            List<List<HashMap<String, String>>> routes = new ArrayList<>();
            JSONArray jRoutes;
            JSONArray jLegs;
            JSONArray jSteps;

            try {

                jRoutes = jObject.getJSONArray("routes");

                /** Traversing all routes */
                for (int i = 0; i < jRoutes.length(); i++) {
                    jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                    List path = new ArrayList<>();

                    /** Traversing all legs */
                    for (int j = 0; j < jLegs.length(); j++) {
                        jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                        /** Traversing all steps */
                        for (int k = 0; k < jSteps.length(); k++) {
                            String polyline = "";
                            polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);

                            /** Traversing all points */
                            for (int l = 0; l < list.size(); l++) {
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("lat", Double.toString((list.get(l)).latitude));
                                hm.put("lng", Double.toString((list.get(l)).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
            }

            return routes;
        }

        /**
         * Method to decode polyline points
         * Courtesy : https://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
         */
        private List<LatLng> decodePoly(String encoded) {

            List<LatLng> poly = new ArrayList<>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
                poly.add(p);
            }

            return poly;
        }
    }

    private void plotUserRouteOnMap(String id) {
        ArrayList<LatLng> points;
        TripModel tripModel = null;
        PolylineOptions lineOptions = null;
        ArrayList<TripDetailsModel> tripDetailsModels = new ArrayList<>();
        if (tripDAO != null) {
            tripModel = tripDAO.getLatestTrip();
            if (tripDetailsDAO != null) {
                tripDetailsModels = tripDetailsDAO.getTripDetails(id);
            }
        }

        if (tripDetailsModels == null || tripModel == null || tripDetailsModels.size() == 0) {
            Toast.makeText(this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
            return;
        }

        // start point
        lineOptions = new PolylineOptions();

        double lat = Double.parseDouble(tripModel.getStartLat());
        double lng = Double.parseDouble(tripModel.getStartLon());
        LatLng position = new LatLng(lat, lng);

        // Adding all the points in the route to LineOptions
        lineOptions.add(position);
        lineOptions.width(20);
        lineOptions.color(Color.BLUE);

        // Traversing through all the routes
        for (int i = 0; i < tripDetailsModels.size(); i++) {
            points = new ArrayList<>();
            //lineOptions = new PolylineOptions();

            lat = Double.parseDouble(tripDetailsModels.get(i).getLat());
            lng = Double.parseDouble(tripDetailsModels.get(i).getLon());
            position = new LatLng(lat, lng);

            points.add(position);

            // Adding all the points in the route to LineOptions
            lineOptions.addAll(points);
            lineOptions.width(20);
            lineOptions.color(Color.BLUE);

            Log.d("onPostExecute", "onPostExecute lineoptions decoded");
        }

        // Drawing polyline in the Google Map for the i-th route
        if (lineOptions != null) {

            mMap.clear();

            mMap.addPolyline(lineOptions);

            mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

            plotStartAndEndPointOnMap("");
        } else {
            Log.d("onPostExecute", "without Polylines drawn");
        }
    }
}