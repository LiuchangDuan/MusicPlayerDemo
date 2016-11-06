package com.example.musicplayer;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2016/11/6.
 */
public class Utils {

    public static String convertMSecondToTime(long time) {

        SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("mm:ss");

        Date date = new Date(time);

        String times = mSimpleDateFormat.format(date);

        return times;

    }

    // 创建封面图片
    public static Bitmap createThumbFromUri(ContentResolver res, Uri albumUri) {
        InputStream in = null;
        Bitmap bitmap = null;
        try {
            in = res.openInputStream(albumUri);
            BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
            bitmap = BitmapFactory.decodeStream(in, null, sBitmapOptions);
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

}
