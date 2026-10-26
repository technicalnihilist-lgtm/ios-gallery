package com.hubclub.iosgallery.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Android 13-16 media permissions.
 *
 *  - API 33+: READ_MEDIA_IMAGES / READ_MEDIA_VIDEO
 *  - API 34+: READ_MEDIA_VISUAL_USER_SELECTED (partial "select photos" access)
 *  - API <=32: READ_EXTERNAL_STORAGE (write is only needed for restoring
 *    trashed items on those legacy releases)
 */
fun requiredMediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    )

    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO
    )

    else -> arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
}

private fun granted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/** True when the app can read at least some of the device's media. */
fun hasMediaPermission(context: Context): Boolean = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
        granted(context, Manifest.permission.READ_MEDIA_IMAGES) ||
            granted(context, Manifest.permission.READ_MEDIA_VIDEO) ||
            granted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
        granted(context, Manifest.permission.READ_MEDIA_IMAGES) ||
            granted(context, Manifest.permission.READ_MEDIA_VIDEO)

    else -> granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
}

/** True when the user picked "Allow all" rather than "Select photos". */
fun hasFullMediaAccess(context: Context): Boolean = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
        granted(context, Manifest.permission.READ_MEDIA_IMAGES) ||
            granted(context, Manifest.permission.READ_MEDIA_VIDEO)

    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
        granted(context, Manifest.permission.READ_MEDIA_IMAGES) ||
            granted(context, Manifest.permission.READ_MEDIA_VIDEO)

    else -> granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
}

fun shouldExplainAgain(activity: Activity?): Boolean {
    if (activity == null) return false
    return requiredMediaPermissions().any {
        ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
    }
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
