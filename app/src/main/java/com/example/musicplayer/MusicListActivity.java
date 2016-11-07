package com.example.musicplayer;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MusicListActivity extends AppCompatActivity {

    public static final String TAG = "MusicListActivity";

    private List<MusicItem> mMusicList;
    private ListView mMusicListView;
    private Button mPlayBtn;
    private Button mPreBtn;
    private Button mNextBtn;
    private TextView mMusicTitle;
    private TextView mPlayedTime;
    private TextView mDurationTime;
    private SeekBar mMusicSeekBar;
    private MusicUpdateTask mMusicUpdateTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list);

        mMusicList = new ArrayList<MusicItem>();
        mMusicListView = (ListView) findViewById(R.id.music_list);
        MusicItemAdapter adapter = new MusicItemAdapter(this, R.layout.music_item, mMusicList);
        mMusicListView.setAdapter(adapter);
//        mMusicListView.setOnItemClickListener()

        mPlayBtn = (Button) findViewById(R.id.play_btn);
        mPreBtn = (Button) findViewById(R.id.pre_btn);
        mNextBtn = (Button) findViewById(R.id.next_btn);

        mMusicTitle = (TextView) findViewById(R.id.music_title);

        mDurationTime = (TextView) findViewById(R.id.duration_time);
        mPlayedTime = (TextView) findViewById(R.id.played_time);
        mMusicSeekBar = (SeekBar) findViewById(R.id.seek_music);

        mMusicUpdateTask = new MusicUpdateTask();
        mMusicUpdateTask.execute();

        Intent i = new Intent(this, MusicService.class);
        startService(i);
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);

    }

    private class MusicUpdateTask extends AsyncTask<Object, MusicItem, Void> {
        List<MusicItem> mDataList = new ArrayList<MusicItem>();

        @Override
        protected Void doInBackground(Object... params) {
            /**
             * 向Media Provider发出查询请求的地址 - uri
             * 这里查询的音乐文件都是存放在外部存储地址上的
             */
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            String[] searchKey = new String[] {
                MediaStore.Audio.Media._ID, // 对应文件在数据库中的检索ID
                MediaStore.Audio.Media.TITLE, // 对应文件的标题
                MediaStore.Audio.Albums.ALBUM_ID, // 对应文件所在的专辑ID，在后面获取封面图片时会用到
                MediaStore.Audio.Media.DATA, // 对应文件的存放位置
                MediaStore.Audio.Media.DURATION // 对应文件的播放时长
            };

            // 查询包含有music这个字段的文件
            String where = MediaStore.Audio.Media.DATA + " like \"%" + getString(R.string.searh_path) + "%\"";

            String[] keywords = null;

            // 设定查询结果的排序方式，使用默认的排序方式
            String sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

            ContentResolver resolver = getContentResolver();

            Cursor cursor = resolver.query(uri, searchKey, where, keywords, sortOrder);

            if (cursor != null) {
                while (cursor.moveToNext() && !isCancelled()) {
                    // 获取音乐的路径
                    // 这个参数我们实际上不会用到
                    // 不过在调试程序的时候可以方便我们看到音乐的真实路径
                    // 确定寻找的文件的确就在我们规定的目录当中
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    // 获取音乐的ID
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    // 通过URI和ID，组合出改音乐特有的Uri地址
                    // 每首音乐都有一个全局唯一的URI地址
                    // 操作某首具体的音乐就可以通过这个地址来完成
                    // 而id就是用来获取该音乐的URI地址的
                    // 将id与音频的URI地址组合一下就能得到特定某首音乐的URI地址
                    // 它的形式就像content://media/external/audio/media/12345，
                    Uri musicUri = Uri.withAppendedPath(uri, id);
                    // 获取音乐的名称
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    // 获取音乐的时长，单位是毫秒
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    // 获取该音乐所在专辑的id
                    int albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ID));
                    // 通过AlbumId组合出专辑的Uri地址
                    // like "content://media/external/audio/albumart/4
                    Uri albumUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);

                    MusicItem data = new MusicItem(musicUri, albumUri, name, duration, 0);

                    if (uri != null) {
                        ContentResolver res = getContentResolver();
                        data.thumb = Utils.createThumbFromUri(res, albumUri);
                    }

                    Log.d(TAG, "real music found : " + path);

                    publishProgress(data);

                }
                cursor.close();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(MusicItem... values) {
            MusicItem data = values[0];

            mMusicList.add(data);
            MusicItemAdapter adapter = (MusicItemAdapter) mMusicListView.getAdapter();
            adapter.notifyDataSetChanged();
        }
    }

    private MusicService.MusicServiceIBinder mMusicService;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 这里的service参数，就是Service当中onBind()返回的Binder
            mMusicService = (MusicService.MusicServiceIBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 当Service遇到异常情况退出时，会通过这里通知绑定过它的组件
        }
    };

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play_btn:
                break;
            case R.id.next_btn:
                break;
            case R.id.pre_btn:
                break;
        }
    }

}
