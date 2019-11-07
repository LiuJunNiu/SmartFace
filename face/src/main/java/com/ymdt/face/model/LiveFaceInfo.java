package com.ymdt.face.model;

import android.util.Size;

import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.LivenessInfo;

/**
 * @author niu
 * @des 包装类 包含活体信息,人脸特征信息
 * @date 2019/11/6 6:00 PM
 */
public class LiveFaceInfo extends FaceInfo {
    /**
     * 活体信息
     */
    public LivenessInfo livenessInfo = new LivenessInfo();
    /**
     * 人脸特征信息
     */
    public FaceFeature faceFeature = new FaceFeature();

    /**
     * 图片nv21数据
     */
    public byte[] data = new byte[0];

    /**
     * 图片宽高信息
     */
    public Size size = new Size(0, 0);

    public LivenessInfo getLivenessInfo() {
        return livenessInfo;
    }

    public void setLivenessInfo(LivenessInfo livenessInfo) {
        this.livenessInfo = livenessInfo;
    }

    public FaceFeature getFaceFeature() {
        return faceFeature;
    }

    public void setFaceFeature(FaceFeature faceFeature) {
        this.faceFeature = faceFeature;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    public LiveFaceInfo wrap(FaceInfo faceInfo) {
        setRect(faceInfo.getRect());
        setOrient(faceInfo.getOrient());
        setFaceId(faceInfo.getFaceId());
        return this;
    }

}
