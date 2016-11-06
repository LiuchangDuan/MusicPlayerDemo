package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Administrator on 2016/11/6.
 */
public class MusicItemAdapter extends BaseAdapter {

    private List<MusicItem> mData;
    private final LayoutInflater mInflater;
    private final int mResource;
    private Context mContext;

    public MusicItemAdapter(Context context, int resId, List<MusicItem> data) {
        mContext = context;
        mData = data;
        mInflater = LayoutInflater.from(context);
        mResource = resId;
    }

    @Override
    public int getCount() {
        return mData != null ? mData.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mData != null ? mData.get(position) : 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(mResource, parent, false);
        }

        MusicItem item = mData.get(position);

        TextView title = (TextView) convertView.findViewById(R.id.music_title);
        title.setText(item.name);

        TextView creteTime = (TextView) convertView.findViewById(R.id.music_duration);

        String times = Utils.convertMSecondToTime(item.duration);
        times = String.format(mContext.getString(R.string.duration), times);
        creteTime.setText(times);

        ImageView thumb = (ImageView) convertView.findViewById(R.id.music_thumb);
        if (thumb != null) {
            if (item.thumb != null) {
                thumb.setImageBitmap(item.thumb);
            } else {
                thumb.setImageResource(R.drawable.default_cover);
            }
        }

        return convertView;
    }

}
