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
            // this is not prefect way to control camera flash light cos different device got different intent key.
            // better way is using thrid party lib like cameraX, they provide api to control flash mode
            // flash mode toggle is not tested.
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (isFlashOn) {
                cameraIntent.putExtra("android.intent.extras.FLASH_MODE", "torch")
                cameraIntent.putExtra("xiaomi.intent.extra.FLASH_MODE", "torch")
                cameraIntent.putExtra("SamsungCameraAppKey_FLASH_MODE", "torch")
                Toast.makeText(currentActivity, "flash mode on", Toast.LENGTH_SHORT).show()
            } else {
                cameraIntent.putExtra("android.intent.extras.FLASH_MODE", "off")
                cameraIntent.putExtra("xiaomi.intent.extra.FLASH_MODE", "off")
                cameraIntent.putExtra("SamsungCameraAppKey_FLASH_MODE", "off")
                Toast.makeText(currentActivity, "flash mode off", Toast.LENGTH_SHORT).show()
            }
            currentActivity.startActivityForResult(cameraIntent, CAMERA_REQUEST)
        }
    }

    @ReactMethod
    fun toggleFlash() {
        isFlashOn = !isFlashOn
    }

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            val photo = data?.extras?.get("data") as Bitmap
            val byteArrayOutputStream = ByteArrayOutputStream()
            photo.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
            cameraPromise?.resolve(encodedImage)
        } else {
            cameraPromise?.reject("Camera error try to run on physical device")
        }
    }

    override fun onNewIntent(intent: Intent?) {}
}