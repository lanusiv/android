package com.leray.android.kiosk.services;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.leray.android.kiosk.utils.PrefUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class NavigationBarMaskService extends Service {

    private static final long INTERVAL = TimeUnit.SECONDS.toMillis(2); // periodic interval to check in seconds -> 2 seconds
    private static final String TAG = NavigationBarMaskService.class.getSimpleName();

    private Thread t = null;
    private Context context = null;
    private boolean running = false;

    private WindowManager mWindowManager;
    private NavigationBarMaskView navigationBarMaskView;
    private static int clickCounter = 0;                 // 有效点击次数
    private static final int REQUIRED_CLICK_TIMES = 7;   // 进入launcher需要连续点击的次数
    private long timeout = 0;                            // 点击超时时间
    private long lastClickTime = 0;                      // 上一次点击事件的时间戳
    private long curClickTime = 0;                       // 本次点击事件的时间戳


    @Override
    public void onDestroy() {
        Log.i(TAG, "Stopping service 'NavigationBarMaskService'");
        running =false;
        if (mWindowManager != null && navigationBarMaskView != null) {
            mWindowManager.removeViewImmediate(navigationBarMaskView);
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting service 'NavigationBarMaskService'");
        running = true;
        context = this;

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams localLayoutParams = getWindowLayoutParams();
        navigationBarMaskView = new NavigationBarMaskView(this);
//        navigationBarMaskView.setBackgroundColor(Color.GREEN);
        mWindowManager.addView(navigationBarMaskView, localLayoutParams);

        return Service.START_NOT_STICKY;
    }

    @NonNull
    private WindowManager.LayoutParams getWindowLayoutParams() {
        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
        localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        localLayoutParams.gravity = Gravity.BOTTOM;
        localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                                  // this is to enable the notification to recieve touch events
                                  WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                                  // Draws over status bar
                                  WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        int naviHeight = getNavigationBarHeight(context, Configuration.ORIENTATION_PORTRAIT);
        float density = getResources().getDisplayMetrics().scaledDensity;
        Log.d(TAG, "navi height: " + naviHeight + ", density: " + density);
        int maskHeight = 50;
        if (naviHeight > 0) {
            maskHeight = (int) (naviHeight / density);
        }
        localLayoutParams.height = (int) (maskHeight * density);
        localLayoutParams.format = PixelFormat.TRANSPARENT;
        return localLayoutParams;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 点击计数器, 连续点击7次后进入launcher
     */
    private void clickCounter() {
        curClickTime = SystemClock.uptimeMillis();
        if (clickCounter < 1) {
            lastClickTime = SystemClock.uptimeMillis();
        } else {
            timeout = curClickTime - lastClickTime;
            if (timeout < 2 * 1000) {
                lastClickTime = SystemClock.uptimeMillis();
            } else {
                clickCounter = 0;
                lastClickTime = curClickTime = SystemClock.uptimeMillis();
            }
        }
        if (clickCounter == 4 ) {
//            int remainTimes = REQUIRED_CLICK_TIMES - clickCounter;
            Toast.makeText(context, "再按" + 3 + "次进入设置", Toast.LENGTH_SHORT).show();
        }
        clickCounter++;
        if (clickCounter >= REQUIRED_CLICK_TIMES) {
            lastClickTime = curClickTime = SystemClock.uptimeMillis();
            clickCounter = 0;
            showSettings();
        }
    }

    /**
     * 进入launcher进行设置, 退出KIOSK模式，进入普通模式
     */
    private void showSettings() {
        // 退出kiosk模式
        PrefUtils.setKioskModeActive(false, getApplicationContext());

        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    /**
     * 判断当前app是否在前台运行
     * @param curPackageName
     * @return
     */
    private boolean isCurrentTaskInFront(String curPackageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        ComponentName componentInfo = taskInfo.get(0).topActivity;
        if (curPackageName.equals(componentInfo.getPackageName())) {
            return true;
        }
        return false;
    }

    /**
     * 自定义view屏蔽状态栏事件
     */
    public class NavigationBarMaskView extends ViewGroup {

        public NavigationBarMaskView(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            Log.v("customViewGroup", "**********Intercepted");
//            Toast.makeText(context, "hello", Toast.LENGTH_SHORT).show();
            if (!isCurrentTaskInFront(context.getPackageName())) {
                clickCounter();
            }

            return true;
        }
    }

    private int getNavigationBarHeight(Context context, int orientation) {
        Resources resources = context.getResources();

        int id = resources.getIdentifier(
                orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape",
                "dimen", "android");
        if (id > 0) {
            return resources.getDimensionPixelSize(id);
        }
        return 0;
    }

}