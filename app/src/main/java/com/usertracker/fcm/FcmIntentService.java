package com.usertracker.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.usertracker.AppConstants;
import com.usertracker.MainActivity;
import com.usertracker.R;
import com.usertracker.database.TripDAO;
import com.usertracker.database.TripDetailsDAO;
import com.usertracker.model.TripDetailsModel;
import com.usertracker.model.TripModel;

public class FcmIntentService extends FirebaseMessagingService {
    private static final String TAG = "FcmIntentService";
    private String allContacts = "";
    public SharedPreferences pref;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        pref = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        //String message = remoteMessage.getData().get("message");
        String type = remoteMessage.getData().get("type");

        int notificationType = 0;
        if (!TextUtils.isEmpty(type)) {
            try {
                type = type.trim();
                notificationType = Integer.parseInt(type);
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
        }

        if (remoteMessage.getData() != null) {
            if (remoteMessage.getData().containsKey("contacts")) {
                allContacts = remoteMessage.getData().get("contacts");
                String myNumber = pref.getString(AppConstants.PHONE_NUMBER, "");
                if (allContacts != null && allContacts.length() > 0 && !TextUtils.isEmpty(myNumber)) {
                    if (allContacts.contains(myNumber)) {
                        //sendNotification(message, 0);
                        switch (notificationType) {
                            // Start
                            case 1:
                                TripDAO tripDAO = new TripDAO(FcmIntentService.this);
                                fetchAndSaveTripStartDetails(tripDAO, remoteMessage.getData());
                                startTripNotification(remoteMessage.getData().get("message"));
                                Intent startIntent = new Intent();
                                startIntent.setAction(AppConstants.TRIP_STARTED);
                                sendBroadcast(startIntent);
                                break;

                            // Details
                            case 2:
                                String tripId = fetchAndSaveTripDetails(remoteMessage.getData());
                                Intent detailsIntent = new Intent();
                                detailsIntent.setAction(AppConstants.TRIP_DETAILS);
                                detailsIntent.putExtra(AppConstants.TRIP_ID, tripId);
                                sendBroadcast(detailsIntent);
                                break;

                            // End
                            case 3:
                                sendNotification(remoteMessage.getData().get("message"), 3);
                                Intent EndTripIntent = new Intent();
                                EndTripIntent.setAction(AppConstants.END_TRIP);
                                sendBroadcast(EndTripIntent);
                                break;

                            // SOS
                            case 4:
                                sendNotification(remoteMessage.getData().get("message"), 4);
                                Intent sosIntent = new Intent();
                                sosIntent.setAction(AppConstants.SOS_SITUATION);
                                sendBroadcast(sosIntent);

                                try {
                                    // set volume to max
                                    AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                                    am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamMaxVolume(AudioManager.STREAM_RING), AudioManager.FLAG_SHOW_UI);
                                    am.setStreamVolume(AudioManager.STREAM_ALARM, 20, AudioManager.FLAG_SHOW_UI);
                                    AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, AudioManager.FLAG_SHOW_UI);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                                // play alarm sound
                                MediaPlayer mp = MediaPlayer.create(FcmIntentService.this, R.raw.alarm);
                                mp.start();
                                break;

                            case 5 :
                                sendNotification(remoteMessage.getData().get("message"), 5);
                            default:
                                return;
                        }
                    } else {
                        // Do Nothing. Notification is not for current user
                    }
                }
            }
        }
    }

    private void fetchAndSaveTripStartDetails(TripDAO tripDAO, Map<String, String> data) {
        TripModel tripModel = new TripModel();
        tripModel.setName(data.get("name"));
        tripModel.setUserId(data.get("sender"));
        tripModel.setStartTime(data.get("startTime"));
        tripModel.setStartLat(data.get("startLat"));
        tripModel.setStartLon(data.get("startLon"));
        tripModel.setEndLat(data.get("endLat"));
        tripModel.setEndLon(data.get("endLon"));
        tripModel.setId(data.get("tripId"));
        tripDAO.save(tripModel);
    }

    private String fetchAndSaveTripDetails(Map<String, String> data) {
        TripDetailsModel model = new TripDetailsModel();
        model.setTripId(data.get("id"));
        model.setTime(data.get("startTime"));
        model.setLat(data.get("currentLat"));
        model.setLon(data.get("currentLon"));
        TripDetailsDAO detailsDAO = new TripDetailsDAO(FcmIntentService.this);
        detailsDAO.save(model);
        return data.get("id");
    }

    private void sendNotification(String messageBody, int type) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (type == 4) {
            intent.putExtra("SOS", true);
            intent.putExtra("MESSAGE", messageBody);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setLargeIcon(bm)/*Notification icon image*/
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int id = new NotificationID().getID();
        notificationManager.notify(id /* ID of notification */, notificationBuilder.build());
    }

    private void startTripNotification(String messageBody) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                //.setLargeIcon(bm)/*Notification icon image*/
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int id = new NotificationID().getID();
        notificationManager.notify(id /* ID of notification */, notificationBuilder.build());
    }

    public class NotificationID {
        private final AtomicInteger c = new AtomicInteger(0);

        public int getID() {
            return c.incrementAndGet();
        }
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
    }
}
