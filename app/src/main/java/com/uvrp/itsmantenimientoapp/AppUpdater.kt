package com.uvrp.itsmantenimientoapp

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AlertDialog
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AppUpdater(private val context: Context) {

    val versionName = BuildConfig.VERSION_NAME
    private val currentVersion = versionName

    fun checkForUpdate() {
        Thread {
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
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@Thread
                    val responseData = response.body?.string() ?: return@Thread
                    val json = JSONObject(responseData)
                    val latestVersion = json.getString("version")
                    val apkUrl = json.getString("apk_url")
                    if (latestVersion > currentVersion) {
                        showUpdateDialog(apkUrl)
                    }
                }
            } catch (e: IOException) {
                Log.e("AppUpdater", "Error al consultar la versión en $serverUrl", e)
            }
        }.start()
    }

    private fun showUpdateDialog(apkUrl: String) {
        (context as? android.app.Activity)?.runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle("Actualización Disponible")
                .setMessage("Hay una nueva versión disponible. ¿Deseas actualizar ahora?")
                .setPositiveButton("Actualizar") { _, _ -> downloadApk(apkUrl) }
                .setNegativeButton("Más tarde", null)
                .show()
        }
    }

    private fun downloadApk(apkUrl: String) {
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
                    }
                }
                cursor.close()
            }
        }.start()
    }

    private fun installApk(apkUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
