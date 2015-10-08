package com.leray.android.kiosk;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.leray.android.kiosk.services.NavigationBarMaskService;
import com.leray.android.kiosk.utils.PrefUtils;
import com.leray.android.kiosk.utils.ShellUtils;
import com.leray.android.kiosk.widgets.AppModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by John on 2015/9/17.
 */
public class BaseActivity extends ActionBarActivity implements View.OnSystemUiVisibilityChangeListener {
    private static final String TAG = "BaseActivity";
    private final List blockedKeys = new ArrayList(Arrays.asList(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP));

    //    public static ApplicationInfo mSingleAppInfo = null;
    public static AppModel mSingleAppInfo = null;

    //    public static boolean allowLeave = false;
    private boolean currentFocus;           // To keep track of activity's window focus

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        hideSystemBar();

        // every time someone enters the kiosk mode, set the flag true
        PrefUtils.setKioskModeActive(true, getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideSystemBar();
        // TODO
        startService(new Intent(this, NavigationBarMaskService.class));
        hideIme();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Activity's been paused
//        isPaused = true;
//        if (!allowLeave) {
//            blockOtherTasks();
//        }
    }

    private void blockOtherTasks() {
        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);

        activityManager.moveTaskToFront(getTaskId(), 0);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        currentFocus = hasFocus;
        if (!hasFocus) {
            // Close every kind of system dialog
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);

        }
        hideSystemBar();
    }

    @Override
    public void onBackPressed() {
        // nothing to do here
        // … really
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (blockedKeys.contains(event.getKeyCode())) {
            return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }


    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        hideSystemBar();
    }

    public void hideSystemBar() {
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LOW_PROFILE;
        decorView.setSystemUiVisibility(uiOptions);
    }


    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return true; // super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        moveTaskToBack(true);
        Log.d(TAG, "keyCode = " + keyCode);   // KeyEvent.KEYCODE_HOME; // 3
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            Log.d(TAG, "keyCode ============== " + keyCode);
            return true;
        }
