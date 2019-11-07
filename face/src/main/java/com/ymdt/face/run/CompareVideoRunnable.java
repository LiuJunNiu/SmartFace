package com.ymdt.face.run;

import android.content.Context;
import android.util.Log;

import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceSimilar;
import com.ymdt.face.EngineType;
import com.ymdt.face.FaceEngineHelper;
import com.ymdt.face.model.LiveFaceInfo;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * @author niu
 * @des 视频比对
 * @date 2019/11/7 3:22 PM
 */
public class CompareVideoRunnable implements Runnable {

    private static final String TAG = "CompareVideoRunnable";
    /**
     * 上下文
     */
    private Context mContext;
    /**
     * 人脸库
     * key:身份证号或人员信息等
     * value:人脸特征
     */
    private volatile Map<Object, FaceFeature> mMap;

    /**
     * 提取到的人员特征队列
     */
    private Queue<LiveFaceInfo> mQueue;

    /**
     * 引擎辅助
     */
    private FaceEngineHelper mFaceEngineHelper;

    /**
     * 线程控制标示
     */
    private volatile boolean mCompare = true;

    /**
     * 人脸相似度阈值
     * 超过认为同一人
     * 低于认为不同
     */
    private volatile float mSimilar = 0.75f;

    private OnResult mOnResult;

    public CompareVideoRunnable(Context context, Map<Object, FaceFeature> map, Queue<LiveFaceInfo> dataQueue) {
        this.mContext = context;
        this.mMap = map;
        this.mQueue = dataQueue;
    }

    public void setOnResult(OnResult onResult) {
        this.mOnResult = onResult;
    }

    public CompareVideoRunnable initSimilar(float similar){
        this.mSimilar = similar;
        return this;
    }

    @Override
    public void run() {
        mFaceEngineHelper = new FaceEngineHelper(mContext).initEngine(EngineType.VIDEO_ENGINE);
        Log.i(TAG, "run: 线程启动");
        while (mCompare) {
            try {
                if (null != mQueue && !mQueue.isEmpty()) {
                    LiveFaceInfo liveFaceInfo = mQueue.poll();
                    mFaceEngineHelper.initOriginalFaceFeature(liveFaceInfo.faceFeature);
                    for (Map.Entry<Object, FaceFeature> entry : mMap.entrySet()) {
                        mFaceEngineHelper.initComparedFaceFeature(entry.getValue());
                        FaceSimilar faceSimilar = mFaceEngineHelper.similar();
                        if (faceSimilar.getScore() >= mSimilar) {
                            if (null != mOnResult) {
                                mOnResult.result(liveFaceInfo, faceSimilar, entry.getKey());
                            }

                            //识别到人员后推荐睡眠2秒，以便UI处理识别到人的事件
                            TimeUnit.MILLISECONDS.sleep(2000);
                            break;
                        }
                    }
                } else {
                    TimeUnit.MILLISECONDS.sleep(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        uninit();
        Log.i(TAG, "run: 线程结束");
    }

    /**
     * 停止当前线程
     */
    public void stop() {
        mCompare = false;
    }


    /**
     * 释放人脸引擎
     */
    private void uninit() {
        synchronized (CompareVideoRunnable.class) {
            if (null != mFaceEngineHelper) {
                mFaceEngineHelper.uninit();
            }
        }
    }


    /**
     * 大于阈值的回调
     */
    public interface OnResult {
        /**
         * 结果回调
         *
         * @param liveFaceInfo 人脸特征
         * @param faceSimilar  比对结果
         * @param obj          结果对应的key
         */
        void result(LiveFaceInfo liveFaceInfo, FaceSimilar faceSimilar, Object obj);
    }
}
