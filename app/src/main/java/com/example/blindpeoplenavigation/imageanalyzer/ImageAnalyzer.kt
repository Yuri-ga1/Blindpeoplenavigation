package com.example.blindpeoplenavigation.imageanalyzer

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.example.blindpeoplenavigation.Point
import com.example.blindpeoplenavigation.imageanalyzer.EnumStateLocation
import com.example.blindpeoplenavigation.ml.Yolo
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import com.example.blindpeoplenavigation.imageanalyzer.EnumStateLocation as statePosition

class ImageAnalyzer(
    private val model: Yolo,
    private val imageProcessor: ImageProcessor,
    private val labels: List<String>,
    private val cameraCenter: Point
){

     fun analyze(image: ImageProxy): MutableList<DetectedItems> {
        val bitmap: Bitmap = image.toBitmap()

        var tensorImage = TensorImage.fromBitmap(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        val outputs = model.process(tensorImage)

        // Обработка результатов
        val detectedObjects = processOutputs(outputs, bitmap)

        var result = mutableListOf<DetectedItems>()

        detectedObjects.forEach {
            val pos: statePosition = positionToCenter(it.location)
            val objName: String = it.objectClass

            if (result.none { objName !in it.objectName}){
                val info = DetectedItems(
                    count = 1,
                    objectName = objName,
                    position = pos
                )
                result.add(info)
            } else{
                val existingInfo = result.find { it.objectName == objName && it.position == pos }
                if(existingInfo != null) {
                    existingInfo.count++
                } else{
                    val info = DetectedItems(
                        count = 1,
                        objectName = objName,
                        position = pos
                    )
                    result.add(info)
                }
            }
        }

        return result
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
            if (fl < 0.5)
                return@forEachIndexed

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

    private fun positionToCenter(
        position: ObjectLocation
    ): EnumStateLocation {

        return when {
            position.top <= cameraCenter.y && position.bottom >= cameraCenter.y &&
                    position.left <= cameraCenter.x && position.right >= cameraCenter.x ->
                statePosition.CENTER
            position.bottom > cameraCenter.y -> statePosition.TOP
            position.top < cameraCenter.y -> statePosition.BOTTOM
            position.right < cameraCenter.x -> statePosition.LEFT
            else -> statePosition.RIGHT
        }
    }
}