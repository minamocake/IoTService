package com.sap.digitallab.iotservice.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.concurrent.Executor;

public class BackgroundPerformExecutor implements Executor {
    private Context context;

    public BackgroundPerformExecutor(Context context) {
        this.context = context;
    }

    @Override public void execute(Runnable command) {
        if (isOnline()) {
            command.run();
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}
