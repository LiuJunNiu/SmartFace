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
import com.ymdt.face.EngineType
import com.ymdt.face.FaceEngineHelper
import com.ymdt.face.model.DrawInfo
import com.ymdt.face.utils.DrawHelper
import com.ymdt.face.utils.camera.CameraHelper
import com.ymdt.face.utils.camera.CameraListener
import kotlinx.android.synthetic.main.activity_preview.*

/**
 * 人脸识别预览绘制人脸框
 */
class PreviewActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener {

    private lateinit var mFaceEngineHelper: FaceEngineHelper
    private val mDrawHelper = DrawHelper()

    private lateinit var mCameraHelper: CameraHelper
    private var mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val attributes = window.attributes
        attributes.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION and View.SYSTEM_UI_FLAG_IMMERSIVE
        window.attributes = attributes
        // Activity启动后就锁定为启动时的方向
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        setContentView(R.layout.activity_preview)
        mFaceEngineHelper = FaceEngineHelper(this).initEngine(EngineType.VIDEO_ENGINE)
        texture_view.viewTreeObserver.addOnGlobalLayoutListener(this)

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
                face_rect_view.clearFaceInfo()
                mFaceEngineHelper.initNV21(data)
                val faceInfo = mFaceEngineHelper.detect()
                val drawInfos = ArrayList<DrawInfo>()
                drawInfos.add(DrawInfo(mDrawHelper.adjustRect(faceInfo.rect), 1, Color.GREEN))
                mDrawHelper.draw(face_rect_view, drawInfos)


            }

            override fun onCameraConfigurationChanged(cameraID: Int, displayOrientation: Int) {

            }

            override fun onCameraOpened(
                camera: Camera?,
                cameraId: Int,
                displayOrientation: Int,
                isMirror: Boolean
            ) {
                mCameraId = cameraId

                val previewSize = camera?.parameters?.previewSize
                mDrawHelper.initPreviewSize(Size(previewSize!!.width, previewSize.height))
                    .initCanvasSize(Size(texture_view.width, texture_view.height))
                    .initOrientation(displayOrientation)
                    .initCameraId(cameraId)
                mFaceEngineHelper.initPreviewSize(Size(previewSize?.width, previewSize?.height))

            }

            override fun onCameraClosed() {

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
