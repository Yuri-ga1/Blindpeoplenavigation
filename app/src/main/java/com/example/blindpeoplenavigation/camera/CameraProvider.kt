package com.example.blindpeoplenavigation.camera

import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.coroutines.flow.StateFlow

interface CameraProvider {
    val cameraProvider: StateFlow<ProcessCameraProvider?>
}