package com.leray.android.kiosk.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.leray.android.kiosk.KioskApplication;
import com.leray.android.kiosk.utils.PrefUtils;

public class OnScreenOffReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){
            KioskApplication ctx = (KioskApplication) context.getApplicationContext();
            // is Kiosk Mode active?
            if(PrefUtils.isKioskModeActive(ctx)) {
                wakeUpDevice(ctx);
            }
        }
    }

    private void wakeUpDevice(KioskApplication context) {
        PowerManager.WakeLock wakeLock = context.getWakeLock(); // get WakeLock reference via KioskApplication
        if (wakeLock.isHeld()) {
            wakeLock.release(); // release old wake lock
        }

        // create a new wake lock...
        wakeLock.acquire();

        // ... and release again
        wakeLock.release();
    }


}