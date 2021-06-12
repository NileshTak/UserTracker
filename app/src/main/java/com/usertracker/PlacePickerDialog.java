package com.usertracker;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Bundle;

import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * Created by paragbhuse on 17/01/18.
 */
public class PlacePickerDialog extends Dialog {
    UserTrackerActivity activity;
    Button ibStart;
    Button ibEnd;
    EditText etTripName;
    Button btnStart;
    Button btnCancel;
    Button btnPickContacts;
    RadioButton tvNormal, tvSilent, tvVibrate, tvDND;
    RadioGroup radioGroup;

    public PlacePickerDialog(@NonNull UserTrackerActivity context) {
        super(context);
        this.activity = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCancelable(false);
        setContentView(R.layout.dialog_select_location);

        ibStart = (Button) findViewById(R.id.btn_select_start_location);
        ibEnd = (Button) findViewById(R.id.btn_select_end_location);
        etTripName = (EditText) findViewById(R.id.tv_trip_name);
        btnStart = (Button) findViewById(R.id.btn_start_trip);
        btnCancel = (Button) findViewById(R.id.btn_cancel);

        tvNormal = (RadioButton) findViewById(R.id.tv_normal);
        tvSilent = (RadioButton) findViewById(R.id.tv_silent);
        tvVibrate = (RadioButton) findViewById(R.id.tv_vibrate);
        tvDND = (RadioButton) findViewById(R.id.tv_dnd);

        radioGroup = (RadioGroup) findViewById(R.id.radio_group);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                switch (checkedId) {
                    case R.id.tv_normal:
                        setProfile(1);
                        break;
                    case R.id.tv_silent:
                        setProfile(2);
                        break;
                    case R.id.tv_vibrate:
                        setProfile(3);
                        break;
                    case R.id.tv_dnd:
                        setProfile(4);
                        break;
                }

                /*SharedPreferences pref = activity.getSharedPreferences(activity.getString(R.string.app_name), Context.MODE_PRIVATE);
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
                }*/
            }
        });

        btnPickContacts = (Button) findViewById(R.id.btn_pick_contact);
        btnPickContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                activity.startActivityForResult(intent, UserTrackerActivity.REQUEST_ID_CONTACTS);
            }
        });
        ibStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showPlacePicker(true);
            }
        });

        ibEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showPlacePicker(false);
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateFields()) {
                    activity.startTrip();
                    PlacePickerDialog.this.dismiss();
                } else {
                    Toast.makeText(activity, activity.getString(R.string.invalid_input), Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePickerDialog.this.dismiss();
            }
        });
    }

    private void setProfile(int profile) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(activity.getString(R.string.app_name), Context.MODE_PRIVATE).edit();
        editor.putInt(AppConstants.END_TRIP_PROFILE, profile);
        editor.commit();
    }

    private boolean validateFields() {
        boolean bValid = true;
        if (radioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(activity, activity.getString(R.string.select_profile), Toast.LENGTH_SHORT).show();
            return false;
        } else if (activity.tripModel != null) {
            if (TextUtils.isEmpty(activity.tripModel.getStartLat())) {
                Toast.makeText(activity, activity.getString(R.string.select_start_location), Toast.LENGTH_SHORT).show();
                return false;
            } else if (TextUtils.isEmpty(activity.tripModel.getEndLat())) {
                Toast.makeText(activity, activity.getString(R.string.select_end_location), Toast.LENGTH_SHORT).show();
                return false;
            } else if (TextUtils.isEmpty(etTripName.getText())) {
                Toast.makeText(activity, activity.getString(R.string.select_trip_name), Toast.LENGTH_SHORT).show();
                return false;
            } else if (activity.contacts.size() == 0) {
                Toast.makeText(activity, activity.getString(R.string.select_contact), Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return bValid;
    }

    /*private void setVolumeMax() {
        AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamMaxVolume(AudioManager.STREAM_RING), AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.STREAM_ALARM, 20, AudioManager.FLAG_SHOW_UI);
        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, AudioManager.FLAG_SHOW_UI);
    }

    private void setVibrate() {
        AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.RINGER_MODE_VIBRATE, am.getStreamMaxVolume(AudioManager.STREAM_RING), AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.STREAM_ALARM, 0, AudioManager.FLAG_SHOW_UI);
        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);
    }

    private void setDND() {
        AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.RINGER_MODE_VIBRATE, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.STREAM_ALARM, 0, AudioManager.FLAG_SHOW_UI);
        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);
    }

    private void setVolumeSilent() {
        AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.RINGER_MODE_VIBRATE, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.STREAM_ALARM, 10, AudioManager.FLAG_SHOW_UI);
        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);
    }*/

    private void setVolumeMax() {
        AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        /*am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamMaxVolume(AudioManager.STREAM_RING), AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.STREAM_ALARM, 20, AudioManager.FLAG_SHOW_UI);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, AudioManager.FLAG_SHOW_UI);*/
        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }

    private void setVibrate() {
        AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        /*am.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.RINGER_MODE_VIBRATE, 20, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.STREAM_ALARM, 0, AudioManager.FLAG_SHOW_UI);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);*/
        am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
    }

    private void setDND() {
        AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
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
        AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        /*am.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.RINGER_MODE_VIBRATE, 0, AudioManager.FLAG_SHOW_UI);
        am.setStreamVolume(AudioManager.STREAM_ALARM, 10, AudioManager.FLAG_SHOW_UI);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);*/
        am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    }
}
