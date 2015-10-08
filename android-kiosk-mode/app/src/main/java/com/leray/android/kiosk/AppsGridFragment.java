package com.leray.android.kiosk;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.GridView;

import com.leray.android.kiosk.utils.PreferencesUtils;
import com.leray.android.kiosk.widgets.AppListAdapter;
import com.leray.android.kiosk.widgets.AppModel;
import com.leray.android.kiosk.widgets.AppsLoader;
import com.leray.android.kiosk.widgets.GridFragment;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import static com.leray.android.kiosk.services.KioskService.SINGLE_APPLICATION;


/**
 * Created by Arnab Chakraborty
 */
public class AppsGridFragment extends GridFragment implements LoaderManager.LoaderCallbacks<ArrayList<AppModel>> {

    AppListAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText("No Applications");

        mAdapter = new AppListAdapter(getActivity());
        setGridAdapter(mAdapter);

        // till the data is loaded display a spinner
        setGridShown(false);

        // create the loader to load the apps list in background
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ArrayList<AppModel>> onCreateLoader(int id, Bundle bundle) {
        return new AppsLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<AppModel>> loader, ArrayList<AppModel> apps) {
        mAdapter.setData(apps);

        if (isResumed()) {
            setGridShown(true);
        } else {
            setGridShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<AppModel>> loader) {
        mAdapter.setData(null);
    }

    @Override
    public void onGridItemClick(GridView g, View v, int position, long id) {
        AppModel app = (AppModel) getGridAdapter().getItem(position);
        if (app != null) {
//            Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(app.getApplicationPackageName());
//
//            if (intent != null) {
//                startActivity(intent);
//            }
            setSelectedAppInfo(app);

//            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        }
    }

    private void setSelectedAppInfo(AppModel app) {
        BaseActivity.mSingleAppInfo = app;
        String packageName = app.getApplicationPackageName();
        Log.d("hello", packageName);
        PreferencesUtils.putString(getActivity(), KEY, packageName);

        ContentValues cv = new ContentValues();
        Drawable iconDrawable = app.getIcon();
        Bitmap bitmap = ((BitmapDrawable)iconDrawable).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        cv.put(LauncherSettings.BaseLauncherColumns.ICON, stream.toByteArray());
        cv.put("title", app.getLabel());
        cv.put("iconPackage", packageName);

        SQLiteHelper sqLiteHelper = new SQLiteHelper(getActivity());
        sqLiteHelper.insert(cv);
    }

    private static final String KEY = SINGLE_APPLICATION;
}
