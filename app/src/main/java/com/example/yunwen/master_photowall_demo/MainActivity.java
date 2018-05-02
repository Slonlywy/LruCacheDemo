package com.example.yunwen.master_photowall_demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.GridView;

public class MainActivity extends AppCompatActivity {

    private GridView mGridView;
    private PhotoWallAdapter mPhotoWallAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGridView = (GridView) findViewById(R.id.gridview);
        mPhotoWallAdapter = new PhotoWallAdapter(this, 0, Images.images, mGridView);

        mGridView.setAdapter(mPhotoWallAdapter);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPhotoWallAdapter.cancelAllTask();
    }
}
