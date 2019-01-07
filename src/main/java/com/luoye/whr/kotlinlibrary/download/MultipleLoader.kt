package com.luoye.whr.kotlinlibrary.download

import android.os.Looper
import com.luoye.whr.kotlinlibrary.util.fileName
import java.util.*

/**
 * 工厂类
 */
object MultipleLoader {

    private val resourceList = ArrayList<Downloader>()

    /**
     * 开始下载
     */
    fun getDownloader(url: String, folder: String, header: Map<String, String>, looper: Looper, callback: ResourceCallback) =
            InstanceDownloader(url, folder, url.fileName, header, resourceList, looper, callback).apply {
                resourceList.add(this)
            }

    /**
     * 停止所有任务
     */
    fun stopAll() {
        resourceList.forEach {
            it.stop()
        }
    }

//    /**
//     * 开始上传
//     */
//    fun upload(filePath: String, callback: ResourceCallback, handler: Handler) {
//        start(Uploader(resourceList, filePath, callback, handler))
//    }
//
//    private fun start(r: BaseTask) = with(r) {
//        resourceList.add(this)
//        start()
//    }

}


