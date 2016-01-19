precision mediump float;

attribute vec4 a_Position;
attribute vec4 a_TextureCoordinate;

uniform mat4 u_TextureTransform;
uniform mat4 u_MVP;

varying vec2 v_TextureCoordinate;

void main() {
   v_TextureCoordinate = (u_TextureTransform * a_TextureCoordinate).xy;
   gl_Position = u_MVP * a_Position;
}