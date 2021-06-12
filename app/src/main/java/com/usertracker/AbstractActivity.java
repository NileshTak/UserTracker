package com.usertracker;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessaging;
import com.usertracker.database.TripDAO;
import com.usertracker.model.TripModel;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by paragbhuse on 17/01/18.
 */
public class AbstractActivity extends AppCompatActivity {
    public static String SEND_NOTIFICATION = "https://fcm.googleapis.com/fcm/send";
    public static final String WEB_API_KEY = "AIzaSyB3_q6sRI3y-tdqQSigs2b5jd2ojDJHi5s";
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    public TripModel tripModel = new TripModel();
    String payload = "";
    public SharedPreferences pref;
    public SharedPreferences.Editor editor;
    FloatingActionButton fabVoiceInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseMessaging.getInstance().subscribeToTopic("UserTracker");

        pref = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
        editor = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE).edit();
    }

    public void setVoiceListener() {
        fabVoiceInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (pref != null) {
                    if (!pref.getBoolean(AppConstants.TRIP_STARTED, false)) {
                        Toast.makeText(AbstractActivity.this, getString(R.string.please_start_trip), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?");
                try {
                    startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
                } catch (ActivityNotFoundException a) {
                    a.printStackTrace();
                    Toast.makeText(AbstractActivity.this, getString(R.string.voice_recording_not_supported), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * @param notificationType 1 - Start, 2 - Trip Details, 3 - End Trip, 4 - SOS (Emergency Message)
     */
    public void sendNotification(int notificationType, TripModel tripModel, String allContacts, String message) {

        RequestQueue requestQueue = Volley.newRequestQueue(AbstractActivity.this);

        switch (notificationType) {
            case 1:
                payload = getTripPayload(tripModel, allContacts, notificationType, message);
                break;
            case 2:
                payload = getTripPayload(tripModel, allContacts, notificationType, "");
                break;
            case 3:
                payload = getTripPayload(tripModel, allContacts, notificationType, message);
                break;
            case 4:
                payload = getTripPayload(null, allContacts, notificationType, getString(R.string.emergency_message) + "\n " + message);
                break;
            case 5 :
                payload = getTripPayload(tripModel, allContacts, notificationType, getString(R.string.message) + "\n "+message);
                break;
        }

        //Toast.makeText(this, "Payload " + payload, Toast.LENGTH_SHORT).show();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, SEND_NOTIFICATION, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                Log.d("SEND_NOTIFICATION", "Response " + s);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("SEND_NOTIFICATION_ERROR", "Error " + volleyError.getMessage());
            }
        }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                final HashMap qm = new HashMap();

                qm.put("Content-Type", "application/json");
                qm.put("Authorization", "key=" + WEB_API_KEY);
                return qm;
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return payload == null ? null : payload.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", payload, "utf-8");
                    return null;
                }
            }
        };

        requestQueue.add(stringRequest);
    }

    private String getTripPayload(TripModel tripModel, String allContacts, int type, String message) {

        String notificationType = String.valueOf(type);
        if (tripModel == null) {
            tripModel = new TripModel();
        }
        final String payload = "{\n" +
                "    \"to\": \"/topics/UserTracker\",\n" +
                "    \"data\": {\n" +
                "    \"message\": \"" + message + "\",\n" +
                "    \"sender\": \"" + pref.getString(AppConstants.PHONE_NUMBER, "") + "\",\n" +
                "    \"type\": \"" + notificationType + "\",\n" +
                "    \"contacts\": \"" + allContacts + "\",\n" +
                "    \"name\": \"" + tripModel.getName() + "\",\n" +
                "    \"id\": \"" + tripModel.getId() + "\",\n" +
                "    \"startTime\": \"" + tripModel.getStartTime() + "\",\n" +
                "    \"startLat\": \"" + tripModel.getStartLat() + "\",\n" +
                "    \"startLon\": \"" + tripModel.getStartLon() + "\",\n" +
                "    \"endLat\": \"" + tripModel.getEndLat() + "\",\n" +
                "    \"endLon\": \"" + tripModel.getEndLon() + "\"\n" +
                "    \"currentLat\": \"" + tripModel.getCurrentLat() + "\",\n" +
                "    \"currentLon\": \"" + tripModel.getCurrentLon() + "\"\n" +
                "    }\n" +
                "}";
        return payload;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    //Toast.makeText(this, "Voice Input -" + result.get(0), Toast.LENGTH_SHORT).show();
                    if (result != null && result.get(0) != null) {
                        if (result.get(0).contains("Help") || result.get(0).contains("help") ||result.get(0).contains("Important") || result.get(0).contains("important") || result.get(0).contains("Urgent") || result.get(0).contains("urgent") || result.get(0).contains("Accident") || result.get(0).contains("accident")) {
                            TripDAO tripDAO = new TripDAO(AbstractActivity.this);
                            String contacts = tripDAO.getLatestTripContacts();
                            sendNotification(4, null, contacts, result.get(0));
                        }
                    }
                }
                break;
            }
        }
    }
}
