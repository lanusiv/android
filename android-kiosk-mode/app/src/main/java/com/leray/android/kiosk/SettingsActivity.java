package com.leray.android.kiosk;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
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

/**
 * kiosk mode 主屏幕页，在主屏幕页为普通模式
 * 功能:：1. 进入single application
 *       2. 选择single application
 *       3. 进入系统设置
 *       4. 隐藏导航栏（需要root权限）
 *       5. 重启设备（需要root权限）
 */
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

    public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;
    private boolean hideNaviBar = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().setFlags(FLAG_HOMEKEY_DISPATCHED, FLAG_HOMEKEY_DISPATCHED);  // 屏蔽home键
        setContentView(R.layout.activity_settings);

        getSupportActionBar().hide();  // 隐藏actionbar

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
        Log.d("hello", "Single mode application: " + app);
        /*
        SQLiteHelper sqLiteHelper = new SQLiteHelper(this);
        List<AppInfo> list = sqLiteHelper.getAppInfoList();
        */
        // 获取开机自启动app列表
        displayBootStartApps();
        // Activity's been resumed
        isPaused = false;
        // 加载single application
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Activity's been paused
        isPaused = true;
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        currentFocus = hasFocus;
        if(!hasFocus) {
            // Close every kind of system dialog
//            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
//            sendBroadcast(closeDialog);
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

    /**
     * called in onPause(), when user navigate to leave this app, move the app back to front immediately
     */
    private void blockOtherTasks() {
        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(ACTIVITY_SERVICE);
        activityManager.moveTaskToFront(getTaskId(), 0);
    }

    public void onClick(View view) {
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

    /**
     * 选择single application
     */
    private void chooseApp() {
        Intent intent = new Intent();
        intent.setClass(this, ChooseAppActivity.class);
        startActivityForResult(intent, RESULT_OK);
    }

    /**
     * 进入系统设置
     */
    private void goSettings() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings"));
        startActivity(intent);
        PreferencesUtils.putBoolean(this, "isAdminInSettings", true);
    }

    /**
     * 隐藏/显示navi bar, 需要root权限
     */
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

    /**
     * 启动single application
     * @param app
     */
    private void startSingleApplication(String app) {
        // 同时开启kiosk服务
        startService(new Intent(SettingsActivity.this, KioskService.class));
        Intent intent = getPackageManager().getLaunchIntentForPackage(app);
        if (intent != null) {
            startActivity(intent);
        }
    }

    /**
     * 获取开机自启动app列表
     */
    private void displayBootStartApps() {
        List<AppModel> list = getBootStartUpReceivers(queryInstalledApps());
        if (list != null) {
            mRecyclerView.setAdapter(new BootStartUpAdapter(this, list));
        }
    }

    /**
     * 重启设备, 需要root权限
     */
    private void reboot() {
        boolean isRooted = ShellUtils.checkRootPermission();
        if (isRooted) {
            String cmd = "reboot";
            ShellUtils.execCommand(cmd, true);
        }
    }

    /**
     * 测试方法，无用
     */
    private void test() {
        int pid = findPIDbyPackageName("com.android.systemui");
        Log.d(TAG, "pid = " + pid);
//        android.os.Process.killProcess(pid);
        queryInstalledApps();

        getBootStartupApps();

        getBootStartUpReceivers(queryInstalledApps());
    }
}
