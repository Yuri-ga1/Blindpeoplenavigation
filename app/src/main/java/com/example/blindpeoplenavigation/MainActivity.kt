package com.example.blindpeoplenavigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.TextureView
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView

    private lateinit var textureView: TextureView
    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)

        textureView = findViewById(R.id.textureView)
        cameraManager = CameraManager(this, textureView, imageView)

        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this, CAMERA_PERMITION, CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            cameraManager.initialize()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasRequiredPermissions()) {
            val savedInstanceState = Bundle()
            onRestoreInstanceState(savedInstanceState)
        } else {
            ActivityCompat.requestPermissions(
                this, CAMERA_PERMITION, CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onPause() {
        super.onPause()
        val outState = Bundle()
        onSaveInstanceState(outState)
        cameraManager.closeCamera()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        cameraManager.saveCameraState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        cameraManager.restoreCameraState(savedInstanceState)
    }
    private fun hasRequiredPermissions(): Boolean{
        /*
        Функция проверки.
        Возвращает true, если мы получили все разрешения.
         */
        return CAMERA_PERMITION.all{
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        /*
        Автоматическая функция.
        Срабатывает, поле того, как пользователь выбрал как использовать разрешения.
         */
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (hasRequiredPermissions()) {
                cameraManager.initialize()
            } else {
                ActivityCompat.requestPermissions(
                    this, CAMERA_PERMITION, CAMERA_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    companion object{
        private  val CAMERA_PERMITION = arrayOf(
            Manifest.permission.CAMERA,
        )
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }
}