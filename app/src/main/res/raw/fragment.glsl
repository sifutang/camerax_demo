#extension GL_OES_EGL_image_external:require
precision mediump float;

uniform samplerExternalOES uTextureSampler;
varying vec2 vTextureCoord;

void main() {
    vec4 mask = texture2D(uTextureSampler, vTextureCoord);
    // 灰度滤镜:权值法, 人眼对红绿蓝色敏感程度不一样, 绿色 > 红色 > 蓝色
//    float fGrayColor = (0.3 * mask.r + 0.59 * mask.g + 0.11 * mask.b);
//    gl_FragColor = vec4(fGrayColor, fGrayColor, fGrayColor, 1.0);

    // 暖色滤镜:暖色增强, 红色和绿色为暖色, 适当增强
//    gl_FragColor = mask + vec4(0.3, 0.3, 0.0, 0.0);

    // 冷色滤镜:冷色增强, 蓝色为冷色, 适当增强
    gl_FragColor = mask + vec4(0.0, 0.0, 0.3, 0.0);
}