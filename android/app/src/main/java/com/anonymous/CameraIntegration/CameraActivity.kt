package com.anonymous.CameraIntegration

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.ContentValues
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class CameraActivity : AppCompatActivity() {

    companion object {
        const val CAMERA_RESULT_CODE = 1889
    }

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private var isFlashOn = false
    private val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a frame layout to hold the camera preview and buttons
        val frameLayout = FrameLayout(this)
        frameLayout.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        setContentView(frameLayout)

        // Create PreviewView for camera preview
        val previewView = PreviewView(this)
        previewView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        frameLayout.addView(previewView)

        // Create capture button
        val captureButton = Button(this)
        captureButton.text = "Capture"
        val captureButtonParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        captureButtonParams.bottomMargin = 50
        captureButtonParams.leftMargin = 50
        captureButtonParams.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
        captureButton.layoutParams = captureButtonParams
        frameLayout.addView(captureButton)

        // Get the flash state from the intent
        isFlashOn = intent.getBooleanExtra("isFlashOn", false)

        startCamera(previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Set capture button listener
        captureButton.setOnClickListener {
            takePhoto()
        }
    }

    private fun startCamera(previewView: PreviewView) {
       // requestPermissions()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build()
            imageCapture = ImageCapture.Builder()
                .setFlashMode(if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                preview.setSurfaceProvider(previewView.surfaceProvider)
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraIntegration")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                Toast.makeText(this@CameraActivity, "Image capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri
                if (savedUri != null) {
                    try {
                        val resultIntent = Intent().apply {
                            putExtra("imageUri", savedUri.toString())
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@CameraActivity, "Failed to encode image", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_CANCELED)
                    }
                } else {
                    Toast.makeText(this@CameraActivity, "Image URI is null", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_CANCELED)
                }
                finish()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}