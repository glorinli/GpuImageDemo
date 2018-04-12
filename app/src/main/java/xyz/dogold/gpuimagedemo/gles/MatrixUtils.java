package xyz.dogold.gpuimagedemo.gles;

import android.opengl.Matrix;

/**
 * MatrixUtils
 * <p>
 * Created by moore on 2017/12/1.
 */
public class MatrixUtils {

    private static final String TAG = MatrixUtils.class.getSimpleName();

    public static final int TYPE_FIT_XY = 0;
    public static final int TYPE_CENTER_CROP = 1;
    public static final int TYPE_CENTER_IN_SIDE = 2;
    public static final int TYPE_FIT_START = 3;
    public static final int TYPE_FIT_END = 4;


    public static float[] getOriginalTextureCo() {
        return new float[]{
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f
        };
    }

    public static float[] getOriginalVertexCo() {
        return new float[]{
                -1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f
        };
    }

    public static float[] getOriginalMatrix() {
        return new float[]{
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
        };
    }


    public static void getMatrix(float[] matrix, int type, int imgWidth, int imgHeight, int viewWidth,
                                 int viewHeight) {
        if (imgHeight > 0 && imgWidth > 0 && viewWidth > 0 && viewHeight > 0) {
            float[] projection = new float[16];
            float[] camera = new float[16];
            if (type == TYPE_FIT_XY) {
                Matrix.orthoM(projection, 0, -1, 1, -1, 1, 1, 3);
                Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
                Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
                return;
            }

//            LogUtils.showFloatArrayLog(TAG, "before type=" + type + ";imgWidth=" + imgWidth + ";imgHeight=" + imgHeight + ";viewWidth=" + viewWidth + ";viewHeight=" + viewHeight + ";matrix=", matrix, 4);
            float sWhView = (float) viewWidth / viewHeight;
            float sWhImg = (float) imgWidth / imgHeight;
            if (sWhImg > sWhView) {
                switch (type) {
                    case TYPE_CENTER_CROP:
                        Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1, 1, 1, 3);//Matrix.orthoM()=正交投影; Matrix.frustumM()=透视投影
                        break;
                    case TYPE_CENTER_IN_SIDE:
                        Matrix.orthoM(projection, 0, -1, 1, -sWhImg / sWhView, sWhImg / sWhView, 1, 3);
                        break;
                    case TYPE_FIT_START:
                        Matrix.orthoM(projection, 0, -1, 1, 1 - 2 * sWhImg / sWhView, 1, 1, 3);
                        break;
                    case TYPE_FIT_END:
                        Matrix.orthoM(projection, 0, -1, 1, -1, 2 * sWhImg / sWhView - 1, 1, 3);
                        break;
                    default:
                        break;
                }
            } else {
                switch (type) {
                    case TYPE_CENTER_CROP:
                        Matrix.orthoM(projection, 0, -1, 1, -sWhImg / sWhView, sWhImg / sWhView, 1, 3);
                        break;
                    case TYPE_CENTER_IN_SIDE:
                        Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1, 1, 1, 3);
                        break;
                    case TYPE_FIT_START:
                        Matrix.orthoM(projection, 0, -1, 2 * sWhView / sWhImg - 1, -1, 1, 1, 3);
                        break;
                    case TYPE_FIT_END:
                        Matrix.orthoM(projection, 0, 1 - 2 * sWhView / sWhImg, 1, -1, 1, 1, 3);
                        break;
                    default:
                        break;
                }
            }
            Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);

            Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);

//            LogUtils.showFloatArrayLog(TAG, "camera=", camera, 4);
//            LogUtils.showFloatArrayLog(TAG, "projection=", projection, 4);
        }
//        LogUtils.showFloatArrayLog(TAG, "after type=" + type + ";imgWidth=" + imgWidth + ";imgHeight=" + imgHeight + ";viewWidth=" + viewWidth + ";viewHeight=" + viewHeight + ";matrix=", matrix, 4);
    }

    public static float[] flip(float[] m, boolean x, boolean y) {
        if (x || y) {
            Matrix.scaleM(m, 0, x ? -1 : 1, y ? -1 : 1, 1);//sx,sy, sz=缩放因子
        }
        return m;
    }

    public static void rotateM(float[] mMMatrix, float angle, float x, float y, float z) {

        Matrix.rotateM(mMMatrix, 0, angle, x, y, z);
    }

}

