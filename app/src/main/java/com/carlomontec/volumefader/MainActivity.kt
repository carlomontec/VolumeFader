package com.carlomontec.volumefader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.carlomontec.volumefader.audio.FadeCurve
import com.carlomontec.volumefader.service.VolumeFadeService
import com.carlomontec.volumefader.ui.screens.MainFaderScreen
import com.carlomontec.volumefader.ui.theme.VolumeFaderTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()

        setContent {
            VolumeFaderTheme {
                MainFaderScreen(
                    onStartFade = { durationMinutes, curve, pauseOnFinish ->
                        startFadeService(durationMinutes, curve, pauseOnFinish)
                    },
                    onStopFade = {
                        stopFadeService()
                    }
                )
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startFadeService(
        durationMinutes: Int,
        curve: FadeCurve,
        pauseOnFinish: Boolean
    ) {
        val intent = VolumeFadeService.startIntent(
            context = this,
            durationMinutes = durationMinutes,
            curve = curve,
            pauseOnFinish = pauseOnFinish
        )
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopFadeService() {
        val intent = VolumeFadeService.stopIntent(this)
        startService(intent)
    }
}
