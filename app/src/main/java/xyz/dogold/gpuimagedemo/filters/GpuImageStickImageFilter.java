package xyz.dogold.gpuimagedemo.filters;

public class GpuImageStickImageFilter extends GPUImageTwoInputFilter2 {
    private static final String STICK_IMAGE_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            "varying highp vec2 textureCoordinate2; // TODO: This is not used\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform sampler2D inputImageTexture2; // lookup texture\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "     vec4 t0 = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     vec4 t1 = texture2D(inputImageTexture2, textureCoordinate2);\n" +
            "     gl_FragColor = mix(t0, t1, t1.a);\n" +
            "}";

    public GpuImageStickImageFilter() {
        super(STICK_IMAGE_FRAGMENT_SHADER);
    }
}
