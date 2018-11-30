package com.luoye.whr.kotlinutil

import com.alibaba.fastjson.JSON
import org.json.JSONException
import org.json.JSONObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by whr on 17-3-10.
 * 基于Retrofit
 * presenter工具类
 */

class PresenterUtil {

    /**
     * 需要返回数据
     *
     * @param reBody   需要转出的类型
     * @param call     网络请求call模型
     * @param callBack 网络回调
     */
    fun <T> getData(reBody: Class<T>, call: Call<ResponseBody>, callBack: NetCallback<T>) {
        connection(reBody.simpleName, call, callBack) { json: String ->
            val t = JSON.parseObject<T>(json, reBody)
            if (t != null) {
                callBack.onSuccess(t)
            } else {
                callBack.onFailed("没有数据")
            }
        }
    }

    /**
     * 不处理直接返回Json
     */
    fun getJson(name: String, call: Call<ResponseBody>, callBack: NetCallback<String>) {
        connection(name, call, callBack) { json: String ->
            callBack.onSuccess(json)
        }
    }

    /**
     * 不需要返回数据
     */
    fun getCall(name: String, call: Call<ResponseBody>, callBack: NetCallBack) {
        connection(name, call, callBack) {
            callBack.onSuccess()
        }
    }

    /**
     * 创建连接并解析
     */
    private inline fun connection(connectionName: String, call: Call<ResponseBody>, callBack: BaseCallback, crossinline operation: (String) -> Unit) {
        callBack.onStart()
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    try {
                        dealJson(response, callBack)?.let {
                            log("$connectionName json: \n$it","http")
                            operation(it)
                        }
                    } catch (e: Exception) {
                        log("$connectionName error:\n$e","http")
                        e.message?.let { callBack.onFailed(it) }
                    }
                } else {
                    log("$connectionName error:\n${response.code()} ${response.body()}","http")
                    callBack.onFailed(response.code().toString() + "")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                log("$connectionName error:\n$t","http")
                callBack.onFailed("" + t.message)
            }
        })

    }

    /**
     * 对json进行初步处理,避免NULL及错误
     */
    private fun dealJson(response: Response<ResponseBody>, callBack: BaseCallback): String? {
        val json = response.body()!!.string()
        if (json.trim { it <= ' ' } == "") {
            callBack.onFailed("")
            return null
        }
        //根据接口规范进行状态判断
        try {
            val jsonObject = JSONObject(json)
            val status = jsonObject.getString("status")
            if (status == "error") {
                var msg: String? = jsonObject.getString("msg")
                if (msg == null) {
                    msg = "未知原因"
                }
                callBack.onFailed(msg)
                return null
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return json
    }
}
