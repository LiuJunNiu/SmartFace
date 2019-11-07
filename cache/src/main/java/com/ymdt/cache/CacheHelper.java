package com.ymdt.cache;

import android.content.Context;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.MapUtils;
import com.google.gson.Gson;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by niu on 2019/8/6.
 * 缓存管理
 * 单文件存储方式
 * 所有要保存的对象转成json保存
 */

public class CacheHelper implements ICache {

    /**
     * 默认名称
     */
    private static final String CACHE_NAME = "cache_json.json";

    /**
     * 缓存保存
     * 因为需要序列化到本地，
     * key，value必须支持序列化
     */
    private volatile ConcurrentHashMap<String, String> mCache;

    /**
     * 缓存文件
     */
    private File mCacheFile;

    /**
     * 写入文件时线程池
     */
    private ThreadPoolExecutor mThreadPool;

    /**
     * 单例
     */
    private volatile static CacheHelper sInstance;


    public static CacheHelper getInstance() {
        if (null == sInstance) {
            synchronized (CacheHelper.class) {
                if (null == sInstance) {
                    sInstance = new CacheHelper();
                }
            }
        }
        return sInstance;
    }


    private CacheHelper() {
        mCache = new ConcurrentHashMap<>();
        mThreadPool = new ThreadPoolExecutor(1, 1, 10, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(1),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    /**
     * 初始化设置缓存管理
     *
     * @param context   上下文
     * @param cacheFile 缓存文件，null为默认
     * @throws Throwable context不能为null
     */
    public synchronized void init(Context context, File cacheFile) throws Throwable {
        if (null == context) {
            Throwable throwable = new Throwable("context==null");
            Logger.e(throwable, "context不能为null", sInstance);
            throw throwable;
        }
        if (null == cacheFile) {
            //使用默认
            mCacheFile = new File(context.getExternalCacheDir(), CACHE_NAME);

        } else {
            if (!FileUtils.isFileExists(cacheFile)) {
                boolean orExistsFile = FileUtils.createOrExistsFile(cacheFile);
                mCacheFile = orExistsFile ? cacheFile :
                        new File(context.getExternalCacheDir(), CACHE_NAME);
            } else {
                mCacheFile = cacheFile;
            }
        }

        //读取出缓存内容
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(mCacheFile))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                mCache.putAll((Map) obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(e, "读取缓存文件内容异常", mCacheFile);
        }
    }

    /**
     * 保存对象到集合中，并持久化到磁盘
     *
     * @param key key
     * @param obj 对象，将使用Gson进行序列化
     */
    @Override
    public void put(String key, Object obj) {
        String json = new Gson().toJson(obj);
        mCache.put(key, json);
        persist();
    }

    /**
     * 删除
     *
     * @param key key
     */
    @Override
    public void remove(String key) {
        mCache.remove(key);
        persist();
    }

    /**
     * 持久化到文件中
     * 需要线程池写入，避免主线程IO耗时
     */
    private void persist() {
        if (MapUtils.isEmpty(mCache)) {
            return;
        }
        mThreadPool.submit(() -> {
            try (ObjectOutputStream oos =
                         new ObjectOutputStream(new FileOutputStream(mCacheFile))) {
                oos.writeObject(mCache);
                oos.flush();
            } catch (Exception e) {
                e.printStackTrace();
                Logger.e(e, "写入缓存文件异常", mCache);
            }
        });
    }

    /**
     * 获取对象
     *
     * @param key  key
     * @param type 对象类型
     * @param <G>  泛型
     * @return g
     */
    @Override
    public <G> G get(String key, Type type) {
        String json = mCache.get(key);
        return new Gson().fromJson(json, type);
    }

    /**
     * 缓存文件大小
     *
     * @return 内部已经进行单位适配，可直接使用
     */
    @Override
    public String cacheSize() {
        return FileUtils.getFileSize(mCacheFile);
    }

    @Override
    public void clear() {
        mCache.clear();
    }

}
