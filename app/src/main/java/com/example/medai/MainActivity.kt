package com.example.medai

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        tflitemodel = loadModelFile(this.assets, "model.tflite")
        tflite = Interpreter(tflitemodel)
        modelWork()
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun modelWork() {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.normal)
        val resized_bitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, true)
        val byteBuffer = ByteBuffer.allocateDirect(resized_bitmap.byteCount)
        byteBuffer.order(ByteOrder.nativeOrder())
        resized_bitmap.copyPixelsToBuffer(byteBuffer)
        byteBuffer.rewind()

        val inputBuffer: TensorBuffer =
            TensorBuffer.createFixedSize(tflite.getInputTensor(0).shape(), DataType.FLOAT32)
        inputBuffer.loadBuffer(byteBuffer)
//        val outputBuffer =
//            TensorBuffer.createFixedSize(tflite.getOutputTensor(0).shape(), DataType.FLOAT32)
//
//        tflite.run(inputBuffer.buffer, outputBuffer.buffer)
//        val output = outputBuffer.floatArray
        //text_result.text = output.joinToString(", ") { it.toString() }
    }
}