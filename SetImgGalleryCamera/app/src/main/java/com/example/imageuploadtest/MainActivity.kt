package com.example.imageuploadtest

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.imageuploadtest.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.tvGallery.setOnClickListener { pickImg() }
        binding.tvCamera.setOnClickListener { picture() }
        binding.tvCameraOriginSize.setOnClickListener { pictureWithOriginSize() }
    }

    /**
     * 1)갤러리 이미지 가져오기
     */
    private fun pickImg() {
        startActivityForResult(pickIntent, REQ_SELECT_IMG)
    }

    /**
     * 2)카메라 촬영 이미지 가져오기
     *
     * 단순히 사진 촬영 해서
     */
    private fun picture() {
        pictureIntent.resolveActivity(packageManager)?.also {
            startActivityForResult(pictureIntent, REQ_IMG_CAPTURE)
        }
    }

    /**
     * 3)카메라 촬영 이미지 (원본 사이즈로 가져오기)
     */
    private fun pictureWithOriginSize() {
        val fullSizePictureIntent = IntentMaker.getFullSizePictureIntent(applicationContext)
        fullSizePictureIntent.resolveActivity(packageManager)?.also {
            startActivityForResult(fullSizePictureIntent, REQ_IMG_CAPTURE_FULL_SIZE)
        }
    }


    //TODO 1) 갤러리 이미지 리사이즈/Orientation
    //TODO 2) 촬영 이미지 저장공간 공용/캐시
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Log.w("syTest", "[onActivityResult] requestCode = $requestCode, resultCode = $resultCode")
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQ_SELECT_IMG -> {
                    Log.w(
                        "syTest",
                        "[onActivityResult] intent = $intent, currentImageUri = ${intent?.data}"
                    )
                    val currentImageUri = intent?.data
                    currentImageUri?.let {
                        if (Build.VERSION.SDK_INT < 28) {
                            val bitmap = MediaStore.Images.Media.getBitmap(
                                this.contentResolver,
                                currentImageUri
                            )
                            binding.ivImg.setImageBitmap(bitmap)
                        } else {
                            val source =
                                ImageDecoder.createSource(this.contentResolver, currentImageUri)
                            val bitmap = ImageDecoder.decodeBitmap(source)
                            binding.ivImg.setImageBitmap(bitmap)
                        }
                    }
                }

                REQ_IMG_CAPTURE -> {
                    val extras = intent?.extras
                    val bitmap = extras?.get("data") as Bitmap?
                    Log.w(
                        "syTest",
                        "[onActivityResult] intent = $intent, Bitmap W/H = ${bitmap?.width}/${bitmap?.height}"
                    )
                    // 얻어온 bitmap은 썸네일 사용 정도의 크기로 resize 된 이미지
                    // Bitmap W/H = 189/252 언저리로 나옴
                    binding.ivImg.setImageBitmap(bitmap)
                }

                REQ_IMG_CAPTURE_FULL_SIZE -> {
                    setPicFromFile()
                }
            }
        }
    }

    private fun setPicFromFile() {
        // Get the dimensions of the View
//        val targetW: Int = binding.ivImg.width
//        val targetH: Int = binding.ivImg.height

        val targetW: Int = 1000 //in pixel
        val targetH: Int = 1000 //in pixel

        val bmOptions = BitmapFactory.Options().apply {

            // Get the dimensions of the bitmap
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(
                IntentMaker.currentPhotoPath,
                this
            )  // 이미지의 크기를 options 에 담아준다 (inJustDecodeBounds = true 로 설정했기에 추가 메모리 낭비 없이 demension만 얻어올 수 있다)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = Math.min(photoW / targetW, photoH / targetH)
            Log.d("syTest", "target W/H : " + targetW + "/" + targetH)
            Log.d("syTest", "photo W/H : " + photoW + "/" + photoH)
            Log.d("syTest", "photo scaleFactor: " + scaleFactor)


            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }

        if (!TextUtils.isEmpty(IntentMaker.currentPhotoPath)) {
            BitmapFactory.decodeFile(IntentMaker.currentPhotoPath, bmOptions)?.also { bitmap ->
                Log.d(
                    "syTest",
                    "photo resized bitmap : " + bitmap.width + "/" + bitmap.height + ", LENGTH = " + bitmap
                )
                //rotate
                val rotateDegree = getExifOrientation(IntentMaker.currentPhotoPath)
                val matrix = Matrix()
                matrix.preRotate(rotateDegree, 0f, 0f)
                binding.ivImg.setImageBitmap(
                    Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.width,
                        bitmap.height,
                        matrix,
                        false
                    )
                )
            }
        }
    }

    /*private fun chooser() {
        Intent.createChooser(pickIntent, "사진을 가져올 앱을 선택해 주세요").run { // pickIntent 추가
            if (pictureIntent.resolveActivity(packageManager) != null) {//pictureIntent 추가
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pictureIntent))
            }
            startActivityForResult(this, REQ_GET_IMG)
        }
    }*/
}

private fun getExifOrientation(imgFilePath: String): Float {
    val ei = ExifInterface(imgFilePath)
    val orientation: Int =
        ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
}


