package com.example.musicplayer;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by Administrator on 2016/11/7.
 */
public class PlayListContentProvider extends ContentProvider {

    private static final String SCHEMA = "content://";

    public static final String AUTHORITY = "com.example.provider";

    private static final String PATH_SONGS = "/songs";

    // "content://com.example.provider/songs"
    public static final Uri CONTENT_SONGS_URI = Uri.parse(SCHEMA + AUTHORITY + PATH_SONGS);

    private DBHelper mDBHelper;

    public PlayListContentProvider() {

    }

    @Override
    public boolean onCreate() {
        mDBHelper = new DBHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // 通过DBHelper获取读数据库的方法
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        // 查询数据库中的数据项
        Cursor cursor = db.query(DBHelper.PLAYLIST_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri result = null;
        // 通过DBHelper获取写数据库的方法
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        // 将数据ContentValues插入到数据库中
        long id = db.insert(DBHelper.PLAYLIST_TABLE_NAME, null, values);

        if (id > 0) {
            // 根据返回到id值组合成该数据项对应的Uri地址,
            // 假设id为8，那么这个Uri地址类似于content://com.example.provider/songs/8
            result = ContentUris.withAppendedId(CONTENT_SONGS_URI, id);
        }

        return result;
    }

    /**
     * 删除播放列表的操作
     * 我们的音乐播放器在批量添加歌曲到播放列表的时候
     * 首先要清空所有的播放歌曲列表
     * 并没有单独删除某一条歌曲的需要
     * 所以在进行删除操作的时候
     * 我们只需要将整个playlist_table清空就好了
     * @param uri
     * @param selection
     * @param selectionArgs
     * @return
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // 通过DBHelper获取写数据库的方法
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        // 清空playlist_table表，并将删除的数据条数返回
        int count = db.delete(DBHelper.PLAYLIST_TABLE_NAME, selection, selectionArgs);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // 通过DBHelper获取写数据库的方法
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        // 更新数据库的指定项
        int count = db.update(DBHelper.PLAYLIST_TABLE_NAME, values, selection, selectionArgs);
        return count;
    }

}
