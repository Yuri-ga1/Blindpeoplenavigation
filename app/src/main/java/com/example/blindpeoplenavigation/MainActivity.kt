package com.example.blindpeoplenavigation

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.blindpeoplenavigation.camera.CameraRecognitionCenter
import com.example.blindpeoplenavigation.databinding.ActivityMainBinding
import com.example.blindpeoplenavigation.imageanalyzer.ImageAnalyzer
import com.example.blindpeoplenavigation.imageanalyzer.ObjectLocation
import com.example.blindpeoplenavigation.ml.Yolo
import com.example.blindpeoplenavigation.texttospeech.TextToSpeechModule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // запрос разрешений
    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionGranted ->
        if (permissionGranted) {
            startCamera()
        }
    }

    //озвучивание текста
    private lateinit var textToSpeechModule: TextToSpeechModule

    private lateinit var imageAnalysis: ImageAnalysis

    //модель
    private lateinit var model: Yolo
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var labels: List<String>

    private lateinit var bindding: ActivityMainBinding
    private lateinit var cameraZone: PreviewView
    private lateinit var cameraCenter: Point


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bindding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindding.root)

        cameraZone = bindding.viewFinder
        cameraCenter = getCameraCenter()

        model = Yolo.newInstance(this)

        //ресайз картинки в нужный формат для нейронки
        imageProcessor = ImageProcessor.Builder().add(
            ResizeOp(
                300,
                300,
                ResizeOp.ResizeMethod.BILINEAR
            )
        ).build()

        labels = FileUtil.loadLabels(this, "label.txt")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageAnalysis = ImageAnalysis.Builder().build().apply {
            setAnalyzer(
                Executors.newSingleThreadExecutor(),
                ImageAnalyzer(
                    model = model,
                    imageProcessor = imageProcessor,
                    labels = labels,
                    onResult = {
                        it.forEach {
//                            Log.e("message", it.objectClass)
                            val objectLocation: ObjectLocation = it.location
                            val text = positionToCenter(objectLocation, it.objectClass)
                            textToSpeechModule.speakOut(text)
                        }
//                        runOnUiThread() -> отображение на экране
                    }
                )
            )
        }

        activityResultLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun positionToCenter(
        position: ObjectLocation,
        objectName: String
    ): String {
        val text = "Object $objectName is on "

        return when {
            position.top <= cameraCenter.y && position.bottom >= cameraCenter.y &&
                    position.left <= cameraCenter.x && position.right >= cameraCenter.x ->
                "$text center"
            position.top > cameraCenter.y && position.left < cameraCenter.x ->
                "$text top left"
            position.top > cameraCenter.y && position.right > cameraCenter.x ->
                "$text top right"
            position.bottom < cameraCenter.y && position.left < cameraCenter.x ->
                "$text bottom left"
            position.bottom < cameraCenter.y && position.right > cameraCenter.x ->
                "$text bottom right"
            position.top > cameraCenter.y -> "$text top"
            position.bottom < cameraCenter.y -> "$text bottom"
            position.left < cameraCenter.x -> "$text left"
            else -> "$text right"
        }
    }

    override fun onResume() {
        textToSpeechModule = TextToSpeechModule(this)
        super.onResume()
    }

    override fun onStop() {
        textToSpeechModule.shutdown()
        super.onStop()
    }

    override fun onDestroy() {
        model.close()
        super.onDestroy()
    }

    private fun startCamera() {
        val center = CameraRecognitionCenter(applicationContext)
        center.setupCamera(this)
        lifecycleScope.launch {
            center.cameraProvider
                .filterNotNull()
                .collectLatest {
                    val preview = Preview.Builder().build()
                    it.bindToLifecycle(
                        this@MainActivity,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )

                    preview.setSurfaceProvider(cameraZone.surfaceProvider)
                }
        }
    }

    private fun getCameraCenter(): Point{
        val x = cameraZone.width/2
        val y = cameraZone.height/2
        return Point(x, y)
    }

}