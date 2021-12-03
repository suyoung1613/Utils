package com.example.imageuploadtest

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


const val REQ_SELECT_IMG = 200 //이미지 픽
const val REQ_IMG_CAPTURE = 300 //썸네일 크기
const val REQ_IMG_CAPTURE_FULL_SIZE =
    400 //앱 전용 공간 - No Need WRITE Permission After Android 4.3 (API level 18)
const val REQ_IMG_CAPTURE_FULL_SIZE_SHARED_UNDER_Q = 500 //공용공간 - Q미만, Need WRITE Permission
const val REQ_IMG_CAPTURE_FULL_SIZE_SHARED_Q_AND_OVER = 600//공용공간 Q이상

lateinit var photoURI: Uri
lateinit var photoSharedURI_UNDER_Q: Uri
lateinit var photoSharedURI_Q_N_OVER: Uri


fun getPictureIntent_App_Specific(context: Context): Intent {
    photoURI = Uri.EMPTY
    val fullSizeCaptureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

    //1) File 생성 - 촬영 사진이 저장 될
    //photoFile 경로 = /storage/emulated/0/Android/data/패키지명/files/Pictures/
    val photoFile: File? = try {
        createImageFile(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
    } catch (ex: IOException) {
        // Error occurred while creating the File
        ex.printStackTrace()
        Log.e("syTest", "createImageFile = ${ex.printStackTrace()}")
        null
    }

    Log.w("syTest", "photoFIle  = $photoFile")
    // Continue only if the File was successfully created
    photoFile?.also {
        //2) 생성된 File로 부터 Uri 생성 (by FileProvider)
        photoURI = FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".fileprovider",
            it
        )
        Log.w("syTest", "photoURI  = $photoURI")
        //3) 생성된 Uri를 Intent에 Put
        fullSizeCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
    }
//        021-11-26 14:25:14.204 29107-29107/com.example.img W/syTest: photoFIle  = /storage/emulated/0/Android/data/com.example.img/files/Pictures/JPEG_20211126_142514_2122920073823841434.jpg
//        2021-11-26 14:25:14.204 29107-29107/com.example.img W/syTest: photoURI  = content://com.example.img.fileprovider/cameraImg/JPEG_20211126_142514_2122920073823841434.jpg
//        2021-11-26 14:25:17.904 29107-29107/com.example.img W/syTest: [onActivityResult] requestCode = 400, resultCode = -1
    return fullSizeCaptureIntent
}

@Throws(IOException::class)
private fun createImageFile(storageDir: File?): File {
    // Create an image file name
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    return File.createTempFile(
        "JPEG_${timeStamp}_", /* prefix */
        ".jpg", /* suffix */
        storageDir /* directory */
    ).apply {
        // Save a file: path for use with ACTION_VIEW intents
        Log.i("syTest", "Created File AbsolutePath : $absolutePath")
    }
}

/**
 * 공유 영역 저장
 * Android Q 미만 일 경우 ( ~ Android 9.0)
 * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
 */
fun getPictureIntent_Shared_Under_Q(context: Context): Intent {
    photoSharedURI_UNDER_Q = Uri.EMPTY
    val fullSizeCaptureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

    //1) File 생성 - 촬영 사진이 저장 될
    val photoFile: File? = try {
        // getExternalStoragePublicDirectory(), with the DIRECTORY_PICTURES argument.
        // The directory provided by this method is shared among all apps. On Android 9 (API level 28) and lower, reading and writing to this directory requires the READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE permissions
        createImageFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)) //deprecated in 29(Q) = 28(Android 9)까지 사용 가능
    } catch (ex: IOException) {
        // Error occurred while creating the File
        ex.printStackTrace()
        Log.e("syTest", "createImageFile = ${ex.printStackTrace()}")
        null
    }

    Log.w("syTest", "photoFIle  = $photoFile")
    // Continue only if the File was successfully created
    photoFile?.also {
        //2) 생성된 File로 부터 Uri 생성 (by FileProvider)
        photoSharedURI_UNDER_Q = FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".fileprovider",
            it
        )
        Log.w("syTest", "photoSharedURI_UNDER_Q  = $photoSharedURI_UNDER_Q")
        //3) 생성된 Uri를 Intent에 Put
        fullSizeCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoSharedURI_UNDER_Q)
    }

//        2021-11-26 14:25:04.094 29107-29107/com.example.img W/syTest: photoFIle  = /storage/emulated/0/Pictures/JPEG_20211126_142504_2806917635294556004.jpg
//        2021-11-26 14:25:04.096 29107-29107/com.example.img W/syTest: photoSharedURI_UNDER_Q  = content://com.example.img.fileprovider/cameraImgShared/JPEG_20211126_142504_2806917635294556004.jpg
//        2021-11-26 14:25:09.884 29107-29107/com.example.img W/syTest: [onActivityResult] requestCode = 500, resultCode = -1
    return fullSizeCaptureIntent
}

/**
 * 공유 영역 저장
 * Android Q 이상일 경우, API 29~ (Android 10.0 ~ )
 * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
 */
@RequiresApi(Build.VERSION_CODES.Q)
fun getPictureIntent_Shared_Q_N_Over(context: Context): Intent {
    photoSharedURI_Q_N_OVER = Uri.EMPTY
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val contentValues = ContentValues()
    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "UriForAndroidQ_${timeStamp}.jpg")
    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES) //'RELATIVE_PATH', RequiresApi Q
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//        contentValues.put(MediaStore.Images.Media.IS_PENDING, 1)
//    }

    photoSharedURI_Q_N_OVER = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ) ?: Uri.EMPTY
    //[태스트 결과]
    //1)MediaStore.Images.Media.EXTERNAL_CONTENT_URI : content://media/external/images/media/1000
    //2)MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)  content://media/external/images/media/1009
    //3)MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) content://media/external_primary/images/media/1007

    Log.w("syTest", "getPictureIntent_Shared_Q URI = $photoSharedURI_Q_N_OVER")

    //2)content://media/external_primary/images/media/1006
    val fullSizeCaptureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    fullSizeCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoSharedURI_Q_N_OVER)
    return fullSizeCaptureIntent
}


