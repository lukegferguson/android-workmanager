package com.example.background.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.background.OUTPUT_PATH
import java.io.File

/*
* Cleans up any existing temporary files created by blurring process
* */
private const val TAG = "CleanupWorker"
class CleanupWorker(ctx: Context, params: WorkerParameters): Worker(ctx, params) {

    override fun doWork(): Result {
        //Creates notification when work starts
        //Slows down the work to make it easier to see each WorkRequest start for demonstration purposes
        makeStatusNotification("Cleaning up old temp files", applicationContext)
        sleep()

        return try {
            val outputDirectory = File(applicationContext.filesDir, OUTPUT_PATH)
            if (outputDirectory.exists()){
                val entries = outputDirectory.listFiles()
                if (entries != null){
                  for (entry in entries){
                      val name = entry.name
                      if (name.isNotEmpty() && name.endsWith(".png")){
                          val deleted = entry.delete()
                          Log.i(TAG, "Deleted $name - $deleted")
                      }
                  }
                }
            }
            Result.success()
        } catch (exception: Exception){
            exception.printStackTrace()
            Result.failure()
        }



    }
}