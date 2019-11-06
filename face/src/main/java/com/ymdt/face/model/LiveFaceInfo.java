package com.ymdt.face.model;

import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.LivenessInfo;

/**
 * @author niu
 * @des 包装类 包含活体信息
 * @date 2019/11/6 6:00 PM
 */
public class LiveFaceInfo extends FaceInfo {
    public LivenessInfo livenessInfo=new LivenessInfo();

    public LivenessInfo getLivenessInfo() {
        return livenessInfo;
    }

    public void setLivenessInfo(LivenessInfo livenessInfo) {
        this.livenessInfo = livenessInfo;
    }

    public LiveFaceInfo wrap(FaceInfo faceInfo) {
        setRect(faceInfo.getRect());
        setOrient(faceInfo.getOrient());
        setFaceId(faceInfo.getFaceId());
        return this;
    }

}
