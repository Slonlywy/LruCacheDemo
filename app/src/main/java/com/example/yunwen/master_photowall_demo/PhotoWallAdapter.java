package com.example.yunwen.master_photowall_demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by yunwen on 2018/5/2.
 */

public class PhotoWallAdapter extends ArrayAdapter<String> implements AbsListView.OnScrollListener {

    /**
     * GridView实例
     */
    private GridView mPhotoWall;

    /**
     * 图片缓存技术的核心类，用于缓存所有下载好的图片，在程序内存达到设定值时会将最少最近使用的图片移除掉。
     */
    private LruCache<String, Bitmap> mMemoryCache;


    /**
     * 第一张可见图片的下标
     */
    private int mFirstVisibleItem;


    /**
     * 一页可以显示图片Item数量
     */
    private int mVisibleItemCount;

    /**
     * 记录所有正在下载或等待下载的任务。
     */
    private Set<DownBitmapWoker> mWokerHashSet;


    /**
     * 记录是否刚打开程序，用于解决进入程序不滚动屏幕，不会下载图片的问题。
     */
    private boolean isFirsEnter = true;

    public PhotoWallAdapter(Context context, int textViewResourceId, String[] objects, GridView photoWall) {
        super(context, textViewResourceId, objects);
        mPhotoWall = photoWall;

        //实例化集合来存储异步任务
        mWokerHashSet = new HashSet<>();


        //获取应用最大的可用内存
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        //将最大内存的1/8作为缓存内存
        int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };

        mPhotoWall.setOnScrollListener(this);
    }

    public PhotoWallAdapter(@NonNull Context context, @LayoutRes int resource) {
        super(context, resource);
    }

    public PhotoWallAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String url = getItem(position);
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.photo_images_layout, null);
        } else {
            view = convertView;
        }

        ImageView photo_image = (ImageView) view.findViewById(R.id.photo_image);
        // 给ImageView设置一个Tag，保证异步加载图片时不会乱序
        photo_image.setTag(url);
        setImageView(url, photo_image);

        return view;
    }


    /**
     * 给ImageView设置图片。首先从LruCache中取出图片的缓存，设置到ImageView上。如果LruCache中没有该图片的缓存，
     * 就给ImageView设置一张默认图片。
     *
     * @param imageUrl  图片的URL地址，用于作为LruCache的键。
     * @param imageView 用于显示图片的控件。
     */
    private void setImageView(String imageUrl, ImageView imageView) {
        Bitmap bitmapFromMemoryCache = getBitmapFromMemoryCache(imageUrl);

        if (bitmapFromMemoryCache != null) {
            imageView.setImageBitmap(bitmapFromMemoryCache);
        } else {
            imageView.setImageResource(R.drawable.default_photo);
        }
    }

    /**
     * 将一张图片存储到Lrucache中
     *
     * @param key    键值  指图片的Url
     * @param bitmap Bitmap对象，指从网上下载的图片
     */
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }


    /**
     * 根据键值从Lrucache中获取缓存的图片，不存在返回null
     *
     * @param key 这里指图片的Url
     * @return
     */
    public Bitmap getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // TODO: 2018/5/2 当GridView静止无操作时才下载图片 ，滑动时停止取消所有下载任务
        if (scrollState == SCROLL_STATE_IDLE) {
            loadBitmaps(mFirstVisibleItem, mVisibleItemCount);
        } else {
            //取消下载
            cancelAllTask();
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        mFirstVisibleItem = firstVisibleItem;
        mVisibleItemCount = visibleItemCount;
        if (isFirsEnter && visibleItemCount > 0) {
            loadBitmaps(firstVisibleItem, visibleItemCount);
            isFirsEnter = false;
        }
    }


    /**
     * 加载Bitmap对象。此方法会在LruCache中检查所有屏幕中可见的ImageView的Bitmap对象，
     * 如果发现任何一个ImageView的Bitmap对象不在缓存中，就会开启异步线程去下载图片。
     *
     * @param firstVisibleItem 第一个可见的ImageView的下标
     * @param visibleItemCount 屏幕中总共可见的元素数
     */
    private void loadBitmaps(int firstVisibleItem, int visibleItemCount) {
        for (int i = firstVisibleItem; i < firstVisibleItem + visibleItemCount; i++) {
            String image_url = Images.images[i];
            //获取缓存中的Bitmap
            Bitmap bitmapFromMemoryCache = getBitmapFromMemoryCache(image_url);
            if (bitmapFromMemoryCache == null) {
                //开启异步任务去下载
                DownBitmapWoker downBitmapWoker = new DownBitmapWoker();
                //保存正在下载的任务
                mWokerHashSet.add(downBitmapWoker);
                downBitmapWoker.execute(image_url);
            } else {
                //根据绑定的Tag去寻找对应的ImageView
                ImageView imageView = (ImageView) mPhotoWall.findViewWithTag(image_url);
                if (imageView != null && bitmapFromMemoryCache != null) {
                    imageView.setImageBitmap(bitmapFromMemoryCache);
                }
            }
        }
    }

    /**
     * 取消所有下载任务
     */
    public void cancelAllTask() {
        if (mWokerHashSet != null) {
            for (DownBitmapWoker task : mWokerHashSet) {
                task.cancel(false);
            }
        }
    }


    /**
     * 开启异步任务去下载图片
     */
    private class DownBitmapWoker extends AsyncTask<String, Void, Bitmap> {


        private String mImageUrl;

        @Override
        protected Bitmap doInBackground(String... params) {
            mImageUrl = params[0];
            Bitmap bitmap = downLoadBitmap(mImageUrl);
            if (bitmap != null) {
                //将下载的图片添加到Lrucache缓存中
                addBitmapToMemoryCache(params[0], bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            // 根据Tag找到相应的ImageView控件，将下载好的图片显示出来。
            ImageView imageTag = (ImageView) mPhotoWall.findViewWithTag(mImageUrl);
            if (bitmap != null && imageTag != null) {
                imageTag.setImageBitmap(bitmap);
            }

            mWokerHashSet.remove(bitmap);
        }

        /**
         * 建立Http请求去下载图片
         *
         * @param imageUrl
         * @return
         */
        private Bitmap downLoadBitmap(String imageUrl) {
            HttpURLConnection connection = null;
            Bitmap bitmap = null;
            try {
                URL url = new URL(imageUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5 * 1000);
                connection.setReadTimeout(10 * 1000);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                bitmap = BitmapFactory.decodeStream(connection.getInputStream(),null,options);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return bitmap;
        }
    }

}
