package com.anime.limma.databases.sqlite;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.anime.limma.models.Video;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("Recycle")
public class DbFavorite extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "db_video_favorite";
    private static final String TABLE_NAME = "tbl_video_favorite";
    private static final String KEY_ID = "id";

    private static final String KEY_CAT_NAME = "category_name";

    private static final String KEY_VID = "vid";
    private static final String KEY_VIDEO_TITLE = "video_title";
    private static final String KEY_VIDEO_URL = "video_url";
    private static final String KEY_VIDEO_ID = "video_id";
    private static final String KEY_VIDEO_THUMBNAIL = "video_thumbnail";
    private static final String KEY_VIDEO_DURATION = "video_duration";
    private static final String KEY_VIDEO_DESCRIPTION = "video_description";
    private static final String KEY_VIDEO_TYPE = "video_type";
    private static final String KEY_TOTAL_VIEWS = "total_views";
    private static final String KEY_DATE_TIME = "date_time";


    public DbFavorite(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_CAT_NAME + " TEXT,"
                + KEY_VID + " TEXT,"
                + KEY_VIDEO_TITLE + " TEXT,"
                + KEY_VIDEO_URL + " TEXT,"
                + KEY_VIDEO_ID + " TEXT,"
                + KEY_VIDEO_THUMBNAIL + " TEXT,"
                + KEY_VIDEO_DURATION + " TEXT,"
                + KEY_VIDEO_DESCRIPTION + " TEXT,"
                + KEY_VIDEO_TYPE + " TEXT,"
                + KEY_TOTAL_VIEWS + " INTEGER,"
                + KEY_DATE_TIME + " TEXT"
                + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    //Adding Record in Database
    public void addToFavorite(Video video) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CAT_NAME, video.getCategory_name());
        values.put(KEY_VID, video.getVid());
        values.put(KEY_VIDEO_TITLE, video.getVideo_title());
        values.put(KEY_VIDEO_URL, video.getVideo_url());
        values.put(KEY_VIDEO_ID, video.getVideo_id());
        values.put(KEY_VIDEO_THUMBNAIL, video.getVideo_thumbnail());
        values.put(KEY_VIDEO_DURATION, video.getVideo_duration());
        values.put(KEY_VIDEO_DESCRIPTION, video.getVideo_description());
        values.put(KEY_VIDEO_TYPE, video.getVideo_type());
        values.put(KEY_TOTAL_VIEWS, video.getTotal_views());
        values.put(KEY_DATE_TIME, video.getDate_time());
        db.insert(TABLE_NAME, null, values);
        db.close();

    }

    // Getting All Data
    public List<Video> getAllData() {
        List<Video> videos = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_NAME + " ORDER BY id DESC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Video video = new Video();
                video.setId(Integer.parseInt(cursor.getString(0)));
                video.setCategory_name(cursor.getString(1));
                video.setVid(cursor.getString(2));
                video.setVideo_title(cursor.getString(3));
                video.setVideo_url(cursor.getString(4));
                video.setVideo_id(cursor.getString(5));
                video.setVideo_thumbnail(cursor.getString(6));
                video.setVideo_duration(cursor.getString(7));
                video.setVideo_description(cursor.getString(8));
                video.setVideo_type(cursor.getString(9));
                video.setTotal_views(cursor.getLong(10));
                video.setDate_time(cursor.getString(11));
                videos.add(video);
            } while (cursor.moveToNext());
        }
        return videos;
    }

    //getting single row
    public List<Video> getFavRow(String id) {
        List<Video> videos = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_NAME + " WHERE vid=" + id;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Video video = new Video();
                video.setId(Integer.parseInt(cursor.getString(0)));
                video.setCategory_name(cursor.getString(1));
                video.setVid(cursor.getString(2));
                video.setVideo_title(cursor.getString(3));
                video.setVideo_url(cursor.getString(4));
                video.setVideo_id(cursor.getString(5));
                video.setVideo_thumbnail(cursor.getString(6));
                video.setVideo_duration(cursor.getString(7));
                video.setVideo_description(cursor.getString(8));
                video.setVideo_type(cursor.getString(9));
                video.setTotal_views(cursor.getLong(10));
                video.setDate_time(cursor.getString(11));
                videos.add(video);
            } while (cursor.moveToNext());
        }
        return videos;
    }

    //for remove favorite
    public void RemoveFav(Video contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, KEY_VID + " = ?", new String[]{String.valueOf(contact.getVid())});
        db.close();
    }

    public enum DatabaseManager {
        INSTANCE;
        private SQLiteDatabase db;
        private boolean isDbClosed = true;
        DbFavorite dbHelper;
        public void init(Context context) {
            dbHelper = new DbFavorite(context);
            if (isDbClosed) {
                isDbClosed = false;
                this.db = dbHelper.getWritableDatabase();
            }
        }

        public boolean isDatabaseClosed() {
            return isDbClosed;
        }

        public void closeDatabase() {
            if (!isDbClosed && db != null) {
                isDbClosed = true;
                db.close();
                dbHelper.close();
            }
        }
    }

}
