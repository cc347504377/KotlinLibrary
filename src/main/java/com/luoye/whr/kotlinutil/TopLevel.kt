package com.luoye.whr.kotlinutil

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.File
import java.util.*
import kotlin.concurrent.schedule

/**
 * Created by whr on 9/25/17.
 */
private var lastToast: Toast? = null

fun Context.toast(msg: String) {
    lastToast?.cancel()
    with(Toast.makeText(this, msg, Toast.LENGTH_SHORT)) {
        lastToast = this
        show()
        Timer().schedule(3000) {
            lastToast = null
        }
    }
}

private val stat = true
fun log(msg: String, tag: String = "TAG") = stat.isTue {
    //规定每段显示的长度
    val LOG_MAX_LENGTH = 2000
    val strLength = msg.length
    var start = 0
    var end = LOG_MAX_LENGTH
    for (i in 0 until Integer.MAX_VALUE) {
        //剩下的文本还是大于规定长度则继续重复截取并输出
        if (strLength > end) {
            Log.i(tag + i, "\n" + msg.substring(start, end))
            start = end
            end += LOG_MAX_LENGTH
        } else {
            Log.i(tag, msg.substring(start, strLength))
            Log.d(tag, "-------------------------------------------------------\n ")
            break
        }
    }
}

inline fun timer(delay: Long, crossinline operation: () -> Unit) {
    Timer().schedule(object : TimerTask() {
        override fun run() {
            operation()
        }
    }, delay)
}

inline fun Boolean.isTue(operation: () -> Unit) {
    if (this) {
        kotlin.run(operation)
    }
}

fun createFile(folder: String, fileName: String) = with(File(folder)) {
    if (!exists())
        mkdirs()
    File(this, fileName)
}

fun createFile(path: String) = with(File(path)) {
    if (!exists()) mkdirs()
    this
}

/**
 * 解析Exception
 */
fun Exception.stackMsg() =
        with(StringBuilder()) {
            append(message + "\n")
            stackTrace.forEach {
                append(it.toString() + "\n")
            }
            toString()
        }

val String.fileName: String
    get() = substringAfterLast("/")