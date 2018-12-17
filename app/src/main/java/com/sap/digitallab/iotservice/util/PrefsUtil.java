package com.sap.digitallab.iotservice.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.sap.digitallab.iotservice.Constants;

public final class PrefsUtil {

    public static final String PREF_NAME = "iotservice_PREFS";
    public static final String PREF_KEY_DEVICE_ID = "DEVICE_ID";
    public static final String PREF_KEY_BACKEND_URL = "BACKEND_URL";
    public static final String PREF_KEY_SEND_FREQUENCY = "SEND_FREQUENCY";
    public static final String PREF_KEY_LAT = "LAT";
    public static final String PREF_KEY_LNG = "LNG";

    private SharedPreferences prefs;
    private static Context sContext;

    private PrefsUtil(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static class PrefsUtilHolder {
        private static final PrefsUtil INSTANCE = new PrefsUtil(sContext);
    }

    public static PrefsUtil getInstance(Context context) {
        PrefsUtil.sContext = context.getApplicationContext();
        return PrefsUtilHolder.INSTANCE;
    }

    /**
     * Returns cached device id if exists otherwise empty string will be returned.
     *
     * @return
     */
    public String getDeviceID() {
        return prefs.getString(PREF_KEY_DEVICE_ID, "");
    }

    /**
     * Cache device id
     */
    public void setDeviceID(final String deviceID) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_KEY_DEVICE_ID, deviceID);
        editor.apply();
    }

    /**
     * Returns cached backend url if exists otherwise default host will be returned.
     *
     * @return
     */
    public String getBackendURL() {
        return prefs.getString(PREF_KEY_BACKEND_URL, Constants.API_BASE_URL.concat(Constants.URL_IOT));
    }

    /**
     * Cache backend url
     */
    public void setBackendURL(final String backendURL) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_KEY_BACKEND_URL, backendURL);
        editor.apply();
    }

    /**
     * Returns cached send frequency if exists otherwise 5sec will be returned.
     *
     * @return
     */
    public int getSendFrequency() {
        return prefs.getInt(PREF_KEY_SEND_FREQUENCY, 5000);
    }

    /**
     * Cache send frequency
     */
    public void setSendFrequency(final int frequency) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_KEY_SEND_FREQUENCY, frequency);
        editor.apply();
    }

    /**
     * Returns cached lat if exists otherwise empty string will be returned.
     *
     * @return
     */
    public String getLat() {
        return prefs.getString(PREF_KEY_LAT, "");
    }

    /**
     * Cache lat
     */
    public void setLat(final String lat) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_KEY_LAT, lat);
        editor.apply();
    }

    /**
     * Returns cached lng if exists otherwise empty string will be returned.
     *
     * @return
     */
    public String getLng() {
        return prefs.getString(PREF_KEY_LNG, "");
    }

    /**
     * Cache lng
     */
    public void setLng(final String lng) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_KEY_LNG, lng);
        editor.apply();
    }

    /**
     * Remove all cache.
     */
    public void removePrefs() {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Remove specific pref
     */
    public void removePref(String key) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.apply();
    }

}
