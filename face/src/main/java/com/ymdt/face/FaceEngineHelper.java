package com.ymdt.face;

import android.content.Context;
import android.util.Size;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.FaceSimilar;
import com.arcsoft.face.LivenessInfo;
import com.ymdt.face.model.LiveFaceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author niu
 * @des 人脸识别
 * @date 2019/11/4 3:56 PM
 */
public class FaceEngineHelper {

    private Context mContext;
    private FaceEngine mFaceEngine;
    /**
     * 预览的图片尺寸
     */
    private Size mPreviewSize = new Size(0, 0);
    /**
     * 原始的人脸特征
     */
    private FaceFeature mOriginalFaceFeature = new FaceFeature();
    /**
     * 需要对比的人脸特征
     */
    private FaceFeature mComparedFaceFeature = new FaceFeature();

    /**
     * nv21图片数据
     */
    private byte[] mNV21 = new byte[0];

    /**
     * 是否要求活体检测
     */
    private volatile boolean mLiving = true;

    private static final int MASK = FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_LIVENESS | FaceEngine.ASF_FACE_RECOGNITION;

    public FaceEngineHelper(Context context) {
        this.mContext = context;
    }

    /**
     * 初始化一个引擎
     *
     * @param engineType 引擎类别
     */
    public FaceEngineHelper initEngine(EngineType engineType) {
        mFaceEngine = new FaceEngine();
        switch (engineType) {
            case IMAGE_ENGINE:
            case LIVING_ENGINE:
                //活体检测
                //提取、识别
                mFaceEngine.init(mContext,
                        FaceEngine.ASF_DETECT_MODE_IMAGE,
                        FaceEngine.ASF_OP_0_ONLY,
                        16, 1,
                        MASK);
                return this;
            case VIDEO_ENGINE:
                //只能负责提取，识别需要使用image_engine
                mFaceEngine.init(mContext,
                        FaceEngine.ASF_DETECT_MODE_VIDEO,
                        FaceEngine.ASF_OP_0_HIGHER_EXT,
                        16, 1,
                        MASK);
                return this;
            default:
                return this;
        }
    }

    public FaceEngineHelper initPreviewSize(Size size) {
        this.mPreviewSize = size;
        return this;
    }

    public FaceEngineHelper initNV21(byte[] nv21) {
        this.mNV21 = nv21;
        return this;
    }

    public FaceEngineHelper living(boolean living) {
        this.mLiving = living;
        return this;
    }

    public FaceEngineHelper initOriginalFaceFeature(FaceFeature faceFeature) {
        this.mOriginalFaceFeature = faceFeature;
        return this;
    }

    public FaceEngineHelper initComparedFaceFeature(FaceFeature faceFeature) {
        this.mComparedFaceFeature = faceFeature;
        return this;
    }

    /**
     * 激活，必须的
     *
     * @return ture激活，false激活失败
     */
    public boolean active() {
        int activeCode = mFaceEngine.activeOnline(mContext, Constants.APP_ID, Constants.SDK_KEY);
        return ErrorInfo.MOK == activeCode || ErrorInfo.MERR_ASF_ALREADY_ACTIVATED == activeCode;
    }


    public FaceSimilar similar() {
        FaceSimilar faceSimilar = new FaceSimilar();
        mFaceEngine.compareFaceFeature(mOriginalFaceFeature, mComparedFaceFeature, faceSimilar);
        return faceSimilar;
    }


    /**
     * 找到最大的人脸框
     *
     * @return
     */
    public LiveFaceInfo detect() {
        List<FaceInfo> faceInfoList = new ArrayList<>();
        mFaceEngine.detectFaces(mNV21, mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                FaceEngine.CP_PAF_NV21, faceInfoList);
        LiveFaceInfo liveFaceInfo = new LiveFaceInfo();
        if (faceInfoList.isEmpty()) {
            return liveFaceInfo;
        }
        FaceInfo faceInfo = faceInfoList.get(0);
        liveFaceInfo.setFaceInfo(faceInfo);
        if (mLiving) {
            mFaceEngine.process(mNV21, mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                    FaceEngine.CP_PAF_NV21, faceInfoList,
                    FaceEngine.ASF_LIVENESS);
            List<LivenessInfo> faceLivenessInfoList = new ArrayList<>();
            mFaceEngine.getLiveness(faceLivenessInfoList);
            if (faceLivenessInfoList.isEmpty()) {
                return liveFaceInfo;
            }
            LivenessInfo livenessInfo = faceLivenessInfoList.get(0);
            liveFaceInfo.setLivenessInfo(livenessInfo);
            return liveFaceInfo;
        }
        liveFaceInfo.setData(mNV21);
        liveFaceInfo.setSize(mPreviewSize);
        return liveFaceInfo;
    }


    /**
     * 提取人脸特征信息
     *
     * @return faceFeature
     */
    public FaceFeature extract() {
        LiveFaceInfo liveFaceInfo = detect();
        return extract(liveFaceInfo.getFaceInfo());
    }

    /**
     * 提取人脸特征信息
     *
     * @param faceInfo 人脸框信息
     * @return faceFeature
     */
    public FaceFeature extract(FaceInfo faceInfo) {
        FaceFeature faceFeature = new FaceFeature();
        mFaceEngine.extractFaceFeature(mNV21, mPreviewSize.getWidth(), mPreviewSize.getHeight(), FaceEngine.CP_PAF_NV21, faceInfo, faceFeature);
        return faceFeature;
    }

    /**
     * 推荐使用该方法避免二次识别调用减少提取时间
     *
     * @return LiveFaceInfo
     */
    public LiveFaceInfo detectAndExtract() {
        LiveFaceInfo liveFaceInfo = detect();
        FaceFeature faceFeature = new FaceFeature();
        mFaceEngine.extractFaceFeature(mNV21,
                mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                FaceEngine.CP_PAF_NV21,
                liveFaceInfo.getFaceInfo(),
                faceFeature);
        liveFaceInfo.setFaceFeature(faceFeature);
        liveFaceInfo.setSize(mPreviewSize);
        liveFaceInfo.setData(mNV21);
        return liveFaceInfo;
    }


    public void uninit() {
        synchronized (FaceEngineHelper.class) {
            mFaceEngine.unInit();
        }

    }

}
