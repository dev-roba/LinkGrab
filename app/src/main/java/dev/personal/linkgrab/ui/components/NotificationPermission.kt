package dev.personal.linkgrab.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Returns a function that asks for the notification permission (Android 13+) the first time it is
 * called while the permission is missing, so download progress can show in the notification shade.
 * It asks at most once from here; the system also stops asking after repeated denials.
 */
@Composable
fun rememberNotificationPermissionRequester(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return remember(launcher, context) {
        val request: () -> Unit = {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                val prefs = context.getSharedPreferences("linkgrab_ui", Context.MODE_PRIVATE)
                if (!prefs.getBoolean("asked_notifications", false)) {
                    prefs.edit().putBoolean("asked_notifications", true).apply()
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        request
    }
}
