attribute vec4 aPosition;
attribute vec4 aTextureCoordinate;

uniform mat4 uTextureMatrix;

varying vec2 vTextureCoord;

void main() {
    vTextureCoord = (uTextureMatrix * aTextureCoordinate).xy;
    gl_Position = aPosition;
}
