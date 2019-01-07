package com.luoye.whr.kotlinlibrary.download

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.util.ArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.timer

/**
 * 定义对外行为
 */
interface Downloader : DownloadConfigure {
    //开始
    fun start()

    //停止
    fun stop()

    //继续
    fun resume()

    //重新开始
    fun restart()

    //暂停
    fun pause()
}

interface DownloadConfigure {
    //设置线程数量
    fun setThread(num: Int)

    //设置单片下载超时时间
    fun setTimeout(second: Int)

    //设置下载缓冲区大小
    fun setNetBuffer(byte: Int)

    //设置失败重连次数
    fun setRetryTime(time: Int)

    //设置失败重连间隔时间
    fun setRetrySleep(second: Int)
}

/**
 * 状态回调,一共有4种状态，开始，更新进度，下载完成，下载出错。
 */
interface ResourceCallback {

    fun onDownloadStart()

    fun onUpdate(progress: Int)

    fun onFinish(file: String)

    fun onFailed(error: String)

    fun onNotify(msg: String)
}

/**
 *
 */
abstract class BaseDownloader(
        private val list: ArrayList<Downloader>?,
        private val looper: Looper,
        private val callback: ResourceCallback) : Downloader {

    protected companion object {
        //FLAG
        const val FLAG_START = 0x11
        const val FLAG_UPDATE = 0x12
        const val FLAG_FINISH = 0x13
        const val FLAG_FAILED = 0x14
        const val FLAG_NOTIFY = 0x15
    }

    //通知主线程
    protected val handler = object : Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                FLAG_START -> callback.onDownloadStart()
                FLAG_UPDATE -> callback.onUpdate(msg.arg1)
                FLAG_FINISH -> callback.onFinish(msg.obj as String)
                FLAG_FAILED -> callback.onFailed(msg.obj as String)
                FLAG_NOTIFY -> callback.onNotify(msg.obj as String)
            }
        }
    }
    //块大小
    protected var blockSize = 1 * 512 * 1024
    //线程池数量
    private var threadPoolNum = 8
    //任务线程超时时间
    protected var taskTimeout = 10 * 1000
    //下载缓冲区大小(字节)
    protected var bufferSize = 2048
    //错误重试间隔
    private var retryTime = 2 * 1000L
    //最大重试数
    private var maxRetry = 3
    private var retryNum: Int = 0
    //状态
    private var isError = false
    //线程池
    protected lateinit var executors: ExecutorService
    //是否正在下载
    private var isDownloading = false

    /**
     * 通用模板
     */
    override fun start() {
        unDownloadingExecute {
            handler.sendEmptyMessage(FLAG_START)
            executors = Executors.newFixedThreadPool(threadPoolNum)
            retryNum = 0
        }
    }

    override fun restart() {
        stop()
        start()
    }

    override fun stop() {
        downloadingExecute {
            handler.sendMessage(Message().apply {
                what = FLAG_FAILED
                obj = "手动停止"
            })
            if (!executors.isShutdown) {
                executors.shutdownNow()
            }
        }
    }

    override fun pause() {
        downloadingExecute {
            if (!executors.isShutdown) {
                executors.shutdownNow()
            }
        }
    }

    override fun resume() {
        unDownloadingExecute {
            executors = Executors.newFixedThreadPool(threadPoolNum)
        }
    }

    override fun setThread(num: Int) {
        if (num < 1 || num > 36) {
            throw IllegalAccessException("thread num is Illegal,should be 1~36 ")
        }
        threadPoolNum = num
        if (isDownloading) {
            pause()
            resume()
        }
    }

    override fun setTimeout(second: Int) {
        taskTimeout = second * 1000
    }

    override fun setNetBuffer(byte: Int) {
        bufferSize = byte
    }

    override fun setRetryTime(time: Int) {
        maxRetry = time
    }

    override fun setRetrySleep(second: Int) {
        retryTime = second * 1000L
    }

    /**
     * 用户执行操作之前判断当前的下载状态
     */
    //没有下载时执行的操作
    private fun unDownloadingExecute(operation: () -> Unit) {
        if (!isDownloading) {
            operation()
            isDownloading = true
        } else {
            handler.sendMessage(Message.obtain().apply {
                what = FLAG_NOTIFY
                obj = "当前正在下载"
            })
            return
        }
    }

    //正在下载时执行的操作
    private fun downloadingExecute(operation: () -> Unit) {
        if (isDownloading) {
            operation()
            isDownloading = false
        } else {
            handler.sendMessage(Message.obtain().apply {
                what = FLAG_NOTIFY
                obj = "当前没有下载"
            })
            return
        }
    }

    /**
     * 事件回调
     */
    //块下载错误
    protected fun onBlockError(error: String) {
        //当没有达到最大重试数 并且 没有处于错误状态
        //避免重复多次执行
        if (retryNum != maxRetry && !isError) {
            isError = true
            retryNum++
            pause()
            timer(period = retryTime) {
                isError = false
                resume()
            }
        } else {
            onFailed(error)
        }
    }

    //错误次数次数过多，任务失败
    private fun onFailed(error: String) {
        pause()
        handler.sendMessage(Message.obtain().apply {
            what = FLAG_FAILED
            obj = error
        })
    }

    //下载完成
    protected fun onComplete(file: String) {
        isDownloading = false
        handler.sendMessage(Message.obtain().apply {
            what = FLAG_FINISH
            obj = file
        })
        list?.remove(this)
    }
}