package com.ymdt.news.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.arcsoft.face.FaceFeature
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.ymdt.cache.CacheHelper
import com.ymdt.face.model.LiveFaceInfo
import com.ymdt.face.utils.NV21ToBitmap
import com.ymdt.news.R

/**
 * @des
 * 保存人员特征对话框
 * @date 2019/11/7 4:26 PM
 * @author niu
 */
class SaveFaceInfoAlertDialog(context: Context) : AlertDialog(context) {

    private lateinit var mLiveFaceInfo: LiveFaceInfo


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_save_face_info)
        findViewById<View>(R.id.btn_save).setOnClickListener {
            val faceMap = HashMap<String, FaceFeature>()
            faceMap.put("刘俊", mLiveFaceInfo.faceFeature)
            LogUtils.dTag("SaveFaceInfo", Gson().toJson(faceMap))
            CacheHelper.getInstance().put("FaceMap", faceMap)
            dismiss()
        }
    }

    fun showData(liveFaceInfo: LiveFaceInfo) {
        mLiveFaceInfo = liveFaceInfo
        val nv21ToBitmap = NV21ToBitmap(context).nv21ToBitmap(
            liveFaceInfo.data,
            liveFaceInfo.size.width,
            liveFaceInfo.size.height
        )
        val matrix = Matrix()
        matrix.postRotate(90f)

        // 创建新的图片
        val resizedBitmap = Bitmap.createBitmap(
            nv21ToBitmap, 0, 0,
            nv21ToBitmap.getWidth(), nv21ToBitmap.getHeight(), matrix, true
        )

        findViewById<ImageView>(R.id.iv_face).setImageBitmap(resizedBitmap)

    }
}