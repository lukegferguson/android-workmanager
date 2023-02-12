/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.bumptech.glide.Glide.init
import com.example.background.workers.BlurWorker
import com.example.background.workers.CleanupWorker
import com.example.background.workers.SaveImageToFileWorker


class BlurViewModel(application: Application) : ViewModel() {

    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null

    private val workManager = WorkManager.getInstance(application)

    //variable for tacking status of WorkManager
    internal val outputWorkInfos: LiveData<List<WorkInfo>>

    init {
        imageUri = getImageUri(application.applicationContext)

        //This ensures that when the work ID changes the UI is still listening to changes
        outputWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)
    }
    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    internal fun applyBlur(blurLevel: Int) {
        //Create WorkManager chain and pass in worker to initiate file cleanup
        var continuation = workManager
            .beginUniqueWork(
                IMAGE_MANIPULATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(CleanupWorker::class.java)
            )

        //Repeat blurring operation in WorkManager chain based on
        // parameter passed in from user selection of blur intensity
        for (i in 0 until blurLevel){
            val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()

            //Create first blur request on input image
            if (i == 0) {
                blurBuilder.setInputData(createInputDataForUri())
            }
            // Subsequent WorkManager requests take the previous output as input
            continuation = continuation.then(blurBuilder.build())
        }

        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .build()

        //Save temp blurred image to permanent file location
        val saveImageRequest = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
                .setConstraints(constraints)
                .addTag(TAG_OUTPUT)
                .build()
        //Add to WorkManager chain
        continuation = continuation.then(saveImageRequest)

        //Start workManager chain operations
        continuation.enqueue()
    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    private fun getImageUri(context: Context): Uri {
        val resources = context.resources

        val imageUri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceTypeName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceEntryName(R.drawable.android_cupcake))
            .build()

        return imageUri
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

    class BlurViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(BlurViewModel::class.java)) {
                BlurViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    //Create Data input for worker containing imageUri
    private fun createInputDataForUri(): Data {
        val builder = Data.Builder()
        imageUri?.let {
               builder.putString(KEY_IMAGE_URI, imageUri.toString())
           }
        return builder.build()
    }

    internal fun cancelWork(){
        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    }

}


