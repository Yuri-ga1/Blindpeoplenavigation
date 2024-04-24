package com.example.blindpeoplenavigation.camera

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraRecognitionCenter(
    private val context: Context
) : CameraProvider {
    private val _cameraProvider = MutableStateFlow<ProcessCameraProvider?>(null)
    override val cameraProvider: StateFlow<ProcessCameraProvider?>
        get() = _cameraProvider.asStateFlow()

    fun setupCamera(lifecycleOwner: LifecycleOwner){
        lifecycleOwner.lifecycleScope.launch {
            val cameraProvider = withContext(Dispatchers.IO){
                ProcessCameraProvider
                    .getInstance(context)
                    .await()
            }
            _cameraProvider.emit(cameraProvider)
        }
    }
}