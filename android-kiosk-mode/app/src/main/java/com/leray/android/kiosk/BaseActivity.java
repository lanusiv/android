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
import android.os.Handler;
import android.os.Message;
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
                String cmd = "am startservice -n com.android.systemui/.SystemUIService";
                ret = ShellUtils.execCommand(cmd, true);
                return ret;
            }

            @Override
            protected void onPostExecute(ShellUtils.CommandResult commandResult) {
                showResultDialog(commandResult);
            }
        }.execute();
    }

    private void showResultDialog(ShellUtils.CommandResult commandResult) {
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

    /**
     * 隐藏系统状态栏和导航栏
     */
    public void playNavigationBar(final boolean hide) {
        if (hide) {
            mHandler.sendEmptyMessage(MSG_HIDE_NAVI_BAR);
        } else {
            mHandler.sendEmptyMessage(MSG_SHOW_NAVI_BAR);
        }
//        new AsyncTask<Void, Void, ShellUtils.CommandResult>() {
//            @Override
//            protected ShellUtils.CommandResult doInBackground(Void... params) {
//                ShellUtils.CommandResult ret = null;
//                String procID = "79"; //HONEYCOMB AND OLDER
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//                    procID = "42"; //ICS AND NEWER
//                }
//                String[] cmds = {"su", "-c", "service call activity " + procID + " s16 com.android.systemui"};
//                String cmd = "am startservice -n com.android.systemui/.SystemUIService";
//                if (hide) {
//                    ret = ShellUtils.execCommand(cmds, true);
//                    cancel(false);
//                } else {
//                    ret = ShellUtils.execCommand(cmd, true);
//                }
//
//                return ret;
//            }
//
//            @Override
//            protected void onPostExecute(ShellUtils.CommandResult commandResult) {
//                if (commandResult == null) {
//                    Toast.makeText(getApplicationContext(), "onPostExecute  null ============", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                new AlertDialog.Builder(BaseActivity.this).setTitle("Hint")
//                        .setMessage("\n                   killNavigationBar                  " +
//                                "\n    error msg:" + commandResult.errorMsg
//                                + "\n success msg: " + commandResult.successMsg
//                                + "\n\n")
//                        .create()
//                        .show();
//            }
//        }.execute();
    }

    private static final int MSG_HIDE_NAVI_BAR = 1;
    private static final int MSG_SHOW_NAVI_BAR = 2;
    private Handler mHandler = new MyHandler();
    private static class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case MSG_HIDE_NAVI_BAR:
                    hideNaviBar();
                    break;
                case MSG_SHOW_NAVI_BAR:
                    showNaviBar();
                    break;
                default:
                    return;
            }
        }
    }

    private static void hideNaviBar() {
        ShellUtils.CommandResult ret = null;
        String procID = "79"; //HONEYCOMB AND OLDER
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            procID = "42"; //ICS AND NEWER
        }
        String[] cmds = {"su", "-c", "service call activity " + procID + " s16 com.android.systemui"};
        ret = ShellUtils.execCommand(cmds, true);
        Log.d("hello", "hideNaviBar, result: " + "\n                   killNavigationBar                  " +
                "\n    error msg:" + ret.errorMsg
                + "\n success msg: " + ret.successMsg
                + "\n\n");
    }

    private static void showNaviBar() {
        ShellUtils.CommandResult ret = null;
        String cmd = "am startservice -n com.android.systemui/.SystemUIService";
        ret = ShellUtils.execCommand(cmd, true);
        Log.d("hello", "showNaviBar, result: " + "\n                   showNavigationBar                  " +
                "\n    error msg:" + ret.errorMsg
                + "\n success msg: " + ret.successMsg
                + "\n\n");
    }

    /**
     * 显示系统状态栏和导航栏
     */
    public void showNavigationBar() {

        new AsyncTask<Void, Void, ShellUtils.CommandResult>() {
            @Override
            protected ShellUtils.CommandResult doInBackground(Void... params) {
                Log.d("hello", "show navi bar ============");
                ShellUtils.CommandResult ret = null;
//                String cmd = "su am startservice -n com.android.systemui/com.android.systemui.statusbar.StatusBarService";
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//                    cmd = "am startservice -n com.android.systemui/.SystemUIService";
//                }
//                // String command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib am startservice -n com.android.systemui/.SystemUIService";
////                String[] cmds = {/*"su", */"am startservice -n com.android.systemui/.SystemUIService"};
//                String[] cmds = {"am","startservice","-n","com.android.systemui/com.android.systemui.statusbar.StatusBarService"};
                String cmd = "am startservice -n com.android.systemui/.SystemUIService";
                ret = ShellUtils.execCommand(cmd, true);
                Log.d("hello", "showNavigationBar =============== " + cmd);
                return ret;
            }

            @Override
            protected void onPostExecute(ShellUtils.CommandResult commandResult) {
                showResultDialog(commandResult);
            }
        }.execute();
    }

    /**
     * 隐藏输入法
     */
    public void hideIme() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * 查询安装的app列表
     * @return
     */
    public List<Map<String, Object>> queryInstalledApps() {
        final String BOOT_START_PERMISSION = "android.permission.RECEIVE_BOOT_COMPLETED";
        List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(0);
        ArrayList<Map<String, Object>> list = new ArrayList<>(packages.size());
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

    /**
     * 判断是否是系统应用
     * @param appInfo
     * @return
     */
    public boolean isSystemApp(ApplicationInfo appInfo) {
        return (appInfo.flags & appInfo.FLAG_SYSTEM) > 0;
    }

    /**
     * 判断是否是系统应用
     * @param appInfo
     * @return
     */
    public boolean isSystemApp1(ApplicationInfo appInfo) {
        /**
         * uid是应用程序安装时由系统分配(1000 ～ 9999为系统应用程序保留)
         */
        return appInfo.uid > 1000;
    }

    /**
     * 查询开机启动app列表
     */
    public void getBootStartupApps() {
        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
        List<ResolveInfo> resolveInfoList = getPackageManager().queryBroadcastReceivers(intent, PackageManager.GET_DISABLED_COMPONENTS);
        for (ResolveInfo resolveInfo : resolveInfoList) {
            ComponentName mComponentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            Log.d(TAG, resolveInfo.activityInfo.packageName + " ===== " + resolveInfo.activityInfo.name);
            Log.d(TAG, "COMPONENT_ENABLED_STATE:" + getPackageManager().getComponentEnabledSetting(mComponentName) + "\tpackageName:" + resolveInfo.activityInfo.packageName);
        }
    }

    /**
     * 获取开机启动app列表，不包括系统应用
     * @param appList
     * @return
     */
    public List<AppModel> getBootStartUpReceivers(List<Map<String, Object>> appList) {
        List<AppModel> list = new ArrayList<>();
        for (Map<String, Object> item : appList) {
            ApplicationInfo appinfo = (ApplicationInfo) item.get("appinfo");
            String pkg = appinfo.packageName;
            String receiver = queryReceiverByPackage(pkg);
            if (receiver != null) {
                AppModel app = new AppModel(this, appinfo);
                app.setStartUpReceiver(pkg + "/" + receiver);
                ComponentName mComponentName = new ComponentName(pkg, receiver);
                boolean bootStartEnabled = ! (getPackageManager().getComponentEnabledSetting(mComponentName) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                app.setBootStartEnabled(bootStartEnabled);
                list.add(app);
                Log.d("boot receiver", receiver);
            }
        }
        return list;
    }

    /**
     * 根据包名获取开机相应app监听开机启动的receiver
     * @param packageName
     * @return
     */
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
            boolean bootStartEnabled = ! (getPackageManager().getComponentEnabledSetting(mComponentName) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
//            Log.d(TAG, "COMPONENT_ENABLED_STATE:" + getPackageManager().getComponentEnabledSetting(mComponentName) + "\tpackageName:" + resolveInfo.activityInfo.packageName);
            if (packageName.equals(resolveInfo.activityInfo.packageName)) {
                receiver = resolveInfo.activityInfo.name;
            }
        }
        return receiver;
    }

    /**
     * 根据包名查询pid
     * @param packagename
     * @return
     */
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
