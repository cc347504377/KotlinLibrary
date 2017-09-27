package com.luoye.whr.kotlinutil

/**
 * Created by whr on 17-4-10.
 */
interface BaseCallback {

    fun onStart()

    fun onFailed(msg: String)

}

interface NetCallBack : BaseCallback {

    fun onSuccess()
}

interface NetCallback<T> : BaseCallback {

    fun onSuccess(t: T)

}

interface NetProCallBack : NetCallBack {

    fun onProgress(progress: Int)

}

interface NetProCallback<T> : NetCallback<T> {

    fun onProgress(progress: Int)

}

private interface NetCallbackVerify<T> {

    fun onVerification()

    fun onStart()

    fun onSuccess(t: T)

    fun onFailed(msg: String)
}

class SimpleCallback<T> : NetCallback<T> {

    override fun onStart() {

    }

    override fun onSuccess(t: T) {

    }

    override fun onFailed(msg: String) {

    }
}

class SimpleCallbackVerifi<T> : NetCallbackVerify<T> {

    override fun onVerification() {

    }

    override fun onStart() {

    }

    override fun onSuccess(t: T) {

    }

    override fun onFailed(msg: String) {

    }
}


