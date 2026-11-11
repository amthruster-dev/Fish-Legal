package com.fishlegal.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.fishlegal.app.R
import com.fishlegal.app.data.AppDatabase
import com.fishlegal.app.data.CatchLog
import com.fishlegal.app.ml.FishClassifier
import com.fishlegal.app.ml.LabelMap
import com.fishlegal.app.location.ZoneLookup
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CameraActivity : AppCompatActivity() {

    private lateinit var classifier: FishClassifier
    private var modelReady = false
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        classifier = FishClassifier(this)
        try {
            LabelMap.load(this)
            classifier.loadModel()
            modelReady = true
        } catch (e: Exception) {
            // Placeholder/missing model (e.g. not yet trained or synced). Continue
            // without crashing — capture still records the photo, location and
            // zone; the result screen will say the species couldn't be identified.
            e.printStackTrace()
            modelReady = false
        }

        startCamera()

        findViewById<android.widget.Button>(R.id.btnCapture).setOnClickListener {
            capturePhoto()
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<androidx.camera.view.PreviewView>(R.id.previewView).surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, selector, preview, imageCapture)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("MissingPermission")
    private fun capturePhoto() {
        val capture = imageCapture ?: return
        val photoFile = File(filesDir, "catch_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    LocationServices.getFusedLocationProviderClient(this@CameraActivity)
                        .lastLocation.addOnSuccessListener { location ->
                            processCapture(photoFile, location)
                        }
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                }
            }
        )
    }

    private fun processCapture(photoFile: File, location: Location?) {
        CoroutineScope(Dispatchers.Main).launch {
            val bitmap = withContext(Dispatchers.IO) {
                android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
            }
            val prediction = if (modelReady) {
                try {
                    withContext(Dispatchers.Default) { classifier.classify(bitmap) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    FishClassifier.Prediction(-1, 0f)
                }
            } else {
                FishClassifier.Prediction(-1, 0f)
            }
            val speciesId = if (prediction.labelIndex >= 0) LabelMap.speciesIdForIndex(prediction.labelIndex) else null

            val db = AppDatabase.get(applicationContext)
            val lat = location?.latitude ?: 0.0
            val lon = location?.longitude ?: 0.0

            val zones = withContext(Dispatchers.IO) { db.regulationDao().getAllZones() }
            val zone = ZoneLookup.findZone(lat, lon, zones)

            withContext(Dispatchers.IO) {
                db.catchLogDao().insert(
                    CatchLog(
                        photoPath = photoFile.absolutePath,
                        speciesId = speciesId,
                        confidence = prediction.confidence,
                        latitude = lat,
                        longitude = lon,
                        zoneId = zone?.zoneId,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            val intent = Intent(this@CameraActivity, ResultActivity::class.java).apply {
                putExtra(ResultActivity.EXTRA_SPECIES_ID, speciesId)
                putExtra(ResultActivity.EXTRA_CONFIDENCE, prediction.confidence)
                putExtra(ResultActivity.EXTRA_ZONE_ID, zone?.zoneId)
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (modelReady) classifier.close()
    }
}
