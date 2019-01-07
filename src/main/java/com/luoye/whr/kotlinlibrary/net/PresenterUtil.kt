package com.luoye.whr.kotlinlibrary.net

import android.content.ContextWrapper
import android.os.Handler
import com.google.gson.GsonBuilder
import com.luoye.whr.kotlinlibrary.util.log
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/**
 * Created by whr on 17-3-10.
 * 基于Retrofit
 * presenter工具类
 */

object PresenterUtil {
    const val TAG = "net"
    val threadPool = Executors.newSingleThreadExecutor()!!
    /**
     * 需要返回数据
     *
     * @param reBody   需要转出的类型
     * @param call     网络请求call模型
     * @param callback 网络回调
     */
    inline fun <reified T> getData(call: Call<ResponseBody>, callback: PublicCallback.DataCallBack<T>) {
        connection(T::class.java.simpleName, call, callback) { json: String ->
            val handler = Handler()
            threadPool.execute {
                val t = GsonBuilder().create().fromJson(json, T::class.java)
                handler.post {
                    if (t != null) {
                        callback.onSuccess(t)
                    } else {
                        callback.onFailed("没有数据")
                    }
                }
            }
        }
    }

    /**
     * 不处理直接返回Json
     */
    fun getJson(name: String, call: Call<ResponseBody>, callBack: PublicCallback.DataCallBack<String>) {
        connection(name, call, callBack) { json: String ->
            callBack.onSuccess(json)
        }
    }

    /**
     * 不需要返回数据
     */
    fun getCall(name: String, call: Call<ResponseBody>, callBack: PublicCallback.StatCallBack) {
        connection(name, call, callBack) {
            callBack.onSuccess()
        }
    }

    /**
     * 保存json 调试用
     */
    fun saveJson(name: String, activity: ContextWrapper, call: Call<ResponseBody>) {
        connection("debug", call, object : PublicCallback.BaseCallback {
            override fun onStart() {

            }

            override fun onFailed(msg: String) {
            }
        }) {
            Thread {
                val dirs = activity.getExternalFilesDirs(null)
                val file = File(dirs[0], "$name.json")
                val bos = BufferedOutputStream(FileOutputStream(file))
                bos.write(it.toByteArray())
                bos.flush()
                bos.close()
            }.start()
        }
    }

    /**
     * 创建连接并解析
     */
    inline fun connection(connectionName: String, call: Call<ResponseBody>, callBack: PublicCallback.BaseCallback, crossinline operation: (String) -> Unit) {
        callBack.onStart()
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    dealJson(connectionName, response, callBack)?.let {
                        try {
                            operation(it)
                        } catch (e: Exception) {
                            log("$connectionName operation exception:\n$e", TAG)
                            callBack.onFailed("" + e.message)
                        }
                    }
                } else {
                    log("$connectionName Response error:\n${response.code()} ${response.errorBody()?.string()}", TAG)
                    callBack.onFailed(response.code().toString() + "")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                log("$connectionName Connect error:\n$t", TAG)
                callBack.onFailed("" + t.message)
            }
        })

    }

    /**
     * 对json进行初步处理,避免NULL及错误
     */
    fun dealJson(connectionName: String, response: Response<ResponseBody>, callBack: PublicCallback.BaseCallback): String? {
        val json = response.body()!!.string()
        if (null == json || json.isBlank()) {
            callBack.onFailed("")
            return null
        }
        log("$connectionName json: \n$json", TAG)
        //根据接口规范进行状态判断
//        try {
//            val jsonObject = JSONObject(json)
//            val status = jsonObject.getString("status")
//            if (status != "success") {
//                var msg: String? = jsonObject.getString("resultMessage")
//                if (msg == null) {
//                    msg = "未知原因"
//                }
//                callBack.onFailed("没有更多")
//                return null
//            }
//        } catch (e: JSONException) {
//            e.printStackTrace()
//        }
        return json
    }
}
