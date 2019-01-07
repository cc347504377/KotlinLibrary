package com.luoye.whr.kotlinlibrary.base

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.luoye.whr.kotlinlibrary.util.timer
import com.luoye.whr.kotlinlibrary.util.toast


/**
 * Created by whr on 9/19/17.
 */

abstract class PermissionBaseActivity : AppCompatActivity() {

    companion object {
        private val PERMISSION_REQUEST_CODE = 1
        private val REQUEST_CODE = 0x11
    }
    protected abstract var permissions: Array<String>
    private var dialog: AlertDialog? = null
    //返回键次数
    private var backPress = 0

    override fun onBackPressed() {
        backPress++
        if (backPress == 1) {
            toast("再按一次退出")
            timer(1000) { backPress = 0 }
        }
        if (backPress == 2) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDialog()
        initPermission()
    }

    private fun initPermission() {
        //是否具有权限
        if (havePermissions()) {
            init()
        } else {
            requestPermission()
        }
    }

    /**
     * 初始化操作
     */
    abstract fun init()

    /**
     * 判断是否具有权限
     */
    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    /**
     * 初始化对话框
     */
    private fun initDialog() {
        dialog = AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("获取权限失败")
                .setMessage("请手动开启对应权限")
                .setPositiveButton("跳转到权限管理", null)
                .create()
        dialog?.let {
            it.show()
            it.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val intent = Intent()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                intent.data = Uri.fromParts("package", packageName, null)
                startActivityForResult(intent, REQUEST_CODE)
            }
            it.dismiss()
        }

    }

    /**
     * 判断用户是否手动给予了相应权限
     */
    override fun onStart() {
        super.onStart()
        dialog?.let {
            if (it.isShowing && havePermissions()) {
                it.dismiss()
                init()
            }
        }
    }

    /**
     * 权限请求回调
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && hasAllPermissionsGranted(grantResults)) {
            init()
        } else {
            //是否可以显示授权框
            if (canShowRequest(permissions)) {
                requestPermission()
            } else {
                dialog?.show()
            }
        }
    }

    /**
     * 判断是否含有全部权限
     */
    private fun hasAllPermissionsGranted(grantResults: IntArray): Boolean {
        return !grantResults.contains(PackageManager.PERMISSION_DENIED)
    }

    /**
     * 判断用户是否禁用了权限请求
     */
    private fun canShowRequest(permissions: Array<String>): Boolean {
        var isShow = true
        permissions.forEach {
            val isTip = ActivityCompat.shouldShowRequestPermissionRationale(this, it)
            isShow = isShow && isTip
        }
        return isShow
    }


    /**
     * 判断是否具有权限
     */
    private fun havePermissions(): Boolean {
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }
}