package com.example.imageuploadtest

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.example.imageuploadtest.databinding.ActivityMainBinding


//TODO 촬영 이미지 저장공간 - 공용 저장
//TODO 촬영 이미지 저장공간 - 캐시 사용
//TODO 촬영 안하고 뒤로가기 시, 만들어져 있는 빈 파일 삭제
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvGallery.setOnClickListener { pickImg() }
        binding.tvCamera.setOnClickListener { takePicture() }
        binding.tvCameraOriginSize.setOnClickListener { takePictureFullSize() }
    }

    /**
     * Call 1)갤러리 이미지
     */
    private fun pickImg() {
        startActivityForResult(pickIntent, REQ_SELECT_IMG)
    }

    /**
     * Call 2)카메라 촬영 이미지 (썸네일 크기의 작은 이미지)
     */
    private fun takePicture() {
        pictureIntent.resolveActivity(packageManager)?.also {
            startActivityForResult(pictureIntent, REQ_IMG_CAPTURE)
        }
    }

    /**
     * Call 3)카메라 촬영 이미지 (원본 Full-Size)
     */
    private fun takePictureFullSize() {
        val fullSizePictureIntent = IntentMaker.getFullSizePictureIntent(applicationContext)
        fullSizePictureIntent.resolveActivity(packageManager)?.also {
            startActivityForResult(fullSizePictureIntent, REQ_IMG_CAPTURE_FULL_SIZE)
        }
    }

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
                    val currentImageUri = intent?.data ?: return

                    val needResize = true
                    if (needResize) {
                        setAdjustedUriAdjusted(currentImageUri)
                    } else {
                        setImgUri(currentImageUri)
                    }
                }

                REQ_IMG_CAPTURE -> {
                    val extras = intent?.extras
                    val bitmap = extras?.get("data") as Bitmap?
                    Log.w(
                        "syTest",
                        "[onActivityResult] intent = $intent, Bitmap W/H = ${bitmap?.width}/${bitmap?.height}"
                    )
                    binding.ivImg.setImageBitmap(bitmap)
                }

                REQ_IMG_CAPTURE_FULL_SIZE -> {
                    setAdjustedImgFilePath(IntentMaker.currentPhotoPath)
                }
            }
        }

    }


    /**
     * Set Img 1) Uri를 사용해 이미지 셋 (Full-Size)
     */
    private fun setImgUri(imgUri: Uri) {
        imgUri.let {
            val bitmap: Bitmap
            if (Build.VERSION.SDK_INT < 28) {
                bitmap = MediaStore.Images.Media.getBitmap(
                    this.contentResolver,
                    imgUri
                )
                binding.ivImg.setImageBitmap(bitmap)
            } else {
                val source =
                    ImageDecoder.createSource(this.contentResolver, imgUri)
                bitmap = ImageDecoder.decodeBitmap(source)
                binding.ivImg.setImageBitmap(bitmap)
            }
            Log.i("syTest", "Bitmap W/H = " + bitmap.width + " / " + bitmap.height)
        }
    }

    /**
     * Set Img 2) Uri를 사용해 이미지 셋 (Resize + Rotate가 필요할 경우 사용)
     */
    private fun setAdjustedUriAdjusted(imgUri: Uri) {
        //1)회전할 각도 구하기
        var degrees = 0f
        contentResolver.openInputStream(imgUri)?.use { inputStream ->
            degrees = getExifDegrees(ExifInterface(inputStream))
        }

        //2)Resizing 할 BitmapOption 만들기
        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true
            contentResolver.openInputStream(imgUri)?.use { inputStream ->
                //get img dimension
                BitmapFactory.decodeStream(inputStream, null, this)
            }

            // Determine how much to scale down the image
            val targetW: Int = 1000 //in pixel
            val targetH: Int = 1000 //in pixel
            val scaleFactor: Int = Math.min(outWidth / targetW, outHeight / targetH)
            Log.d("syTest", "target W/H : $targetW/$targetH")
            Log.d("syTest", "photo W/H : $outWidth/$outHeight")
            Log.d("syTest", "photo scaleFactor: $scaleFactor")


            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }

        //3) Bitmap 생성 및 셋팅 (resized + rotated)
        contentResolver.openInputStream(imgUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, bmOptions)?.also { bitmap ->
                Log.d(
                    "syTest",
                    "photo resized bitmap : " + bitmap.width + "/" + bitmap.height + ", LENGTH = " + bitmap
                )
                val matrix = Matrix()
                matrix.preRotate(degrees, 0f, 0f)
                binding.ivImg.setImageBitmap(
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
                )
            }
        }
    }

    /**
     * Set Img 3) FilePath 사용해 이미지 셋(Resize + Rotate가 필요할 경우 사용)
     */
    private fun setAdjustedImgFilePath(filePath: String) {
        //1)회전할 각도 구하기
        var degrees = getExifDegrees(ExifInterface(filePath))

        //2)Resizing 할 BitmapOption 만들기
        val bmOptions = BitmapFactory.Options().apply {

            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            //get img dimension
            BitmapFactory.decodeFile(
                filePath,
                this
            )

            // Determine how much to scale down the image
            val targetW: Int = 1000 //in pixel
            val targetH: Int = 1000 //in pixel
            val scaleFactor: Int = Math.min(outWidth / targetW, outHeight / targetH)
            Log.d("syTest", "target W/H : $targetW/$targetH")
            Log.d("syTest", "photo W/H : $outWidth/$outHeight")
            Log.d("syTest", "photo scaleFactor: $scaleFactor")

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }

        //3) Bitmap 생성 및 셋팅 (resized + rotated)
        BitmapFactory.decodeFile(filePath, bmOptions)?.also { bitmap ->
            Log.d(
                "syTest",
                "photo resized bitmap : " + bitmap.width + "/" + bitmap.height + ", LENGTH = " + bitmap
            )
            //rotate
            val matrix = Matrix()
            matrix.preRotate(degrees, 0f, 0f)
            binding.ivImg.setImageBitmap(
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
            )
        }
    }
}

private fun getExifDegrees(ei: ExifInterface): Float {
    val orientation: Int =
        ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
}

