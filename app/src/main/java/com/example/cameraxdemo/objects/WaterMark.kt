package com.example.cameraxdemo.objects

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.example.cameraxdemo.R
import com.example.cameraxdemo.util.ShaderHelper
import com.example.cameraxdemo.util.TextResourceReader
import com.example.cameraxdemo.util.TextureHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class WaterMark(context: Context) {
    companion object {
        private val WATER_MARK_DATA = floatArrayOf(
            // x, y, s, t
            -1f, -1f, 0f, 1f,
             1f, -1f, 1f, 1f,
            -1f,  1f, 0f, 0f,
             1f,  1f, 1f, 0f
        )

        private const val TAG = "WaterMark"
        private const val POSITION_ATTRIBUTE = "aPosition"
        private const val COORDINATE = "aWaterMarkTextureCoordinate"
        private const val SAMPLER = "sWaterMarkSampler"
        private const val MVP_MATRIX = "uMVPMatrix"
    }

    private val mWaterMarkBuffer:FloatBuffer?
    private val mTextureId:Int?

    private var mMVPMatrix = Array(16) { 0.0f }.toFloatArray()
    private var mModelMatrix = Array(16) { 0.0f }.toFloatArray()
    private var mViewMatrix = Array(16) { 0.0f }.toFloatArray()
    private var mProjectionMatrix = Array(16) { 0.0f }.toFloatArray()

    private var mShaderProgram = -1
    private var mPositionLoc = -1;
    private var mSamplerLoc = -1;
    private var mMvpMatrixLoc = -1;
    private var mCoordinateLoc = -1;

    init {
        mWaterMarkBuffer = ByteBuffer.allocateDirect(WATER_MARK_DATA.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        mWaterMarkBuffer.put(WATER_MARK_DATA, 0, WATER_MARK_DATA.size).position(0)
        mTextureId = TextureHelper.loadTexture(context, R.drawable.watermark_logo)

        val vertexShader = TextResourceReader.readTextFileFromResource(context, R.raw.vertex_water_mark)
        val fragmentShader = TextResourceReader.readTextFileFromResource(context, R.raw.fragment_water_mark)
        mShaderProgram = ShaderHelper.buildProgram(vertexShader, fragmentShader)

        mPositionLoc = GLES20.glGetAttribLocation(mShaderProgram, POSITION_ATTRIBUTE)
        mCoordinateLoc = GLES20.glGetAttribLocation(mShaderProgram, COORDINATE)
        mSamplerLoc = GLES20.glGetUniformLocation(mShaderProgram, SAMPLER)
        mMvpMatrixLoc = GLES20.glGetUniformLocation(mShaderProgram, MVP_MATRIX)
    }

    fun renderSize(width: Int, height: Int) {
        Log.d(TAG, "renderSize: width = $width, height = $height")
        Matrix.setLookAtM(mViewMatrix, 0,
            0F, 0F, 7F,
            0F, 0F, 0F,
            0F, 1F, 0F)

        val ratio = 1F * width / height
        Matrix.frustumM(mProjectionMatrix, 0,
            -ratio, ratio, -1F, 1F, 3F, 7F)

        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, -0.8f, 2f, 0f)
        Matrix.scaleM(mModelMatrix, 0, 1 / 5f, 1 / 5f, 1.0f)

        val tmpMatrix = Array(16) {0.0f}.toFloatArray()
        Matrix.multiplyMM(tmpMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, tmpMatrix, 0)
    }

    fun drawSelf() {
        GLES20.glUseProgram(mShaderProgram)
        // blend
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId!!)
        GLES20.glUniform1i(mSamplerLoc, 0)
        // mvp matrix
        GLES20.glUniformMatrix4fv(mMvpMatrixLoc, 1, false, mMVPMatrix, 0)
        // position
        mWaterMarkBuffer?.position(0)
        GLES20.glEnableVertexAttribArray(mPositionLoc)
        GLES20.glVertexAttribPointer(mPositionLoc,
            2, GLES20.GL_FLOAT, false, 16, mWaterMarkBuffer)
        // texture coordinate.
        mWaterMarkBuffer?.position(2)
        GLES20.glEnableVertexAttribArray(mCoordinateLoc)
        GLES20.glVertexAttribPointer(mCoordinateLoc,
            2, GLES20.GL_FLOAT, false, 16, mWaterMarkBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(mPositionLoc)
        GLES20.glDisableVertexAttribArray(mCoordinateLoc)
        GLES20.glDisable(GLES20.GL_BLEND)
    }
}