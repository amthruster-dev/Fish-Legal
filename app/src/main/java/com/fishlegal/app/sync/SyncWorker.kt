package com.fishlegal.app.sync

import android.content.Context
import androidx.work.*
import com.fishlegal.app.data.AppDatabase
import com.fishlegal.app.data.UploadState
import com.fishlegal.app.ml.LabelMap
import com.fishlegal.app.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Runs only when the device has an internet connection (constrained via
 * WorkManager's NetworkType.CONNECTED). Downloads updated species data,
 * regulations, and the ML model; uploads any queued catch logs taken while
 * offline. Everything else in the app reads from local storage only.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val db = AppDatabase.get(applicationContext)
            val lastSync = applicationContext.getSharedPreferences("sync", Context.MODE_PRIVATE)
                .getLong("last_sync", 0L)

            // Pull updates
            val speciesResp = ApiClient.service.getSpeciesUpdates(lastSync)
            if (speciesResp.isSuccessful) speciesResp.body()?.let { db.speciesDao().upsertAll(it) }

            val zonesResp = ApiClient.service.getZoneUpdates(lastSync)
            if (zonesResp.isSuccessful) zonesResp.body()?.let { db.regulationDao().upsertZones(it) }

            val rulesResp = ApiClient.service.getRuleUpdates(lastSync)
            if (rulesResp.isSuccessful) rulesResp.body()?.let { db.regulationDao().upsertRules(it) }

            // Check for a newer on-device model
            val modelInfoResp = ApiClient.service.getLatestModelInfo()
            if (modelInfoResp.isSuccessful) {
                modelInfoResp.body()?.let { info ->
                    downloadAndReplace(info.modelUrl, "fish_classifier.tflite")
                    downloadAndReplace(info.labelsUrl, "labels.txt")
                    LabelMap.load(applicationContext)
                }
            }

            // Push queued catches recorded while offline
            val pending = db.catchLogDao().getPendingUploads()
            for (entry in pending) {
                val uploaded = uploadCatch(entry)
                db.catchLogDao().update(
                    entry.copy(uploadState = if (uploaded) UploadState.UPLOADED else UploadState.FAILED)
                )
            }

            applicationContext.getSharedPreferences("sync", Context.MODE_PRIVATE)
                .edit().putLong("last_sync", System.currentTimeMillis()).apply()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun downloadAndReplace(url: String, filename: String) {
        val resp = ApiClient.service.downloadModelFile(url)
        if (resp.isSuccessful) {
            resp.body()?.byteStream()?.use { input ->
                File(applicationContext.filesDir, filename).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private suspend fun uploadCatch(entry: com.fishlegal.app.data.CatchLog): Boolean {
        return try {
            val file = File(entry.photoPath)
            if (!file.exists()) return false
            val reqFile = okhttp3.RequestBody.create("image/jpeg".toMediaType(), file)
            val photoPart = okhttp3.MultipartBody.Part.createFormData("photo", file.name, reqFile)
            fun text(v: String) = okhttp3.RequestBody.create("text/plain".toMediaType(), v)

            val resp = ApiClient.service.uploadCatch(
                photoPart,
                text(entry.speciesId ?: ""),
                text(entry.latitude.toString()),
                text(entry.longitude.toString()),
                text(entry.timestamp.toString())
            )
            resp.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val WORK_NAME = "fishlegal_auto_sync"

        /** Schedules periodic sync that only fires while connected to the internet. */
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        /** Triggers an immediate opportunistic sync, still gated on connectivity. */
        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}

private fun String.toMediaType() = okhttp3.MediaType.parse(this)
