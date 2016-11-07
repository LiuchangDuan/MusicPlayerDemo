package com.example.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

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

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public class MusicServiceIBinder extends Binder {

        // 添加播放列表
        // 一次添加多首音乐
        public void addPlayList(List<MusicItem> items) {

        }

        // 一次添加一首音乐
        public void addPlayList(MusicItem item) {

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

        }

        // 注销监听函数
        public void unregisterOnStateChangeListener(OnStateChangeListener l) {

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
//        public List<MusicItem> getPlayList() {
//
//        }

    }

    private final IBinder mBinder = new MusicServiceIBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
