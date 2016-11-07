package com.example.musicplayer;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Created by Administrator on 2016/11/6.
 *
 * 1.定义一个音乐信息的数据结构MusicItem
 */
public class MusicItem {

    // 存储音乐的名字
    String name;
    //存储音乐的Uri地址
    Uri songUri;
    // 存储音乐封面的Uri地址
    Uri albumUri;
    // 存储封面图片
    Bitmap thumb;
    // 存储音乐的播放时长，单位是毫秒
    long duration;
    long playedTime;

    public MusicItem(Uri songUri, Uri albumUri, String strName, long duration, long playedTime) {
        this.name = strName;
        this.songUri = songUri;
        this.albumUri = albumUri;
        this.duration = duration;
        this.playedTime = playedTime;
    }

    @Override
    public boolean equals(Object o) {
        MusicItem another = (MusicItem) o;

        // 音乐的Uri相同，则说明两者相同
        return another.songUri.equals(this.songUri);
    }

}
