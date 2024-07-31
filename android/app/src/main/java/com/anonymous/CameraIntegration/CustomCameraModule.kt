package com.anonymous.CameraIntegration

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Base64
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import java.io.ByteArrayOutputStream
import android.content.pm.PackageManager
import android.widget.Toast
import android.util.Log
import android.net.Uri

@ReactModule(name = CustomCameraModule.NAME)
class CustomCameraModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), ActivityEventListener {

    companion object {
        const val NAME = "CustomCamera"
        private const val CAMERA_REQUEST = 1888
        private const val PERMISSION_REQUEST_CODE = 200
    }

    private var cameraPromise: Promise? = null
    private var isFlashOn = false

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun getName(): String {
        return NAME
    }

    @ReactMethod
    fun openCamera(promise: Promise) {
        val currentActivity = currentActivity
        if (currentActivity == null) {
            promise.reject("Activity doesn't exist")
            return
        }

        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(currentActivity, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, launch camera
            launchCamera()
            cameraPromise = promise
        } else {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(currentActivity, arrayOf(android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
            cameraPromise = promise
        }
    }

    private fun launchCamera() {
        val currentActivity = currentActivity
        if (currentActivity != null) {
            val intent = Intent(currentActivity, CameraActivity::class.java)
            intent.putExtra("isFlashOn", isFlashOn)
            currentActivity.startActivityForResult(intent, CAMERA_REQUEST)
        }
    }

    @ReactMethod
    fun toggleFlash() {
        isFlashOn = !isFlashOn
    }

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Log.d("CameraModule", "receive data")
            val uriString = data?.getStringExtra("imageUri")
            if (uriString != null && currentActivity != null) {
                var savedUri = Uri.parse(uriString)
                val bitmap = MediaStore.Images.Media.getBitmap(currentActivity!!.contentResolver, savedUri)
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
                cameraPromise?.resolve(encodedImage)
            } else {
                cameraPromise?.reject("uri is empty")
            }
        } else if (requestCode != CAMERA_REQUEST) {
            cameraPromise?.reject("request code not camera")
        }
        else if (resultCode != Activity.RESULT_OK) {
            cameraPromise?.reject("Result not ok")
        }
        else {
            cameraPromise?.reject("Camera error try to run on physical device")
        }
    }

    override fun onNewIntent(intent: Intent?) {}
}