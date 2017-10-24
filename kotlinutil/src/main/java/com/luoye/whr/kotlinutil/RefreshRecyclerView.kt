package com.luoye.whr.kotlinutil

import android.content.Context
import android.support.annotation.LayoutRes
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.support.v7.widget.RecyclerView.*
import android.view.MotionEvent
import android.view.ViewGroup

/**
 * Created by whr on 17-3-13.
 * 封装了上拉加载、下拉刷新的RecyclerView
 */

class RefreshRecyclerView constructor(context: Context, attrs: AttributeSet? = null) : SwipeRefreshLayout(context, attrs) {

    var recyclerView: MyRecyclerView
    private var currentNum = 0

    companion object {
        interface OnRefreshListener<T> {
            fun onRefresh(): ArrayList<T>?

            fun onLoad(currentPage: Int): ArrayList<T>?
        }
    }

    init {
        recyclerView = LayoutInflater.from(context).inflate(R.layout.view_recycler, null) as MyRecyclerView
        addView(recyclerView)
    }

    //因为manager可能会有别的用处,所以放在外部实现
    fun <T> initRecyclerView(manager: LinearLayoutManager, adapter: BaseAdapter<T>, refreshListener: OnRefreshListener<T>) {
        recyclerView.manager = manager
        setRefreshListener(refreshListener, adapter)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
        //第一次初始化
        startRefresh()
        refresh(refreshListener.onRefresh(), adapter)
    }

    private fun <T> setRefreshListener(refreshListener: OnRefreshListener<T>, adapter: BaseAdapter<T>) {
        setOnRefreshListener { refresh(refreshListener.onRefresh(), adapter) }
        recyclerView.loadListener = { load(refreshListener.onLoad(currentNum), adapter) }
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

/**
 * 滑动事件处理
 */
class MyRecyclerView : RecyclerView {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private var down: Float = 0f
    private var scrollUp = false
    //LinearLayoutManager
    lateinit var manager: LinearLayoutManager
    //上拉加载监听
    var loadListener: ((currentPage: Int) -> Unit)? = null

    init {
        addOnScrollListener(EndlessRecyclerOnScrollListener())
    }

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        when (e!!.action) {
            MotionEvent.ACTION_DOWN -> down = e.y
            MotionEvent.ACTION_UP -> scrollUp = down - e.y > 0
        }
        return super.onTouchEvent(e)
    }

    private inner class EndlessRecyclerOnScrollListener : RecyclerView.OnScrollListener() {

        private var previousTotal = 0
        private var loading = true
        private var totalTime: Long = 0
        //加载次数(页数)
        private var currentPage = 1

        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            val visibleItemCount = recyclerView!!.childCount
            val totalItemCount = manager.itemCount
            val firstVisibleItem = manager.findFirstVisibleItemPosition()
//                when (newState) {
//                    SCROLL_STATE_IDLE // The RecyclerView is not currently scrolling.
//                    ->
//                        log("scroll_stop")
////                //当屏幕停止滚动，加载图片
////                Glide.with(MyApplication.context).resumeRequests()
//                    SCROLL_STATE_DRAGGING // The RecyclerView is currently being dragged by outside input such as user touch input.
//                    ->
//                        log("scroll_touch")
////                //当屏幕滚动且用户使用的触碰或手指还在屏幕上，停止加载图片
////                Glide.with(MyApplication.context).pauseRequests()
//                    SCROLL_STATE_SETTLING // The RecyclerView is currently animating to a final position while not under outside control.
//                    ->
//                        log("scroll_ing")
////                //由于用户的操作，屏幕产生惯性滑动，恢复加载图片
////                Glide.with(MyApplication.context).resumeRequests()
//                }
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
                    && scrollUp
                    && System.currentTimeMillis() - totalTime > 1000            //每次执行最小间隔为1s
                    ) {
                totalTime = System.currentTimeMillis()
                currentPage++
                loading = true
                loadListener?.invoke(currentPage)
            }
        }
    }
}

abstract class BaseAdapter<T>(private val context: Context, val data: ArrayList<T>, @LayoutRes private val layoutRes: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount() = data.size

    protected fun getView(parent: ViewGroup?) = LayoutInflater.from(context).inflate(layoutRes, parent, false)!!

}