package com.example.musicplayer;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/11/7.
 */
public class MusicService extends Service {

    public interface OnStateChangeListener {

        // 用来通知播放进度
        void onPlayProgressChange(MusicItem item);
        // 用来通知当前处于播放状态
        void onPlay(MusicItem item);
        // 用来通知当前处于暂停或停止状态
        void onPause(MusicItem item);

    }

    // 创建存储监听器的列表
    private List<OnStateChangeListener> mListenerList = new ArrayList<OnStateChangeListener>();

    private List<MusicItem> mPlayList;

    private MediaPlayer mMusicPlayer;

    private ContentResolver mResolver;

    @Override
    public void onCreate() {
        super.onCreate();

        mMusicPlayer = new MediaPlayer();

        // 获取ContentProvider的解析器，避免以后每次使用的时候都要重新获取
        mResolver = getContentResolver();

        // 保存播放列表
        mPlayList = new ArrayList<MusicItem>();

        initPlayingList();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public class MusicServiceIBinder extends Binder {

        // 添加播放列表
        // 一次添加多首音乐
        public void addPlayList(List<MusicItem> items) {
            addPlayListInner(items);
        }

        // 一次添加一首音乐
        public void addPlayList(MusicItem item) {
            addPlayListInner(item);
        }

        // 播放播放列表中应该要播放的音乐
        public void play() {

        }

        // 播放播放列表中的下一首音乐
        public void playNext() {

        }

        // 播放播放列表中的上一首音乐
        public void playPre() {

        }

        // 暂停播放
        public void pause() {

        }

        // 将当前音乐播放的进度，拖动到指定的位置
        public void seekTo(int pos) {

        }

        // 注册监听函数
        public void registerOnStateChangeListener(OnStateChangeListener l) {
            registerOnStateChangeListenerInner(l);
        }

        // 注销监听函数
        public void unregisterOnStateChangeListener(OnStateChangeListener l) {
            unregisterOnStateChangeListenerInner(l);
        }

        // 获取当前正在播放的音乐的信息
//        public MusicItem getCurrentMusic() {
//
//        }

        // 当前音乐是否处于播放的状态
//        public boolean isPlaying() {
//
//        }

        // 获取播放列表
        public List<MusicItem> getPlayList() {
            return mPlayList;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mMusicPlayer.release();

        // 当MusicService销毁的时候，清空监听器列表
        mListenerList.clear();
    }

    private final IBinder mBinder = new MusicServiceIBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void addPlayListInner(List<MusicItem> items) {

        // 清空数据库中的playlist_table
        mResolver.delete(PlayListContentProvider.CONTENT_SONGS_URI, null, null);

        // 清空缓存的播放列表
        mPlayList.clear();

        // 将每首音乐添加到播放列表的缓存和数据库中
        for (MusicItem item : items) {
            // 利用现成的代码，便于代码的维护
            addPlayListInner(item);
        }

    }

    /**
     * 添加一首音乐
     * @param item
     */
    private void addPlayListInner(MusicItem item) {

        // 判断列表中是否已经存储过该音乐，如果存储过就不管它
        if (mPlayList.contains(item)) {
            return;
        }

        // 添加到播放列表的第一个位置
        mPlayList.add(0, item);

        // 将音乐信息保存到ContentProvider中
        insertMusicItemToContentProvider(item);

    }

    private void registerOnStateChangeListenerInner(OnStateChangeListener l) {
        // 将监听器添加到列表
        mListenerList.add(l);
    }

    private void unregisterOnStateChangeListenerInner(OnStateChangeListener l) {
        // 将监听器从列表中移除
        mListenerList.remove(l);
    }

    // 访问ContentProvider，保存一条数据
    private void insertMusicItemToContentProvider(MusicItem item) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.NAME, item.name);
        cv.put(DBHelper.DURATION, item.duration);
        cv.put(DBHelper.LAST_PLAY_TIME, item.playedTime);
        cv.put(DBHelper.SONG_URI, item.songUri.toString());
        cv.put(DBHelper.ALBUM_URI, item.albumUri.toString());
        Uri uri = mResolver.insert(PlayListContentProvider.CONTENT_SONGS_URI, cv);
    }

    /**
     * 将数据库中现有的列表，加载到mPlayList当中
     */
    private void initPlayingList() {

        mPlayList.clear();

        Cursor cursor = mResolver.query(
                PlayListContentProvider.CONTENT_SONGS_URI,
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {

            String songUri = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.SONG_URI));
            String albumUri = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.ALBUM_URI));
            String name = cursor.getString(cursor.getColumnIndex(DBHelper.NAME));
            long playedTime = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LAST_PLAY_TIME));
            long duration = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.DURATION));

            MusicItem item = new MusicItem(Uri.parse(songUri), Uri.parse(albumUri), name, duration, playedTime);

            mPlayList.add(item);

        }

        cursor.close();

    }

}
