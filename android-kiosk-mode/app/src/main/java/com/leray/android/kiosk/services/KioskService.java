package com.leray.android.kiosk.services;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.leray.android.kiosk.R;
import com.leray.android.kiosk.receiver.HomeWatcherReceiver;
import com.leray.android.kiosk.utils.PrefUtils;
import com.leray.android.kiosk.utils.PreferencesUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class KioskService extends Service {

    private static final long INTERVAL = TimeUnit.SECONDS.toMillis(2); // periodic interval to check in seconds -> 2 seconds
    private static final String TAG = KioskService.class.getSimpleName();
    public static final String SINGLE_APPLICATION = "single_app";

    private Thread t = null;
    private Context context = null;
    private boolean running = false;

    private WindowManager mWindowManager;
    private StatusBarMaskView statusBarMaskView;
    private static int clickCounter = 0;                 // 有效点击次数
    private static final int REQUIRED_CLICK_TIMES = 7;   // 进入launcher需要连续点击的次数
    private long timeout = 0;                            // 点击超时时间
    private long lastClickTime = 0;                      // 上一次点击事件的时间戳
    private long curClickTime = 0;                       // 本次点击事件的时间戳

    private static final String PASSWORD = "123456";
    private static boolean isAuthenticated = false;
    private View authenticatedDialog;

    private static HomeWatcherReceiver mHomeKeyReceiver = null;

    @Override
    public void onDestroy() {
        Log.i(TAG, "Stopping service 'KioskService'");
        running =false;
        unregisterHomeKeyReceiver(this);
        if (mWindowManager != null && statusBarMaskView != null) {
            mWindowManager.removeViewImmediate(statusBarMaskView);
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting service 'KioskService'");
        running = true;
        context = this;

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams localLayoutParams = getWindowLayoutParams();
        statusBarMaskView = new StatusBarMaskView(this);
        mWindowManager.addView(statusBarMaskView, localLayoutParams);

        registerHomeKeyReceiver(this);

        // start a thread that periodically checks if your app is in the foreground
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    handleKioskMode();
                    try {
                        Thread.sleep(INTERVAL);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Thread interrupted: 'KioskService'");
                    }
                 } while(running);
                stopSelf();
            }
        });

        t.start();
        return Service.START_NOT_STICKY;
    }

    @NonNull
    private WindowManager.LayoutParams getWindowLayoutParams() {
        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
        localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        localLayoutParams.gravity = Gravity.TOP;
        localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                                  // this is to enable the notification to recieve touch events
                                  WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                                  // Draws over status bar
                                  WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        localLayoutParams.height = (int) (getScaledSize(50));
        localLayoutParams.format = PixelFormat.TRANSPARENT;
        return localLayoutParams;
    }

    private void handleKioskMode() {
        // is Kiosk Mode active?
        if(PrefUtils.isKioskModeActive(context)) {
            // is App in background?
            if(isInBackground()) {
                restoreApp(); // restore!
            }
        }
    }

    private boolean isInBackground() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        ComponentName componentInfo = taskInfo.get(0).topActivity;
        if ("com.android.settings".equals(componentInfo.getPackageName())) {
            return false;
        }
        final String app = PreferencesUtils.getString(this, SINGLE_APPLICATION);
        if (TextUtils.isEmpty(app)) {
            return (!context.getApplicationContext().getPackageName().equals(componentInfo.getPackageName()));
        } else {
            return (!app.equals(componentInfo.getPackageName()));
        }
    }

    private void restoreApp() {
        // Restart activity
        final String app = PreferencesUtils.getString(this, SINGLE_APPLICATION);
        if (TextUtils.isEmpty(app)) {
            return;
        }
        Intent intent = getPackageManager().getLaunchIntentForPackage(app);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
//        Log.d(TAG, "curPackageName: " + curPackageName + ", componentInfo: " + componentInfo.getPackageName());
        if (curPackageName.equals(componentInfo.getPackageName())) {
            return true;
        }
        return false;
    }

    /**
     * 自定义view屏蔽状态栏事件
     */
    public class StatusBarMaskView extends ViewGroup {

        public StatusBarMaskView(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            Log.v("customViewGroup", "**********Intercepted");

            if (!isCurrentTaskInFront(context.getPackageName())) {
                clickCounter();
            }

            return true;
        }
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
//            showSettings();

            showAuthenticatedDialog();
        }
    }

    /**
     * 进入launcher进行设置, 退出KIOSK模式，进入普通模式
     */
    private void showSettings() {
        running = false;
        // 退出kiosk模式
        PrefUtils.setKioskModeActive(false, getApplicationContext());

        Log.d("hello", "showSettings");
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    private static void registerHomeKeyReceiver(Context context) {
        Log.i(TAG, "registerHomeKeyReceiver");
        mHomeKeyReceiver = new HomeWatcherReceiver();
        final IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        context.registerReceiver(mHomeKeyReceiver, homeFilter);
    }

    private static void unregisterHomeKeyReceiver(Context context) {
        Log.i(TAG, "unregisterHomeKeyReceiver");
        if (null != mHomeKeyReceiver) {
            context.unregisterReceiver(mHomeKeyReceiver);
        }
    }

    private View getAuthenticationView() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.fragment_dialog, null);
        final EditText etv = (EditText) dialogView.findViewById(R.id.editText);
        Button button = (Button) dialogView.findViewById(R.id.button);
        Button closeBtn = (Button) dialogView.findViewById(R.id.closeBtn);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String pwd = etv.getText().toString();
                if (TextUtils.isEmpty(pwd)) {
                    return;
                }
                checkPassWd(pwd);
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                removeAuthenticatedDialog();
            }
        });

        dialogView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideIme(v);
            }
        });

        return dialogView;
    }

    private void checkPassWd(String password) {
        if (PASSWORD.equals(password)) {
            isAuthenticated = true;
            hideIme(authenticatedDialog);
            removeAuthenticatedDialog();
            showSettings();
        } else {
            isAuthenticated = false;
            Toast.makeText(context, "密码错误", Toast.LENGTH_SHORT).show();
//            startService()
            // remove authenticated dialog
            hideIme(authenticatedDialog);
            removeAuthenticatedDialog();
        }
    }

    /**
     * 显示输入密码对话框
     */
    private void showAuthenticatedDialog() {
        authenticatedDialog = getAuthenticationView();
        mWindowManager.removeViewImmediate(statusBarMaskView);
        mWindowManager.addView(authenticatedDialog, getDialogLayoutParams());
    }

    /**
     * 隐藏输入密码对话框
     */
    private void removeAuthenticatedDialog() {
        mWindowManager.removeViewImmediate(authenticatedDialog);
        if (statusBarMaskView == null) {
            statusBarMaskView = new StatusBarMaskView(this);
        }
        mWindowManager.addView(statusBarMaskView, getWindowLayoutParams());
    }

    @NonNull
    private WindowManager.LayoutParams getDialogLayoutParams() {
        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
        localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        localLayoutParams.gravity = Gravity.CENTER;
        localLayoutParams.flags = // WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                // this is to enable the notification to recieve touch events
                /*WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |*/
                // Draws over status bar
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        localLayoutParams.width = (int) (getScaledSize(500)); // WindowManager.LayoutParams.MATCH_PARENT;
        localLayoutParams.height = (int) (getScaledSize(300));
//        localLayoutParams.horizontalMargin = 0.3f;
        localLayoutParams.format = PixelFormat.TRANSPARENT;
        return localLayoutParams;
    }

    private float getScaledSize(float size) {
        return size * getResources().getDisplayMetrics().scaledDensity;
    }

    public void hideIme(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}