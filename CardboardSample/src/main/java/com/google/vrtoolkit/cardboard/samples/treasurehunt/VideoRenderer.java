package com.google.vrtoolkit.cardboard.samples.treasurehunt;

import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import com.ofemobile.samples.stereovideovr.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Matthew Wellings on 1/18/16.
 */
public class VideoRenderer implements SurfaceTexture.OnFrameAvailableListener {

    private static float virtualScreenVetrexCoords[] = {
            -1,  1, 0,
            -1, -1, 0,
            1, -1, 0,
            1,  1, 0 };
    private static short virtualScreenVetrexIndicies[] = {
            0, 1, 2,
            0, 2, 3};

    //Non Stereo
    private float videoTextureCoords[] = {
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f};
    //Top Image
    private float videoTextureCoordsTop[] = {
            0.0f, 0.5f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.5f, 0.0f, 1.0f};
            //Botom Image
    private float videoTextureCoordsBottom[] = {
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.5f, 0.0f, 1.0f,
            1.0f, 0.5f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f,};

    private int shaderProgram;
//    private int textureParam;
    private int textureCoordsParam;
    private int vertexCoordsParam;
    private int textureTranformParam;
    private int MVPParam;

    private FloatBuffer textureCoordsBuffer[] = new FloatBuffer[3];
    private FloatBuffer screenVetrexCoordsBuffer;
    private ShortBuffer screenVetrexIndiciesBuffer;

    private SurfaceTexture VideoSurfaceTexture;
    private int videoTextureID;
    private float[] videoTextureTransform = new float[16];
    private float[] MVPMatrix;

    MainActivity parentMainActivity;
    MediaPlayer mMediaPlayer;

    private int imageArangement;

    private static final String TAG = "3DVideoRenderer";

    public VideoRenderer(MainActivity parentMainActivity)
    {
        this.parentMainActivity=parentMainActivity;
    }

    public boolean setup()
    {
        //Load shaders for video:
        int videoVertexShader = parentMainActivity.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.video_vertex);
        int videoFragmentShader = parentMainActivity.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.video_fragment);
        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, videoVertexShader);
        GLES20.glAttachShader(shaderProgram, videoFragmentShader);
        GLES20.glLinkProgram(shaderProgram);
        GLES20.glUseProgram(shaderProgram);

        textureCoordsParam = GLES20.glGetAttribLocation(shaderProgram, "a_TextureCoordinate");
        vertexCoordsParam = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        textureTranformParam = GLES20.glGetUniformLocation(shaderProgram, "u_TextureTransform");
        MVPParam = GLES20.glGetUniformLocation(shaderProgram, "u_MVP");
        parentMainActivity.checkGLError("VideoRenderer set up shaders");

        //Setup the attribute arrays:
        ByteBuffer bbVTC = ByteBuffer.allocateDirect(videoTextureCoords.length * 4);
        bbVTC.order(ByteOrder.nativeOrder());
        textureCoordsBuffer[0] = bbVTC.asFloatBuffer();
        textureCoordsBuffer[0].put(videoTextureCoords);
        textureCoordsBuffer[0].position(0);

        ByteBuffer bbVTCT = ByteBuffer.allocateDirect(videoTextureCoordsTop.length * 4);
        bbVTCT.order(ByteOrder.nativeOrder());
        textureCoordsBuffer[2] = bbVTCT.asFloatBuffer();
        textureCoordsBuffer[2] .put(videoTextureCoordsTop);
        textureCoordsBuffer[2] .position(0);

        ByteBuffer bbVTCB = ByteBuffer.allocateDirect(videoTextureCoordsBottom.length * 4);
        bbVTCB.order(ByteOrder.nativeOrder());
        textureCoordsBuffer[1]  = bbVTCB.asFloatBuffer();
        textureCoordsBuffer[1] .put(videoTextureCoordsBottom);
        textureCoordsBuffer[1] .position(0);

        ByteBuffer bbSVCB = ByteBuffer.allocateDirect(virtualScreenVetrexCoords.length * 4);
        bbSVCB.order(ByteOrder.nativeOrder());
        screenVetrexCoordsBuffer = bbSVCB.asFloatBuffer();
        screenVetrexCoordsBuffer.put(virtualScreenVetrexCoords);
        screenVetrexCoordsBuffer.position(0);

        ByteBuffer bbSVIB = ByteBuffer.allocateDirect(virtualScreenVetrexIndicies.length * 2);
        bbSVIB.order(ByteOrder.nativeOrder());
        screenVetrexIndiciesBuffer = bbSVIB.asShortBuffer();
        screenVetrexIndiciesBuffer.put(virtualScreenVetrexIndicies);
        screenVetrexIndiciesBuffer.position(0);

        //Setup the video texture
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        videoTextureID = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTextureID);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        parentMainActivity.checkGLError("VideoRenderer set up texture");

        VideoSurfaceTexture = new SurfaceTexture(videoTextureID);
        VideoSurfaceTexture.setOnFrameAvailableListener(this);

        mMediaPlayer = new MediaPlayer();
        try
        {
            AssetFileDescriptor afd = parentMainActivity.getAssets().openFd("bbb.mp4");
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mMediaPlayer.setSurface(new Surface(VideoSurfaceTexture));
            mMediaPlayer.setLooping(true);
            mMediaPlayer.prepare();
        }
        catch (IOException e){
            throw new RuntimeException("Error opening video file");}

        Log.e(TAG, "Setup OK");
        return true;
    }

    /**
     * Render non-stereo video
     */
    public void render()
    {
        render(0);
    }

    /**
     * Render stereo video for one eye.
     * @param  eye  The eye to render for (must 1 or 2).
     */
    public void render(int eye)
    {
        VideoSurfaceTexture.updateTexImage();
        VideoSurfaceTexture.getTransformMatrix(videoTextureTransform);

        GLES20.glUseProgram(shaderProgram);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTextureID);

        GLES20.glUniformMatrix4fv(textureTranformParam, 1, false, videoTextureTransform, 0);
        GLES20.glUniformMatrix4fv(MVPParam, 1, false, MVPMatrix, 0);

        GLES20.glEnableVertexAttribArray(vertexCoordsParam);
        GLES20.glVertexAttribPointer(vertexCoordsParam, 3, GLES20.GL_FLOAT, false, 0, screenVetrexCoordsBuffer);

        GLES20.glEnableVertexAttribArray(textureCoordsParam);
        GLES20.glVertexAttribPointer(textureCoordsParam, 4, GLES20.GL_FLOAT, false, 0, textureCoordsBuffer[eye]);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, virtualScreenVetrexIndicies.length, GLES20.GL_UNSIGNED_SHORT, screenVetrexIndiciesBuffer);

        parentMainActivity.checkGLError("render");
    }

    public void setMVPMatrix(float[] MVPMatrix){
        this.MVPMatrix=MVPMatrix;
    }

    public void pause()
    {
        if (mMediaPlayer!=null)
            mMediaPlayer.pause();
    }

    public void start()
    {
        if (mMediaPlayer!=null)
            mMediaPlayer.start();
    }

    public void cleanup() {
        if (mMediaPlayer!=null)
            mMediaPlayer.release();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

    }
}
