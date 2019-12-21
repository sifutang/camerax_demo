package com.example.cameraxdemo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.TextureView
import android.widget.TextView
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxdemo.util.SerialExecutor
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CAMERA = 1
    }

    private val mTextureView:TextureView by lazy {
        findViewById<TextureView>(R.id.textureView)
    }

    private val mIdSwitcher:TextView by lazy {
        findViewById<TextView>(R.id.camera_id_switcher)
    }

    private val mCaptureTextView:TextView by lazy {
        findViewById<TextView>(R.id.camera_capture_item)
    }

    private var mRender: Render? = null
    private var mPreview: Preview? = null
    private var mImageAnalysis: ImageAnalysis? = null
    private var mImageCapture: ImageCapture? = null
    private var mLensFacing = CameraX.LensFacing.FRONT

    private val mHandlerThread: HandlerThread
        get() {
            val workThread = HandlerThread("work-thread")
            workThread.start()
            return workThread
        }

    private val mExecutor: SerialExecutor
        get() {
            val analysisHandler = Handler(mHandlerThread.looper)
            return SerialExecutor(analysisHandler)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initUI()
    }

    private fun initUI() {
        mIdSwitcher.setOnClickListener {
            mIdSwitcher.isEnabled = false
            mLensFacing = if (CameraX.LensFacing.FRONT == mLensFacing) {
                CameraX.LensFacing.BACK
            } else {
                CameraX.LensFacing.FRONT
            }

            CameraX.unbindAll()
            startCamera()
        }

        mCaptureTextView.setOnClickListener {
            val file = File("${Environment.getExternalStorageDirectory().absoluteFile}/camerax/test.jpg")
            if (file.exists()) {
                file.delete()
            }

            mImageCapture?.takePicture(file,
                mExecutor,
                object : ImageCapture.OnImageSavedListener {
                    override fun onImageSaved(file: File) {
                        Log.d(TAG, "onImageSaved: ${file.path}")
                    }

                    override fun onError(
                        imageCaptureError: ImageCapture.ImageCaptureError,
                        message: String,
                        cause: Throwable?
                    ) {
                        Log.d(TAG, "onError: $message")
                        cause?.printStackTrace()
                    }
                })
        }
    }

    override fun onResume() {
        super.onResume()

        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CAMERA)
        } else {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        mRender?.pause()
        CameraX.unbindAll()
    }

    override fun onDestroy() {
        mHandlerThread.quitSafely()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA && grantResults.size == 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "onRequestPermissionsResult: permission camera PERMISSION_GRANTED")
            if (mTextureView.isAvailable) {
                Log.e(TAG, "onRequestPermissionsResult: ")
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        Log.i(TAG, "startCamera: ")
        mPreview = createPreviewCase()
        bindPreviewOutputUpdateListener(mPreview)
        mImageCapture = createImageCaptureCase()
        mImageAnalysis = createImageAnalysisCase()
        CameraX.bindToLifecycle(this, mPreview, mImageCapture, mImageAnalysis)
    }

    private fun createRender(surfaceTexture: SurfaceTexture) {
        if (mRender == null) {
            mRender = Render()
            mRender?.init(applicationContext, mTextureView)
            Log.d(TAG, "create render.")
        }

        mRender?.resume(surfaceTexture)
    }

    private fun createPreviewCase():Preview {
        val previewConfig = PreviewConfig.Builder()
            .setLensFacing(mLensFacing)
            .build()
        return Preview(previewConfig)
    }

    private fun createImageAnalysisCase():ImageAnalysis {
        val imageAnalysisConfig = ImageAnalysisConfig.Builder()
            .setLensFacing(mLensFacing)
            .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            .build()
        val imageAnalysis = ImageAnalysis(imageAnalysisConfig)
        val analysis = ImageAnalysis.Analyzer { image, rotationDegrees ->
//            Log.d(TAG, "analyze: ${image?.timestamp}, rotationDegrees: $rotationDegrees")
        }

        imageAnalysis.setAnalyzer(mExecutor, analysis)
        return imageAnalysis
    }

    private fun createImageCaptureCase():ImageCapture {
        val imageCaptureBuildConfig = ImageCaptureConfig.Builder()
            .setLensFacing(mLensFacing)
            .setTargetRotation(windowManager.defaultDisplay.rotation)
            .build()
        return ImageCapture(imageCaptureBuildConfig)
    }

    private fun bindPreviewOutputUpdateListener(preview: Preview?) {
        preview?.setOnPreviewOutputUpdateListener(Preview.OnPreviewOutputUpdateListener {
            //            mTextureView.surfaceTexture = it.surfaceTexture
            Log.d(TAG, "setOnPreviewOutputUpdateListener mTextureView.isAvailable = ${mTextureView.isAvailable}")
            if (mTextureView.isAvailable) {
                createRender(it.surfaceTexture)
                mIdSwitcher.isEnabled = true
            } else{
                mTextureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
                        Log.d(TAG, "onSurfaceTextureSizeChanged: width = $width, height = $height")
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                        Log.d(TAG, "onSurfaceTextureDestroyed: texture release...")
                        mRender?.destroy()
                        return true
                    }

                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                        Log.d(TAG, "onSurfaceTextureAvailable: texture available")
                        createRender(it.surfaceTexture)
                        mIdSwitcher.isEnabled = true
                    }
                }
            }
        })
    }
}
