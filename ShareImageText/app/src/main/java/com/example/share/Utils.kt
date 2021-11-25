package com.example.share

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object Utils {
    /**
     * Bitmap 저장
     *
     * @param bitmap
     * @param filePath
     */
    fun saveBitmapToFile(bitmap: Bitmap, filePath: String, format: Bitmap.CompressFormat): Boolean {
        val file = File(filePath)
        val out: OutputStream = FileOutputStream(file)
        return try {
            bitmap.compress(format, 100, out)
            true
        } catch (e: Exception) {
            false
        } finally {
            out.close()
        }
    }

    /**
     * View Image -> Bitmap
     */
    fun getBitmapFromView(view: View): Bitmap? {
        Log.w("syTest", "view.width, height = " + view.width + "/" + view.height)
        Log.w(
            "syTest",
            "view.getLayoutParams width, height = " + view.layoutParams.width + "/" + view.layoutParams.height
        )
        Log.w(
            "syTest",
            "view.measuredWidth, height = " + view.measuredWidth + "/" + view.measuredHeight
        )
        if (view.measuredWidth <= 0 || view.measuredHeight <= 0) {
            //DP
            Log.e(
                "syTest",
                "[getBitmapFromView measured W/H]" + view.measuredWidth + ", " + view.measuredHeight
            )
            return null
        }
        Log.i(
            "syTest",
            "[getBitmapFromView measured W/H]" + view.measuredWidth + ", " + view.measuredHeight
        )
        val bitmap = Bitmap.createBitmap(
            view.measuredWidth,
            view.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveImage29AndOver(
        context: Context,
        displayNm: String,
        bitmap: Bitmap,
        mimeType: String,
        folderName: String

    ): Uri? {
        //1) ContentValues 생성
        val contentValue = ContentValues().apply {
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures" + File.separator + folderName
            )
            put(MediaStore.Images.Media.DISPLAY_NAME, displayNm)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)

            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        //2) contentResolver insert and get uri
        val itemUri: Uri? = context.contentResolver.insert(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            contentValue
        )

        if (itemUri == null) {
            Log.e("syTest", "itemUri is Null")
            return null
        }
        Log.w("syTest", "[saveImage29AndOver] saveItemUri = $itemUri")
        try {
            //3) write file using fileDescriptor
            val fileDescriptor = context.contentResolver.openFileDescriptor(itemUri, "w", null)
            if (fileDescriptor == null) {
                Log.e("syTest", "fileDescriptor is Null")
                return null
            }

            fileDescriptor.use {
                // write something to OutputStream
                FileOutputStream(it.fileDescriptor).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)//use bitmap
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValue.clear()
                contentValue.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(itemUri, contentValue, null, null)
            }

        } catch (e: Exception) {
            Log.e("syTest", "[saveImage29AndOver] saveResult = false \n" + e.message)
            e.printStackTrace()
            return null
        }
        Log.w("syTest", "[saveImage29AndOver] saveResult = true")
        return itemUri
    }

    /**
     * 'WRITE_EXTERNAL_STORAGE' is required
     */
    fun saveImageUnder29(
        context: Context,
        fileName: String,
        bitmap: Bitmap,
        mimeType: String,
        folderName: String
    ): Uri? {
        try {
            val dir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    .toString() +
                        File.separator + folderName
            val file = File(dir)
            if (!file.exists()) {
                file.mkdirs()
            }
            val imgFile = File(file, fileName)
            val os = FileOutputStream(imgFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
            os.close()

            val values = ContentValues()
            with(values) {
                put(MediaStore.Images.Media.TITLE, fileName)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.Images.Media.BUCKET_ID, fileName)
                put(MediaStore.Images.Media.DATA, imgFile.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            }
            return context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e("syTest", "[saveImageUnder29]  saveResult = false" + e.message)
            return null
        }

    }

    /**
     * 퍼미션 쳌
     */
    fun checkWritePermission(act: Activity): Boolean {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return PermissionChecker.checkPermissions(act, permissions, 1004)
    }

    /**
     * 현재 시간 반환
     *
     * @param format 현재시간을 반환받을 Format
     * @return
     */
    fun getCurrentTime(format: String): String? {
        val sdf = SimpleDateFormat(format, Locale.KOREA)
        val date = Date()
        return sdf.format(date)
    }

}
