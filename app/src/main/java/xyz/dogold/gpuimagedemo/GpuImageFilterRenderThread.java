package xyz.dogold.gpuimagedemo;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.nio.FloatBuffer;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;
import xyz.dogold.gpuimagedemo.filters.GPUImageLookupFilter2;
import xyz.dogold.gpuimagedemo.filters.GpuImageOesFilter;
import xyz.dogold.gpuimagedemo.gles.EglCore;
import xyz.dogold.gpuimagedemo.gles.GlUtil;
import xyz.dogold.gpuimagedemo.gles.MatrixUtils;
import xyz.dogold.gpuimagedemo.gles.WindowSurface;

public class GpuImageFilterRenderThread extends HandlerThread {
    private static final String TAG = "RenderThread";

    private EglCore mEglCore;

    private int mInputTextureId = -1;
    private SurfaceTexture mInputSurfaceTexture;
    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGLTextureBuffer;

    private GPUImageFilterGroup mGPUImageFilterGroup;
    private GpuImageOesFilter mGpuImageOesFilter;

    private volatile Handler mHandler = null;

    private WindowSurface mOutputWindowSurface;
    private int mOutputWidth = -1;
    private int mOutputHeight = -1;

    private Surface mInputSurface;

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

        mGpuImageOesFilter = new GpuImageOesFilter();

        // TODO, should compute matrix every time drawing occurs
        MatrixUtils.flip(mGpuImageOesFilter.getVertexMatrix(), false, true);

        mGLCubeBuffer = GpuImageUtil.createCubeBuffer();

        mGLTextureBuffer = GpuImageUtil.createTextureBuffer();
    }

    private void drawToOutput() {
        if (mGPUImageFilterGroup != null && mOutputWindowSurface != null) {
            GlUtil.checkGlError("draw start: " + mOutputWindowSurface.hashCode());

            mOutputWindowSurface.makeCurrent();

            mGPUImageFilterGroup.onDraw(mInputTextureId, mGLCubeBuffer, mGLTextureBuffer);

            mOutputWindowSurface.swapBuffers();

            GlUtil.checkGlError("draw done: " + mOutputWindowSurface.hashCode());
        }
    }

    public void setOutputSurface(final Surface surface, final int width, final int height) {
        Log.d(TAG, String.format("setOutputSurface, size: %1$dx%2$d", width, height));
        checkRunning();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                doSetOutputSurface(surface, width, height);

                // Create input surface
                mInputTextureId = GlUtil.createTextureObject(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
                mInputSurfaceTexture = new SurfaceTexture(mInputTextureId);

                mInputSurfaceTexture.setDefaultBufferSize(width, height);

                if (mCallback != null) {
                    Surface oldSurface = mInputSurface;

                    mInputSurface = new Surface(mInputSurfaceTexture);
                    mCallback.onInputSurfaceReady(mInputSurface, oldSurface);

                    if (oldSurface != null) oldSurface.release();
                }

                mInputSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
//                                if (BuildConfig.DEBUG)
//                                    Log.d(TAG, "updateTexImage, thread: " + Thread.currentThread().getName());
                                mInputSurfaceTexture.updateTexImage();
                                mInputSurfaceTexture.getTransformMatrix(mGpuImageOesFilter.getTextureMatrix());
                                drawToOutput();
                            }
                        });
                    }
                });
            }
        });
    }

    public void setOutputSize(final int width, final int height) {
        Log.d(TAG, String.format("setOutputSize, size: %1$dx%2$d", width, height));
        checkRunning();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                doSetOutputSize(width, height);

                mInputSurfaceTexture.setDefaultBufferSize(mOutputWidth, mOutputHeight);

                if (mCallback != null) {
                    mCallback.onInputSurfaceSizeChanged();
                }
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
                    mOutputWindowSurface = null;
                }

                mInputSurfaceTexture.setOnFrameAvailableListener(null);
                mInputSurfaceTexture.release();
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

        doSetOutputSize(width, height);
    }

    private void doSetOutputSize(int width, int height) {
        mOutputWidth = width;
        mOutputHeight = height;

        if (mGPUImageFilterGroup != null) {
            mGPUImageFilterGroup.onOutputSizeChanged(width, height);
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
        Log.d(TAG, "setFilter: " + filter);

        checkRunning();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mGPUImageFilterGroup != null) {
                    mGPUImageFilterGroup.destroy();
                }

                mGPUImageFilterGroup = new GPUImageFilterGroup();
                mGPUImageFilterGroup.addFilter(mGpuImageOesFilter);

                if (filter != null) mGPUImageFilterGroup.addFilter(filter);

                if (!mGPUImageFilterGroup.isInitialized()) mGPUImageFilterGroup.init();

                if (mOutputWidth > 0 && mOutputHeight > 0) {
                    mGPUImageFilterGroup.onOutputSizeChanged(mOutputWidth, mOutputHeight);
                }

                if (mInputSurfaceTexture != null && mOutputWindowSurface != null) {
                    drawToOutput();
                }
            }
        });
    }

    public void shutdown() {
        checkRunning();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mEglCore.release();
                quitSafely();

                if (mGPUImageFilterGroup != null) {
                    mGPUImageFilterGroup.destroy();
                    mGPUImageFilterGroup = null;
                }
            }
        });
    }

    public Surface getInputSurface() {
        return mInputSurface;
    }

    public void setFilterIntensity(final float intensity) {
        Log.d(TAG, "setFilterIntensity: " + intensity);
        checkRunning();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                boolean changed = false;

                if (mGPUImageFilterGroup != null) {
                    final List<GPUImageFilter> mergedFilters = mGPUImageFilterGroup.getMergedFilters();

                    if (mergedFilters != null) {
                        for (GPUImageFilter filter : mergedFilters) {
                            if (filter instanceof GPUImageLookupFilter2) {
                                ((GPUImageLookupFilter2) filter).setIntensity(intensity);
                                changed = true;
                            }
                        }
                    }
                }

                if (changed) {
                    if (mInputSurfaceTexture != null && mOutputWindowSurface != null) {
                        drawToOutput();
                    }
                }
            }
        });
    }

    public interface Callback {
        void onInputSurfaceReady(Surface surface, Surface oldSurface);

        void onInputSurfaceSizeChanged();
    }

    private Callback mCallback;

    public void setCallback(Callback listener) {
        mCallback = listener;
    }

    public void runOnGLThread(Runnable runnable) {
        checkRunning();

        mHandler.post(runnable);
    }
}
