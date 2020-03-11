package com.example.downloadsample

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.net.URLConnection


class DownloadService : IntentService("DownloadService") {

    lateinit var mBuilder: NotificationCompat.Builder
    lateinit var notificationManager: NotificationManager

    override fun onHandleIntent(intent: Intent?) {
        val urlPath = intent?.getStringExtra(URL)
        val fileName = intent?.getStringExtra(FILENAME)
        try {
            val url = URL(urlPath)
            val connection: URLConnection = url.openConnection()
            connection.allowUserInteraction = true
            connection.connect()
            // getting file length of file
            val lengthOfFile = connection.contentLength
            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "my_channel_01"

            // channel is required for only versions after oreo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name: CharSequence = "my_channel"
                val description = "This is my channel"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val mChannel = NotificationChannel(channelId, name, importance)
                mChannel.description = description
                mChannel.enableLights(true)
                mChannel.lightColor = Color.RED
                mChannel.enableVibration(true)
                mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                mChannel.setShowBadge(false)
                notificationManager.createNotificationChannel(mChannel)
            }
            mBuilder = NotificationCompat.Builder(this, channelId);
            mBuilder.setContentTitle("File Download")
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.sample)
                .setAutoCancel(true)

            //to get contents of url
            val input: InputStream = url.openStream()
            val path = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val output: OutputStream = FileOutputStream(File(path, fileName))

            try {
                val buffer = ByteArray(1024)
                var bytesRead = 0
                var total: Long = 0
                while (input.read(buffer, 0, buffer.size).also { bytesRead = it } >= 0) {
                    total += bytesRead
                    mBuilder.setProgress(lengthOfFile, total.toInt(), false)
                        .setContentText("Downloaded ${total * 100 / lengthOfFile}%")
                    notificationManager.notify(1, mBuilder.build());
                    publishProgress((total * 100 / lengthOfFile))
                    output.write(buffer, 0, bytesRead)
                }
                publishProgress(100)
                Thread.sleep(500) // To Update the notification after downloading. Can be removed later
                mBuilder.setProgress(0, 0, false)  // Removes the progress bar
                    .setContentText("Download completed")
                notificationManager.notify(1, mBuilder.build());
            } finally {
                output.close()
                input.close()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun publishResults(result: Int) {
        val intent = Intent(NOTIFICATION)
        intent.putExtra(RESULT, result)
        sendBroadcast(intent)
    }

    private fun publishProgress(percentage: Long) {
        val intent = Intent(PROGRESS)
        intent.putExtra(PERCENTAGE, percentage)
        sendBroadcast(intent)
    }

    companion object {
        const val URL = "url"
        const val FILENAME = "name"
        const val RESULT = "result"
        const val NOTIFICATION = "notification"
        const val PERCENTAGE = "percentage"
        const val PROGRESS = "progress"

    }
}