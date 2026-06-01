package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.components.MainDashboard
import com.example.viewmodel.AudioViewModel
import com.example.ui.theme.MyApplicationTheme

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.dsp.MediaProjectionService

class MainActivity : ComponentActivity() {
  private val viewModel: AudioViewModel by viewModels()

  companion object {
    var instance: MainActivity? = null
        private set
  }

  private val mediaProjectionLauncher = registerForActivityResult(
      ActivityResultContracts.StartActivityForResult()
  ) { result ->
      if (result.resultCode == RESULT_OK && result.data != null) {
          val serviceIntent = Intent(this, MediaProjectionService::class.java).apply {
              putExtra("RESULT_CODE", result.resultCode)
              putExtra("RESULT_DATA", result.data)
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              startForegroundService(serviceIntent)
          } else {
              startService(serviceIntent)
          }
          viewModel.setSystemAudioCaptureActive(true)
      } else {
          viewModel.setSystemAudioCaptureActive(false)
      }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    instance = this

    // Request RECORD_AUDIO runtime permission for real-time audio analysis
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
    }

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainDashboard(viewModel = viewModel)
        }
      }
    }
  }

  fun startSystemAudioCapture() {
      try {
          val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
          val intent = mediaProjectionManager.createScreenCaptureIntent()
          mediaProjectionLauncher.launch(intent)
      } catch (e: Exception) {
          e.printStackTrace()
      }
  }

  fun stopSystemAudioCapture() {
      try {
          val intent = Intent(this, MediaProjectionService::class.java)
          stopService(intent)
          com.example.dsp.AudioEngine.instance.setMediaProjection(null)
          viewModel.setSystemAudioCaptureActive(false)
      } catch (e: Exception) {
          e.printStackTrace()
      }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (instance == this) {
        instance = null
    }
  }
}

