package com.example.yunwen.master_photowall_demo;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.FileDescriptor;

/**
 * Created by hwj on 2018/5/2.
 * 图片压缩的工具类
 */

public class ImageResizer {
    private static final String TAG = "ImageResizer";


    public ImageResizer() {
    }

    /**
     *从资源文件中加载图片。
     * @param resources
     * @param resId
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public Bitmap decodeSampleBitmapFromResource(Resources resources,int resId,int reqWidth,int reqHeight){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(resources,resId,options);

        //calculate SampleSize
        options.inSampleSize = calculateSampleSize(options,reqWidth,reqHeight);

        options.inJustDecodeBounds=false;
        return BitmapFactory.decodeResource(resources,resId,options);
    }


    /**
     * 从内存卡中加载图片
     * @param fd
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public Bitmap decodeSampleBitmapFromFileDescriptor(FileDescriptor fd,int reqWidth,int reqHeight){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds=true;

        options.inSampleSize = calculateSampleSize(options,reqWidth,reqHeight);

        options.inJustDecodeBounds=false;
        return BitmapFactory.decodeFileDescriptor(fd,null,options);
    }


    /**
     *计算图片的采样率
     * 原理，如果设置的图片宽、高小于原图的宽、高。则inSampleSize呈2的指数缩小
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int calculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        if (reqWidth == 0 || reqHeight == 0){
            return 1;
        }

        int outWidth = options.outWidth;
        int outHeight = options.outHeight;

        int inSampleSize = 1;

        if (outWidth>reqWidth ||outHeight>reqHeight){
            int halfWidth = outWidth / 2;
            int halfHeight = outHeight / 2;

            while ((halfWidth/inSampleSize)>=reqWidth
                    &&(halfHeight/inSampleSize)>=reqHeight){
                inSampleSize *= 2;
            }
        }

        Log.e(TAG,"inSampleSize = "+inSampleSize);
        return inSampleSize;

    }
}
