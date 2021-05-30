package com.example.carletonkart

import android.content.Context
import android.content.res.AssetManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class HumanDetector : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private var mRgba: Mat? = null
    private var mOpenCvCameraView: CameraBridgeViewBase? = null
    private var net: Net? = null
    private val classNames = arrayOf(
        "background",
        "aeroplane", "bicycle", "bird", "boat",
        "bottle", "bus", "car", "cat", "chair",
        "cow", "diningtable", "dog", "horse",
        "motorbike", "person", "pottedplant",
        "sheep", "sofa", "train", "tvmonitor"
    )
    private var counter = 0

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
        setContentView(R.layout.activity_human_detector)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        OpenCVLoader.initDebug()

        mOpenCvCameraView = findViewById<CameraBridgeViewBase>(R.id.human_detector_view)
        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)

    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
        val proto: String = getPath("deploy.prototxt", this)
        Log.i("proto", proto)
        val weights: String = getPath("weights.caffemodel", this)
        Log.i("weights", weights)
        net = Dnn.readNetFromCaffe(proto, weights)
        Log.i("Start", "Network loaded successfully")
    }

    override fun onCameraViewStopped() {
        mRgba!!.release()
    }

    override fun onCameraFrame(frame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val IN_WIDTH = 300.0
        val IN_HEIGHT = 300.0
        val WH_RATIO = IN_WIDTH.toFloat() / IN_HEIGHT
        val IN_SCALE_FACTOR: Double = 0.007843
        val MEAN_VAL = 127.5
        val THRESHOLD = 0.2
        mRgba = frame!!.rgba()

        Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGBA2RGB)

        val blob = Dnn.blobFromImage(
            mRgba, IN_SCALE_FACTOR,
            Size(IN_WIDTH, IN_HEIGHT),
            Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL),  /*swapRB*/false,  /*crop*/false
        )
        net!!.setInput(blob)

        //val layersNames = net!!.layerNames

        var detections: Mat = net!!.forward()

        val cols: Int = mRgba!!.cols()
        val rows: Int = mRgba!!.rows()

        //for(i in detections)

        Log.i("col,row", "$cols $rows " + detections.total() + " " + detections.rows() + " " + detections.cols())

        detections = detections.reshape(1, (detections.total() / 7).toInt())

        for (i in 0 until detections.rows()) {
            val confidence = detections[i, 2][0]
            if (confidence > THRESHOLD) {
                val classId = detections[i, 1][0].toInt()
                val left = (detections[i, 3][0] * cols)
                val top = (detections[i, 4][0] * rows)
                val right = (detections[i, 5][0] * cols)
                val bottom = (detections[i, 6][0] * rows)
                // Draw rectangle around detected object.
                Imgproc.rectangle(
                    mRgba, Point(left, top), Point(right, bottom),
                    Scalar(0.0, 255.0, 0.0)
                )
                val label: String = classNames[classId] + ": " + confidence
                val baseLine = IntArray(1)
                val labelSize =
                    Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine)
                // Draw background for label.
                Imgproc.rectangle(
                    mRgba, Point(left, top - labelSize.height),
                    Point(left + labelSize.width, top + baseLine[0]),
                    Scalar(255.0, 255.0, 255.0), 5
                )
                // Write class name and confidence.
                Imgproc.putText(
                    mRgba, label, Point(left, top),
                    Core.FONT_HERSHEY_SIMPLEX, 0.5, Scalar(0.0, 0.0, 0.0)
                )
            }
        }
        return mRgba!!

    }

    private fun getPath(file: String, context: Context): String{
        val assetManager: AssetManager = context.assets

        var inputStream: BufferedInputStream? = null
        try {
            // Read data from assets.
            inputStream = BufferedInputStream(assetManager.open(file))
            val data = ByteArray(inputStream.available())
            inputStream.read(data)
            inputStream.close()
            // Create copy file in storage.
            val outFile = File(context.filesDir, file)
            val os = FileOutputStream(outFile)
            os.write(data)
            os.close()
            // Return a path to file which may be read in common way.
            return outFile.absolutePath
        } catch (ex: IOException) {
            Log.i("getPath", "Failed to upload a file")
        }
        return ""

    }
}