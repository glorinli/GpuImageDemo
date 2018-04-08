package xyz.dogold.gpuimagedemo;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.TextureView;

import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;
import xyz.dogold.gpuimagedemo.filters.GpuImageOesFilter;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private GpuImageFilterRenderThread mGpuImageFilterRenderThread;
    private MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        TextureView textureView = findViewById(R.id.textureView);

        textureView.setSurfaceTextureListener(this);

        // MediaPlayer
        mMediaPlayer = MediaPlayer.create(this, R.raw.video_480x360_mp4_h264_500kbps_30fps_aac_stereo_128kbps_44100hz);
        mMediaPlayer.setLooping(true);
        mMediaPlayer.setVolume(0f, 0f);

        mGpuImageFilterRenderThread = new GpuImageFilterRenderThread("gpuimage-filter-renderer");

        mGpuImageFilterRenderThread.setCallback(new GpuImageFilterRenderThread.Callback() {
            @Override
            public void onInputSurfaceReady(Surface surface) {
                final GPUImageFilterGroup filterGroup = new GPUImageFilterGroup();
                filterGroup.addFilter(new GpuImageOesFilter());
//                filterGroup.addFilter(new GPUImageSepiaFilter());

                mGpuImageFilterRenderThread.setFilter(filterGroup);

                mMediaPlayer.setSurface(surface);

                mMediaPlayer.start();
            }
        });

        mGpuImageFilterRenderThread.start();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mGpuImageFilterRenderThread.setOutputSurface(new Surface(surface), width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mGpuImageFilterRenderThread.handleSurfaceDestroyed();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }

        mGpuImageFilterRenderThread.shutdown();
    }
}
