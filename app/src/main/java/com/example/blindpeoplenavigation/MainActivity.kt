package com.example.blindpeoplenavigation

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.blindpeoplenavigation.camera.CameraRecognitionCenter
import com.example.blindpeoplenavigation.databinding.ActivityMainBinding
import com.example.blindpeoplenavigation.imageanalyzer.DetectedItems
import com.example.blindpeoplenavigation.imageanalyzer.ImageAnalyzer
import com.example.blindpeoplenavigation.ml.Yolo
import com.example.blindpeoplenavigation.texttospeech.TextToSpeechModule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.util.concurrent.ExecutorService
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

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService

    //модель
    private lateinit var model: Yolo
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var labels: List<String>

    private lateinit var takePhotoBtn: ImageButton
    private lateinit var bindding: ActivityMainBinding
    private lateinit var cameraZone: PreviewView
    private lateinit var cameraCenter: Point


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bindding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindding.root)

        cameraZone = bindding.viewFinder
        cameraExecutor = Executors.newSingleThreadExecutor()

        takePhotoBtn = findViewById(R.id.takePhotoBtn)
        takePhotoBtn.setOnClickListener{
            takeFrame()
        }

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

        activityResultLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun takeFrame() {
        cameraExecutor.execute {
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val imageAnalyzer = ImageAnalyzer(
                            model,
                            imageProcessor,
                            labels,
                            cameraCenter
                        )
                        val result = imageAnalyzer.analyze(image)
                        voice(result)
                        image.close()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("TAG", "Photo capture failed: ${exception.message}")
                    }
                }
            )
        }
    }

    private fun voice(result: MutableList<DetectedItems>) {
        result.forEach {
            val text = "${it.count} ${it.objectName} is on ${it.position}"
//            Log.e("message", text)
            textToSpeechModule.speakOut(text)
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
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    it.bindToLifecycle(
                        this@MainActivity,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )

                    preview.setSurfaceProvider(cameraZone.surfaceProvider)
                }
        }
    }

    private fun getCameraCenter(): Point{
        val x: Int = cameraZone.width/2
        val y: Int = cameraZone.height/2
        return Point(x, y)
    }

}