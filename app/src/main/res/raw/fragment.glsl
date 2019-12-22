#extension GL_OES_EGL_image_external:require
precision mediump float;

uniform samplerExternalOES uTextureSampler;
varying vec2 vTextureCoord;

const vec2 previewSize = vec2(2160.0, 1080.0);
const vec2 msSize = vec2(64.0, 64.0);

void main() {
//    vec4 mask = texture2D(uTextureSampler, vTextureCoord);
    // 灰度滤镜:权值法, 人眼对红绿蓝色敏感程度不一样, 绿色 > 红色 > 蓝色
//    float fGrayColor = (0.3 * mask.r + 0.59 * mask.g + 0.11 * mask.b);
//    gl_FragColor = vec4(fGrayColor, fGrayColor, fGrayColor, 1.0);

    // 暖色滤镜:暖色增强, 红色和绿色为暖色, 适当增强
//    gl_FragColor = mask + vec4(0.3, 0.3, 0.0, 0.0);

    // 冷色滤镜:冷色增强, 蓝色为冷色, 适当增强
//    gl_FragColor = mask + vec4(0.0, 0.0, 0.3, 0.0);

    vec2 intXY = vec2(vTextureCoord.x * previewSize.x, vTextureCoord.y * previewSize.y);
    vec2 mskXY = vec2(floor(intXY.x / msSize.x) * msSize.x, floor(intXY.y / msSize.y) * msSize.y);
    vec2 previewXY = vec2(mskXY.x / previewSize.x, mskXY.y / previewSize.y);
    vec4 mask = texture2D(uTextureSampler, previewXY);
    gl_FragColor = mask;
}