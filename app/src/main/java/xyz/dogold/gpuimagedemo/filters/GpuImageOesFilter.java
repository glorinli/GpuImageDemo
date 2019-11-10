package xyz.dogold.gpuimagedemo.filters;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils;
import xyz.dogold.gpuimagedemo.gles.MatrixUtils;

public class GpuImageOesFilter extends GPUImageFilter {
    private float[] mVertexMatrix = MatrixUtils.getOriginalMatrix();
    private float[] mTextureMatrix = MatrixUtils.getOriginalMatrix();
    private int mGLTextureMatrix, mGLVertexMatrix;

    public GpuImageOesFilter() {
        super("attribute vec4 position;\n" +
                        "attribute vec4 inputTextureCoordinate;\n" +
                        " \n" +
                        "uniform mat4 vertextMatrix;\n" +
                        "uniform mat4 textureMatrix;\n" +
                        "\n" +
                        "varying vec2 textureCoordinate;\n" +
                        " \n" +
                        "void main()\n" +
                        "{\n" +
                        "    gl_Position = vertextMatrix * position;\n" +
                        "    textureCoordinate = (textureMatrix * inputTextureCoordinate).xy;\n" +
                        "}",
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 textureCoordinate;\n" +
                        " \n" +
                        "uniform samplerExternalOES inputImageTexture;\n" +
                        " \n" +
                        "void main()\n" +
                        "{\n" +
                        "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                        "}");
    }

    @Override
    public void onInit() {
        super.onInit();

        mGLTextureMatrix = GLES20.glGetUniformLocation(getProgram(), "textureMatrix");
        mGLVertexMatrix = GLES20.glGetUniformLocation(getProgram(), "vertextMatrix");
    }

    @Override
    public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        GLES20.glUseProgram(getProgram());
        runPendingOnDrawTasks();

        if (!isInitialized()) {
            return;
        }

        GLES20.glUniformMatrix4fv(mGLTextureMatrix, 1, false, mTextureMatrix, 0);
        GLES20.glUniformMatrix4fv(mGLVertexMatrix, 1, false, mVertexMatrix, 0);

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(getAttribPosition(), 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(getAttribPosition());
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(getAttribTextureCoordinate(), 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(getAttribTextureCoordinate());
        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(getUniformTexture(), 0);
        }
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(getAttribPosition());
        GLES20.glDisableVertexAttribArray(getAttribTextureCoordinate());
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    public float[] getTextureMatrix() {
        return mTextureMatrix;
    }

    public float[] getVertexMatrix() {
        return mVertexMatrix;
    }
}
