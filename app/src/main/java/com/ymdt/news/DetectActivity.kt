package com.ymdt.news

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Point
import android.hardware.Camera
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Size
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.arcsoft.face.LivenessInfo
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.ymdt.face.model.DrawInfo
import com.ymdt.face.model.LiveFaceInfo
import com.ymdt.face.run.DetectVideoRunnable
import com.ymdt.face.utils.DrawHelper
import com.ymdt.face.utils.camera.CameraHelper
import com.ymdt.face.utils.camera.CameraListener
import com.ymdt.news.dialog.SaveFaceInfoAlertDialog
import kotlinx.android.synthetic.main.activity_detect.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 拍照提取特征并保存
 */

class DetectActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener {

    private lateinit var mCameraHelper: CameraHelper
    private val mDrawHelper = DrawHelper()

    private var mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT
    private lateinit var mCamera: Camera

    /**
     * 非阻塞队列，存储相机预览，处理预览图片
     * 不要使用阻塞队列
     */
    private val mQueue: Queue<ByteArray> = ConcurrentLinkedQueue<ByteArray>()

    /**
     * 存储人脸队列最大个数
     */
    private val QUEUE_MAX_SIZE = 2

    /**
     * 提取出的结果
     * 不要使用阻塞队列
     */
    private val mResultQueue: Queue<LiveFaceInfo> = ConcurrentLinkedQueue<LiveFaceInfo>()

    private lateinit var mDetectAndExtractRunnable: DetectVideoRunnable


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val attributes = window.attributes
        attributes.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION and View.SYSTEM_UI_FLAG_IMMERSIVE
        window.attributes = attributes
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        setContentView(R.layout.activity_detect)


        texture_view.viewTreeObserver.addOnGlobalLayoutListener(this)
        switch_button.setOnClickListener {
            mInitCameraCompleted = false
            mCameraHelper.switchCamera()
        }


        mDetectAndExtractRunnable = DetectVideoRunnable(this, mQueue)

        mDetectAndExtractRunnable.setOnResult {
            LogUtils.dTag("TAG", Gson().toJson(it.faceFeature.featureData))
            if (mResultQueue.size <= QUEUE_MAX_SIZE) {
                mResultQueue.offer(it)
            } else {
                mResultQueue.poll()
            }
            val drawInfos: MutableList<DrawInfo> = ArrayList()
            val drawInfo = DrawInfo(
                mDrawHelper.adjustRect(it.faceInfo.getRect()),
                it.livenessInfo.getLiveness(),
                if (LivenessInfo.ALIVE == it.livenessInfo.getLiveness()) Color.GREEN else Color.GRAY
            )
            drawInfos.add(drawInfo)
            mDrawHelper.draw(face_rect_view, drawInfos)
        }


        Thread(mDetectAndExtractRunnable, "提取识别线程").start()
    }


    override fun onGlobalLayout() {
        LogUtils.dTag("DetectActivity", "initCamera")

        initCamera()

        capture_button.setOnClickListener {
            //提取此时的人脸特征
            if (mResultQueue.isNotEmpty()) {
                val liveFaceInfo = mResultQueue.peek()
                LogUtils.dTag("DetectActivity", Gson().toJson(liveFaceInfo.faceFeature.featureData))
                //弹窗保存该人员特征信息
                val dialog = SaveFaceInfoAlertDialog(DetectActivity@ this)
                dialog.show()
                dialog.showData(liveFaceInfo)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mInitCameraCompleted && mCameraHelper.isStopped) {
            mCameraHelper.start()
        }

    }

    override fun onPause() {
        super.onPause()
        if (mInitCameraCompleted && !mCameraHelper.isStopped) {
            mCameraHelper.stop()
        }
    }

    override fun onDestroy() {
        mDetectAndExtractRunnable.stop()
        super.onDestroy()
    }

    private var mInitCameraCompleted = false


    private fun initCamera() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val cameraListener = object : CameraListener {
            override fun onCameraError(e: Exception?) {

            }

            override fun onPreview(data: ByteArray?, camera: Camera?) {
                if (mQueue.size <= QUEUE_MAX_SIZE && null != data && data.isNotEmpty()) {
                    mQueue.offer(data)
                } else {
                    mQueue.poll()
                }
            }

            override fun onCameraConfigurationChanged(cameraID: Int, displayOrientation: Int) {

            }

            override fun onCameraOpened(
                camera: Camera?,
                cameraId: Int,
                displayOrientation: Int,
                isMirror: Boolean
            ) {
                mCamera = camera!!
                mCameraId = cameraId
                mInitCameraCompleted = true


                val previewSize = camera?.parameters?.previewSize
                mDetectAndExtractRunnable.initPreviewSize(
                    Size(
                        previewSize!!.width,
                        previewSize.height
                    )
                )

                mDrawHelper.initPreviewSize(Size(previewSize.width, previewSize.height))
                    .initCanvasSize(Size(texture_view.width, texture_view.height))
                    .initOrientation(displayOrientation)
                    .initCameraId(cameraId)


            }

            override fun onCameraClosed() {
                mInitCameraCompleted = false
            }

        }

        mCameraHelper = CameraHelper.Builder()
            .previewViewSize(Point(texture_view.measuredWidth, texture_view.measuredHeight))
            .rotation(windowManager.defaultDisplay.rotation)
            .specificCameraId(mCameraId)
            .isMirror(false)
            .previewOn(texture_view)
            .cameraListener(cameraListener)
            .build()
        mCameraHelper.init()
        mCameraHelper.start()


    }
}
