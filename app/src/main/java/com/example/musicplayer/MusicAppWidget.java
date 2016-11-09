package com.example.musicplayer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

/**
 * Created by Administrator on 2016/11/9.
 */
public class MusicAppWidget extends AppWidgetProvider {

    // 更新指定id的小工具界面
    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId, String musicName, boolean isPlaying, Bitmap thumb) {

        // 创建RemoteViews
        // 在App widget的设计框架中，每个显示出来的小工具界面，叫做RemoteViews
        // 第二个参数就是桌面小工具的布局文件
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.music_app_widget);

        // 添加界面元素的逻辑控制代码，例如按钮、文字、图片等等
        // 设置音乐的名称
        views.setTextViewText(R.id.music_name, musicName);

        // 设置按钮响应的对象，这里是MusicService
        final ComponentName serviceName = new ComponentName(context, MusicService.class);

        // 用隐式调用的方法－指定Action－创建一个Intent
        // 设置下一首按钮对应的PendingIntent
        // 通过MusicService.ACTION_PLAY_MUSIC_NEXT定义隐性Intent，唤醒MusicService的响应
        Intent nextIntent = new Intent(MusicService.ACTION_PLAY_MUSIC_NEXT);
        // 设置响应的组件名称
        nextIntent.setComponent(serviceName);
        // 将Intent转化成PendingIntent
        PendingIntent nextPendingIntent = PendingIntent.getService(context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // 设置id为R.id.next_btn2的Button，设计点击后要触发的PendingIntent
        views.setOnClickPendingIntent(R.id.next_btn2, nextPendingIntent);

        // 设置前一首按钮对应的PendingIntent
        // 通过MusicService.ACTION_PLAY_MUSIC_PRE定义隐性Intent，唤醒MusicService的响应
        Intent preIntent = new Intent(MusicService.ACTION_PLAY_MUSIC_PRE);
        preIntent.setComponent(serviceName);
        PendingIntent prePendingIntent = PendingIntent.getService(context, 0, preIntent, 0);
        views.setOnClickPendingIntent(R.id.pre_btn2, prePendingIntent);

        // 设置播放暂停按钮对应的PendingIntent
        // 通过MusicService.ACTION_PLAY_MUSIC_TOGGLE定义隐性Intent，唤醒MusicService的响应
        Intent toggleIntent = new Intent(MusicService.ACTION_PLAY_MUSIC_TOGGLE);
        toggleIntent.setComponent(serviceName);
        PendingIntent togglePendingIntent = PendingIntent.getService(context, 0, toggleIntent, 0);
        views.setOnClickPendingIntent(R.id.play_btn2, togglePendingIntent);

        // 设置id为R.id.play_btn2的Button，调用它的setBackgroundResource()函数，
        // 传入R.drawable.ic_pause参数，相当于调用了Button的setBackgroundResource(R.drawable.ic_pause)
        // 设置播放暂停按钮的图标
        views.setInt(R.id.play_btn2, "setBackgroundResource", isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);

        // 设置音乐的封面
        if (thumb != null) {
            views.setImageViewBitmap(R.id.image_thumb, thumb);
        } else {
            views.setImageViewResource(R.id.image_thumb, R.drawable.default_cover);
        }

        // 通过appWidgetId，为指定的小工具界面更新
        appWidgetManager.updateAppWidget(appWidgetId, views);

    }

    @Override
    public void onEnabled(Context context) {
        Intent i = new Intent(context, MusicService.class);
        context.startService(i);
    }
}
