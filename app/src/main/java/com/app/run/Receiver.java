package com.app.run;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

/**
 * Receiver for broadcasts sent by {@link LocationUpdatesService}.
 */
public class Receiver extends BroadcastReceiver {
    private MainActivity mainActivity;

    public void setMainActivity(MainActivity mainActivity){this.mainActivity = mainActivity;}

    @Override
    public void onReceive(Context context, Intent intent) {
        Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
        if (location != null){
            mainActivity.updateLocation(location);
        }

        DataPack dataPack = intent.getParcelableExtra(LocationUpdatesService.EXTRA_DATAPACK);
        if (dataPack != null){
            mainActivity.updateDataPack(dataPack);
        }
    }
}