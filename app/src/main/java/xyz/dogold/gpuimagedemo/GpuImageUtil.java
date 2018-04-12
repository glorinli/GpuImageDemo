package xyz.dogold.gpuimagedemo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

public class GpuImageUtil {
    public static final float[] CUBE = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    public static FloatBuffer createCubeBuffer() {
        final FloatBuffer glCubeBuffer = ByteBuffer.allocateDirect(GpuImageUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glCubeBuffer.put(GpuImageUtil.CUBE).position(0);

        return glCubeBuffer;
    }

    public static FloatBuffer createTextureBuffer() {
        final FloatBuffer glTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);

        return glTextureBuffer;
    }
}
