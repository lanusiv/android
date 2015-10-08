package com.leray.android.kiosk.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.leray.android.kiosk.SettingsActivity;
import com.leray.android.kiosk.utils.PreferencesUtils;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String app = PreferencesUtils.getString(context, "app");
        Intent i = context.getPackageManager().getLaunchIntentForPackage(app);
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } else {
            Intent myIntent = new Intent(context, SettingsActivity.class);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            myIntent.putExtra("firstStart", true);
            context.startActivity(myIntent);
        }
        PreferencesUtils.putBoolean(context, "bootCompleted", true);
    }
}