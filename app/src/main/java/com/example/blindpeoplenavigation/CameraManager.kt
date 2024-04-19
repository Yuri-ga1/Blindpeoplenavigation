package com.example.blindpeoplenavigation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.Toast
import com.example.blindpeoplenavigation.ml.Yolo
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class CameraManager(private val context: Context,
                    private val textureView: TextureView,
                    private val imageView: ImageView) {

    private lateinit var model: Yolo

    private lateinit var labels: List<String>
    val paint = Paint()
    var colors = listOf<Int>(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
        Color.BLACK, Color.WHITE, Color.CYAN
    )

    private lateinit var imageProcessor: ImageProcessor
    private lateinit var bitmap: Bitmap
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraManager: CameraManager
    private lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread

    fun initialize() {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
        labels = FileUtil.loadLabels(context, "label.txt")

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                val camera = getCameraId()
                if (camera != null) {
                    openCamera(camera)
                } else {
                    Toast.makeText(context, "Ошибка: основная камера не найдена", Toast.LENGTH_LONG).show()
                }
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                bitmap = textureView.bitmap!!

                model = Yolo.newInstance(context)

                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)

                drawTriangleWithLabel(bitmap, image)
            }
        }
    }

    private fun drawTriangleWithLabel(bitmap: Bitmap, image: TensorImage){
        /*
            Получает на вход bitmap и image.
            Обводит треугольниками объекты определенные нейронкой и подписывает их.
            Названия классов объектов находятся в переменной labels
        */
        val outputs = model.process(image)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray
//                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

        var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)

        val h = mutable.height
        val w = mutable.width

        paint.textSize=h/20f
        paint.strokeWidth = h/100f

        // Для каждого опознаного объекта рисуем прямоугольник и подписываем его
        var x: Int
        scores.forEachIndexed{ index, fl ->
            x = index * 4
            // Если fl - уверенность
            if (fl >0.5){
                paint.setColor(colors.get(index))
                paint.style = Paint.Style.STROKE
                canvas.drawRect(RectF(
                    locations.get(x+1)*w,
                    locations.get(x)*h,
                    locations.get(x+3)*w,
                    locations.get(x+2)*h), paint
                )
                paint.style = Paint.Style.FILL
                canvas.drawText(
                    labels.get(classes.get(index).toInt()),
                    locations.get(x+1)*w,
                    locations.get(x)*h,
                    paint
                )
            }
        }

        imageView.setImageBitmap(mutable)
    }

    fun closeCamera() {
        /*
        Функция закрытия камеры
         */
        if (::cameraDevice.isInitialized) {
            cameraDevice.close()
            model.close()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(cameraId: String) {
        /*
        Функция открытия камеры.
        Получиет во внутрь Айди камеры которую используем
         */
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera

                val surfaceTexture = textureView.surfaceTexture
                val surface = Surface(surfaceTexture)

                val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.setRepeatingRequest(captureRequest.build(), null, null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, handler)
            }

            override fun onDisconnected(camera: CameraDevice) {}

            override fun onError(camera: CameraDevice, error: Int) {}
        }, handler)
    }

    fun saveCameraState(outState: Bundle) {
        if (::cameraDevice.isInitialized) {
            outState.putString("cameraId", cameraDevice.id)
        }
    }

    fun restoreCameraState(savedInstanceState: Bundle) {
        if (savedInstanceState.containsKey("cameraId")) {
            val cameraId = savedInstanceState.getString("cameraId", null)
            if (cameraId != null) {
                openCamera(cameraId)
            }
        }
    }

    private fun getCameraId(): String? {
        /*
        Получаем Айди основной камеры и возвращаем его.
        В Случае, если не найдена, выдаем null.
         */
        val cameraIds = cameraManager.cameraIdList
        for (cameraId in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId
            }
        }
        return null
    }
}