package com.luoye.whr.kotlinlibrary.util

import android.app.Activity
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.WebViewClient
import android.widget.ProgressBar

/**
 * Created by luoye on 2018/3/5.
 * 配置WebView
 */
object WebManager {
    fun webViewConfig(webview: WebView, pb: ProgressBar, activity: Activity) {
        webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                //               view.loadUrl(url);
                //               return true;
                //消除重定向
                return false// doc上的注释为: True if the host application wants to handle the key event itself, otherwise return false(如果程序需要处理,那就返回true,如果不处理,那就返回false)
                // 我们这个地方返回false, 并不处理它,把它交给webView自己处理.
            }

        }
        webview.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            activity.startActivity(intent)
        }
        webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                // Activity和Webview根据加载程度决定进度条的进度大小
                // 当加载到100%的时候 进度条自动消失
                if (newProgress == 100) {
                    pb.visibility = View.GONE
                } else {
                    if (View.GONE === pb.visibility) {
                        pb.visibility = View.VISIBLE
                    }
                    pb.progress = newProgress
                }
                pb.progress = newProgress
                super.onProgressChanged(view, newProgress)
            }

        }

        webview.settings.let {
            // 设置WebView属性，能够执行Javascript脚本
            it.javaScriptEnabled = true
            it.setSupportZoom(true)
            it.builtInZoomControls = true
            it.useWideViewPort = true //自适应屏幕
            //自适应屏幕
            //webview.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
            it.loadWithOverviewMode = true
            it.displayZoomControls = false //隐藏webview缩放按钮
            //缓存模式
            it.cacheMode = WebSettings.LOAD_DEFAULT
            it.domStorageEnabled = true//支持DomStor缓存
            it.databaseEnabled = true
            it.useWideViewPort = true//将图片调整到适合webview的大小
        }
    }
}