//        return super.onKeyDown(keyCode, event);
        return true;
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        return super.onKeyShortcut(keyCode, event);
    }

    /**
     * 隐藏系统状态栏和导航栏
     */
    public void killNavigationBar() {
        new AsyncTask<Void, Void, ShellUtils.CommandResult>() {
            @Override
            protected ShellUtils.CommandResult doInBackground(Void... params) {
                ShellUtils.CommandResult ret = null;
                String procID = "79"; //HONEYCOMB AND OLDER
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    procID = "42"; //ICS AND NEWER
                }
                String[] cmds = {"su", "-c", "service call activity " + procID + " s16 com.android.systemui"};
                ret = ShellUtils.execCommand(cmds, true);
                return ret;
            }

            @Override
            protected void onPostExecute(ShellUtils.CommandResult commandResult) {
                if (commandResult == null) {
                    return;
                }
                new AlertDialog.Builder(BaseActivity.this).setTitle("Hint")
                        .setMessage("\n                   killNavigationBar                  " +
                                "\n    error msg:" + commandResult.errorMsg
                                + "\n success msg: " + commandResult.successMsg
                                + "\n\n")
                        .create()
                        .show();
            }
        }.execute();
    }

    /**
     * 显示系统状态栏和导航栏
     */
    public void showNavigationBar() {
        new AsyncTask<Void, Void, ShellUtils.CommandResult>() {
            @Override
            protected ShellUtils.CommandResult doInBackground(Void... params) {
                ShellUtils.CommandResult ret = null;
                String cmd = "su am startservice -n com.android.systemui/com.android.systemui.statusbar.StatusBarService";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    cmd = "su am startservice -n com.android.systemui/.SystemUIService";
                }
                // String command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib am startservice -n com.android.systemui/.SystemUIService";
                String[] cmds = {/*"su", */"am startservice -n com.android.systemui/.SystemUIService"};
//                String[] cmds = {"am","startservice","-n","com.android.systemui/com.android.systemui.statusbar.StatusBarService"};
                ret = ShellUtils.execCommand(cmds, true);
                return ret;
            }

            @Override
            protected void onPostExecute(ShellUtils.CommandResult commandResult) {
                if (commandResult == null) {
                    return;
                }
                new AlertDialog.Builder(BaseActivity.this).setTitle("Hint")
                        .setMessage("\n                   killNavigationBar                  " +
                                "\n    error msg:" + commandResult.errorMsg
                                + "\n success msg: " + commandResult.successMsg
                                + "\n\n")
                        .create()
                        .show();
            }
        }.execute();
    }


    public void hideIme() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private static final String BOOT_START_PERMISSION = "android.permission.RECEIVE_BOOT_COMPLETED";
    private ArrayList<Map<String, Object>> list;

    public List<Map<String, Object>> queryInstalledApps() {
        List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(0);
        list = new ArrayList<>(packages.size());
        Iterator<ApplicationInfo> appInfoIterator = packages.iterator();

        while (appInfoIterator.hasNext()) {
            ApplicationInfo app = appInfoIterator.next();
            //查找安装的package是否有开机启动权限
            if (PackageManager.PERMISSION_GRANTED ==
                    getPackageManager().checkPermission(BOOT_START_PERMISSION, app.packageName) && !isSystemApp(app)) {
                String label = getPackageManager().getApplicationLabel(app).toString();
                Drawable appIcon = getPackageManager().getApplicationIcon(app);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("name", label);
                map.put("desc", app.packageName);
                map.put("img", appIcon);
                map.put("appinfo", app);
                list.add(map);
                Log.d("hello", label);
            }
        }
        return list;
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        return (appInfo.flags & appInfo.FLAG_SYSTEM) > 0;
    }

    private boolean isSystemApp1(ApplicationInfo appInfo) {
        /**
         * uid是应用程序安装时由系统分配(1000 ～ 9999为系统应用程序保留)
         */
        return appInfo.uid > 1000;
    }

    public void getBootStartupApps() {
        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
        List<ResolveInfo> resolveInfoList = getPackageManager().queryBroadcastReceivers(intent, PackageManager.GET_DISABLED_COMPONENTS);
        for (ResolveInfo resolveInfo : resolveInfoList) {
            ComponentName mComponentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            Log.d(TAG, resolveInfo.activityInfo.packageName + " ===== " + resolveInfo.activityInfo.name);
//            Log.d(TAG, "COMPONENT_ENABLED_STATE:" + getPackageManager().getComponentEnabledSetting(mComponentName) + "\tpackageName:" + resolveInfo.activityInfo.packageName);
        }
    }

    public List<AppModel> getBootStartUpReceivers(List<Map<String, Object>> appList) {
        List<AppModel> list = new ArrayList<>();
//        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
//        List<ResolveInfo> resolveInfoList = getPackageManager().queryBroadcastReceivers(intent, PackageManager.GET_DISABLED_COMPONENTS);
//        for (ResolveInfo resolveInfo : resolveInfoList) {
//            ComponentName mComponentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
//            Log.d(TAG, resolveInfo.activityInfo.packageName + " ===== " + resolveInfo.activityInfo.name);
////            Log.d(TAG, "COMPONENT_ENABLED_STATE:" + getPackageManager().getComponentEnabledSetting(mComponentName) + "\tpackageName:" + resolveInfo.activityInfo.packageName);
//
//        }

        for (Map<String, Object> item : appList) {
            ApplicationInfo appinfo = (ApplicationInfo) item.get("appinfo");
            String pkg = appinfo.packageName;
            String receiver = queryReceiverByPackage(pkg);
            if (receiver != null) {
                AppModel app = new AppModel(this, appinfo);
                app.setStartUpReceiver(pkg + "/" + receiver);
                list.add(app);
                Log.d("boot receiver", receiver);
            }
        }
        return list;
    }

    public String queryReceiverByPackage(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        String receiver = null;
        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
        List<ResolveInfo> resolveInfoList = getPackageManager().queryBroadcastReceivers(intent, PackageManager.GET_DISABLED_COMPONENTS);
        for (ResolveInfo resolveInfo : resolveInfoList) {
            ComponentName mComponentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            Log.d(TAG, resolveInfo.activityInfo.packageName + " ===== " + resolveInfo.activityInfo.name);
//            Log.d(TAG, "COMPONENT_ENABLED_STATE:" + getPackageManager().getComponentEnabledSetting(mComponentName) + "\tpackageName:" + resolveInfo.activityInfo.packageName);
            if (packageName.equals(resolveInfo.activityInfo.packageName)) {
                receiver = resolveInfo.activityInfo.name;
            }
        }
        return receiver;
    }
}
