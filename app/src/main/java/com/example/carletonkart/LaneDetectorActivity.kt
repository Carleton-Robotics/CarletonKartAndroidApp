package com.example.carletonkart

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import org.opencv.android.*
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class LaneDetectorActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private var mRgba: Mat? = null
    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i("TAG", "OpenCV loaded successfully")

                    mOpenCvCameraView!!.enableView()
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
        setContentView(R.layout.activity_lane_detector)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        OpenCVLoader.initDebug()

        mOpenCvCameraView = findViewById<CameraBridgeViewBase>(R.id.LaneDetectorView)
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

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() {
        mRgba!!.release()
    }

    override fun onCameraFrame(frame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        mRgba = frame!!.gray()
        val edges = Mat()
        Imgproc.Canny(mRgba, edges,60.0, 60.0, 3, false)
        val cannyColor = Mat()
        Imgproc.cvtColor(edges, cannyColor, Imgproc.COLOR_GRAY2BGR)
        val lines = Mat()
        Imgproc.HoughLines(edges, lines, 1.0, Math.PI/180, 150)
        for (i in 0 until lines.rows()) {
            Log.i("lines",lines.get(i,0).toString())
            val data: DoubleArray = lines.get(i, 0)
            val rho: Double = data[0]
            val theta: Double = data[1]
            val a: Double = cos(theta)
            val b: Double = sin(theta)
            val x0: Double = a*rho
            val y0: Double = b*rho
            val pt1 = Point()
            val pt2 = Point()
            pt1.x = (x0 + 1000 * (-b)).roundToInt().toDouble()
            pt1.y = (y0 + 1000 * (a)).roundToInt().toDouble()
            pt2.x = (x0 - 1000 * (-b)).roundToInt().toDouble()
            pt2.y = (y0 - 1000 * (a)).roundToInt().toDouble()
            Imgproc.line(cannyColor, pt1, pt2, Scalar(0.0, 0.0, 255.0), 3)
        }
        return cannyColor!!
    }
}