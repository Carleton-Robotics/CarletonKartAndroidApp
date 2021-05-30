package com.example.carletonkart

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lane_detection_btn.setOnClickListener {
            val laneIntent = Intent(this, LaneDetectorActivity::class.java)
            startActivity(laneIntent)
        }

        color_blob_btn.setOnClickListener {
            val blobIntent = Intent(this, ColorBlobActivity::class.java)
            startActivity(blobIntent)
        }

        human_detection_btn.setOnClickListener {
            val blobIntent = Intent(this, HumanDetector::class.java)
            startActivity(blobIntent)
        }

    }

}