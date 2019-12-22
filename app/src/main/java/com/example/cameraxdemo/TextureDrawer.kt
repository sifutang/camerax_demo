package com.example.cameraxdemo

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.example.cameraxdemo.util.ShaderHelper
import com.example.cameraxdemo.util.TextResourceReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class TextureDrawer(context: Context, OESTextureId: Int) {

    private val mBuffer: FloatBuffer?
    private var mOESTextureId = -1
    private var mShaderProgram = -1

    private var aPositionLocation = -1
    private var aTextureCoordLocation = -1
    private var uTextureMatrixLocation = -1
    private var uTextureSamplerLocation = -1

    init {
        mOESTextureId = OESTextureId
        mBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        mBuffer.put(vertexData, 0, vertexData.size).position(0)

        val vertexShader = TextResourceReader.readTextFileFromResource(context, R.raw.vertex)
        val fragmentShader = TextResourceReader.readTextFileFromResource(context, R.raw.fragment)
        mShaderProgram = ShaderHelper.buildProgram(vertexShader, fragmentShader)

        aPositionLocation = GLES20.glGetAttribLocation(mShaderProgram, POSITION_ATTRIBUTE)
        aTextureCoordLocation = GLES20.glGetAttribLocation(mShaderProgram, TEXTURE_COORD_ATTRIBUTE)
        uTextureMatrixLocation = GLES20.glGetUniformLocation(mShaderProgram, TEXTURE_MATRIX_UNIFORM)
        uTextureSamplerLocation = GLES20.glGetUniformLocation(mShaderProgram, TEXTURE_SAMPLER_UNIFORM)
    }

    fun drawTexture(transformMatrix: FloatArray) {
        GLES20.glUseProgram(mShaderProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mOESTextureId)
        GLES20.glUniform1i(uTextureSamplerLocation, 0)
        GLES20.glUniformMatrix4fv(uTextureMatrixLocation, 1, false, transformMatrix, 0)

        if (mBuffer != null) {
            mBuffer.position(0)
            GLES20.glEnableVertexAttribArray(aPositionLocation)
            GLES20.glVertexAttribPointer(aPositionLocation,
                2,
                GLES20.GL_FLOAT,
                false,
                16,
                mBuffer)

            mBuffer.position(2)
            GLES20.glEnableVertexAttribArray(aTextureCoordLocation)
            GLES20.glVertexAttribPointer(
                aTextureCoordLocation,
                2,
                GLES20.GL_FLOAT,
                false,
                16,
                mBuffer
            )

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }
    }


    companion object {
        private val vertexData = floatArrayOf(
            -1f, 1f, 0f, 1f,
            -1f, -1f, 0f, 0f,
            1f, 1f, 1f, 1f,
            1f, -1f, 1f, 0f
        )

        private const val POSITION_ATTRIBUTE = "aPosition"
        private const val TEXTURE_COORD_ATTRIBUTE = "aTextureCoordinate"
        private const val TEXTURE_MATRIX_UNIFORM = "uTextureMatrix"
        private const val TEXTURE_SAMPLER_UNIFORM = "uTextureSampler"
    }
}