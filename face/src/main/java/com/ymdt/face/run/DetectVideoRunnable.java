package com.ymdt.face.run;

import android.content.Context;
import android.util.Log;
import android.util.Size;

import com.ymdt.face.EngineType;
import com.ymdt.face.FaceEngineHelper;
import com.ymdt.face.model.LiveFaceInfo;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * @author niu
 * @des 视频提取人脸框，在视频提取人脸特征时，建议一直运行该线程
 * @date 2019/11/7 9:58 AM
 */
public class DetectVideoRunnable implements Runnable {

    private static final String TAG = "DetectVideoRunnable";
    /**
     * 上下文
     */
    private Context mContext;
    /**
     * 控制是否提取
     */
    private volatile boolean mDetect = true;
    /**
     * 引擎辅助
     */
    private FaceEngineHelper mDetectEngineHelper;

    /**
     * 图片存储队列
     * 要求:视频预览大小为mSize
     * 要求:数据类型为nv21
     */
    private Queue<byte[]> mQueue;

    private OnResult mOnResult;

    public void setOnResult(OnResult onResult) {
        this.mOnResult = onResult;
    }

    public DetectVideoRunnable(Context context,
                               Queue<byte[]> dataQueue) {
        this.mContext = context;
        this.mQueue = dataQueue;
        mDetectEngineHelper = new FaceEngineHelper(mContext);
        mDetectEngineHelper.initEngine(EngineType.VIDEO_ENGINE);
    }

    public DetectVideoRunnable initPreviewSize(Size size) {
        mDetectEngineHelper.initPreviewSize(size);
        return this;
    }


    @Override
    public void run() {
        Log.i(TAG, "run: 线程启动");
        while (mDetect) {
            try {
                if (null != mQueue && !mQueue.isEmpty()) {
                    byte[] data = mQueue.poll();
                    mDetectEngineHelper.initNV21(data);
                    LiveFaceInfo liveFaceInfo = mDetectEngineHelper.detectAndExtract();
                    if (null != mOnResult && liveFaceInfo.isHasFace()) {
                        mOnResult.result(liveFaceInfo);
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
     * 释放人脸引擎
     */
    private void uninit() {
        synchronized (DetectVideoRunnable.class) {
            if (null != mDetectEngineHelper) {
                mDetectEngineHelper.uninit();
            }
        }
    }

    /**
     * 停止当前线程
     */
    public void stop() {
        mDetect = false;
    }

    /**
     * 提取出人脸特征的结果回调
     */
    public interface OnResult {
        void result(LiveFaceInfo liveFaceInfo);
    }
}
