package com.usertracker;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


/**
 * Created by paragbhuse on 22/01/18.
 */
public class RegisterMobileNumberDialog extends Dialog {
    private UserTrackerActivity activity;
    private EditText etPhoneNumber;
    private EditText etUserName;
    private Button btnOk;
    private SharedPreferences.Editor editor;

    public RegisterMobileNumberDialog(UserTrackerActivity context, SharedPreferences.Editor editor) {
        super(context);
        activity = context;
        this.editor = editor;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCancelable(false);
        setContentView(R.layout.dialog_register_number);

        etPhoneNumber = (EditText) findViewById(R.id.et_mobile_number);
        etUserName = (EditText) findViewById(R.id.et_user_name);
        btnOk = (Button) findViewById(R.id.btn_ok);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(etPhoneNumber.getText().toString())) {
                    Toast.makeText(activity, activity.getString(R.string.enter_mobile_number), Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(etUserName.getText().toString())) {
                    Toast.makeText(activity, activity.getString(R.string.enter_user_name), Toast.LENGTH_SHORT).show();
                    return;
                }

                editor.putString(AppConstants.PHONE_NUMBER, etPhoneNumber.getText().toString());
                editor.apply();

                editor.putString(AppConstants.USER_NAME, etUserName.getText().toString());
                editor.apply();

                editor.putBoolean(AppConstants.REGISTER_MOBILE, false);
                editor.apply();

                RegisterMobileNumberDialog.this.dismiss();
            }
        });
    }
}
