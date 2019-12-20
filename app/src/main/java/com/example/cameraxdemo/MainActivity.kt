package com.example.cameraxdemo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.TextureView
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxdemo.util.SerialExecutor

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CAMERA = 1
    }

    private val mTextureView:TextureView by lazy {
        findViewById<TextureView>(R.id.textureView)
    }

    private var mRender: Render? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        } else {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        mRender?.pause()
        CameraX.unbindAll()
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
        val preview = createPreviewCase()
        preview.setOnPreviewOutputUpdateListener(Preview.OnPreviewOutputUpdateListener {
//            mTextureView.surfaceTexture = it.surfaceTexture
            Log.d(TAG, "setOnPreviewOutputUpdateListener mTextureView.isAvailable = ${mTextureView.isAvailable}")
            if (mTextureView.isAvailable) {
                createRender(it.surfaceTexture)
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
                    }
                }
            }
        })

        val imageAnalysis = createImageAnalysisCase()
        val imageCapture = createImageCaptureCase()
        CameraX.bindToLifecycle(this, preview, imageCapture, imageAnalysis)
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
            .setLensFacing(CameraX.LensFacing.FRONT)
            .build()
        return Preview(previewConfig)
    }

    private fun createImageAnalysisCase():ImageAnalysis {
        val imageAnalysisConfig = ImageAnalysisConfig.Builder()
            .setLensFacing(CameraX.LensFacing.FRONT)
            .setTargetResolution(Size(1280, 720))
            .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            .build()
        val imageAnalysis = ImageAnalysis(imageAnalysisConfig)
        val analysis = ImageAnalysis.Analyzer { image, rotationDegrees ->
//            Log.d(TAG, "analyze: ${image?.timestamp}, rotationDegrees: $rotationDegrees")
        }

        val analysisThread = HandlerThread("image-analysis-thread")
        analysisThread.start()
        val analysisHandler = Handler(analysisThread.looper)
        val executor = SerialExecutor(analysisHandler)
        imageAnalysis.setAnalyzer(executor, analysis)
        return imageAnalysis
    }

    private fun createImageCaptureCase():ImageCapture {
        val imageCaptureBuildConfig = ImageCaptureConfig.Builder()
            .setLensFacing(CameraX.LensFacing.FRONT)
            .setTargetRotation(windowManager.defaultDisplay.rotation)
            .build()
        return ImageCapture(imageCaptureBuildConfig)
    }
}
