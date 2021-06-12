package com.usertracker.service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.usertracker.AppConstants;
import com.usertracker.R;
import com.usertracker.database.TripDetailsDAO;
import com.usertracker.model.TripDetailsModel;

public class LocationTracker extends Service implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, ResultCallback<Status>,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";
    private static final String ACCURACY = "ACCURACY";
    private static final String LOCATION_TIME = "location_time";
    private static LocationTracker locationTracker = null;
    private LocationRequest mLocationRequest = null;
    private GoogleApiClient mLocationClient = null;
    boolean mUpdatesRequested = false;
    private Context activity = null;
    private Location currentLocation = null;
    private boolean isFailedFirstTime = true;
    Runnable locationRunnable;
    Handler locationHandler = new Handler();
    private SharedPreferences.Editor editor;
    private boolean bStartTimer = true;
    String strLat = "", strLon = "";
    private int FOREGROUND_NOTIFICATION_ID = 247870;
    public static final int MILLISECONDS_PER_SECOND = 20000;
    public static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND;
    public SharedPreferences pref;

    public static LocationTracker getInstance() {

        return locationTracker;
    }

    public void connectToGooglePlayService(Context activity) {

        this.activity = activity;

        if (mLocationClient == null) {
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setInterval((long) 1000 * 10);
            mLocationRequest.setSmallestDisplacement(0);

            mLocationRequest
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(FASTEST_INTERVAL);
            mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

            mUpdatesRequested = false;

            mLocationClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).build();

            mLocationClient.connect();
        } else {
            if (!mLocationClient.isConnected()) {
                mLocationClient.connect();
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        Intent restartServiceIntent = new Intent(getApplicationContext(),
                this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        PendingIntent restartServicePendingIntent = PendingIntent.getService(
                getApplicationContext(), 1, restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent);
    }

    public void stopLocationTracking() {
        try {
            if (mLocationClient.isConnected()) {
                stopPeriodicUpdates();
            }
        } catch (Exception e) {

            e.printStackTrace();
        }

        try {
            mLocationClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean servicesConnected() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(activity);

        if (ConnectionResult.SUCCESS == resultCode) {

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        if (!mUpdatesRequested) {
            mUpdatesRequested = true;
        }

        if (servicesConnected()) {
            startPeriodicUpdates();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        CountDownTimer timer = new CountDownTimer(60 * 1000, 10 * 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                try {
                    mLocationClient.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFinish() {

            }
        };

        if (isFailedFirstTime) {
            isFailedFirstTime = false;
            timer.start();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        if (editor != null && location != null)
        {
            editor.putString(AppConstants.CURRENT_LAT, String.valueOf(location.getLatitude()));
            editor.commit();
            editor.putString(AppConstants.CURRENT_LON, String.valueOf(location.getLongitude()));
            editor.commit();
            if (pref!= null)
            {
                if (pref.getBoolean(AppConstants.TRIP_STARTED, false))
                {
                    sendBroadcast();
                }
            }
        }
    }

    private void sendBroadcast() {

        insertTripDetailsIntoDB();

        Intent intent = new Intent();
        intent.setAction(AppConstants.NEW_LOCATION);
        sendBroadcast(intent);
    }

    private void insertTripDetailsIntoDB() {
        // check if trip id is entered into DB and trip is started
        if (!TextUtils.isEmpty(pref.getString(AppConstants.TRIP_ID,"")) && pref.getBoolean(AppConstants.TRIP_STARTED, false)) {
            TripDetailsDAO detailsDAO = new TripDetailsDAO(LocationTracker.this);
            TripDetailsModel model = new TripDetailsModel();
            model.setLat(pref.getString(AppConstants.CURRENT_LAT, "0"));
            model.setLon(pref.getString(AppConstants.CURRENT_LON, "0"));
            model.setTime(System.currentTimeMillis() + "");
            model.setPlaceName("");
            model.setTripId(pref.getString(AppConstants.TRIP_ID, "-1"));
            detailsDAO.save(model);
        }
    }

    private void startPeriodicUpdates() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED  && ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mLocationClient, mLocationRequest, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopPeriodicUpdates() {
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mLocationClient, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        locationTracker = this;
        locationRunnable = new Runnable() {
            @Override
            public void run() {

                locationHandler.removeCallbacks(locationRunnable);
                locationHandler.postDelayed(locationRunnable, 5000);
            }
        };
        connectToGooglePlayService(getApplicationContext());
        editor = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE).edit();
        pref = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
        startForeground(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelGpsStatusCountDownTimer();
        stopLocationTracking();
        stopForeground(true);
    }

    public interface LocationChangedDeligate {
        public void onLocationChanged(Location location);
    }

    public class GpsStatusBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null) {
                if (intent.hasExtra(LATITUDE)) {
                    strLat = intent.getStringExtra(LATITUDE);
                }

                if (intent.hasExtra(LONGITUDE)) {
                    strLon = intent.getStringExtra(LONGITUDE);
                }
            }

            if (!locationHandler.hasMessages(0)) {
                bStartTimer = false;
                startGpsStatusCountDownTimer();
            } else {

            }
        }
    }

    private void startGpsStatusCountDownTimer() {
        locationHandler.removeCallbacks(locationRunnable);
        locationHandler.sendEmptyMessage(0);
        locationHandler.postDelayed(locationRunnable, 5000);
    }

    private void cancelGpsStatusCountDownTimer() {
        locationHandler.removeCallbacks(locationRunnable);
    }

    @Override
    public void onResult(Status result) {

    }

    @Override
    public void onConnectionSuspended(int cause) {

    }

    private void startForeground(Service service) {
        Notification notification = getNotification();
        service.startForeground(FOREGROUND_NOTIFICATION_ID, notification);
    }

    private Notification getNotification() {

        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                getApplicationContext())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(bm)
                .setContentTitle(getApplicationContext().getString(R.string.app_name))
                .setContentIntent(contentIntent)
                .setContentText(getApplicationContext().getString(R.string.tracking_info));

        mBuilder.setColor(getResources().getColor(R.color.colorPrimary));

        return mBuilder.build();
    }
}
