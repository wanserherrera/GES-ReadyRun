package com.app.run;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.GoogleMap;

import java.math.BigDecimal;

import static com.app.run.Utils.MODES.AUTOMOVILISMO;
import static com.app.run.Utils.MODES.CICLISMO;

class Utils {

    public enum MODES {
        CICLISMO {
            public String toString() {
                return "Ciclismo";
            }
        },
        PEDESTRISMO {
            public String toString() {
                return "Pedestrismo";
            }
        },
        AUTOMOVILISMO {
            public String toString() {
                return "Automovilismo";
            }
        }
    }

    static final String UPDATE_STATE = "update_state";
    static final String PAUSED_UPDATE = "paused_update";
    static final String MODE = "mode";
    static final String TRACKING = "tracking";
    static final String LEISURELY_TIME = "leisurely_time";
    static final String MARKERS = "markers";
    static final String AUTH_PROVIDER = "auth_provider";
    static final String USER_ID = "user_id";
    static final String MAP_TYPE = "map_type";
    static private String selectedMode = CICLISMO.toString();
    static AlertDialog alertDialog;


    static boolean getUpdateState(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(UPDATE_STATE, false);
    }

    static void setUpdateState(Context context, boolean updateState) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(UPDATE_STATE, updateState)
                .apply();
    }

    static boolean getPausedState(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PAUSED_UPDATE, false);
    }

    static void setPausedState(Context context, boolean pauseState) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PAUSED_UPDATE, pauseState)
                .apply();
    }

    static boolean getTracking(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(TRACKING, false);
    }

    static void setTracking(Context context, boolean tracking) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(TRACKING, tracking)
                .apply();
    }

    static boolean getLeisurelyTime(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(LEISURELY_TIME, false);
    }

    static void setLeisurelyTime(Context context, boolean tracking) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(LEISURELY_TIME, tracking)
                .apply();
    }

    static boolean getMarkers(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(LEISURELY_TIME, false);
    }

    static void setMarkers(Context context, boolean markers) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(MARKERS, markers)
                .apply();
    }

    static int getMapType(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);
    }

    static void setMapType(Context context, int mapType) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(MAP_TYPE, mapType)
                .apply();
    }

    static void setMode(Context context, String mode){
        selectedMode = mode;
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(MODE, mode)
                .apply();
    }

    static String getMode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(MODE, CICLISMO.toString());
    }

    static Float getMtsRefresh(){
        if (selectedMode.equals(MODES.PEDESTRISMO.toString()))
            return 5.0f;
        if (selectedMode.equals(AUTOMOVILISMO.toString()))
            return 30.0f;
        return 10.0f;
    }

    static Float getSpeedLimit() {
        if (selectedMode.equals(MODES.PEDESTRISMO.toString()))
            return 6f;
        if (selectedMode.equals(AUTOMOVILISMO.toString()))
            return 47.27f;
        return 22.22f;
    }

    static Float getAccelerationLimit() {
        if (selectedMode.equals(MODES.PEDESTRISMO.toString()))
            return 4f;
        if (selectedMode.equals(AUTOMOVILISMO.toString()))
            return 11.19f;
        return 6f;
    }

    static String getLocationText(Location location) {
        return location == null ? "Ubicación desconocida" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }

    static String getNotificationText(BigDecimal distance, Long time){
        String strResult = "";
        if (distance != null){
            strResult += "Distancia recorrida: " + distance + " km en ";
        }
        if (time != null){
            int[] auxTime = splitToComponentTimes(time);
            StringBuilder strTiempo = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                if (auxTime[i] < 10){
                    strTiempo.append("0").append(auxTime[i]);
                }
                else{
                    strTiempo.append(auxTime[i]);
                }
                if (i != 2){
                    strTiempo.append(':');
                }
            }
            strResult += strTiempo;
        }
        return strResult;
    }

    static String getNotificationTitle(Context context) {
        return "Seguimiento";
    }

    public static int[] splitToComponentTimes(long timeInSec)
    {
        int hours = (int) timeInSec / 3600;
        int remainder = (int) timeInSec - hours * 3600;
        int mins = remainder / 60;
        remainder = remainder - mins * 60;
        int secs = remainder;

        return new int[]{hours , mins , secs};
    }

    static boolean checkGPSState(final Context context){
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
            createAlertDialog(context);
            return false;
        }
        return true;
    }

    private static void createAlertDialog(final Context context){
        if (alertDialog == null) {
            alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogCustom)
                    .setTitle(R.string.gps_not_found_title)  // GPS not found
                    .setMessage(R.string.gps_not_found_message) // Want to enable?
                    .setCancelable(false)
                    .setPositiveButton(R.string.location_settings_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            dialogInterface.dismiss();
                        }
                    })
                    .show();
        }
    }

}