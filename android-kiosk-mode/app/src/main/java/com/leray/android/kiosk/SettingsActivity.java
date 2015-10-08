package com.leray.android.kiosk;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.leray.android.kiosk.services.KioskService;
import com.leray.android.kiosk.utils.PreferencesUtils;
import com.leray.android.kiosk.utils.ShellUtils;
import com.leray.android.kiosk.widgets.AppModel;
import com.leray.android.kiosk.widgets.BootStartUpAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.leray.android.kiosk.services.KioskService.SINGLE_APPLICATION;


public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity";
    private static final String PASSWORD = "123456";
    RecyclerView mRecyclerView;
    private Button btnApp;
    private Button btnControlNavibar;
    private View singleAppView;
    private final List blockedKeys = new ArrayList(Arrays.asList(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP));
    boolean currentFocus;           // To keep track of activity's window focus
    boolean isPaused;               // To keep track of activity's foreground/background status

//    private boolean allowLeave = false;
//    private boolean firstStart = false;
    public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;
    private boolean hideNaviBar = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().setFlags(FLAG_HOMEKEY_DISPATCHED, FLAG_HOMEKEY_DISPATCHED);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().hide();

        // every time someone enters the kiosk mode, set the flag true
//        PrefUtils.setKioskModeActive(true, getApplicationContext());

        btnApp = (Button) findViewById(R.id.btnApp);
        btnControlNavibar = (Button) findViewById(R.id.btnControlNavibar);
        singleAppView = findViewById(R.id.singleAppView);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));//这里用线性显示 类似于listview
//        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));//这里用线性宫格显示 类似于grid view
//        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, OrientationHelper.VERTICAL));//这里用线性宫格显示 类似于瀑布流
    }

    @Override
    protected void onResume() {
        super.onResume();
        final String app = PreferencesUtils.getString(this, SINGLE_APPLICATION);
        Log.d("hello", "SettingsActivity onResume " + app);
/*
        SQLiteHelper sqLiteHelper = new SQLiteHelper(this);
        List<AppInfo> list = sqLiteHelper.getAppInfoList();
        */
        List<AppModel> list = getBootStartUpReceivers(queryInstalledApps());
        if (list == null) {
        } else {
            Log.d("hello", "list " + list.size());
            mRecyclerView.setAdapter(new BootStartUpAdapter(this, list));
        }
        // Activity's been resumed
        isPaused = false;

        if (!TextUtils.isEmpty(app) && mSingleAppInfo != null) {
            Log.d("hello", "==================================== ");
            singleAppView.setVisibility(View.VISIBLE);
            ImageView imageView = (ImageView) findViewById(R.id.imageView);
            TextView mTextView = (TextView) findViewById(R.id.textView);
            imageView.setImageDrawable(mSingleAppInfo.getIcon());
            mTextView.setText(mSingleAppInfo.getLabel());

            singleAppView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String packageName = mSingleAppInfo.getApplicationPackageName();
                    startSingleApplication(packageName);
                }
            });
        }
        /*
        if (TextUtils.isEmpty(app)) {
            return;
        }
        btnApp.setVisibility(View.VISIBLE);
        btnApp.setText(app);
        btnApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSingleApplication(app);
            }
        });
        */
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Activity's been paused
        isPaused = true;
//        if (!allowLeave) {
//            blockOtherTasks();
//        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAttachedToWindow() {
        Log.d(TAG, "Setting FLAG_SHOW_WHEN_LOCKED");
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        super.onAttachedToWindow();
    }

    private void blockOtherTasks() {
        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(ACTIVITY_SERVICE);

        activityManager.moveTaskToFront(getTaskId(), 0);
    }

    public void onClick(View view) {
//        allowLeave = true;
        int vid = view.getId();
        switch (vid) {
            case R.id.btnChooseApp:
                chooseApp();
                break;
            case R.id.btnSettings:
                goSettings();
                break;
            case R.id.btnControlNavibar:
                hideOrShowNaviBar();
                break;
            case R.id.btnReboot:
                reboot();
                break;
            case R.id.singleAppView:
                String packageName = mSingleAppInfo.getApplicationPackageName();
                startSingleApplication(packageName);
                break;
            default:
                break;
        }
    }

    private void chooseApp() {
        Intent intent = new Intent();
        intent.setClass(this, ChooseAppActivity.class);
        startActivityForResult(intent, RESULT_OK);
    }

    private void goSettings() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings"));
        startActivity(intent);
        PreferencesUtils.putBoolean(this, "isAdminInSettings", true);
    }

    private void hideOrShowNaviBar() {
        if (hideNaviBar) {
            hideNaviBar = false;
            killNavigationBar();
            hideSystemBar();
            btnControlNavibar.setText("显示导航栏");
        } else {
            hideNaviBar = true;
            showNavigationBar();
            btnControlNavibar.setText("隐藏导航栏");
        }
    }

    private void startSingleApplication(String app) {
        startService(new Intent(SettingsActivity.this, KioskService.class));
        Intent intent = getPackageManager().getLaunchIntentForPackage(app);
        if (intent != null) {
            startActivity(intent);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        currentFocus = hasFocus;
        if(!hasFocus) {
            // Close every kind of system dialog
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
        }
    }

    @Override
    public void onBackPressed() {
        // nothing to do here
        // … really

    }

    @Override
    public void finish() {
//        super.finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (blockedKeys.contains(event.getKeyCode())) {
            return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    private void reboot() {
        boolean isRooted = ShellUtils.checkRootPermission();
        if (isRooted) {
            String cmd = "reboot";
            ShellUtils.execCommand(cmd, true);
        }
        disablePackages();
        int pid = findPIDbyPackageName("com.android.systemui");
        Log.d(TAG, "pid = " + pid);
//        android.os.Process.killProcess(pid);
        queryInstalledApps();

        getBootStartupApps();

        getBootStartUpReceivers(queryInstalledApps());
    }

    private void disablePackages() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> list = pm.getInstalledApplications(PackageManager.GET_META_DATA);

//        for (ApplicationInfo app : list) {
//            String pkg = app.packageName;
//            Log.d("hello", "package name: " + pkg);
//        }

        String cmd = "pm disable com.gymcollegelargesize.o2o";
        ShellUtils.execCommand(cmd, true);

        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
        List<ResolveInfo> resolveInfoList = pm.queryBroadcastReceivers(intent, PackageManager.GET_DISABLED_COMPONENTS);
        for (ResolveInfo resolveInfo : resolveInfoList) {
            ComponentName mComponentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            Log.d(TAG, "COMPONENT_ENABLED_STATE:" + pm.getComponentEnabledSetting(mComponentName) + "\tpackageName:" + resolveInfo.activityInfo.packageName);
        }
    }

    public int findPIDbyPackageName(String packagename) {
        int result = -1;
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            for (ActivityManager.RunningAppProcessInfo pi : am.getRunningAppProcesses()){
                Log.d(TAG, "RunningAppProcessInfo ---- " + pi.processName);
                if (pi.processName.equalsIgnoreCase(packagename)) {
                    result = pi.pid;
                }
                if (result != -1) break;
            }
        } else {
            result = -1;
        }

        return result;
    }

}
