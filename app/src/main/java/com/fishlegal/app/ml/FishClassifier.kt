package com.fishlegal.app.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File
import java.nio.MappedByteBuffer

/**
 * Wraps a bundled TFLite model for on-device fish species classification.
 * Model file lives in app's files dir so it can be replaced by sync updates
 * without needing an app update (assets/ is read-only at runtime).
 */
class FishClassifier(private val context: Context) {

    companion object {
        private const val MODEL_FILENAME = "fish_classifier.tflite"
        private const val INPUT_SIZE = 224
    }

    private var interpreter: Interpreter? = null

    fun loadModel() {
        val modelFile = resolveModelFile()
        val buffer: MappedByteBuffer = FileUtil.loadMappedFile(context, modelFile.absolutePath)
        interpreter = Interpreter(buffer)
    }

    /** Prefer a downloaded/updated model in filesDir; fall back to bundled asset. */
    private fun resolveModelFile(): File {
        val updated = File(context.filesDir, MODEL_FILENAME)
        if (updated.exists()) return updated
        // Copy bundled asset out so FileUtil can map it uniformly.
        val out = File(context.filesDir, MODEL_FILENAME)
        context.assets.open(MODEL_FILENAME).use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        return out
    }

    data class Prediction(val labelIndex: Int, val confidence: Float)

    fun classify(bitmap: Bitmap): Prediction {
        val interp = interpreter ?: throw IllegalStateException("Model not loaded")

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        // Output buffer sized to species count; replaced when model/labels update.
        val outputBuffer = Array(1) { FloatArray(LabelMap.size()) }
        interp.run(tensorImage.buffer, outputBuffer)

        val scores = outputBuffer[0]
        var bestIndex = 0
        var bestScore = scores[0]
        for (i in scores.indices) {
            if (scores[i] > bestScore) {
                bestScore = scores[i]
                bestIndex = i
            }
        }
        return Prediction(bestIndex, bestScore)
    }

    fun close() {
        interpreter?.close()
    }
}

/** Index <-> speciesId mapping, reloaded whenever a new labels file is synced. */
object LabelMap {
    private var labels: List<String> = emptyList()

    fun load(context: Context) {
        val file = File(context.filesDir, "labels.txt")
        labels = if (file.exists()) {
            file.readLines()
        } else {
            context.assets.open("labels.txt").bufferedReader().readLines()
        }
    }

    fun size(): Int = labels.size.coerceAtLeast(1)

    fun speciesIdForIndex(index: Int): String? = labels.getOrNull(index)
}
