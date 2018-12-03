package com.luoye.whr.kotlinutil.util

import android.app.Activity
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.app.NotificationCompat
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.schedule

/**
 * Created by whr on 9/25/17.
 * 工具函数
 */

private var lastToast: Toast? = null

fun Context.toast(msg: String?) {
    lastToast?.cancel()
    msg?.let {
        with(Toast.makeText(this, it, Toast.LENGTH_SHORT)) {
            lastToast = this
            show()
            Timer().schedule(3000) {
                lastToast = null
            }
        }
    }
}

fun Fragment.toast(msg: String?) {
    lastToast?.cancel()
    msg?.let {
        with(Toast.makeText(context, it, Toast.LENGTH_SHORT)) {
            lastToast = this
            show()
            Timer().schedule(3000) {
                lastToast = null
            }
        }
    }
}

fun Fragment.runOnUiThread(operation: () -> Unit) {
    activity?.runOnUiThread(operation)
}

private val stat = true
fun log(msg: String, tag: String = "common") = stat.isTrue {
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

/**
 * 是否包含汉字
 */
fun String.isContainChinese(): Boolean {
    this.forEach {
        val c = it.toInt()
        if (c in 19968..40868) {
            return true
        }
    }
    return false
}

/**
 * 包含字母-数字组合
 */
fun String.isStandardPs(): Boolean {
    var hasLetter = false
    var hasNum = false
    this.forEach {
        val c = it.toInt()
        if (c in 65..90 || c in 97..122) {
            hasLetter = true
        }
        if (c in 48..57) {
            hasNum = true
        }
        if (hasLetter && hasNum) {
            return true
        }
    }
    return false
}

/**
 * 包含特殊符号
 */
fun String.isContainSpecialSymbol(): Boolean {
    this.forEach {
        val c = it.toInt()
        if (c !in 19968..40868 && c !in 65..90 && c !in 97..122 && c !in 48..57) {
            return true
        }
    }
    return false
}

/**
 * Activity 跳转
 */
inline fun <reified T> Activity.startActivity() {
    startActivity(Intent(this, T::class.java))
}

inline fun <reified T> Fragment.startActivity() {
    activity?.startActivity<T>()
}

/**
 * 定时器
 */
inline fun timer(delay: Long, crossinline operation: () -> Unit) {
    Timer().schedule(object : TimerTask() {
        override fun run() {
            operation()
        }
    }, delay)
}

/**
 * 通知栏下载
 */
fun download(content: Context, url: String, fileName: String) {
    val request = DownloadManager.Request(Uri.parse(url))
    request.allowScanningByMediaScanner()
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
    // get download service and enqueue file
    val manager = content.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
    manager!!.enqueue(request)
}

/**
 * 推送通知
 */
fun pushNotify(content: Context, contentTitle: String, contentText: String, iconRes: Int, notifyId: Int = 1, notifyChannel: String = "app") {
    val manager = content.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(content, notifyChannel)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(iconRes)
            .build()
    manager.notify(notifyId, notification)
}

/**
 * 判断true
 */
inline fun Boolean.isTrue(operation: () -> Unit) {
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
 * @param url       图片地址
 * @param imageView view
 * @param width     图片宽度
 * @param height    图片高度
 */
fun Context.loadImg(url: String, imageView: ImageView, width: Int, height: Int) {
    GlideApp
            .with(this)
            .load(url)
            .centerCrop()
            .override(width, height)
            .transition(withCrossFade())
            .into(imageView)
}

/**
 * 图片加载
 *
 * @param url       图片地址
 * @param imageView view
 */
fun Context.loadImg(url: Any?, imageView: ImageView) {
    GlideApp
            .with(this)
            .load(url)
            .centerCrop()
            .transition(withCrossFade())
            .into(imageView)
}

fun Context.clearImg(target: Target<Any>) {
    GlideApp
            .with(this)
            .clear(target)
}

fun Context.loadHead(url: String, circleImageView: CircleImageView) {
    GlideApp
            .with(this)
            .asBitmap()
            .load(url)
            .centerCrop()
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    circleImageView.setImageBitmap(resource)
                }
            })
}

fun Context.loadRawImg(url: String, imageView: ImageView) {
    GlideApp.with(this)
            .asBitmap()
            .load(url)
            .into(object : BitmapImageViewTarget(imageView) {
                override fun setResource(resource: Bitmap?) {
                    resource?.let {
                        val ratio = it.height / it.width.toFloat()
                        imageView.apply {
                            layoutParams = layoutParams.apply {
                                height = (imageView.measuredWidth * ratio).toInt()
                            }
                        }
                    }
                    super.setResource(resource)
                }
            })
}

