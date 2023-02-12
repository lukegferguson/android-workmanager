package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import com.example.background.R

private const val TAG = "BlurWorker"
class BlurWorker(ctx: Context, params: WorkerParameters): Worker(ctx, params) {
    override fun doWork(): Result {
        //Retrieve context
        val appContext = applicationContext

        val resourceUri = inputData.getString(KEY_IMAGE_URI)

        //Display init status notification banner and slow execution for demonstration purposes
        makeStatusNotification("Blurring Image", appContext)
        sleep()

        return try {

            //Verify resourceUri is not empty
            if (TextUtils.isEmpty(resourceUri)){
                Log.e(TAG, "Invalid input URI")
                throw IllegalArgumentException("Invalid input URI")
            }

            //Access content resolver instance,
            // This serves to allow data stored by one process to be accessed in another process
            val resolver = appContext.contentResolver

            //Assign image passed in to variable
            val picture = BitmapFactory.decodeStream(
                resolver.openInputStream(Uri.parse(resourceUri))
            )

            //Blur bitmap
            val output = blurBitmap(picture, appContext)

            //Write bitmap to file, returns URI
            val outputUri = writeBitmapToFile(appContext, output)

            //Assign blurred image to Data object
            val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())

            Result.success(outputData)
        } catch (throwable: Throwable){
            Log.e(TAG, "Error applying blur")
            Result.failure()
        }

    }

}