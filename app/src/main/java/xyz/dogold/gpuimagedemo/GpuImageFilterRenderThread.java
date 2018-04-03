package xyz.dogold.gpuimagedemo;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import xyz.dogold.gpuimagedemo.gles.EglCore;
import xyz.dogold.gpuimagedemo.gles.GlUtil;
import xyz.dogold.gpuimagedemo.gles.WindowSurface;

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

public class GpuImageFilterRenderThread extends HandlerThread {
    private static final String TAG = "RenderThread";

    private EglCore mEglCore;

    private static final float[] CUBE = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    private int mInputTextureId = -1;
    private SurfaceTexture mInputSurfaceTexture;
    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGLTextureBuffer;

    private GPUImageFilter mGpuImageFilter;

    private volatile Handler mHandler = null;

    private WindowSurface mOutputWindowSurface;
    private int mOutputWidth = -1;
    private int mOutputHeight = -1;

    public GpuImageFilterRenderThread(String name) {
        super(name);
    }

    @Override
    public synchronized void start() {
        super.start();

        mHandler = new Handler(getLooper());

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                doInit();
            }
        });
    }

    private void doInit() {
        Log.d(TAG, "doInit, thread: " + Thread.currentThread().getName());

        checkThread();

        mEglCore = new EglCore(null, 0);

        mInputTextureId = GlUtil.createTextureObject(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mInputSurfaceTexture = new SurfaceTexture(mInputTextureId);

        mInputSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "updateTexImage, thread: " + Thread.currentThread().getName());
                        mInputSurfaceTexture.updateTexImage();
                        drawToOutput();
                    }
                });
            }
        });

        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TEXTURE_NO_ROTATION).position(0);

        if (mCallback != null) mCallback.onInputSurfaceReady(new Surface(mInputSurfaceTexture));
    }

    private void drawToOutput() {
        if (mGpuImageFilter != null && mOutputWindowSurface != null) {
            GlUtil.checkGlError("draw start: " + mOutputWindowSurface.hashCode());

            mOutputWindowSurface.makeCurrent();

            mGpuImageFilter.onDraw(mInputTextureId, mGLCubeBuffer, mGLTextureBuffer);

            mOutputWindowSurface.swapBuffers();

            GlUtil.checkGlError("draw done: " + mOutputWindowSurface.hashCode());
        }
    }

    public void setOutputSurface(final Surface surface, final int width, final int height) {
        checkRunning();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                doSetOutputSurface(surface, width, height);
            }
        });
    }

    public void handleSurfaceDestroyed() {
        checkRunning();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mOutputWindowSurface != null) {
                    mOutputWindowSurface.release();
                }
            }
        });
    }

    private void doSetOutputSurface(Surface surface, int width, int height) {
        checkThread();

        if (mOutputWindowSurface != null) {
            mOutputWindowSurface.release();
        }

        mOutputWindowSurface = new WindowSurface(mEglCore, surface, false);

        mOutputWindowSurface.makeCurrent();

        mOutputWidth = width;
        mOutputHeight = height;

        if (mGpuImageFilter != null) {
            mGpuImageFilter.onOutputSizeChanged(width, height);
        }
    }

    private void checkThread() {
        if (Thread.currentThread().getId() != getLooper().getThread().getId()) {
            throw new IllegalStateException("Should call in Handler thread!!");
        }
    }

    private void checkRunning() {
        if (mHandler == null) {
            throw new IllegalStateException("Not running!!!");
        }
    }

    public void setFilter(final GPUImageFilter filter) {
        checkRunning();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mGpuImageFilter = filter;

                if (!mGpuImageFilter.isInitialized()) mGpuImageFilter.init();

                if (mOutputWidth > 0 && mOutputHeight > 0) {
                    mGpuImageFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
                }
            }
        });
    }

    public interface Callback {
        void onInputSurfaceReady(Surface surface);
    }

    private Callback mCallback;

    public void setCallback(Callback listener) {
        mCallback = listener;
    }
}
