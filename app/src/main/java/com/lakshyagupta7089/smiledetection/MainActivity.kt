package com.lakshyagupta7089.smiledetection

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import com.google.mlkit.vision.face.FaceContour.FACE
import com.lakshyagupta7089.smiledetection.databinding.ActivityMainBinding
import com.theartofdev.edmodo.cropper.CropImage

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var imageView: ImageView
    private lateinit var bottomSheetRecyclerView: RecyclerView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var faceDetectionModelList: ArrayList<FaceDetectionModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        faceDetectionModelList = ArrayList()
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))

        imageView = findViewById(R.id.face_detection_image_view)

        val bottomSheet: FrameLayout = findViewById(R.id.bottom_sheet_button)
        bottomSheetRecyclerView = findViewById(R.id.bottom_sheet_recycler_view)
        bottomSheet.setOnClickListener {
            startCropImageActivity()
        }

        bottomSheetRecyclerView.layoutManager = LinearLayoutManager(this)
        bottomSheetRecyclerView.adapter = FaceDetectionAdapter(
            this,
            faceDetectionModelList
        )
    }

    private fun startCropImageActivity() {
        CropImage.activity()
            .start(this);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                val resultUri: Uri = result.uri

                analyseImage(MediaStore.Images.Media.getBitmap(contentResolver, resultUri))

                makeToast("this is successful working fine!")
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
                makeToast("this is error -> $error")
            }
        }
    }

    private fun analyseImage(bitmap: Bitmap?) {
        if (bitmap == null) {
            makeToast("There was an error")
            return
        }

        imageView.setImageBitmap(null)
        faceDetectionModelList.clear()

        bottomSheetRecyclerView.adapter?.notifyDataSetChanged()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        showProgress()
//        val firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap)
//        val faceDetectionOption = FirebaseVisionFaceDetectorOptions.Builder()
//            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
//            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
//            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
//            .build()
//        val faceDetector = FirebaseVision.getInstance()
//            .getVisionFaceDetector(faceDetectionOption)
//
//        faceDetector.detectInImage(firebaseVisionImage)
//            .addOnSuccessListener {
//                for (i in it) {
//                    makeToast(i.leftEyeOpenProbability.toString())
//                    hideProgress()
//                }
//            }

        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        // Real-time contour detection
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        val faceDetectionOption = FaceDetection.getClient(highAccuracyOpts)
        val image = InputImage.fromBitmap(bitmap, 0)

        faceDetectionOption.process(image)
            .addOnSuccessListener { faces ->
                val image = bitmap.copy(Bitmap.Config.ARGB_8888, true)

                detectFaces(faces, image)
            }
            .addOnFailureListener {

            }
    }

    private fun detectFaces(faces: List<Face>?, bitmap: Bitmap?) {
        if (faces == null || bitmap == null) {
            makeToast("There was an error")
        }
//
        val canvas = Canvas(bitmap!!)
        val facePaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        val faceTextPaint = Paint().apply {
            color = Color.BLUE
            textSize = 200f
            typeface = Typeface.SANS_SERIF
        }

        val landmarkPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            strokeWidth = 8f
        }

        for (i in faces!!.indices) {
            val face = faces[i]
            canvas.drawRect(face.boundingBox, facePaint)
            canvas.drawText(
                "Face $i",
                ((face.boundingBox.centerX()) - (face.boundingBox.width()) / 2) + 8f,
                ((face.boundingBox.centerY()) + (face.boundingBox.height()) / 2) - 8f,
                faceTextPaint
            )

            if (face.getLandmark(FaceLandmark.LEFT_EYE) != null) {
                val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
                canvas.drawCircle(
                    leftEye?.position!!.x,
                    leftEye.position.y,
                    8f,
                    landmarkPaint
                )
            }

            if (face.getLandmark(FaceLandmark.RIGHT_EYE) != null) {
                val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
                canvas.drawCircle(
                    rightEye?.position!!.x,
                    rightEye.position.y,
                    8f,
                    landmarkPaint
                )
            }

            if (face.getLandmark(FaceLandmark.NOSE_BASE) != null) {
                val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
                canvas.drawCircle(
                    nose?.position!!.x,
                    nose.position.y,
                    8f,
                    landmarkPaint
                )
            }

            if (face.getLandmark(FaceLandmark.RIGHT_EAR) != null) {
                val rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR)
                canvas.drawCircle(
                    rightEar?.position!!.x,
                    rightEar.position.y,
                    8f,
                    landmarkPaint
                )
            }

            if (face.getLandmark(FaceLandmark.LEFT_EAR) != null) {
                val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
                canvas.drawCircle(
                    leftEar?.position!!.x,
                    leftEar.position.y,
                    8f,
                    landmarkPaint
                )
            }

            if (face.getLandmark(FaceLandmark.RIGHT_CHEEK) != null) {
                val rightCheek = face.getLandmark(FaceLandmark.RIGHT_CHEEK)
                canvas.drawCircle(
                    rightCheek?.position!!.x,
                    rightCheek.position.y,
                    8f,
                    landmarkPaint
                )
            }

            if (face.getLandmark(FaceLandmark.LEFT_CHEEK) != null) {
                val leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK)
                canvas.drawCircle(
                    leftCheek?.position!!.x,
                    leftCheek.position.y,
                    8f,
                    landmarkPaint
                )
            }

            faceDetectionModelList.add(
                FaceDetectionModel(
                    i,
                    "Smiling Probability ${face.smilingProbability}%"
                )
            )

            faceDetectionModelList.add(
                FaceDetectionModel(
                    i,
                    "Left Eye Open ${face.leftEyeOpenProbability}%"
                )
            )

            faceDetectionModelList.add(
                FaceDetectionModel(
                    i,
                    "Right Eye Open ${face.rightEyeOpenProbability}%"
                )
            )

            bottomSheetRecyclerView.adapter?.notifyDataSetChanged()

        }

        val dotPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            strokeWidth = 6f
        }

        val linePaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        for (face in faces) {
            // Get all points
            val faceContours = face.getContour(FACE)?.points

            if (faceContours != null) {
                for (i in faceContours?.indices!!) {
                    val faceContour = faceContours[i]

                    if (i != (faceContours.size - 1)) {
                        canvas.drawLine(
                            faceContour.x,
                            faceContour.y,
                            faceContours[i + 1].x,
                            faceContours[i + 1].y,
                            linePaint
                        )
                    } else {
                        canvas.drawLine(
                            faceContour.x,
                            faceContour.y,
                            faceContours[0].x,
                            faceContours[0].y,
                            linePaint
                        )
                    }

                    canvas.drawCircle(faceContour.x, faceContour.y, 4f, dotPaint)
                }
            }

            val leftEyeContours = face.getContour(FaceContour.LEFT_EYE)?.points

            if (leftEyeContours != null) {
                for (i in leftEyeContours?.indices!!) {
                    val leftEyeContour = leftEyeContours[i]

                    if (i != (leftEyeContours.size - 1)) {
                        canvas.drawLine(
                            leftEyeContour.x,
                            leftEyeContour.y,
                            leftEyeContours[i + 1].x,
                            leftEyeContours[i + 1].y,
                            linePaint
                        )
                    } else {
                        canvas.drawLine(
                            leftEyeContour.x,
                            leftEyeContour.y,
                            leftEyeContours[0].x,
                            leftEyeContours[0].y,
                            linePaint
                        )
                    }

                    canvas.drawCircle(leftEyeContour.x, leftEyeContour.y, 4f, dotPaint)
                }
            }

            val rightEyeContours = face.getContour(FaceContour.RIGHT_EYE)?.points

            if (rightEyeContours != null) {
                for (i in rightEyeContours?.indices!!) {
                    val rightEyeContour = rightEyeContours[i]

                    if (i != (rightEyeContours.size - 1)) {
                        canvas.drawLine(
                            rightEyeContour.x,
                            rightEyeContour.y,
                            rightEyeContours[i + 1].x,
                            rightEyeContours[i + 1].y,
                            linePaint
                        )
                    } else {
                        canvas.drawLine(
                            rightEyeContour.x,
                            rightEyeContour.y,
                            rightEyeContours[0].x,
                            rightEyeContours[0].y,
                            linePaint
                        )
                    }

                    canvas.drawCircle(rightEyeContour.x, rightEyeContour.y, 4f, dotPaint)
                }
            }

            val noseBridgeContours = face.getContour(FaceContour.NOSE_BRIDGE)?.points

            if (noseBridgeContours != null) {
                for (i in noseBridgeContours.indices) {
                    val noseBridgeContour = noseBridgeContours[i]

                    if (i != (noseBridgeContours.size - 1)) {
                        canvas.drawLine(
                            noseBridgeContour.x,
                            noseBridgeContour.y,
                            noseBridgeContours[i + 1].x,
                            noseBridgeContours[i + 1].y,
                            linePaint
                        )
                    } else {
                        canvas.drawLine(
                            noseBridgeContour.x,
                            noseBridgeContour.y,
                            noseBridgeContour.x,
                            noseBridgeContour.y,
                            linePaint
                        )
                    }

                    canvas.drawCircle(noseBridgeContour.x, noseBridgeContour.y, 4f, dotPaint)
                }
            }

            val noseBottomContours = face.getContour(FaceContour.NOSE_BOTTOM)?.points

            if (noseBottomContours != null) {
                for (i in noseBottomContours.indices) {
                    val noseBottomContour = noseBottomContours[i]

                    if (i != (noseBottomContours.size - 1)) {
                        canvas.drawLine(
                            noseBottomContour.x,
                            noseBottomContour.y,
                            noseBottomContours[i + 1].x,
                            noseBottomContours[i + 1].y,
                            linePaint
                        )
                    } else {
                        canvas.drawLine(
                            noseBottomContour.x,
                            noseBottomContour.y,
                            noseBottomContour.x,
                            noseBottomContour.y,
                            linePaint
                        )
                    }

                    canvas.drawCircle(noseBottomContour.x, noseBottomContour.y, 4f, dotPaint)
                }
            }

            val leftEyebrowTopContours = face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.points

            if (leftEyebrowTopContours != null) {
                for (i in leftEyebrowTopContours.indices) {
                    val leftEyebrowTopContour = leftEyebrowTopContours[i]

                    if (i != (leftEyebrowTopContours.size - 1)) {
                        canvas.drawLine(
                            leftEyebrowTopContour.x,
                            leftEyebrowTopContour.y,
                            leftEyebrowTopContours[i + 1].x,
                            leftEyebrowTopContours[i + 1].y,
                            linePaint
                        )
                    } else {
                        canvas.drawLine(
                            leftEyebrowTopContour.x,
                            leftEyebrowTopContour.y,
                            leftEyebrowTopContour.x,
                            leftEyebrowTopContour.y,
                            linePaint
                        )
                    }

                    canvas.drawCircle(leftEyebrowTopContour.x, leftEyebrowTopContour.y, 4f, dotPaint)
                }
            }

            val leftEyebrowBottomContours = face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM)?.points

            if (leftEyebrowBottomContours != null) {
                for (i in leftEyebrowBottomContours.indices) {
                    val leftEyebrowBottomContour = leftEyebrowBottomContours[i]

                    if (i != (leftEyebrowBottomContours.size - 1)) {
                        canvas.drawLine(
                            leftEyebrowBottomContour.x,
                            leftEyebrowBottomContour.y,
                            leftEyebrowBottomContours[i + 1].x,
                            leftEyebrowBottomContours[i + 1].y,
                            linePaint
                        )
                    } else {
                        canvas.drawLine(
                            leftEyebrowBottomContour.x,
                            leftEyebrowBottomContour.y,
                            leftEyebrowBottomContour.x,
                            leftEyebrowBottomContour.y,
                            linePaint
                        )
                    }

                    canvas.drawCircle(leftEyebrowBottomContour.x, leftEyebrowBottomContour.y, 4f, dotPaint)
                }
            }

            val rightEyebrowTopContours = face.getContour(FaceContour.RIGHT_EYEBROW_TOP)?.points

            if (rightEyebrowTopContours != null) {
                for (i in rightEyebrowTopContours.indices) {
                    val rightEyebrowTopContour = rightEyebrowTopContours[i]

                    if (i != (rightEyebrowTopContours.size - 1)) {
                        canvas.drawLine(
                            rightEyebrowTopContour.x,
                            rightEyebrowTopContour.y,
                            rightEyebrowTopContours[i + 1].x,
                            rightEyebrowTopContours[i + 1].y,
                            linePaint
                        )
                    } else {
                        canvas.drawLine(
                            rightEyebrowTopContour.x,
                            rightEyebrowTopContour.y,
                            rightEyebrowTopContour.x,
                            rightEyebrowTopContour.y,
                            linePaint
                        )
                    }

                    canvas.drawCircle(rightEyebrowTopContour.x, rightEyebrowTopContour.y, 4f, dotPaint)
                }
            }

            val rightEyebrowBottomContours = face.getContour(FaceContour.RIGHT_EYEBROW_BOTTOM)?.points

            if (rightEyebrowBottomContours != null) {
                for (i in rightEyebrowBottomContours.indices) {
                    val rightEyebrowBottomContour = rightEyebrowBottomContours[i]

                    if (i != (rightEyebrowBottomContours.size - 1)) {
                        canvas.drawLine(
                            rightEyebrowBottomContour.x,
                            rightEyebrowBottomContour.y,
                            rightEyebrowBottomContours[i + 1].x,
                            rightEyebrowBottomContours[i + 1].y,
                            linePaint
                        )
                    } else {
                        canvas.drawLine(
                            rightEyebrowBottomContour.x,
                            rightEyebrowBottomContour.y,
                            rightEyebrowBottomContour.x,
                            rightEyebrowBottomContour.y,
                            linePaint
                        )
                    }

                    canvas.drawCircle(rightEyebrowBottomContour.x, rightEyebrowBottomContour.y, 4f, dotPaint)
                }
            }

            val upperLipTopContours = face.getContour(FaceContour.UPPER_LIP_TOP)?.points

            if (upperLipTopContours != null) {
                for (i in upperLipTopContours.indices) {
                    val upperLipTopContour = upperLipTopContours[i]

                    if (i != (upperLipTopContours.size - 1)) {
                        canvas.drawLine(
                            upperLipTopContour.x,
                            upperLipTopContour.y,
                            upperLipTopContours[i + 1].x,
                            upperLipTopContours[i + 1].y,
                            linePaint
                        )
                    } else {
                        canvas.drawLine(
                            upperLipTopContour.x,
                            upperLipTopContour.y,
                            upperLipTopContour.x,
                            upperLipTopContour.y,
                            linePaint
                        )
                    }

                    canvas.drawCircle(upperLipTopContour.x, upperLipTopContour.y, 4f, dotPaint)
                }
            }

            val upperLipBottomContours = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points

            if (upperLipBottomContours != null) {
                for (i in upperLipBottomContours.indices) {
                    val upperLipBottomContour = upperLipBottomContours[i]

                    if (i != (upperLipBottomContours.size - 1)) {
                        canvas.drawLine(
                            upperLipBottomContour.x,
                            upperLipBottomContour.y,
                            upperLipBottomContours[i + 1].x,
                            upperLipBottomContours[i + 1].y,
                            linePaint
                        )
                    } else {
                        canvas.drawLine(
                            upperLipBottomContour.x,
                            upperLipBottomContour.y,
                            upperLipBottomContour.x,
                            upperLipBottomContour.y,
                            linePaint
                        )
                    }

                    canvas.drawCircle(upperLipBottomContour.x, upperLipBottomContour.y, 4f, dotPaint)
                }
            }

            val lowerLipTopContours = face.getContour(FaceContour.LOWER_LIP_TOP)?.points

            if (lowerLipTopContours != null) {
                for (i in lowerLipTopContours.indices) {
                    val lowerLipTopContour = lowerLipTopContours[i]

                    if (i != (lowerLipTopContours.size - 1)) {
                        canvas.drawLine(
                            lowerLipTopContour.x,
                            lowerLipTopContour.y,
                            lowerLipTopContours[i + 1].x,
                            lowerLipTopContours[i + 1].y,
                            linePaint
                        )
                    } else {
                        canvas.drawLine(
                            lowerLipTopContour.x,
                            lowerLipTopContour.y,
                            lowerLipTopContour.x,
                            lowerLipTopContour.y,
                            linePaint
                        )
                    }

                    canvas.drawCircle(lowerLipTopContour.x, lowerLipTopContour.y, 4f, dotPaint)
                }
            }

            val lowerLipBottomContours = face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points

            if (lowerLipBottomContours != null) {
                for (i in lowerLipBottomContours.indices) {
                    val lowerLipBottomContour = lowerLipBottomContours[i]

                    if (i != (lowerLipBottomContours.size - 1)) {
                        canvas.drawLine(
                            lowerLipBottomContour.x,
                            lowerLipBottomContour.y,
                            lowerLipBottomContours[i + 1].x,
                            lowerLipBottomContours[i + 1].y,
                            linePaint
                        )
                    } else {
                        canvas.drawLine(
                            lowerLipBottomContour.x,
                            lowerLipBottomContour.y,
                            lowerLipBottomContour.x,
                            lowerLipBottomContour.y,
                            linePaint
                        )
                    }

                    canvas.drawCircle(lowerLipBottomContour.x, lowerLipBottomContour.y, 4f, dotPaint)
                }
            }
        }

        canvas.save()
        imageView.setImageBitmap(bitmap)
        hideProgress()
    }

    private fun showProgress() {
        findViewById<ImageView>(
            R.id.bottom_sheet_button_image
        ).visibility = View.GONE

        findViewById<ProgressBar>(
            R.id.bottom_sheet_button_progressbar
        ).visibility = View.VISIBLE
    }

    private fun hideProgress() {
        findViewById<ImageView>(
            R.id.bottom_sheet_button_image
        ).visibility = View.VISIBLE

        findViewById<ProgressBar>(
            R.id.bottom_sheet_button_progressbar
        ).visibility = View.GONE
    }

    private fun makeToast(s: String) {
        Toast.makeText(
            this,
            s,
            Toast.LENGTH_SHORT
        ).show()
    }
}