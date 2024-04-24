package com.example.blindpeoplenavigation.imageanalyzer

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.blindpeoplenavigation.ml.Yolo
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage

class ImageAnalyzer(
    private val model: Yolo,
    private val imageProcessor: ImageProcessor,
    private val labels: List<String>,
    private val onResult: (List<DetectedObject>) -> Unit
): ImageAnalysis.Analyzer{

    override fun analyze(image: ImageProxy) {
        val bitmap: Bitmap = image.toBitmap()

        var tensorImage = TensorImage.fromBitmap(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        val outputs = model.process(tensorImage)

        // Обработка результатов
        val detectedObjects = processOutputs(outputs, bitmap)

        // Передача результатов через callback
        onResult(detectedObjects)

        image.close()
    }

    private fun processOutputs(outputs: Yolo.Outputs, bitmap: Bitmap): List<DetectedObject> {
        // Здесь можете выполнить обработку выходных данных модели YOLO и создать список обнаруженных объектов
        // Например:
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray

        var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val h = mutable.height
        val w = mutable.width

        // Создаем список обнаруженных объектов
        var x: Int
        val detectedObjects = mutableListOf<DetectedObject>()
        scores.forEachIndexed { index, fl ->
            x = index * 4

            val objectLocation = ObjectLocation(
                locations.get(x+1)*w,
                locations.get(x)*h,
                locations.get(x+3)*w,
                locations.get(x+2)*h
            )

            val detectedObject = DetectedObject(
                objectLocation,
                labels.get(classes.get(index).toInt()),
                fl
            )
            detectedObjects.add(detectedObject)
        }

        return detectedObjects
    }
}