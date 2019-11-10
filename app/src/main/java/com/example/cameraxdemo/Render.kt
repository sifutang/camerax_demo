package com.example.cameraxdemo

import android.graphics.Point
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.view.TextureView
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLSurface

class Render : SurfaceTexture.OnFrameAvailableListener {

    private var mHandlerThread: HandlerThread? = null
    private var mHandler: Handler? = null
    private var mTextureView: TextureView? = null
    private var mOESTextureId: Int = 0
    private var mFilterEngine: TextureDrawer? = null
    private var mDataBuffer: FloatBuffer? = null
    private var mShaderProgram = -1
    private val transformMatrix = FloatArray(16)

    private var mEgl: EGL10? = null
    private var mEGLDisplay = EGL10.EGL_NO_DISPLAY
    private var mEGLContext = EGL10.EGL_NO_CONTEXT
    private val mEGLConfig = arrayOfNulls<EGLConfig>(1)
    private var mEglSurface: EGLSurface? = null
    private var mOESSurfaceTexture: SurfaceTexture? = null

    fun init(textureView: TextureView, oesTextureId: Int) {
        mTextureView = textureView
        getFiltersStartPoints(mTextureView!!.width, mTextureView!!.height)

        mOESTextureId = oesTextureId
        mHandlerThread = HandlerThread("Renderer Thread")
        mHandlerThread!!.start()
        mHandler = object : Handler(mHandlerThread!!.looper) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_INIT -> {
                        initEGL()
                        return
                    }
                    MSG_RENDER -> {
                        drawFrame()
                        return
                    }
                    MSG_INIT_SURFACE -> {
                        initSurfaceTexture()
                        return
                    }
                    MSG_DEINIT -> {
                        deInitSurfaceTexture()
                    }
                    else -> return
                }
            }
        }
        mHandler!!.sendEmptyMessage(MSG_INIT)
    }

    private fun deInitSurfaceTexture() {
        mOESSurfaceTexture?.detachFromGLContext()
    }

    private fun initSurfaceTexture() {
        mOESSurfaceTexture!!.attachToGLContext(mOESTextureId)
        mOESSurfaceTexture!!.setOnFrameAvailableListener(this)
    }

    private fun initEGL() {
        mEgl = EGLContext.getEGL() as EGL10

        //获取显示设备
        mEGLDisplay = mEgl!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (mEGLDisplay === EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("eglGetDisplay failed! " + mEgl!!.eglGetError())
        }

        //version中存放EGL版本号
        val version = IntArray(2)

        //初始化EGL
        if (!mEgl!!.eglInitialize(mEGLDisplay, version)) {
            throw RuntimeException("eglInitialize failed! " + mEgl!!.eglGetError())
        }

        //构造需要的配置列表
        val attributes = intArrayOf(EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_ALPHA_SIZE, 8, EGL10.EGL_BUFFER_SIZE, 32, EGL10.EGL_RENDERABLE_TYPE, 4, EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT, EGL10.EGL_NONE)
        val configsNum = IntArray(1)

        //EGL选择配置
        if (!mEgl!!.eglChooseConfig(mEGLDisplay, attributes, mEGLConfig, 1, configsNum)) {
            throw RuntimeException("eglChooseConfig failed! " + mEgl!!.eglGetError())
        }
        val surfaceTexture = mTextureView!!.surfaceTexture ?: return

        //创建EGL显示窗口
        mEglSurface = mEgl!!.eglCreateWindowSurface(mEGLDisplay, mEGLConfig[0], surfaceTexture, null)

        //创建上下文
        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
        mEGLContext = mEgl!!.eglCreateContext(mEGLDisplay, mEGLConfig[0], EGL10.EGL_NO_CONTEXT, contextAttribs)

        if (mEGLDisplay === EGL10.EGL_NO_DISPLAY || mEGLContext === EGL10.EGL_NO_CONTEXT) {
            throw RuntimeException("eglCreateContext fail failed! " + mEgl!!.eglGetError())
        }

        if (!mEgl!!.eglMakeCurrent(mEGLDisplay, mEglSurface, mEglSurface, mEGLContext)) {
            throw RuntimeException("eglMakeCurrent failed! " + mEgl!!.eglGetError())
        }

        mFilterEngine = TextureDrawer(mOESTextureId)
        mDataBuffer = mFilterEngine!!.buffer
        mShaderProgram = mFilterEngine!!.mShaderProgram
    }

    private fun drawFrame() {
        if (mOESSurfaceTexture != null) {
            mOESSurfaceTexture!!.updateTexImage()
            mOESSurfaceTexture!!.getTransformMatrix(transformMatrix)
        }
        mEgl!!.eglMakeCurrent(mEGLDisplay, mEglSurface, mEglSurface, mEGLContext)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(1f, 1f, 0f, 0f)
        for (i in 0 until 9) {
            GLES20.glViewport(
                mFiltersStartPoints!![i].x, mFiltersStartPoints!![i].y,
                mTextureView!!.width / 4, mTextureView!!.height / 4)
            mFilterEngine!!.drawTexture(transformMatrix)
        }
        mEgl!!.eglSwapBuffers(mEGLDisplay, mEglSurface)
    }

    private var mFiltersStartPoints:MutableList<Point>? = mutableListOf()
    private fun getFiltersStartPoints(width: Int, height: Int) {
        val offsetW = width / 24
        val offsetH = height / 24
        for (i in 0 until 9) {
            val index = i / 9
            val offX = offsetW + index * width + (i % 3) * width / 3
            val offY = offsetH + (2 - (i % 9) / 3) * height / 3
            mFiltersStartPoints?.add(Point(offX, offY))
        }

        Log.d(TAG, "getFiltersStartPoints: mFiltersStartPoints = ${mFiltersStartPoints.toString()}")
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        if (mHandler != null) {
            mHandler!!.sendEmptyMessage(MSG_RENDER)
        }
    }

    fun initOESTexture(st: SurfaceTexture) {
        mOESSurfaceTexture = st

        if (mHandler != null) {
            mHandler!!.sendEmptyMessage(MSG_INIT_SURFACE)
        }
    }

    companion object {
        private val MSG_INIT = 1
        private val MSG_RENDER = 2
        private val MSG_DEINIT = 3
        private val MSG_INIT_SURFACE = 4

        const val TAG = "Render"
    }
}
