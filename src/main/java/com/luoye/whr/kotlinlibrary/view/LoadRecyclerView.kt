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
 * 处理了滑动事件
 */
class LoadRecyclerView : RecyclerView {
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

    override fun setLayoutManager(layout: LayoutManager?) {
        super.setLayoutManager(layout)
        manager = layout as LinearLayoutManager
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

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            val visibleItemCount = recyclerView.childCount
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