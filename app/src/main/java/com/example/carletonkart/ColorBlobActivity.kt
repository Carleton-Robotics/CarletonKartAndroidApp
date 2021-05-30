package com.example.carletonkart

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class ColorBlobActivity : AppCompatActivity(), View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {
    private var mIsColorSelected = false
    private var mRgba: Mat? = null
    private var mBlobColorRgba: Scalar? = null
    private var mBlobColorHsv: Scalar? = null
    private var mDetector: ColorBlobDetector? = null
    private var mSpectrum: Mat? = null
    private var SPECTRUM_SIZE: Size? = null
    private var CONTOUR_COLOR: Scalar? = null

    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    private val onTouch: View.OnTouchListener = this

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i("TAG", "OpenCV loaded successfully")

                    mOpenCvCameraView!!.enableView()
                    mOpenCvCameraView!!.setOnTouchListener(onTouch)
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_blob)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//        ActivityCompat.requestPermissions(
//            this@MainActivity,
//            arrayOf(Manifest.permission.CAMERA),
//            CAMERA_PERMISSION_REQUEST
//        )

        OpenCVLoader.initDebug()

        mOpenCvCameraView = findViewById<CameraBridgeViewBase>(R.id.color_blob_detection_activity_surface_view)

        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE

        mOpenCvCameraView!!.setCvCameraViewListener(this)

    }

    fun makeGray(bitmap: Bitmap) : Bitmap {

        // Create OpenCV mat object and copy content from bitmap
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)

        // Make a mutable bitmap to copy grayscale image
        val grayBitmap = bitmap.copy(bitmap.config, true)
        Utils.matToBitmap(mat, grayBitmap)

        return grayBitmap
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        when (requestCode) {
//            CAMERA_PERMISSION_REQUEST -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    mOpenCvCameraView!!.setCameraPermissionGranted()
//                } else {
//                    val message = "Camera permission was not granted"
//                    Log.e(TAG, message)
//                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
//                }
//            }
//            else -> {
//                Log.e(TAG, "Unexpected permission request")
//            }
//        }
//    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
        mDetector = ColorBlobDetector()
        mSpectrum = Mat()
        mBlobColorRgba = Scalar(255.0)
        mBlobColorHsv = Scalar(255.0)
        SPECTRUM_SIZE = Size(200.0, 64.0)
        CONTOUR_COLOR = Scalar(255.0, 0.0, 0.0, 255.0)
    }

    override fun onCameraViewStopped() {
        mRgba!!.release()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val cols = mRgba!!.cols()
        val rows = mRgba!!.rows()

        val xOffset = (mOpenCvCameraView!!.width - cols) / 2
        val yOffset = (mOpenCvCameraView!!.height - rows) / 2

        val x = event.x.toInt() - xOffset
        val y = event.y.toInt() - yOffset

        Log.i(TAG, "Touch image coordinates: ($x, $y)")

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false

        val touchedRect = Rect()

        touchedRect.x = if (x > 4) x - 4 else 0
        touchedRect.y = if (y > 4) y - 4 else 0

        touchedRect.width = if (x + 4 < cols) x + 4 - touchedRect.x else cols - touchedRect.x
        touchedRect.height = if (y + 4 < rows) y + 4 - touchedRect.y else rows - touchedRect.y

        val touchedRegionRgba = mRgba!!.submat(touchedRect)

        val touchedRegionHsv = Mat()
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL)

        // Calculate average color of touched region
        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv)
        val pointCount= touchedRect.width * touchedRect.height
        var indices: Int? = mBlobColorHsv!!.`val`.size-1
        for(i in 0..indices!!) {
            mBlobColorHsv!!.`val`[i] /= pointCount.toDouble()
        }

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv!!)

        Log.i(
            TAG,
            "Touched rgba color: (" + mBlobColorRgba!!.`val`[0] + ", " + mBlobColorRgba!!.`val`[1] +
                    ", " + mBlobColorRgba!!.`val`[2] + ", " + mBlobColorRgba!!.`val`[3] + ")"
        )

        mDetector!!.setHsvColor(mBlobColorHsv)

        Imgproc.resize(mDetector!!.spectrum, mSpectrum, SPECTRUM_SIZE, 0.0, 0.0, Imgproc.INTER_LINEAR_EXACT)

        mIsColorSelected = true

        touchedRegionRgba.release()
        touchedRegionHsv.release()

        return false // don't need subsequent touch events

    }

    override fun onCameraFrame(frame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        // get current camera frame as OpenCV Mat object
        val mat = frame.gray()

        mRgba = frame.rgba()

        if (mIsColorSelected) {
            mDetector!!.process(mRgba)
            val contours = mDetector!!.contours
            Log.e(TAG, "Contours count: " + contours.size)
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR)
            val colorLabel = mRgba!!.submat(4, 68, 4, 68)
            colorLabel.setTo(mBlobColorRgba)
            val spectrumLabel =
                mRgba!!.submat(4, 4 + mSpectrum!!.rows(), 70, 70 + mSpectrum!!.cols())
            mSpectrum!!.copyTo(spectrumLabel)
        }

        // native call to process current camera frame
        //adaptiveThresholdFromJNI(mat.nativeObjAddr)

        // return processed frame for live preview
        //return mat
        return mRgba!!
    }

    private fun converScalarHsv2Rgba(hsvColor: Scalar): Scalar? {
        val pointMatRgba = Mat()
        val pointMatHsv = Mat(1, 1, CvType.CV_8UC3, hsvColor)
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4)
        return Scalar(pointMatRgba[0, 0])
    }

    //private external fun adaptiveThresholdFromJNI(matAddr: Long)

    companion object {

        private const val TAG = "ColorBlobActivity"
        private const val CAMERA_PERMISSION_REQUEST = 1
    }


}