package com.leray.android.kiosk;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

import com.leray.android.kiosk.receiver.OnScreenOffReceiver;
import com.leray.android.kiosk.services.KioskService;
import com.leray.android.kiosk.utils.PrefUtils;

/**
 * Created by Andreas Schrade on 19.02.2015.
 */
public class KioskApplication extends Application {

    private KioskApplication instance;
    private PowerManager.WakeLock wakeLock;
    private OnScreenOffReceiver onScreenOffReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // every time someone enters the kiosk mode, set the flag true
        PrefUtils.setKioskModeActive(true, getApplicationContext());
        registerKioskModeScreenOffReceiver();
        startKioskService();

    }

    private void registerKioskModeScreenOffReceiver() {
        // register screen off receiver
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        onScreenOffReceiver = new OnScreenOffReceiver();
        registerReceiver(onScreenOffReceiver, filter);
    }

    public PowerManager.WakeLock getWakeLock() {
        if(wakeLock == null) {
            // lazy loading: first call, create wakeLock via PowerManager.
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "wakeup");
        }
        return wakeLock;
    }

    private void startKioskService() { // ... and this method
        startService(new Intent(this, KioskService.class));
    }
}