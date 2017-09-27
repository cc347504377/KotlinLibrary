package com.luoye.whr.kotlinutil

import java.io.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by whr on 9/5/17.
 */
class HttpConnection(url: String) {

    private val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection

    init {
        with(connection) {
            //传输数据的超时时间
            readTimeout = 2 * 60 * 1000
            //建立连接的超时时间
            connectTimeout = 10 * 1000
            //关闭默认缓存
            setChunkedStreamingMode(0)
        }
    }

    fun setHeaders(vararg header: Pair<String, String>) =
            with(connection) {
                for ((key, value) in header) {
                    setRequestProperty(key, value)
                }
            }

    fun getLength() = with(connection) {
        val length = getHeaderField("Content-Length").toLong()
        disconnect()
        length
    }

    fun post(callback: RequestCallback) =
            with(connection) {
                try {
                    doOutput = true
                    requestMethod = "POST"
                    //解析返回结果
                    parseResponse(callback)
                } catch (e: Exception) {
                    e.printStackTrace()
                    //连接失败
                    callback.onConnectFailed(e.toString())
                } finally {
                    disconnect()
                }
            }

    /**
     * 统一管理请求判断
     */
    private fun connect(callback: RequestCallback, operation: () -> Unit) =
            with(connection) {
                try {
                    kotlin.run(operation)
                } catch (e: Exception) {
                    when (e) {
                        is InterruptedIOException -> log("停止线程")
                        else -> {
                            e.printStackTrace()
                            //连接失败
                            callback.onConnectFailed(e.toString())
                        }
                    }
                } finally {
                    disconnect()
                }
            }

    /**
     * 上传随机文件
     */
    fun postRandomFile(accessFile: RandomAccessFile, length: Int, callback: RequestCallback) {
        connect(callback) {
            with(connection) {
                log(headerFields.toString())

                doOutput = true
                requestMethod = "POST"
                val stream = connection.outputStream
                val outputStream = BufferedOutputStream(stream)
                val b = ByteArray(2048)
                var read = accessFile.read(b, 0, b.size)
                var readLength = read
                while (read != -1) {
                    outputStream.write(b, 0, read)
                    read = accessFile.read(b, 0, b.size)
                    if (readLength + read >= length) {
                        outputStream.write(b, 0, length - readLength)
                        break
                    } else {
                        readLength += read
                    }
                }
                outputStream.close()
                stream.close()
                accessFile.close()
                //解析返回结果
                parseResponse(callback)
            }
        }
    }


    /**
     * 上传字符串
     */
    fun postString(body: String, callback: RequestCallback) =
            connect(callback) {
                with(connection) {
                    doOutput = true
                    requestMethod = "POST"
                    val bw = BufferedWriter(OutputStreamWriter(outputStream))
                    bw.write(body)
                    bw.flush()
                    bw.close()
                    //解析返回结果
                    parseResponse(callback)
                }
            }

    fun downloadFile(file: File, seek: Long, blockLength: Int, callback: RequestCallback) {
        connect(callback) {
            with(connection) {
                requestMethod = "GET"
                if (responseCode == 206 || responseCode == 200) {
                    val bis = BufferedInputStream(inputStream)
                    val randomFile = RandomAccessFile(file, "rw")
                    randomFile.seek(seek)
                    val b = ByteArray(2048)
                    var length = 0
                    var read = bis.read(b)
                    while (read != -1) {
                        randomFile.write(b, 0, read)
                        length += read
                        read = bis.read(b)
                    }
                    bis.close()
                    randomFile.close()
                    callback.onSuccess("")
                } else {
                    val reader = BufferedReader(InputStreamReader(errorStream))
                    var str = reader.readLine()
                    val response = StringBuilder()
                    while (str != null) {
                        response.append(str)
                        str = reader.readLine()
                    }
                    reader.close()
                    //请求失败
                    callback.onQuestFailed(responseCode, response.toString())
                    log("seek: $seek")
                }
            }
        }
    }

    /**
     * 解析请求结果
     */
    private fun parseResponse(callback: RequestCallback) =
            with(connection) {
                if (responseCode != 200) {
                    val reader = BufferedReader(InputStreamReader(errorStream))
                    var str = reader.readLine()
                    val response = StringBuilder()
                    while (str != null) {
                        response.append(str)
                        str = reader.readLine()
                    }
                    reader.close()
                    //请求失败
                    callback.onQuestFailed(responseCode, response.toString())
                } else {
                    val inputStream = inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var str = reader.readLine()
                    val response = StringBuilder()
                    while (str != null) {
                        response.append(str)
                        str = reader.readLine()
                    }
                    reader.close()
                    //成功
                    callback.onSuccess(response.toString())
                }
            }

}

interface RequestCallback {
    fun onSuccess(body: String)

    fun onConnectFailed(msg: String)

    fun onQuestFailed(code: Int, msg: String)
}
