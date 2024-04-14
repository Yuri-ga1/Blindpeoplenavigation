package com.example.blindpeoplenavigation

import android.os.Bundle
import android.view.TextureView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
class MainActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.textureView)
        cameraManager = CameraManager(this, textureView)
        cameraManager.initialize()
        cameraManager.getPermission()
    }
}