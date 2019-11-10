package com.example.cameraxdemo

import android.graphics.SurfaceTexture
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private val textureView:TextureView by lazy {
        findViewById<TextureView>(R.id.textureView)
    }

    private var mOESTextureId = -1
    private var mRender: Render = Render()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        val previewConfig = PreviewConfig.Builder()
                .setLensFacing(CameraX.LensFacing.FRONT)
                .build()
        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener(Preview.OnPreviewOutputUpdateListener {
//            textureView.surfaceTexture = it.surfaceTexture


            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
                    Log.d(TAG, "onSurfaceTextureSizeChanged: width = $width, height = $height")
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                    Log.d(TAG, "onSurfaceTextureDestroyed: texture release...")
                    return true
                }

                override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                    Log.d(TAG, "onSurfaceTextureAvailable: texture available")
                    mOESTextureId = TextureDrawer.createOESTextureObject()
                    mRender.init(textureView, mOESTextureId)
                    mRender.initOESTexture(it.surfaceTexture)
                }
            }


            // Compute the center of preview (TextureView)
            val centerX = textureView.width.toFloat().div(2)
            val centerY = textureView.height.toFloat().div(2)

            // Correct preview output to account for display rotation
            val rotationDegrees = when (textureView.display?.rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> return@OnPreviewOutputUpdateListener
            }

            val matrix = android.graphics.Matrix()
            matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

            // Finally, apply transformations to TextureView
            textureView.setTransform(matrix)
        })
        CameraX.bindToLifecycle(this, preview)
    }

    override fun onPause() {
        super.onPause()
        CameraX.unbindAll()
    }
}
