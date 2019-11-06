package com.ymdt.news

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Point
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Size
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import com.ymdt.face.EngineType
import com.ymdt.face.FaceEngineHelper
import com.ymdt.face.model.DrawInfo
import com.ymdt.face.utils.DrawHelper
import com.ymdt.face.utils.camera.CameraHelper
import com.ymdt.face.utils.camera.CameraListener
import kotlinx.android.synthetic.main.activity_detect.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 拍照提取特征并保存
 */
const val TAKE_PICTURE_MESSAGE = 11

class DetectActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener {

    private lateinit var mFaceEngineHelper: FaceEngineHelper
    private lateinit var mDetectEngineHelper: FaceEngineHelper
    private val mDrawHelper = DrawHelper()
    private lateinit var mCameraHelper: CameraHelper
    private var mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT
    private lateinit var mCamera: Camera

    private val mTakePictureThread = HandlerThread("TakePictureThread")
    private lateinit var mTakeHandler: Handler

    private val mHandler = Handler()

    /**
     * 非阻塞队列，存储相机预览，处理预览图片
     */
    private val mQueue: Queue<ByteArray> = ConcurrentLinkedQueue<ByteArray>()
    /**
     * 是否识别绘制人脸框
     */
    @Volatile
    private var mDetectAndDraw = true


    private val mFaceThread = Thread {
        mFaceEngineHelper = FaceEngineHelper(this).initEngine(EngineType.VIDEO_ENGINE)
        while (mDetectAndDraw) {
            if (mQueue.isNotEmpty()) {
                val data = mQueue.poll()
                mFaceEngineHelper.initNV21(data)
                val liveFaceInfo = mFaceEngineHelper.detect()
                val drawInfos = ArrayList<DrawInfo>()
                drawInfos.add(
                    DrawInfo(
                        mDrawHelper.adjustRect(liveFaceInfo.rect),
                        liveFaceInfo.livenessInfo.liveness,
                        if (liveFaceInfo.livenessInfo.liveness == 1) Color.GREEN else Color.GRAY
                    )
                )

                mHandler.post { mDrawHelper.draw(face_rect_view, drawInfos) }
                LogUtils.dTag("mFaceThread:", mQueue.size)

            }
        }
        mFaceEngineHelper.uninit()

    }


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

        mDetectEngineHelper = FaceEngineHelper(this).initEngine(EngineType.IMAGE_ENGINE)

        texture_view.viewTreeObserver.addOnGlobalLayoutListener(this)
        switch_button.setOnClickListener {
            mInitCameraCompleted = false
            mCameraHelper.switchCamera()
        }

        mTakePictureThread.start()
        mTakeHandler = Handler(mTakePictureThread.looper) {
            if (TAKE_PICTURE_MESSAGE == it.what) {
                mCamera.takePicture(null, Camera.PictureCallback { data, camera ->
                    //提取特征
                    mDetectEngineHelper.initNV21(data)
                    val faceFeature = mDetectEngineHelper.extract()
                    LogUtils.dTag("takePicture:" + faceFeature.featureData)
                }, null)

            }
            true

        }

        capture_button.setOnClickListener {
            mTakeHandler.sendEmptyMessage(TAKE_PICTURE_MESSAGE)
        }

        mFaceThread.start()
    }


    override fun onGlobalLayout() {
        initCamera()
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
        mDetectAndDraw = false
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
                if (mQueue.size <= 2 && null != data && data.isNotEmpty()) {
                    mQueue.add(data)
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
                mDrawHelper.initPreviewSize(Size(previewSize!!.width, previewSize.height))
                    .initCanvasSize(Size(texture_view.width, texture_view.height))
                    .initOrientation(displayOrientation)
                    .initCameraId(cameraId)
                mFaceEngineHelper.initPreviewSize(Size(previewSize?.width, previewSize?.height))
                mDetectEngineHelper.initPreviewSize(Size(previewSize?.width, previewSize?.height))
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
