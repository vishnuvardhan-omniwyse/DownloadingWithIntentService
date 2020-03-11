package com.example.downloadsample

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    companion object {
        const val PERMISSION_STORAGE_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_download.setOnClickListener {
            //Checking OS Version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //Check Permission
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted , requesting it
                    val permissions: Array<String> =
                        arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    //Show Pop Up runtime
                    requestPermissions(permissions, PERMISSION_STORAGE_CODE)
                } else {
                    onDownloadClick()
                }
            } else {
                onDownloadClick()
            }
        }
    }

    override fun onStart() {
        registerReceiver(progressReceiver, IntentFilter(DownloadService.PROGRESS))
        super.onStart()
    }

    private fun onDownloadClick() {
        if (isMyServiceRunning(DownloadService::class.java)) {
            Toast.makeText(this, "File is already being downloaded", Toast.LENGTH_SHORT).show()
            return
        }
        val serviceIntent = Intent(this@MainActivity, DownloadService::class.java)
        serviceIntent?.putExtra(DownloadService.FILENAME, "test1Mb.db")
        serviceIntent?.putExtra(
            DownloadService.URL,
            "http://speedtest.ftp.otenet.gr/files/test1Mb.db"
        )
        startService(serviceIntent)
        download_status.text = getString(R.string.download)
    }

    private val progressReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val bundle = intent.extras
            if (bundle != null) {
                val percentage = bundle.getLong(DownloadService.PERCENTAGE).toInt()
                when {
                    percentage < 100 -> {
                        download_status.text = "$percentage% Downloaded"
                    }
                    percentage == 100 -> {
                        Toast.makeText(this@MainActivity, getString(R.string.file), Toast.LENGTH_LONG)
                            .show()
                        download_status.text = getString(R.string.complete)
                    }
                    else -> {
                        download_status.text = getString(R.string.fail)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_STORAGE_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onDownloadClick()
                } else {
                    //permission Denied
                    Toast.makeText(this, getString(R.string.error_permission), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(progressReceiver)
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}