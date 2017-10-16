package com.luoye.whr.kotlinutil

import android.content.Context
import android.support.annotation.LayoutRes
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.support.v7.widget.RecyclerView.*
import android.view.ViewGroup

/**
 * Created by whr on 17-3-13.
 * 封装了上拉加载、下拉刷新的RecyclerView
 */

class RefreshRecyclerView constructor(context: Context, attrs: AttributeSet? = null) : SwipeRefreshLayout(context, attrs) {

    val recyclerView: RecyclerView
    private var currentNum = 0

    init {
        recyclerView = LayoutInflater.from(context).inflate(R.layout.view_recycler, null) as RecyclerView
        addView(recyclerView)
    }

    //因为manager可能会有别的用处,所以放在外部实现
    fun <T> initRecyclerView(manager: LinearLayoutManager, adapter: BaseAdapter<T>, refreshListener: OnRefreshListener<T>) {
        setRefreshListener(refreshListener, manager, adapter)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
    }

    private fun <T> setRefreshListener(refreshListener: OnRefreshListener<T>, manager: LinearLayoutManager, adapter: BaseAdapter<T>) {
        setOnRefreshListener { refresh(refreshListener.onRefresh(), adapter) }
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener(manager) {
            override fun onLoadMore(currentPage: Int) {
                load(refreshListener.onLoad(currentPage), adapter)
            }
        })
    }

    interface OnRefreshListener<T> {
        fun onRefresh(): ArrayList<T>?

        fun onLoad(currentPage: Int): ArrayList<T>?
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
                var lastNum = 0
                val aData = it.data
                val size = data.size
                aData.clear()
                if (size > 0) {
                    lastNum = currentNum
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

/**
 * Created by whr on 17-3-31.
 * RecyclerView滑动事件处理
 */

abstract class EndlessRecyclerOnScrollListener(private val mLinearLayoutManager: LinearLayoutManager) : RecyclerView.OnScrollListener() {

    private var previousTotal = 0
    private var loading = true
    private var currentPage = 1
    private var totalTime: Long = 0

    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        val visibleItemCount = recyclerView!!.childCount
        val totalItemCount = mLinearLayoutManager.itemCount
        val firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition()

        when (newState) {
            SCROLL_STATE_IDLE // The RecyclerView is not currently scrolling.
            ->
                log("scroll_stop")
//                //当屏幕停止滚动，加载图片
//                Glide.with(MyApplication.context).resumeRequests()
            SCROLL_STATE_DRAGGING // The RecyclerView is currently being dragged by outside input such as user touch input.
            ->
                log("scroll_touch")
//                //当屏幕滚动且用户使用的触碰或手指还在屏幕上，停止加载图片
//                Glide.with(MyApplication.context).pauseRequests()
            SCROLL_STATE_SETTLING // The RecyclerView is currently animating to a final position while not under outside control.
            ->
                log("scroll_ing")
//                //由于用户的操作，屏幕产生惯性滑动，恢复加载图片
//                Glide.with(MyApplication.context).resumeRequests()
        }

        //保证每次数据修改后只会刷新执行一次
        if (loading) {
            if (totalItemCount != previousTotal) {
                loading = false
                previousTotal = totalItemCount
            }
        }
        if (!loading //已经执行过之前的判断
                && totalItemCount - visibleItemCount <= firstVisibleItem  //判断当滑到底部
                && newState == 1                                            //判断到底后再次滑动
                && System.currentTimeMillis() - totalTime > 1000            //每次执行最小间隔为1s
                ) {
            totalTime = System.currentTimeMillis()
            currentPage++
            onLoadMore(currentPage)
            loading = true
        }
    }

    abstract fun onLoadMore(currentPage: Int)
}

abstract class BaseAdapter<T>(private val context: Context, val data: ArrayList<T>, @LayoutRes private val layoutRes: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount() = data.size

    private fun getView(parent: ViewGroup?) = LayoutInflater.from(context).inflate(layoutRes, parent, false)

}