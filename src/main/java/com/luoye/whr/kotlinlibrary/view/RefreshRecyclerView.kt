package com.luoye.whr.kotlinlibrary.view

import android.content.Context
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import com.luoye.whr.kotlinlibrary.R
import com.luoye.whr.kotlinlibrary.base.BaseAdapter

/**
 * Created by whr on 17-3-13.
 * 封装了上拉加载、下拉刷新的RecyclerView
 */

class RefreshRecyclerView constructor(context: Context, attrs: AttributeSet? = null) : SwipeRefreshLayout(context, attrs) {

    val recyclerView: LoadRecyclerView
    private var currentNum = 0

    interface OnDataChangeListener<T> {
        fun onRefresh(dataChanged: (MutableList<T>) -> Unit)

        fun onLoad(currentPage: Int, operation: (MutableList<T>) -> Unit)
    }

    init {
        recyclerView = LayoutInflater.from(context).inflate(R.layout.view_recycler, null) as LoadRecyclerView
        addView(recyclerView)
    }

    //因为manager可能会有别的用处,所以放在外部实现
    fun <T> initRecyclerView(manager: LinearLayoutManager, adapter: BaseAdapter<T>, refreshListener: OnDataChangeListener<T>) {
        recyclerView.manager = manager
        setRefreshListener(refreshListener, adapter)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
        //第一次初始化
        startRefresh()
//        refresh(refreshListener.onRefresh(), adapter)
    }

    private fun <T> setRefreshListener(refreshListener: OnDataChangeListener<T>, adapter: BaseAdapter<T>) {
//        setOnRefreshListener { refresh(refreshListener.onRefresh(), adapter) }
//        recyclerView.loadListener = { load(refreshListener.onLoad(currentNum), adapter) }
    }

    /**
     * 下拉刷新
     */
    private fun <T> refresh(data: ArrayList<T>?, adapter: BaseAdapter<T>) {
        if (!isRefreshing) {
            startRefresh()
        }
        currentNum = 1
        data?.let {
            adapter.let {
                val aData = it.data
                val size = data.size
                aData.clear()
                if (size > 0) {
                    currentNum += size
                    aData.addAll(data)
                }
                stopRefresh()
                it.notifyDataSetChanged()
            }
        }
    }

    /**
     * 上拉加载
     */
    private fun <T> load(data: ArrayList<T>?, adapter: BaseAdapter<T>) {
        data?.let {
            adapter.let {
                val aData = it.data
                val size = data.size
                if (size > 0) {
                    val lastNum = currentNum
                    currentNum += size
                    aData.addAll(data)
                    it.notifyItemChanged(lastNum, size)
                } else {
                    it.notifyDataSetChanged()
                }
            }
        }
    }

    private fun startRefresh() {
        post { isRefreshing = true }
    }

    private fun stopRefresh() {
        post { isRefreshing = false }
    }
}