/**
 *
 */
fun compressBitmap(file: String, targetWith: Int = 1080, tartgetHeight: Int = 1920): Bitmap {
    val options = BitmapFactory.Options()
    //inJustDecodeBounds为true时创建的Bitmap只有尺寸信息，而没有真正的创建，可以节省资源
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(file, options)
    val bitHeight = options.outHeight
    val bitWidth = options.outWidth
    var sample = 1
    //通过原始尺寸和需要输出尺寸判断sample值
    if (bitHeight > tartgetHeight || bitWidth > targetWith) {
        while (bitHeight / (2 * sample) >= tartgetHeight || bitWidth / (2 * sample) >= targetWith) {
            sample *= 2
        }
    }
    options.inJustDecodeBounds = false
    options.inSampleSize = sample
    options.inPreferredConfig = Bitmap.Config.RGB_565
    return BitmapFactory.decodeFile(file, options)
}

/**
 * 保存位图到文件
 */
fun bitmapToFile(bitmap: Bitmap, path: String) {
    val file = File(path)
    val parentFile = file.parentFile
    if (!parentFile.exists()) {
        parentFile.mkdirs()
    }
    try {
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

/**
 * 文件移动
 */
fun file2File(originPath: String, targetPath: String) {
    val targetFile = File(targetPath)
    if (!targetFile.parentFile.exists()) {
        targetFile.parentFile.mkdirs()
    }
    if (targetFile.exists() && targetFile.length() > 0) {
        throw Exception("已经存在")
    }
    val foc = FileOutputStream(targetFile).channel
    val fic = FileInputStream(File(originPath)).channel
    foc.transferFrom(fic, 0, fic.size())
    foc.close()
    fic.close()
}

/**
 * 获得Retrofit实例
 */
fun createRetrofit(baseUrl: String, connectTimeout: Long = 5, readTimeout: Long = 5, writeTimeout: Long = 5, timeUnit: TimeUnit = TimeUnit.SECONDS,
                   okOperation: ((OkHttpClient.Builder) -> Unit)? = null): Retrofit {
    val client = OkHttpClient().newBuilder()
            .connectTimeout(connectTimeout, timeUnit)
            .readTimeout(readTimeout, timeUnit)
            .writeTimeout(writeTimeout, timeUnit)
            .apply { okOperation?.invoke(this) }
            .build()
    return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}

/**
 * 手动将视频信息添加到系统数据库
 */
fun addImageToSystem(fileName: String, contentProvider: ContentResolver?) {
    contentProvider?.let {
        val file = File(fileName)
        if (file.exists()) {
            it.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    ContentValues().apply {
                        put(MediaStore.Images.Media.SIZE, file.length())
                        put(MediaStore.Images.Media.TITLE, file.name)
                        put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                        put(MediaStore.Images.Media.DATA, fileName)
                    })
        }
    }

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
    get() = substringAfterLast('/')

val String.fileSuffix: String
    get() = substringAfterLast('.')

/**
 * AES加密
 *
 * @param content 需要加密的内容
 * @param password  加密密码
 * @return
 */
fun encrypt(content: String, password: String): ByteArray {
    val kgen = KeyGenerator.getInstance("AES")
    kgen.init(128, SecureRandom(password.toByteArray()))
    val secretKey = kgen.generateKey()
    val enCodeFormat = secretKey.getEncoded()
    val key = SecretKeySpec(enCodeFormat, "AES")
    val cipher = Cipher.getInstance("AES")// 创建密码器
    val byteContent = content.toByteArray(charset("utf-8"))
    cipher.init(Cipher.ENCRYPT_MODE, key)// 初始化
    return cipher.doFinal(byteContent) // 加密
}

/**
 * base64编码
 * 默认方式
 */
fun base64Encode(input: ByteArray) = Base64.encodeToString(input, Base64.DEFAULT)!!

/**
 * dp转px
 */
fun dpToPx(dp: Float, content: Context) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, content.resources.displayMetrics)


fun Context.getAppName(): String? {
    try {
        val packageManager = packageManager
        val packageInfo = packageManager.getPackageInfo(
                packageName, 0)
        val labelRes = packageInfo.applicationInfo.labelRes
        return resources.getString(labelRes)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return null
}

inline fun logExecuteDuration(operation: () -> Unit) {
    val millis = System.currentTimeMillis()
    operation.invoke()
    log("execute:${System.currentTimeMillis() - millis} ms")
}