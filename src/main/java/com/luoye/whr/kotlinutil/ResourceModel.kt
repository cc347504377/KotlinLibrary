package com.luoye.whr.kotlinutil

import android.os.Handler
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.RandomAccessFile
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.timer

/**
 * Created by whr on 9/7/17.
 */

//块大小
val blockSize = 4 * 1024 * 1024
//线程池数量
val threadPoolNum = 8
//任务线程超时时间
val taskTimeout = 1 * 60 * 1000L

abstract class ResourceModel {
    //最大重试数
    private val maxRetry = 3
    private var retryNum: Int = 0
    //状态
    private var isError = false
    //线程池
    protected lateinit var executors: ExecutorService

    /**
     * 开始
     */
    open fun start() {
        retryNum = 0
    }

    /**
     * 停止
     */
    abstract fun stop()

    /**
     * 继续
     */
    abstract fun resume()

    /**
     * 重新开始
     */
    abstract fun restart()

    /**
     * 失败
     */
    abstract fun failed(error: String)

    /**
     * 当某个任务失败时
     */
    protected fun onError(error: String) {
        //当没有达到最大重试数 并且 没有处于错误状态
        //避免重复多次执行
        if (retryNum != maxRetry && !isError) {
            isError = true
            retryNum++
            stop()
            timer(period = 2000) {
                isError = false
                resume()
            }
        } else {
            failed(error)
        }
    }

    /**
     * 完成
     */
    abstract protected fun onComplete(msg: String)
}

/**
 * 状态回调
 */
interface ResourceCallback {

    fun onStart()

    fun onUpdate(progress: Int)

    fun onFinish(msg: String)

    fun onFailed(error: String)
}

/**
 * 下载
 */
class Downloader(private val list: ArrayList<ResourceModel>?, private val url: String, private val folder: String, private val folderName: String, val handler: Handler, val callback: ResourceCallback) : ResourceModel() {
    //分块数量
    private var partNum = 0
    //下载完成数量
    private var finishNum = 0
    //总下载量
    private var contentLength = 0L
    //当前下载量
    private var contentDownload = 0
    //任务集合
    private val partList = ArrayList<DownTask>()

    override fun start() {
        super.start()
        executors = Executors.newFixedThreadPool(threadPoolNum)
        callback.onStart()
        doAsync {
            //获得下载总长度
            contentLength = HttpConnection(url).getLength()
            //创建本地文件
            val file = createFile(folder, folderName)
            val randomFile = RandomAccessFile(file, "rw")
            randomFile.setLength(contentLength)
            randomFile.close()
            finishNum = 0
            contentDownload = 0
            partNum = when (contentLength % blockSize) {
                0L -> (contentLength / blockSize).toInt()
                else -> (contentLength / blockSize + 1).toInt()
            }
            (1..partNum)
                    .map { (blockSize * (it - 1).toLong()) }
                    .forEach {
                        val part = DownTask(it, url, file)
                        partList.add(part)
                        executors.execute(part)
                    }
        }
    }

    override fun stop() {
        if (!executors.isShutdown) {
            executors.shutdownNow()
        }
    }

    override fun resume() {
        executors = Executors.newFixedThreadPool(threadPoolNum)
        partList.forEach {
            executors.execute(it)
        }
    }

    override fun restart() {
        stop()
        start()
    }

    override fun failed(error: String) {
        log("error:\n $error")
        stop()
        handler.post {
            callback.onFailed(error)
        }
    }

    override fun onComplete(msg: String) {
        handler.post {
            callback.onFinish(msg)
        }
        list?.remove(this)
    }

    //定时器，避免假死
    private var timer: Timer? = null

    @Synchronized
    private fun onPartDownloaded(task: DownTask) {
        timer?.cancel()
        partList.remove(task)
        finishNum++
        handler.post {
            callback.onUpdate((finishNum / partNum.toFloat() * 100).toInt())
        }
        if (finishNum == partNum) {
            onComplete("")
        } else {
            timer?.schedule(object : TimerTask() {
                override fun run() {
                    onError("超时未响应")
                }
            }, taskTimeout)
        }
    }

    /**
     * 单个任务对象
     */
    inner private class DownTask(val start: Long, val url: String
                                 , val file: File) : Runnable {
        override fun run() {
            with(HttpConnection(url)) {
                if (start + blockSize > contentLength) {
                    setHeaders("Range" to "bytes=$start-$contentLength")
                } else {
                    setHeaders("Range" to "bytes=$start-${start + blockSize}")
                }

                downloadFile(file, start, blockSize, object : RequestCallback {
                    override fun onSuccess(body: String) {
                        onPartDownloaded(this@DownTask)
                    }

                    override fun onConnectFailed(msg: String) {
                        onError("网络连接失败")
                    }

                    override fun onQuestFailed(code: Int, msg: String) {
                        onError("请求失败，$code\n $msg")
                    }
                })
            }
        }
    }
}
