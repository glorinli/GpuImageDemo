package xyz.dogold.gpuimagedemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import xyz.dogold.gpuimagedemo.ui.VideoFilterActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnImageFilter:
                break;
            case R.id.btnVideoFilter:
                startActivity(new Intent(this, VideoFilterActivity.class));
                break;
        }
    }
}
