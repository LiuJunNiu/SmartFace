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
import com.arcsoft.face.FaceFeature
import com.arcsoft.face.LivenessInfo
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ymdt.cache.CacheHelper
import com.ymdt.face.model.DrawInfo
import com.ymdt.face.model.LiveFaceInfo
import com.ymdt.face.run.CompareVideoRunnable
import com.ymdt.face.run.DetectVideoRunnable
import com.ymdt.face.utils.DrawHelper
import com.ymdt.face.utils.camera.CameraHelper
import com.ymdt.face.utils.camera.CameraListener
import kotlinx.android.synthetic.main.activity_compare.texture_view
import kotlinx.android.synthetic.main.activity_detect.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashMap

class CompareActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener {


    private lateinit var mCameraHelper: CameraHelper
    private val mDrawHelper = DrawHelper()

    private var mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT
    private lateinit var mCamera: Camera

    private var mInitCameraCompleted = false

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
    private lateinit var mCompareVideoRunnable: CompareVideoRunnable


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val attributes = window.attributes
        attributes.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION and View.SYSTEM_UI_FLAG_IMMERSIVE

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        setContentView(R.layout.activity_compare)

        texture_view.viewTreeObserver.addOnGlobalLayoutListener(this)

        switch_button.setOnClickListener {
            mCameraHelper.switchCamera()
        }

        mDetectAndExtractRunnable = DetectVideoRunnable(this, mQueue)

        mDetectAndExtractRunnable.setOnResult {
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

        mCompareVideoRunnable = CompareVideoRunnable(this)

        val faceMap = CacheHelper.getInstance().get(
            "FaceMap",
            object : TypeToken<Map<String, FaceFeature>>() {}.type
        ) as Map<String, FaceFeature>

        LogUtils.dTag("CompareActivity", Gson().toJson(faceMap))

        val map: HashMap<Any, FaceFeature> = HashMap<Any, FaceFeature>()
        faceMap.forEach { (t, u) -> map.put(t, u) }


        mCompareVideoRunnable.initQueue(mResultQueue)
            .initMap(map)
            .setOnResult { liveFaceInfo, faceSimilar, obj ->
                LogUtils.eTag(
                    "CompareActivity",
                    "对比人员通过" + faceSimilar.score
                )
            }


        Thread(mDetectAndExtractRunnable, "提取识别线程").start()
        Thread(mCompareVideoRunnable, "对比线程").start()
    }


    override fun onGlobalLayout() {
        initCamera()

    }

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


                mInitCameraCompleted = true
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

    override fun onResume() {
        super.onResume()
        if (mInitCameraCompleted) {
            mCameraHelper.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mInitCameraCompleted) {
            mCameraHelper.onPause()
        }
    }

    override fun onDestroy() {
        if (mInitCameraCompleted) {
            mCameraHelper.release()
        }
        super.onDestroy()
    }
}
