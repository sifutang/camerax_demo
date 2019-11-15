package com.example.cameraxdemo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxdemo.util.TextResourceReader

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CAMERA = 1
    }

    private val mTextureView:TextureView by lazy {
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

        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        } else {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
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
        val previewConfig = PreviewConfig.Builder()
            .setLensFacing(CameraX.LensFacing.FRONT)
            .build()
        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener(Preview.OnPreviewOutputUpdateListener {
            //            textureView.surfaceTexture = it.surfaceTexture
            Log.d(TAG, "setOnPreviewOutputUpdateListener mTextureView.isAvailable = ${mTextureView.isAvailable}")
            if (mTextureView.isAvailable) {
                startGlContext(it.surfaceTexture)
            } else{
                mTextureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
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
                        startGlContext(it.surfaceTexture)
                    }
                }
            }
        })
        CameraX.bindToLifecycle(this, preview)
    }

    private fun startGlContext(surfaceTexture: SurfaceTexture) {
        if (mOESTextureId == -1) {
            mOESTextureId = TextureDrawer.createOESTextureObject()
            mRender.init(applicationContext, mTextureView, mOESTextureId)
            mRender.initOESTexture(surfaceTexture)
        }
    }
}
