package com.uvrp.itsmantenimientoapp

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AlertDialog
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AppUpdater(private val context: Context) {

    val versionName = BuildConfig.VERSION_NAME
    private val currentVersion = versionName

    fun checkForUpdate() {
        Thread {
            try {
                val serverUrl = EndpointResolver.resolveUpdateJsonUrl(context.applicationContext)
                if (serverUrl == null) {
                    Log.w("AppUpdater", "No se encontró servidor de actualizaciones")
                    return@Thread
                }
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder().url(serverUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@Thread
                    val responseData = response.body?.string()?.trim().orEmpty()
                    if (responseData.isEmpty() || !responseData.startsWith("{")) {
                        Log.w("AppUpdater", "Respuesta de versión no es JSON: ${responseData.take(80)}")
                        return@Thread
                    }
                    val json = JSONObject(responseData)
                    val latestVersion = json.optString("version", "")
                    val apkUrl = json.optString("apk_url", "")
                    if (latestVersion.isBlank() || apkUrl.isBlank()) {
                        Log.w("AppUpdater", "JSON de versión incompleto")
                        return@Thread
                    }
                    if (latestVersion > currentVersion) {
                        showUpdateDialog(apkUrl)
                    }
                }
            } catch (e: JSONException) {
                Log.e("AppUpdater", "JSON de versión inválido", e)
            } catch (e: IOException) {
                Log.e("AppUpdater", "Error de red al consultar versión", e)
            } catch (e: Exception) {
                Log.e("AppUpdater", "Error inesperado al consultar versión", e)
            }
        }.start()
    }

    private fun showUpdateDialog(apkUrl: String) {
        val activity = context as? Activity ?: return
        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
            try {
                AlertDialog.Builder(activity)
                    .setTitle("Actualización Disponible")
                    .setMessage("Hay una nueva versión disponible. ¿Deseas actualizar ahora?")
                    .setPositiveButton("Actualizar") { _, _ -> downloadApk(apkUrl) }
                    .setNegativeButton("Más tarde", null)
                    .show()
            } catch (e: Exception) {
                Log.e("AppUpdater", "No se pudo mostrar diálogo de actualización", e)
            }
        }
    }

    private fun downloadApk(apkUrl: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Descargando actualización...")
                .setDescription("Espere mientras se descarga la actualización")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app-debug.apk")

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            val query = DownloadManager.Query().setFilterById(downloadId)
            Thread {
                var downloading = true
                while (downloading) {
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false
                            val apkUri = downloadManager.getUriForDownloadedFile(downloadId)
                            installApk(apkUri)
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            downloading = false
                        }
                    }
                    cursor.close()
                    try {
                        Thread.sleep(500)
                    } catch (_: InterruptedException) {
                        return@Thread
                    }
                }
            }.start()
        } catch (e: Exception) {
            Log.e("AppUpdater", "Error al descargar APK", e)
        }
    }

    private fun installApk(apkUri: Uri?) {
        if (apkUri == null) return
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdater", "Error al instalar APK", e)
        }
    }
}
