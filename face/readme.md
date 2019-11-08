#问题及注意事项
##关于虹软引擎
###引擎激活问题

```
 存在当用户删除激活过的文件后，导致激活失败的情况
 推荐全部改用在激活
 faceEngine.activeOnline(Context context,String appid,String key)

```

###引擎初始化问题

```

 mFaceEngine.init(mContext,
                         FaceEngine.ASF_DETECT_MODE_IMAGE,
                         FaceEngine.ASF_OP_0_ONLY,
                         16, 1,
                         MASK);
 最后一个字段关联限制引擎的使用功能和性能，参数越多，功能越全，相应对比的耗时越长
 推荐使用时针对需求而定
 比如：在视频中提取人脸特征时只需要识别detect，然后在调用活体识别，最后在

```