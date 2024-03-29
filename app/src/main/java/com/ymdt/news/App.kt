package com.ymdt.news

import android.app.Application
import com.blankj.utilcode.util.Utils
import com.ymdt.cache.CacheHelper
import java.io.File

/**
 * @des
 *
 * @date 2019/11/5 10:23 AM
 * @author niu
 */
class App :Application() {
    override fun onCreate() {
        super.onCreate()
        Utils.init(this)
        val file = File(externalCacheDir, "cache.json")
        CacheHelper.getInstance().init(this,file)
    }
}