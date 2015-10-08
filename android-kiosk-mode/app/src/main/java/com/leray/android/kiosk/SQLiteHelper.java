package com.leray.android.kiosk;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.leray.android.kiosk.widgets.AppInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by John on 2015/9/22.
 */
public class SQLiteHelper extends SQLiteOpenHelper {
    private static final String TAG = "SQLiteHelper";
    private static final int DATABASE_VERSION = 20;
    private static final String DB_NAME = "kiosk_launcher_db";
    static final String PARAMETER_NOTIFY = "notify";
    private SQLiteHelper mOpenHelper;
    private Context context;


    public SQLiteHelper(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        this.context = context;
        mOpenHelper = this;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

        long userSerialNumber = -1;
/*
        db.execSQL("CREATE TABLE favorites (" +
                "_id INTEGER PRIMARY KEY," +
                "title TEXT," +
                "intent TEXT," +
                "container INTEGER," +
                "screen INTEGER," +
                "cellX INTEGER," +
                "cellY INTEGER," +
                "spanX INTEGER," +
                "spanY INTEGER," +
                "itemType INTEGER," +
                "appWidgetId INTEGER NOT NULL DEFAULT -1," +
                "isShortcut INTEGER," +
                "iconType INTEGER," +
                "iconPackage TEXT," +
                "iconResource TEXT," +
                "icon BLOB," +
                "uri TEXT," +
                "displayMode INTEGER," +
                "appWidgetProvider TEXT," +
                "modified INTEGER NOT NULL DEFAULT 0," +
                "restored INTEGER NOT NULL DEFAULT 0," +
                "profileId INTEGER DEFAULT " + userSerialNumber +
                ");");
        */
        db.execSQL("CREATE TABLE favorites (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "iconPackage TEXT," +
                "icon BLOB," +
                "displayMode INTEGER," +
                "modified INTEGER NOT NULL DEFAULT 0," +
                "restored INTEGER NOT NULL DEFAULT 0," +
                "profileId INTEGER DEFAULT " + userSerialNumber +
                ");");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int version = oldVersion;
        if (version < 16) {
            db.beginTransaction();
            try {
                // Insert new column for holding restore status
                db.execSQL("ALTER TABLE favorites " +
                        "ADD COLUMN restored INTEGER NOT NULL DEFAULT 0;");
                db.setTransactionSuccessful();
                version = 16;
            } catch (SQLException ex) {
                // Old version remains, which means we wipe old data
                Log.e(TAG, ex.getMessage(), ex);
            } finally {
                db.endTransaction();
            }
        }

        if (version < 17) {
            // We use the db version upgrade here to identify users who may not have seen
            // clings yet (because they weren't available), but for whom the clings are now
            // available (tablet users). Because one of the possible cling flows (migration)
            // is very destructive (wipes out workspaces), we want to prevent this from showing
            // until clear data. We do so by marking that the clings have been shown.
//            LauncherClings.synchonouslyMarkFirstRunClingDismissed(mContext);
            version = 17;
        }

        if (version < 18) {
            // No-op
            version = 18;
        }

        if (version < 19) {
            // Due to a data loss bug, some users may have items associated with screen ids
            // which no longer exist. Since this can cause other problems, and since the user
            // will never see these items anyway, we use database upgrade as an opportunity to
            // clean things up.
//            removeOrphanedItems(db);
            version = 19;
        }

        if (version < 20) {
            // Add userId column
//            if (addProfileColumn(db)) {
//                version = 20;
//            }
            // else old version remains, which means we wipe old data
        }
        if (version != DATABASE_VERSION) {
            Log.w(TAG, "Destroying all old data.");
//            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
//            db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKSPACE_SCREENS);

            onCreate(db);
        }
    }


    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(context.getContentResolver(), uri);
        mOpenHelper.close();
        return result;
    }

    private static long dbInsertAndCheck(SQLiteHelper helper,
                                         SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
//        if (values == null) {
//            throw new RuntimeException("Error: attempting to insert null values");
//        }
//        if (!values.containsKey(LauncherSettings.ChangeLogColumns._ID)) {
//            throw new RuntimeException("Error: attempting to add item without specifying an id");
//        }
//        helper.checkId(table, values);
        return db.insert(table, nullColumnHack, values);
    }

    public Uri insert(Uri uri, ContentValues initialValues) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        addModifiedTime(initialValues);
        final long rowId = dbInsertAndCheck(mOpenHelper, db, args.table, null, initialValues);
        if (rowId <= 0) return null;

        uri = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri);
        mOpenHelper.close();
        return uri;
    }


    public int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                addModifiedTime(values[i]);
                if (dbInsertAndCheck(mOpenHelper, db, args.table, null, values[i]) < 0) {
                    return 0;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        sendNotify(uri);
        mOpenHelper.close();
        return values.length;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        if (count > 0) sendNotify(uri);
        mOpenHelper.close();
        return count;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        addModifiedTime(values);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) sendNotify(uri);
        mOpenHelper.close();
        return count;
    }


    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            context.getContentResolver().notifyChange(uri, null);
        }

//        if (mListener != null) {
//            mListener.onLauncherProviderChange();
//        }
    }

    private void addModifiedTime(ContentValues values) {
//        values.put(LauncherSettings.ChangeLogColumns.MODIFIED, System.currentTimeMillis());
    }

    public long generateNewItemId() {
        return mOpenHelper.generateNewItemId();
    }

    public void updateMaxItemId(long id) {
        mOpenHelper.updateMaxItemId(id);
    }

    public long generateNewScreenId() {
        return mOpenHelper.generateNewScreenId();
    }

    // This is only required one time while loading the workspace during the
    // upgrade path, and should never be called from anywhere else.
    public void updateMaxScreenId(long maxScreenId) {
        mOpenHelper.updateMaxScreenId(maxScreenId);
    }


    static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }


    // ------------------------------

    private static final String TABLE_NAME = "favorites";

    public void insert(ContentValues cv) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long rowId = dbInsertAndCheck(mOpenHelper, db, TABLE_NAME, null, cv);
        if (rowId > 0) {
            return;
        }
        mOpenHelper.close();
    }

    public void delete(String where, String[] args) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(TABLE_NAME, where, args);
        mOpenHelper.close();
    }

    public List<AppInfo> getAppInfoList() {
        List<AppInfo> list = new ArrayList<>();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE_NAME);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String[] projection = {"title", "iconPackage", "icon"};
        /**
         * "title TEXT," +
         "iconPackage TEXT," +
         "icon BLOB," +
         */
        Cursor result = qb.query(db, projection, null, null, null, null, null);
        if (!result.moveToFirst()) {
            return null;
        }
        do {
            AppInfo app = new AppInfo();
            String title = result.getString(result.getColumnIndex("title"));
            String packageName = result.getString(result.getColumnIndex("iconPackage"));
            byte[] imgBytes = result.getBlob(result.getColumnIndex("icon"));
            Bitmap bitmap = convertByteArrayToBitmap(imgBytes);
            Drawable drawable = new BitmapDrawable(context.getResources(), bitmap);
            app.setIcon(drawable);
            app.setPackageName(packageName);
            app.setTitle(title);
            list.add(app);
        } while (result.moveToNext());

        mOpenHelper.close();

        return list;
    }

    public static Bitmap convertByteArrayToBitmap(byte[] bytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(
                bytes, 0,
                bytes.length);
        return bitmap;
    }
}
