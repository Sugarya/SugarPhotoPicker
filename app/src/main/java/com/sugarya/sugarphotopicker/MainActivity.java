package com.sugarya.sugarphotopicker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.sugarya.sugarphotopicker.widget.photorecycle.PhotoEntity;
import com.sugarya.sugarphotopicker.widget.photorecycle.SelectPhotoRecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.recycler_select_photo)
    SelectPhotoRecyclerView mRecyclerPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initRecycler();
    }

    private void initRecycler(){
        mRecyclerPhoto.setCurrentActivity(this);
        mRecyclerPhoto.setOnOperatorPhotoListener(new SelectPhotoRecyclerView.OnOperatorPhotoListener() {
            @Override
            public void uploadPhoto(List<Uri> imageList) {

            }

            @Override
            public void deletePhoto(PhotoEntity removePhotoEntity) {
                mRecyclerPhoto.removePhotoData(removePhotoEntity);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mRecyclerPhoto.onActivityResult(requestCode, resultCode, data);
    }
}
