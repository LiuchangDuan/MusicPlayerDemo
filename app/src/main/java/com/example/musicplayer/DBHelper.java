package com.example.musicplayer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Administrator on 2016/11/7.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "playlist.db";
    private static final int DB_VERSION = 1;
    public static final String PLAYLIST_TABLE_NAME = "playlist_table";

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String LAST_PLAY_TIME = "last_play_time";
    public static final String SONG_URI = "song_uri";
    public static final String ALBUM_URI = "album_uri";
    public static final String DURATION = "duration";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String PLAYLIST_TABLE_CMD = "CREATE TABLE " + PLAYLIST_TABLE_NAME
                + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + NAME + " VARCHAR(256),"
                + LAST_PLAY_TIME + " LONG,"
                + SONG_URI + " VARCHAR(128),"
                + ALBUM_URI + " VARCHAR(128),"
                + DURATION + " LONG"
                + ");";
        db.execSQL(PLAYLIST_TABLE_CMD);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + PLAYLIST_TABLE_NAME);
        onCreate(db);
    }

}
