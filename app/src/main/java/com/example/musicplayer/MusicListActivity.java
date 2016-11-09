package com.example.musicplayer;

import android.app.AlertDialog;
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
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
    // 播放和暂停使用的按钮
    private Button mPlayBtn;
    // 前一首
    private Button mPreBtn;
    // 下一首
    private Button mNextBtn;
    // 音乐名称
    private TextView mMusicTitle;
    // 播放时长
    private TextView mPlayedTime;
    // 当前播放时间
    private TextView mDurationTime;
    // 进度条
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
        mMusicListView.setOnItemClickListener(mOnMusicItemClickListener);

        // 将音乐列表设置成多选modal模式
        mMusicListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);

        mMusicListView.setMultiChoiceModeListener(mMultiChoiceListener);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mMusicUpdateTask != null && mMusicUpdateTask.getStatus() == AsyncTask.Status.RUNNING) {
            mMusicUpdateTask.cancel(true);
        }

        mMusicUpdateTask = null;

//        mMusicService.unregisterOnStateChangeListener();

        unbindService(mServiceConnection);

        for (MusicItem item : mMusicList) {
            if (item.thumb != null) {
                item.thumb.recycle();
                item.thumb = null;
            }
        }

        mMusicList.clear();

    }

    // 进度条拖动的监听器
    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // 停止拖动时，根据进度条的位置来设定播放的位置
            if (mMusicService != null) {
                mMusicService.seekTo(seekBar.getProgress());
            }
        }

    };

    private AdapterView.OnItemClickListener mOnMusicItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mMusicService != null) {
                // 通过MusicService提供的接口，把要添加的音乐交给MusicService处理
                mMusicService.addPlayList(mMusicList.get(position));
            }
        }

    };

    private ListView.MultiChoiceModeListener mMultiChoiceListener = new AbsListView.MultiChoiceModeListener() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // 增加进入多选modal模式后的菜单栏菜单项
            getMenuInflater().inflate(R.menu.music_choice_actionbar, menu);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_play:
                    // TODO
                    // 这里添加点击添加到播放列表后的响应
                    // 获取被选中的音乐项
                    List musicList = new ArrayList<MusicItem>();
                    SparseBooleanArray checkedResult = mMusicListView.getCheckedItemPositions();
                    for (int i = 0; i < checkedResult.size(); i++) {
                        if (checkedResult.valueAt(i)) {
                            int pos = checkedResult.keyAt(i);
                            MusicItem music = mMusicList.get(pos);
                            musicList.add(music);
                        }
                    }

                    // 调用MusicService提供的接口，把播放列表保存起来
                    mMusicService.addPlayList(musicList);

                    // 退出ListView的modal状态
                    mode.finish();

                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

        }
    };

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
            String where = MediaStore.Audio.Media.DATA + " like \"%" + getString(R.string.search_path) + "%\"";

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
            // 点击播放按钮
            case R.id.play_btn:
                if (mMusicService != null) {
                    if (!mMusicService.isPlaying()) {
                        // 开始播放
                        mMusicService.play();
                    } else {
                        // 暂停播放
                        mMusicService.pause();
                    }
                }
                break;
            // 点击下一首按钮
            case R.id.next_btn:
                if (mMusicService != null) {
                    mMusicService.playNext();
                }
                break;
            // 点击前一首按钮
            case R.id.pre_btn:
                if (mMusicService != null) {
                    mMusicService.playPre();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.play_list_menu:
                // 响应用户对菜单的点击，显示播放列表
                showPlayList();
                break;
        }

        return true;
    }

    private void showPlayList() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 设置对话框的图标
        builder.setIcon(R.drawable.ic_playlist);
        // 设置对话框的显示标题
        builder.setTitle(R.string.play_list);

        // 获取播放列表，把播放列表中歌曲的名字取出组成新的列表
        List<MusicItem> playList = mMusicService.getPlayList();
        ArrayList<String> data = new ArrayList<String>();
        for (MusicItem music : playList) {
            data.add(music.name);
        }

        if (data.size() > 0) {
            // 播放列表有曲目，显示音乐的名称
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data);
            builder.setAdapter(adapter, null);
        } else {
            // 播放列表没有曲目，显示没有音乐
            builder.setMessage(getString(R.string.no_song));
        }

        // 设置该对话框是可以自动取消的，例如当用户在空白处随便点击一下，对话框就会关闭消失
        builder.setCancelable(true);

        // 创建并显示对话框
        builder.create().show();

    }

}
