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

class AppUpdater(private val context: Context) {

    //private val serverUrl = "http://10.208.5.53:8080/actualizaciones/version.json"
    private val serverUrl = "http://181.225.65.82:8195/actualizaciones/version.json"


    val versionName = BuildConfig.VERSION_NAME
    private val currentVersion = versionName // Cambia esto por la versión actual de la app

    fun checkForUpdate() {
        val client = OkHttpClient()
        val request = Request.Builder().url(serverUrl).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AppUpdater", "Error al consultar la versión", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseData ->
                    val json = JSONObject(responseData)
                    val latestVersion = json.getString("version")
                    val apkUrl = json.getString("apk_url")

                    if (latestVersion > currentVersion) {
                        showUpdateDialog(apkUrl)
                    }
                }
            }
        })
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

        // Verificar cuando finalice la descarga y abrir el APK
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
