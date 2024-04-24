package com.example.blindpeoplenavigation

import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.coroutines.flow.StateFlow

interface CameraProvider {
    val cameraProvider: StateFlow<ProcessCameraProvider?>
}