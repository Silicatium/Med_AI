package com.example.medai

import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


class MainActivity : AppCompatActivity() {
    private lateinit var tflite : Interpreter
    private lateinit var tflitemodel : ByteBuffer
    private lateinit var text_result : TextView
    private val classLabels = arrayOf("Атопический дерматит", "Базалиома", "Кератома", "Экзема",
        "Родинка", "Меланома", "Здоровая кожа", "Псориаз", "Себорейная кератоз",
        "Грибковая инфекция", "Моллюск")
    private lateinit var image_view : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        text_result = findViewById(R.id.textView_result)

        tflitemodel = loadModelFile(this.assets, "model.tflite")
        tflite = Interpreter(tflitemodel)

        val button_load = findViewById<Button>(R.id.button_load)
        button_load.setOnClickListener {
            imageLoader()
        }

        val button_analysis = findViewById<Button>(R.id.button_analysis)
        button_analysis.setOnClickListener {
            try {
                modelWork()
            } catch (_:Exception) {
                text_result.text = "Отсутсвует изображение"
            }
        }
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun imageLoader() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startForResult.launch(intent)
    }


    private val startForResult = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data != null) {
                val selectedImageUri = data.data
                image_view = findViewById(R.id.imageView)
                image_view.setImageURI(selectedImageUri)
            }
        }
    }

    private fun modelWork() {
        val imageView = findViewById<ImageView>(R.id.imageView)
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap
        val resized_bitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, true)
        val pixelData = FloatArray(resized_bitmap.width * resized_bitmap.height * 3)
        val pixels = IntArray(resized_bitmap.width * resized_bitmap.height)
        resized_bitmap.getPixels(pixels, 0, resized_bitmap.width, 0, 0, resized_bitmap.width, resized_bitmap.height)
        var index = 0
        for (i in 0 until pixels.size) {
            pixelData[index++] = Color.red(pixels[i]) / 255f // Нормализация R
            pixelData[index++] = Color.green(pixels[i]) / 255f // Нормализация G
            pixelData[index++] = Color.blue(pixels[i]) / 255f // Нормализация B
        }

        val byteBuffer = ByteBuffer.allocateDirect(resized_bitmap.byteCount)
        byteBuffer.order(ByteOrder.nativeOrder())
        resized_bitmap.copyPixelsToBuffer(byteBuffer)
        byteBuffer.rewind()

        val inputBuffer: TensorBuffer =
            TensorBuffer.createFixedSize(tflite.getInputTensor(0).shape(), DataType.FLOAT32)
        inputBuffer.loadArray(pixelData)
        val outputBuffer =
            TensorBuffer.createFixedSize(tflite.getOutputTensor(0).shape(), DataType.FLOAT32)

        tflite.run(inputBuffer.buffer, outputBuffer.buffer)
        val output = outputBuffer.floatArray
        text_result.text = classLabels[output.indexOfFirst{ it == output.maxOrNull() }]
    }
}