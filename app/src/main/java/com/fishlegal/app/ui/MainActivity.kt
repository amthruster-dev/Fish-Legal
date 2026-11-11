package com.fishlegal.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fishlegal.app.R
import com.fishlegal.app.sync.SyncWorker

class MainActivity : AppCompatActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SyncWorker.schedulePeriodic(applicationContext)
        SyncWorker.syncNow(applicationContext) // opportunistic; no-ops if offline

        if (hasPermissions()) {
            startActivity(Intent(this, CameraActivity::class.java))
            finish()
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CODE)
        }
    }

    private fun hasPermissions() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE && hasPermissions()) {
            startActivity(Intent(this, CameraActivity::class.java))
            finish()
        } else {
            android.widget.Toast.makeText(
                this, "Camera and location permissions are required to use FishLegal", android.widget.Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    companion object {
        private const val REQUEST_CODE = 100
    }
}
