package com.luoye.whr.kotlinlibrary.download

import android.os.Looper
import com.luoye.whr.kotlinlibrary.util.createFile
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.RandomAccessFile
import java.util.*


class InstanceDownloader(private val url: String, private val folder: String,
                         private val folderName: String,
                         private val header: Map<String, String>,
                         list: ArrayList<Downloader>?,
                         looper: Looper,
                         callback: ResourceCallback)
    : BaseDownloader(list, looper, callback) {

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
    //下载文件对象
    private lateinit var file: File

    override fun start() {
        super.start()
        doAsync {
            //获得下载总长度
            contentLength = HttpConnection(url).setHeaders(header).getLength()
            //创建本地文件
            file = createFile(folder, folderName)
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
        super.stop()
        File(folder, folderName).delete()
    }

    override fun resume() {
        super.resume()
        partList.forEach {
            executors.execute(it)
        }
    }

    @Synchronized
    private fun onPartDownloaded(task: DownTask) {
        finishNum++
        handler.sendMessage(handler.obtainMessage().apply {
            arg1 = (finishNum / partNum.toFloat() * 100).toInt()
            what = FLAG_UPDATE
        })
        partList.remove(task)
        if (finishNum == partNum) {
            onComplete(file.absolutePath)
        }
    }

    /**
     * 超时器，防止任务假死
     */
    private var timer: TimeoutTimer? = null

    private inner class TimeoutTimer : Timer() {
        init {
            super.schedule(object : TimerTask() {
                override fun run() {
                    onBlockError("超时未响应")
                }
            }, 10 * 1000)
        }
    }

    /**
     * 单个任务对象
     */
    private inner class DownTask(val start: Long, val url: String
                                 , val file: File) : Runnable {
        override fun run() {
            with(HttpConnection(url, taskTimeout, bufferSize)) {
                if (start + blockSize > contentLength) {
                    setHeaders(header)
                    setHeaders("Range" to "bytes=$start-$contentLength")
                } else {
                    setHeaders(header)
                    setHeaders("Range" to "bytes=$start-${start + blockSize}")
                }

                downloadFile(file, start, object : RequestCallback {
                    override fun onDownloadStart() {

                    }

                    override fun onProgress(progress: Int) {

                    }

                    override fun onSuccess(body: String) {
                        onPartDownloaded(this@DownTask)
                    }

                    override fun onConnectFailed(msg: String) {
                        onBlockError("网络连接失败")
                    }

                    override fun onQuestFailed(code: Int, msg: String) {
                        onBlockError("请求失败，$code\n $msg")
                    }
                })
            }
        }
    }
}