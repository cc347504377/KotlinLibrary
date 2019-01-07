package com.luoye.whr.kotlinlibrary.net

/**
 * Created by haoran-wang on 11/23/17.
 */

object PublicCallback {
    interface BaseCallback {

        fun onStart()

        fun onFailed(msg: String)

    }

    interface StatCallBack : BaseCallback {
        fun onSuccess()
    }

    interface DataCallBack<T> : BaseCallback {
        fun onSuccess(t: T)
    }
}
