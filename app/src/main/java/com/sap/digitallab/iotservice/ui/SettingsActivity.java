package com.sap.digitallab.iotservice.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sap.digitallab.iotservice.util.PrefsUtil;
import com.sap.digitallab.iotservice.R;

public class SettingsActivity extends AppCompatActivity {

    private EditText etDeviceID;
    private EditText etBackendURL;
    private Button btnSave;
    private Button btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // init views by id
        etDeviceID = (EditText) findViewById(R.id.etDeviceID);
        etBackendURL = (EditText) findViewById(R.id.etBackendURL);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnCancel = (Button) findViewById(R.id.btnCancel);

        // register button listener
        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doSave();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doCancel();
            }
        });

        // update screen content
        updateContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    // save into preference util
    private void doSave() {
        String deviceID = etDeviceID.getText().toString();
        String backendURL = etBackendURL.getText().toString();

        PrefsUtil.getInstance(this).setDeviceID(deviceID);
        PrefsUtil.getInstance(this).setBackendURL(backendURL);

        Toast.makeText(getApplicationContext(), getString(R.string.msg_settings_saved), Toast.LENGTH_SHORT).show();
    }

    // cancel
    private void doCancel() {
        finish();
    }

    // update screen content
    private void updateContent() {
        String deviceID = PrefsUtil.getInstance(this).getDeviceID();
        String backendURL = PrefsUtil.getInstance(this).getBackendURL();

        etDeviceID.setText(deviceID);
        etBackendURL.setText(backendURL);

    }
}

