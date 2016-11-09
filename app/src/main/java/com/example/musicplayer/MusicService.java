package com.example.musicplayer;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;

import java.io.IOException;
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

    // 定义循环发送的消息
    private final int MSG_PROGRESS_UPDATE = 0;

    // 定义处理消息的Handler
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PROGRESS_UPDATE:
                    // 将音乐的时长和当前播放的进度保存到MusicItem数据结构中
                    mCurrentMusicItem.playedTime = mMusicPlayer.getCurrentPosition();
                    mCurrentMusicItem.duration = mMusicPlayer.getDuration();

                    // 通知监听者当前的播放进度
                    for (OnStateChangeListener l : mListenerList) {
                        l.onPlayProgressChange(mCurrentMusicItem);
                    }

                    // 将当前的播放进度保存到数据库中
                    updateMusicItem(mCurrentMusicItem);

                    // 间隔一秒发送一次更新播放进度的消息
                    sendEmptyMessageDelayed(MSG_PROGRESS_UPDATE, 1000);

                    break;
            }
        }
    };

    public static final String ACTION_PLAY_MUSIC_PRE = "com.example.musicplayer.playpre";
    public static final String ACTION_PLAY_MUSIC_NEXT = "com.example.musicplayer.playnext";
    public static final String ACTION_PLAY_MUSIC_TOGGLE = "com.example.musicplayer.playtoggle";
    public static final String ACTION_PLAY_MUSIC_UPDATE = "com.example.musicplayer.playupdate";

    // 创建存储监听器的列表
    private List<OnStateChangeListener> mListenerList = new ArrayList<OnStateChangeListener>();

    private List<MusicItem> mPlayList;

    // 存放当前要播放的音乐
    private MusicItem mCurrentMusicItem;

    private MediaPlayer mMusicPlayer;

    private ContentResolver mResolver;

    // 当前是否为播放暂停状态
    private boolean mPaused;

    @Override
    public void onCreate() {
        super.onCreate();

        mMusicPlayer = new MediaPlayer();

        // 获取ContentProvider的解析器，避免以后每次使用的时候都要重新获取
        mResolver = getContentResolver();

        // 保存播放列表
        mPlayList = new ArrayList<MusicItem>();

        mPaused = false;

        mMusicPlayer.setOnCompletionListener(mOnCompletionListener);

        initPlayingList();

        if (mCurrentMusicItem != null) {
            prepareToPlay(mCurrentMusicItem);
        }

    }

    // 将要播放的音乐载入MediaPlayer，但是并不播放
    private void prepareToPlay(MusicItem item) {
        try {
            // 重置播放器状态
            mMusicPlayer.reset();
            // 设置播放音乐的地址
            mMusicPlayer.setDataSource(MusicService.this, item.songUri);
            // 准备播放音乐
            mMusicPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            // 获取App widget发来的操作命令
            String action = intent.getAction();
            if (ACTION_PLAY_MUSIC_PRE.equals(action)) {
                // 播放前一首音乐
                playPreInner();
            } else if (ACTION_PLAY_MUSIC_NEXT.equals(action)) {
                // 播放下一首音乐
                playNextInner();
            } else if (ACTION_PLAY_MUSIC_TOGGLE.equals(action)) {
                // 根据当前播放的状态暂停或继续播放音乐
                if (isPlayingInner()) {
                    pauseInner();
                } else {
                    playInner();
                }
            }
        }

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
            addPlayListInner(item, true);
        }

        // 播放播放列表中应该要播放的音乐
        public void play() {
            playInner();
        }

        // 播放播放列表中的下一首音乐
        public void playNext() {
            playNextInner();
        }

        // 播放播放列表中的上一首音乐
        public void playPre() {
            playPreInner();
        }

        // 暂停播放
        public void pause() {
            pauseInner();
        }

        // 将当前音乐播放的进度，拖动到指定的位置
        public void seekTo(int pos) {
            seekToInner(pos);
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
        public MusicItem getCurrentMusic() {
            return getCurrentMusicInner();
        }

        // 当前音乐是否处于播放的状态
        public boolean isPlaying() {
            return isPlayingInner();
        }

        // 获取播放列表
        public List<MusicItem> getPlayList() {
            return mPlayList;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mMusicPlayer.isPlaying()) {
            mMusicPlayer.stop();
        }

        mMusicPlayer.release();

        // 停止更新
        mHandler.removeMessages(MSG_PROGRESS_UPDATE);

        for (MusicItem item : mPlayList) {
            if (item.thumb != null) {
                item.thumb.recycle();
            }
        }

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
            addPlayListInner(item, false);
        }

    }

    /**
     * 添加一首音乐
     * @param item
     */
    private void addPlayListInner(MusicItem item, boolean needPlay) {

        // 判断列表中是否已经存储过该音乐，如果存储过就不管它
        if (mPlayList.contains(item)) {
            return;
        }

        // 添加到播放列表的第一个位置
        mPlayList.add(0, item);

        // 将音乐信息保存到ContentProvider中
        insertMusicItemToContentProvider(item);

        if (needPlay) {
            // 添加完成后，开始播放
            mCurrentMusicItem = mPlayList.get(0);
            playInner();
        }

    }

    // 播放播放列表中，当前音乐的下一首音乐
    private void playNextInner() {
        int currentIndex = mPlayList.indexOf(mCurrentMusicItem);
        if (currentIndex < mPlayList.size() - 1) {
            // 获取当前播放（或者被加载）音乐的下一首音乐
            // 如果后面有要播放的音乐，把那首音乐设置成要播放的音乐
            // 并重新加载该音乐，开始播放
            mCurrentMusicItem = mPlayList.get(currentIndex + 1);
            playMusicItem(mCurrentMusicItem, true);
        }
    }

    private void playInner() {
        // 如果之前没有选定要播放的音乐，就选列表中的第一首音乐开始播放
        if (mCurrentMusicItem == null && mPlayList.size() > 0) {
            mCurrentMusicItem = mPlayList.get(0);
        }

        // 如果是从暂停状态恢复播放音乐，那么不需要重新加载音乐
        // 如果是从完全没有播放过的状态开始播放音乐，那么就需要重新加载音乐
        if (mPaused) {
            playMusicItem(mCurrentMusicItem, false);
        } else {
            playMusicItem(mCurrentMusicItem, true);
        }
    }

    private void playPreInner() {
        int currentIndex = mPlayList.indexOf(mCurrentMusicItem);
        if (currentIndex - 1 >= 0) {
            // 获取当前播放（或者被加载）音乐的上一首音乐
            // 如果前面有要播放的音乐，把那首音乐设置成要播放的音乐
            // 并重新加载该音乐，开始播放
            mCurrentMusicItem = mPlayList.get(currentIndex - 1);
            playMusicItem(mCurrentMusicItem, true);
        }
    }

    private void pauseInner() {
        // 设置为暂停播放状态
        mPaused = true;
        // 暂停当前正在播放的音乐
        mMusicPlayer.pause();
        // 将播放状态的改变通知给监听者
        for (OnStateChangeListener l : mListenerList) {
            l.onPause(mCurrentMusicItem);
        }
        // 停止更新
        mHandler.removeMessages(MSG_PROGRESS_UPDATE);
        // 音乐暂停时更新到App widget
        updateAppWidget(mCurrentMusicItem);
    }

    private void seekToInner(int pos) {
        // 将音乐拖动到指定的时间
        mMusicPlayer.seekTo(pos);
    }

    private void registerOnStateChangeListenerInner(OnStateChangeListener l) {
        // 将监听器添加到列表
        mListenerList.add(l);
    }

    private void unregisterOnStateChangeListenerInner(OnStateChangeListener l) {
        // 将监听器从列表中移除
        mListenerList.remove(l);
    }

    private MusicItem getCurrentMusicInner() {
        // 返回当前正加载好的音乐
        return mCurrentMusicItem;
    }

    private boolean isPlayingInner() {
        // 返回当前的播放器是否正在播放音乐
        return mMusicPlayer.isPlaying();
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

        // 把第一首音乐作为默认的待播放的音乐
        if (mPlayList.size() > 0) {
            mCurrentMusicItem = mPlayList.get(0);
        }

    }

    // 播放音乐，根据reload标志位判断是非需要重新加载音乐
    private void playMusicItem(MusicItem item, boolean reload) {
        // 如果这里传入的是空值，就什么也不做
        if (item == null) {
            return;
        }

        if (reload) {
            // 需要重新加载音乐
            prepareToPlay(item);
        }

        // 开始播放，如果之前只是暂停播放，那么音乐将继续播放
        mMusicPlayer.start();

        // 将音乐设置到指定时间开始播放，时间单位为毫秒
        seekToInner((int) item.playedTime);

        // 将播放的状态通过监听器通知给监听者
        for (OnStateChangeListener l : mListenerList) {
            l.onPlay(item);
        }

        // 设置为非暂停播放状态
        mPaused = false;

        // 移除现有的更新消息，重新启动更新
        mHandler.removeMessages(MSG_PROGRESS_UPDATE);
        mHandler.sendEmptyMessage(MSG_PROGRESS_UPDATE);

        // 音乐播放时更新到App widget
        updateAppWidget(mCurrentMusicItem);

    }

    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            // 将当前播放的音乐记录时间重置为0，更新到数据库
            // 下次播放就可以从头开始
            mCurrentMusicItem.playedTime = 0;
            updateMusicItem(mCurrentMusicItem);
            // 播放下一首音乐
            playNextInner();
        }
    };

    // 将播放时间更新到ContentProvider中
    private void updateMusicItem(MusicItem item) {

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.DURATION, item.duration);
        cv.put(DBHelper.LAST_PLAY_TIME, item.playedTime);

        String strUri = item.songUri.toString();
        mResolver.update(PlayListContentProvider.CONTENT_SONGS_URI, cv, DBHelper.SONG_URI + "=\"" + strUri + "\"", null);

    }

    // 将音乐的播放信息更新到App widget中
    private void updateAppWidget(MusicItem item) {
        if (item != null) {
            // 创建音乐封面
            if (item.thumb == null) {
                ContentResolver res = getContentResolver();
                item.thumb = Utils.createThumbFromUri(res, item.albumUri);
            }
            // 调用App widget提供的更新接口开始更新
            MusicAppWidget.performUpdates(MusicService.this, item.name, isPlayingInner(), item.thumb);
        }
    }

}
