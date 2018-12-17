package com.sap.digitallab.iotservice.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sap.digitallab.iotservice.util.ApiHelper;
import com.sap.digitallab.iotservice.service.BackgroundPerformExecutor;
import com.sap.digitallab.iotservice.BuildConfig;
import com.sap.digitallab.iotservice.Constants;
import com.sap.digitallab.iotservice.service.GeoLocationService;
import com.sap.digitallab.iotservice.util.PrefsUtil;
import com.sap.digitallab.iotservice.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView tvDeviceID;
    private TextView tvStatus;
    private Button btnStart;
    private Button btnStop;

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private boolean mAlreadyStartedService = false;

    private Timer timerExecutor;
    private TimerTask doAsynchronousTaskExecutor;
    private Date mDate = new Date();
    private int mTimerInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // init views by id
        tvDeviceID = findViewById(R.id.tvDeviceIDValue);
        tvStatus = findViewById(R.id.tvStatus);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        // register button listener
        btnStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doStart();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doStop();
            }
        });

        // register geolocation service into LocalBroadcastManager
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String latitude = intent.getStringExtra(GeoLocationService.EXTRA_LATITUDE);
                        String longitude = intent.getStringExtra(GeoLocationService.EXTRA_LONGITUDE);

                        if (latitude != null && longitude != null) {
                            Log.d(TAG, getString(R.string.msg_location_service_started) + "\n Latitude : " + latitude + "\n Longitude: " + longitude);
                            // Toast.makeText(getApplicationContext(), getString(R.string.msg_location_service_started) + "\n Latitude : " + latitude + "\n Longitude: " + longitude, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new IntentFilter(GeoLocationService.ACTION_LOCATION_BROADCAST)
        );

        // update status
        setStatus(getString(R.string.msg_iot_service_not_started));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intentConfiguration = new android.content.Intent(this, SettingsActivity.class);
                startActivity(intentConfiguration);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // update content: device id and send frequency
        updateContent();
        startStep1();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Step 1: Check Google Play services
     */
    private void startStep1() {

        //Check whether this user has installed Google play service which is being used by Location updates.
        if (isGooglePlayServicesAvailable()) {

            //Passing null to indicate that it is executing for the first time.
            startStep2(null);

        } else {
            Toast.makeText(getApplicationContext(), R.string.no_google_playservice_available, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Step 2: Check & Prompt Internet connection
     */
    private Boolean startStep2(DialogInterface dialog) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            promptInternetConnect();
            return false;
        }

        if (dialog != null) {
            dialog.dismiss();
        }

        //Yes there is active internet connection. Next check Location is granted by user or not.
        if (checkPermissions()) { //Yes permissions are granted by the user. Go to the next step.
            startStep3();
        } else {  //No user has not granted the permissions yet. Request now.
            requestPermissions();
        }
        return true;
    }

    /**
     * Show A Dialog with button to refresh the internet state.
     */
    private void promptInternetConnect() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.title_alert_no_intenet);
        builder.setMessage(R.string.msg_alert_no_internet);

        String positiveText = getString(R.string.btn_label_refresh);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //Block the Application Execution until user grants the permissions
                        if (startStep2(dialog)) {

                            //Now make sure about location permission.
                            if (checkPermissions()) {

                                //Step 2: Start the Location Monitor Service
                                //Everything is there to start the service.
                                startStep3();
                            } else if (!checkPermissions()) {
                                requestPermissions();
                            }
                        }
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Step 3: Start the Location Monitor Service
     */
    private void startStep3() {
        //And it will be keep running until you close the entire application from task manager.
        //This method will executed only once.
        if (!mAlreadyStartedService && tvStatus != null) {
            Intent intent = new Intent(this, GeoLocationService.class);
            startService(intent);
            mAlreadyStartedService = true;
        }
    }

    /**
     * Return the availability of GooglePlayServices
     */
    public boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(this, status, 2404).show();
            }
            return false;
        }
        return true;
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState1 = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);

        int permissionState2 = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        return permissionState1 == PackageManager.PERMISSION_GRANTED && permissionState2 == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Start permissions requests.
     */
    private void requestPermissions() {

        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        boolean shouldProvideRationale2 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);

        // Provide an additional rationale to the img_user. This would happen if the img_user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale || shouldProvideRationale2) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the img_user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If img_user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Log.i(TAG, "Permission granted, updates requested, starting location updates");
                startStep3();

            } else {
                // Permission denied.

                // Notify the img_user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the img_user for permission (device policy or "Never ask
                // again" prompts). Therefore, a img_user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    @Override
    public void onDestroy() {
        stopService(new Intent(this, GeoLocationService.class));
        mAlreadyStartedService = false;
        super.onDestroy();

        doAsynchronousTaskExecutor.cancel();
        timerExecutor.cancel();
    }

    // start sending device payload
    private void doStart() {
        // update status
        setStatus(getString(R.string.msg_iot_service_started));

        // start timer
        timerExecutor = new Timer();
        startBackgroundPerformExecutor();
    }

    // stop sending device payload
    private void doStop() {
        // stop service
        stopService(new Intent(this, GeoLocationService.class));
        mAlreadyStartedService = false;
        // stop timer
        stopExecutorClicked();
        setStatus(getString(R.string.msg_iot_service_stopped));
    }

    // update screen content
    private void updateContent() {
        String deviceID = PrefsUtil.getInstance(this).getDeviceID();
        tvDeviceID.setText(deviceID);
    }

    private void setStatus(String status) {
        tvStatus.setText(status);
    }

    public void startBackgroundPerformExecutor() {
        mTimerInterval = PrefsUtil.getInstance(this).getSendFrequency();

        final Handler handler = new Handler();
        doAsynchronousTaskExecutor = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            BackgroundPerformExecutor performBackgroundTask =
                                    new BackgroundPerformExecutor(
                                            getApplicationContext());
                            performBackgroundTask.execute(new Runnable() {
                                @Override
                                public void run() {
                                    // start sending iot sensor payload
                                    Retrofit retrofit = new Retrofit.Builder()
                                            .baseUrl(Constants.API_BASE_URL)
                                            .addConverterFactory(GsonConverterFactory.create())
                                            .build();

                                    ApiHelper helper = retrofit.create(ApiHelper.class);
                                    final JsonObject sensorPayload = getSensorPayload();
                                    Call<ResponseBody> call = helper.sendLocation(sensorPayload);
                                    Log.d(TAG, "Sensor payload: " + sensorPayload);
                                    call.enqueue(new Callback<ResponseBody>() {
                                        @Override
                                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                            try {
                                                final String body = response.body().string();
                                                Log.d(TAG, body);
                                            } catch (IOException e) {
                                                Log.e(TAG, e.getMessage());
                                            } catch (Exception e) {
                                                Log.e(TAG, e.getMessage());
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                                            t.printStackTrace();
                                        }
                                    });

                                    // Toast.makeText(getApplicationContext(), getSensorPayload().toString(), Toast.LENGTH_LONG).show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
        timerExecutor.schedule(doAsynchronousTaskExecutor, 2000, mTimerInterval);
    }

    public void stopExecutorClicked() {
        doAsynchronousTaskExecutor.cancel();
        timerExecutor.cancel();
    }

    private JsonObject getSensorPayload() {
        String deviceID = PrefsUtil.getInstance(this).getDeviceID();
        String lat = PrefsUtil.getInstance(this).getLat();
        String lng = PrefsUtil.getInstance(this).getLng();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.getDefault());
        final String mTimestamp = sdf.format(mDate);

        // form device json
        JsonObject deviceObj = new JsonObject();
        deviceObj.addProperty("device_id", deviceID);
        deviceObj.addProperty("timestamp", mTimestamp);
        deviceObj.addProperty("latitude", lat);
        deviceObj.addProperty("longitude", lng);
        JsonArray measureArr = new JsonArray();
        measureArr.add(deviceObj);

        // form sensor json
        JsonObject sensorObj = new JsonObject();
        sensorObj.addProperty("sensorAlternateId", "either_sensor_rest");
        sensorObj.addProperty("capabilityAlternateId", "capability");
        sensorObj.add("measures", measureArr);

        return sensorObj;
    }

}

