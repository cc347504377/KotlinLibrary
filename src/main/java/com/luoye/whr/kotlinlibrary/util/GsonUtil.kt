package com.luoye.whr.kotlinlibrary.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException

object GsonUtil {
    val instance: Gson = GsonBuilder().create()
}

@Throws(JsonSyntaxException::class)
inline fun <reified T> String.fromJson(): T =
        GsonUtil.instance.fromJson(this, T::class.java)

inline fun <reified T : Any> T.toJson() =
        GsonUtil.instance.toJson(this)