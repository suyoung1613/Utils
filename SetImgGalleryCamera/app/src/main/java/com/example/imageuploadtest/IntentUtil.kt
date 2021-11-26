package com.example.imageuploadtest

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


const val REQ_SELECT_IMG = 200
const val REQ_IMG_CAPTURE = 300
const val REQ_IMG_CAPTURE_FULL_SIZE = 400
const val REQ_IMG_CAPTURE_FULL_SIZE_SHARED = 500



object IntentMaker {
    lateinit var currentPhotoPath: String
    lateinit var currentPhotoUri: Uri
    fun getPictureIntent(context: Context): Intent {
        currentPhotoPath = ""//초기화
        val fullSizeCaptureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        //1) File 생성 - 촬영 사진이 저장 될
        val photoFile: File? = try {
            createImageFile(context)
        } catch (ex: IOException) {
            // Error occurred while creating the File Todo
            null
        }

        Log.w("syTest", "photoFIle  = $photoFile")
        // Continue only if the File was successfully created
        photoFile?.also {
            //2) 생성된 File로 부터 Uri 생성 (by FileProvider)
            val photoURI: Uri = FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                it
            )
            Log.w("syTest", "photoURI  = $photoURI")
            //3) 생성된 Uri를 Intent에 Put
            fullSizeCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        }

        return fullSizeCaptureIntent
    }

    @Throws(IOException::class)
    private fun createImageFile(context: Context): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    /**
     * 공유 영역 저장
     * Android Q 이상일 경우
     * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
     */
    fun getPictureIntent_Shared_Q(contentResolver: ContentResolver): Intent {
        currentPhotoUri = Uri.EMPTY
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val picturePath = Environment.DIRECTORY_PICTURES
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "UriForAndroidQ_${timeStamp}.jpg")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, picturePath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        currentPhotoUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?: Uri.EMPTY
//1)[MediaStore.Images.Media.EXTERNAL_CONTENT_URI] : content://media/external/images/media/1000
        //Android 11 - Crash

//        currentPhotoUri =contentResolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),contentValues)?: Uri.EMPTY
//2)MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) content://media/external_primary/images/media/1007
//2)MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)  content://media/external/images/media/1009

        Log.w("syTest","getPictureIntent_Shared_Q URI = "+ currentPhotoUri)



        //2)content://media/external_primary/images/media/1006
        val fullSizeCaptureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        fullSizeCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
        return fullSizeCaptureIntent
    }
}


