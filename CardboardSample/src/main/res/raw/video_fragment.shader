#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES videoTexture;

varying vec2 v_TextureCoordinate;

void main () {
    vec4 color = texture2D(videoTexture, v_TextureCoordinate);
    gl_FragColor = color;
}