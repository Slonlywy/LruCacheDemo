package com.example.yunwen.master_photowall_demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.LruCache;

import com.example.yunwen.master_photowall_demo.Config.Configs;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;

/**
 * Created by yunwen on 2018/5/2.
 */

public class ImagerLoader {

    private Context mContext;
    private int mMaxMemory;
    private int mCacheSize;
    private LruCache<String, Bitmap> mLruCache;
    private DiskLruCache mDiskLruCache =null;

    public ImagerLoader(Context context) {
        mContext = context.getApplicationContext();
        initLruCache();
        initDiskLruCache(context);
    }

    /**
     * 磁盘缓存
     * @param context
     */
    private void initDiskLruCache(Context context) {
        File file = getDiskCacheDir(context, "diskCache_bitmap");
        if (!file.exists()){
            file.mkdirs();
        }

        if (getUseableSpace(file)> Configs.DISK_CACHE_SIZE){
            //需要添加依赖
            try {
                mDiskLruCache = DiskLruCache.open(file, 1, 1, Configs.DISK_CACHE_SIZE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initLruCache() {
        //最大内存
        mMaxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        //最大内存的 1/8 作为缓存
        mCacheSize = mMaxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(mCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };
    }

    /**
     * 获取磁盘的存储目录，并创建目录
     * @param context
     * @param name
     * @return
     */
    private File getDiskCacheDir(Context context, String name) {
        boolean externalStorageState = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        String path;
        if (externalStorageState){
            path = context.getExternalCacheDir().getPath();
        }else {
            path = context.getCacheDir().getPath();
        }
        return new File(path + File.separator +name);
    }

    /**
     * 获取磁盘中可用的空间
     * @param file
     * @return
     */
    private long getUseableSpace(File file){
        if (Build.VERSION.SDK_INT>Build.VERSION_CODES.GINGERBREAD){
            return file.getUsableSpace();
        }else {
            StatFs statFs = new StatFs(file.getPath());
            return statFs.getBlockSize()*statFs.getAvailableBlocks();
        }
    }
}
