package com.ymdt.news

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ToastUtils
import com.ymdt.face.EngineType
import com.ymdt.face.FaceEngineHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSION_CODE = 10
    private val REQUEST_PERMISSIONS =
        arrayOf(android.Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_STATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permission_btn.setOnClickListener {
            //申请权限
            if (allPermissionGranted()) {
                ToastUtils.showShort("权限都允许")
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    REQUEST_PERMISSIONS,
                    REQUEST_PERMISSION_CODE
                )

            }
        }

        active_btn.setOnClickListener {
            val faceEngineHelper = FaceEngineHelper(baseContext).initEngine(EngineType.VIDEO_ENGINE)
            val active = faceEngineHelper.active()
            if (active) {
                ToastUtils.showShort("人脸引擎已激活")
            } else {
                ToastUtils.showShort("激活失败")
            }
        }

        preview_btn.setOnClickListener {
            val intent = Intent(this@MainActivity, PreviewActivity::class.java)
            startActivity(intent)
        }

        detect_btn.setOnClickListener {
            val intent = Intent(baseContext, DetectActivity::class.java)
            startActivity(intent)
        }
        compare_btn.setOnClickListener {
            val intent = Intent(baseContext, CompareActivity::class.java)
            startActivity(intent)
        }
    }

    private fun allPermissionGranted() = REQUEST_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (REQUEST_PERMISSION_CODE == requestCode) {
            if (allPermissionGranted()) {
                ToastUtils.showShort("权限都允许")
            } else {
                ToastUtils.showShort("没有权限")
            }
        }
    }
}
