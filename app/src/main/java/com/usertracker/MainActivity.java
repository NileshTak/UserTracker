package com.usertracker;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;



import com.usertracker.database.TripDAO;
import com.usertracker.database.TripDetailsDAO;
import com.usertracker.model.TripDetailsModel;
import com.usertracker.model.TripModel;

public class MainActivity extends AppCompatActivity {

    private TextView mVoiceInputTv;
    private ImageButton mSpeakBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TripDAO tripDAO = new TripDAO(MainActivity.this);
        TripDetailsDAO tripDetailsDAO = new TripDetailsDAO(MainActivity.this);

        TripModel tripModel = new TripModel();
        tripModel.setName("Test Trip No1");
        tripModel.setUserId("123456789");
        tripModel.setStartLat("123.456");
        tripModel.setStartLon("234.678");
        tripModel.setEndLat("345.567");
        tripModel.setEndLon("456.678");
        tripModel.setStartTime("1516191503");
        //long saveTrip = tripDAO.save(tripModel);
        //Log.d("SAVE TRIP" , "SAVE TRIP "+saveTrip);

        TripDetailsModel tripDetailsModel = new TripDetailsModel();
        tripDetailsModel.setId("0");
        tripDetailsModel.setTripId("0");
        tripDetailsModel.setPlaceName("PUNE");
        tripDetailsModel.setLat("123.456");
        tripDetailsModel.setLon("234.567");
        tripDetailsModel.setTime("1516191503");
        tripDetailsDAO.save(tripDetailsModel);

        //long saveTripDetails = tripDAO.save(tripModel);
        //Log.d("SAVE TRIP DETAILS" , "SAVE TRIP DETAILS"+saveTripDetails);

        ArrayList<TripDetailsModel> tripDetails = tripDetailsDAO.getTripDetails();
        Log.d("Total Trip Details ",tripDetails.size()+"");

        FirebaseMessaging.getInstance().subscribeToTopic("UserTracker");

        mVoiceInputTv = (TextView) findViewById(R.id.voiceInput);
        mSpeakBtn = (ImageButton) findViewById(R.id.btnSpeak);
        mSpeakBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }




}
