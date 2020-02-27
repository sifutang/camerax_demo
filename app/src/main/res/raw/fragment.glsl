#extension GL_OES_EGL_image_external:require
precision mediump float;

uniform int uEffectIndex;
uniform float uTimestamp;

uniform samplerExternalOES uTextureSampler;

varying vec2 vTextureCoord;

// 此处预览size写死, 实际应该从外部传过来
const vec2 previewSize = vec2(2160.0, 1080.0);
const vec2 mosaicSize = vec2(64.0, 64.0);

const float row = 2.0;
const float col = 2.0;

void main() {
    if (uEffectIndex == -1) {
        gl_FragColor = texture2D(uTextureSampler, vTextureCoord);
    } else if(uEffectIndex == 0) {
        float xPosScaled = vTextureCoord.x * row;
        float yPosScaled = vTextureCoord.y * col;
        vec2 coord = vec2((xPosScaled - floor(xPosScaled)), (yPosScaled - floor(yPosScaled)));
        vec4 color = texture2D(uTextureSampler, coord);

        float borderWidth = 0.01;
        // x >= borderWitdh && x < 1.0 - borderWidth && y >= borderWidth && y < 1.0 - borderWidth
        // step(edge, x): x > egde, return 1.0, else 0.0
        float isNotBorder = step(borderWidth, coord.x)
        * step(coord.x, 1.0 - borderWidth)
        * step(borderWidth, coord.y)
        * step(coord.y, 1.0 - borderWidth);
        // mix(x, y, a) => x * (1 - a) + y * a
        gl_FragColor = mix(vec4(1.0), color, isNotBorder);
    } else if (uEffectIndex == 1) {
        // 灰度滤镜:权值法, 人眼对红绿蓝色敏感程度不一样, 绿色 > 红色 > 蓝色
        vec4 mask = texture2D(uTextureSampler, vTextureCoord);
        float fGrayColor = (0.3 * mask.r + 0.59 * mask.g + 0.11 * mask.b);
        gl_FragColor = vec4(fGrayColor, fGrayColor, fGrayColor, 1.0);
    } else if(uEffectIndex == 2) {
        // 暖色滤镜:暖色增强, 红色和绿色为暖色, 适当增强
        vec4 mask = texture2D(uTextureSampler, vTextureCoord);
        gl_FragColor = mask + vec4(0.3, 0.3, 0.0, 0.0);
    } else if(uEffectIndex == 3) {
        // 冷色滤镜:冷色增强, 蓝色为冷色, 适当增强
        vec4 mask = texture2D(uTextureSampler, vTextureCoord);
        gl_FragColor = mask + vec4(0.0, 0.0, 0.3, 0.0);
    } else if(uEffectIndex == 4) {
        // 马赛克效果的原理就是把图片的一个相当大小区域用同一个点的颜色来表示，
        // 通过降低图像的分辨率，从而使图像一些细节隐藏起来
        vec2 pixelXY = vec2(vTextureCoord.x * previewSize.x, vTextureCoord.y * previewSize.y);
        vec2 mosaicXY = vec2(floor(pixelXY.x / mosaicSize.x) * mosaicSize.x, floor(pixelXY.y / mosaicSize.y) * mosaicSize.y);
        vec2 newPixelStCoord = vec2(mosaicXY.x / previewSize.x, mosaicXY.y / previewSize.y);
        gl_FragColor = texture2D(uTextureSampler, newPixelStCoord);
    } else if(uEffectIndex == 5) {
        float lightUpColor = abs(sin(uTimestamp / 1000.0)) / 4.0;//[0, 0.25]
        vec4 mask = texture2D(uTextureSampler, vTextureCoord);
        gl_FragColor = mask + vec4(lightUpColor, lightUpColor, lightUpColor, 1.0);
    }
}