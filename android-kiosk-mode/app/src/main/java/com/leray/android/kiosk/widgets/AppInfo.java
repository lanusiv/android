package com.leray.android.kiosk.widgets;

import android.graphics.drawable.Drawable;

/**
 * Created by John on 2015/9/22.
 */
public class AppInfo {
    private String title;
    private String packageName;

    private Drawable icon;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


}
