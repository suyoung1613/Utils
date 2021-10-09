package com.example.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.share.databinding.ActivityMainBinding
import java.io.File


class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initBtns()
    }

    private fun initBtns() {

        binding.btnShareImgSpecificStorage.setOnClickListener {
            val testBitmap = Utils.getBitmapFromView(binding.lyContainer)
            if (testBitmap == null) {
                Toast.makeText(applicationContext, "이미지 에러", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            shareImageInSpecificStorage(applicationContext, testBitmap)
        }

        binding.btnShareImgSharedStorage.setOnClickListener {
            val testBitmap = Utils.getBitmapFromView(binding.lyContainer)
            if (testBitmap == null) {
                Toast.makeText(applicationContext, "이미지 에러", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            shareImageInShareStorage(applicationContext, testBitmap)
        }

        binding.btnShareText.setOnClickListener { shareText("공유할 텍스트") }
    }


    /**
     *
     * 앱 전용 공간에 이미지 저장 후 공유
     *
     * @param context Context
     * @param bitmap 공유하고자 하는 이미지
     */
    private fun shareImageInSpecificStorage(context: Context, bitmap: Bitmap) {

        /**
         *1) 이미지 저장(/storage/emulated/0/Android/data/[앱 패키지명]/files/ShareImgFolder)
         */
        val imagePath = File(context.getExternalFilesDir(null)?.absolutePath + "/ShareImgFolder")
        imagePath.mkdirs()
        val fileName = "shareImage_" + Utils.getCurrentTime("yyyyMMddHHmmss") + ".jpg"
        val filePath = imagePath.absolutePath + File.separator + fileName
        Utils.saveBitmapToFile(bitmap, filePath, Bitmap.CompressFormat.JPEG) //bitmap 파일저장

        /**
         *2) Share Intent 생성
         */
        val shareIntent = Intent().apply {
            val mediaFile = File(filePath)
            // file extension 으로 부터 타입 추론 ex)image/jpeg
            val mediaType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(mediaFile.extension)

            //content://<authority>/<path>
            val contentUri =
                FileProvider.getUriForFile(
                    applicationContext,
                    BuildConfig.APPLICATION_ID + ".provider",
                    mediaFile
                )
            // contentUri 셋팅
            // 기타 프로퍼티 설정
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type = mediaType
            action = Intent.ACTION_SEND
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            setDataAndType(contentUri, contentResolver.getType(contentUri));
        }

        /**
         *3) Intent 실행 ->공유할 앱 선택할 수 있는 화면 나타남
         */
        startActivity(Intent.createChooser(shareIntent, ""))
    }

    /**
     *
     * 공용공간 Picture 폴더에 사진 저장 후 공유
     *
     *
     *
     * @param context Context
     * @param bitmap 공유하고자 하는 이미지
     */
    private fun shareImageInShareStorage(context: Context, bitmap: Bitmap) {

        //공유공간 접근시 WRITE 퍼미션 체크
        //targetSdk
        val hasPermission: Boolean
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            hasPermission = Utils.checkWritePermission(this)
        } else {
            hasPermission = true
        }
        if (!hasPermission) {
            return
        }
        /**
         *1) 이미지 저장(/)
         */
        val folderName = context.getString(R.string.app_name) + "_" + Build.VERSION.SDK_INT
        val fileName = "shareImage_" + Utils.getCurrentTime("yyyyMMddHHmmss") + ".jpg"
        val mimeType = "image/jpeg"
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //29이상(10.0)
            Utils.saveImage29AndOver(
                this,
                fileName,
                bitmap,
                mimeType, folderName
            )
        } else {
            //29 미만
            Utils.saveImageUnder29(this, fileName, bitmap, mimeType, folderName)
        }

        if (uri == null) {
            Toast.makeText(applicationContext, "이미지 저장 에러(공용공간)", Toast.LENGTH_LONG).show()
            return
        }

        /**
         *2) Share Intent 생성
         */
        val shareIntent = Intent().apply {
            val pictureDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) //TODO 뭘로 대체해야 한단 말이가...
            val mediaFile = File(pictureDir, folderName + File.separator + fileName)
            putExtra(Intent.EXTRA_STREAM, uri)
            Log.i("syTest", "uri = absolute " + mediaFile.absolutePath)
            Log.i("syTest", "uri =" + Uri.parse(mediaFile.absolutePath))
            action = Intent.ACTION_SEND
            type = "image/jpeg"
        }

        /**
         *3) Intent 실행 ->공유할 앱 선택할 수 있는 화면 나타남
         */
        startActivity(Intent.createChooser(shareIntent, ""))
    }


    /**
     * 텍스트 공유
     */
    private fun shareText(textToShare: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/*"
        shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare)
        startActivity(Intent.createChooser(shareIntent, ""));
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(
            "syTest",
            "[onRequestPermissionsResult] requestCode = $requestCode, permissions = $permissions, grantResults = $grantResults"
        )
        val result = PermissionChecker.checkUserAcceptPermissions(grantResults)
        if (!result) {
            Toast.makeText(applicationContext, "권한 허용이 필요합니다...", Toast.LENGTH_LONG).show()
        }
    }
}