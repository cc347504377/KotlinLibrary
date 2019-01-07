package com.luoye.whr.kotlinlibrary.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.*
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.luoye.whr.kotlinlibrary.R
import com.luoye.whr.kotlinlibrary.base.BaseAdapter
import com.luoye.whr.kotlinlibrary.util.dpToPx

/**
 * Created by whr on 10/18/17.
 * 自定义可弹回ListView
 */
class PullLayout : FrameLayout {
    /**
     * Created by whr on 11/2/17.
     *
     */
    interface OnDataChangeListener<T> {
        fun onRefresh(dataChanged: (MutableList<T>) -> Unit)

        fun onLoad(currentPage: Int, operation: (MutableList<T>) -> Unit)
    }

    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    val recyclerView: RecyclerView
    private val topView: ProgressBar
    private val bottomView: TextView
    //当前列表允许向上滑动
    private var canScrollUp = true
    //当前列表允许向下滑动
    private var canScrollDown = false
    private var refreshEvent: (() -> Unit)? = null
    private var loadEvent: (() -> Unit)? = null
    //第一次刷新成功
    private var isFirstFreshed = false
    //当前页数
    private var currentPage = 1
    //适配器
    private var mAdapter: BaseAdapter<Any>? = null
    //数据监听器
    private var mDataChangeListener: OnDataChangeListener<Any>? = null
    //阴影
    private var elevate = 0f
    //提示控件margin
    private var bottomMargin = 0
    private var topMargin = 0
    //提示控件大小
    private var bottomViewSize = 0f
    private var topViewSize = 0
    //悬停距离
    private var hoverDown = 0
    private var hoverUp = 0
    //头偏移量
    private var topOffset = 0f

    init {
        initDp()
        iniEvent()
        setBackgroundColor(Color.parseColor("#91dfdfdf"))
        recyclerView = (LayoutInflater.from(context).inflate(R.layout.view_recycler, null) as RecyclerView)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        this.elevation = elevate
                    }
                    setHasFixedSize(true)
                    this.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                            super.onScrollStateChanged(recyclerView, newState)
                            canScrollDown = recyclerView.canScrollVertically(-1)
                            canScrollUp = recyclerView.canScrollVertically(1)
                        }
                    })
                }
        topView = ProgressBar(context).apply {
            this.layoutParams = LayoutParams(topViewSize, topViewSize, Gravity.CENTER_HORIZONTAL).apply {
                this.setMargins(0, this@PullLayout.topMargin, 0, this@PullLayout.topMargin)
            }
            post {
                hoverUp = measuredHeight + topMargin * 2
                topOffset = (-hoverUp).toFloat()
                translationY = topOffset
                visibility = View.INVISIBLE
            }
        }
        bottomView = TextView(context).apply {
            this.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                this.setMargins(0, this@PullLayout.bottomMargin, 0, this@PullLayout.bottomMargin)
            }
            this.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, bottomViewSize)
            this.setTextColor(Color.parseColor("#999999"))
            this.text = "加载更多"
            post {
                hoverDown = measuredHeight + bottomMargin * 2
            }
        }
        addView(topView)
        addView(bottomView)
        addView(recyclerView)
    }

    /**
     * 设置参数
     */
    //Todo 这里使用Any是因为Ide抽风 一直报错
    fun <T> setup(manager: Any, adapter: BaseAdapter<T>, dataChangeListener: OnDataChangeListener<T>) {
        recyclerView.apply {
            layoutManager = manager as RecyclerView.LayoutManager
            this.adapter = adapter
            this@PullLayout.mAdapter = adapter as BaseAdapter<Any>
            mDataChangeListener = dataChangeListener as OnDataChangeListener<Any>
        }
    }

    /**
     * 手动刷新
     */
    fun refresh() {
        topView.let {
            it.post {
                it.visibility = View.VISIBLE
                it.translationY = 0f
                it.scaleX = 1f
                it.scaleY = 1f
                transY = hoverDown.toFloat()
                recyclerView.translationY = hoverDown.toFloat()
                refreshEvent?.invoke()
            }
        }
    }

    /**
     *  网络请求失败时调用该方法进行状态重置
     */
    fun onError(msg: String?) {
        bottomView.text = msg
        resetView()
    }

    /**
     * 初始化dp常量
     */
    private fun initDp() {
        elevate = dpToPx(3f, context)
        bottomMargin = dpToPx(25f, context).toInt()
        topMargin = dpToPx(10f, context).toInt()
        bottomViewSize = dpToPx(15f, context)
        topViewSize = dpToPx(50f, context).toInt()
    }

    /**
     * 初始化加载事件
     */
    private fun iniEvent() {
        refreshEvent = {
            mDataChangeListener?.onRefresh { data ->
                currentPage = 2
                mAdapter?.let {
                    val size = data.size
                    if (size > 0) {
                        it.data.clear()
                        it.data.addAll(data)
                    }
                    it.notifyDataSetChanged()
                }
                resetView()
                isFirstFreshed = true
            }
        }
        loadEvent = {
            mDataChangeListener?.onLoad(currentPage) { data ->
                currentPage++
                mAdapter?.let {
                    val adapterData = it.data
                    val size = data.size
                    val lastNum = adapterData.size
                    if (size > 0) {
                        adapterData.addAll(data)
                        it.notifyItemRangeInserted(lastNum, size)
                    }
                }
                resetView()
            }
        }
    }

    private var lastY = 0f
    private var transY = 0f
    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        //判断当满足条件时出发刷新事件，将点击事件拦截交给Layout处理
        when (event!!.action) {
            MotionEvent.ACTION_DOWN -> lastY = event.y
            MotionEvent.ACTION_MOVE -> {
                //如果都能滑动直接return 节省性能
                if (canScrollUp && canScrollDown) {
                    return false
                }
                val scroll = event.y - lastY
                if (!canScrollUp && scroll < 0 && isFirstFreshed) {
                    return true
                }
                if (!canScrollDown && scroll > 0) {
                    topView.visibility = View.VISIBLE
                    return true
                }
            }
        }
        return false
    }

    private var refreshing = false
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event!!.action) {
            MotionEvent.ACTION_MOVE -> {
                val scroll = event.y - lastY
                val abs = Math.abs(transY)
                val scrollRatio = abs / hoverDown
                //滑动阻尼
                val factor = if (abs > hoverDown) {
                    //超过悬停距离
                    bottomView.text = "释放加载"
                    0.5f - scrollRatio * 0.1f
                } else {
                    //正常范围
                    topView.let {
                        it.scaleX = scrollRatio
                        it.scaleY = scrollRatio
                    }
                    bottomView.let {
                        it.scaleX = scrollRatio
                        it.scaleY = scrollRatio
                        it.text = "上拉加载更多"
                    }
                    0.9f - scrollRatio * 0.5f
                }
                transY += scroll * factor
                if (!(!canScrollDown && !canScrollUp)) {
                    if ((!canScrollUp && transY > 0) || (!canScrollDown && transY < 0)) {
                        transY = 0f
                    }
                }
                lastY = event.y
                topView.translationY = topOffset + transY
                recyclerView.translationY = transY
            }
            MotionEvent.ACTION_UP -> {
                when {
                    //下拉刷新
                    transY >= hoverUp -> {
                        topView.let {
                            it.scaleX = 1f
                            it.scaleY = 1f
                        }
                        if (!refreshing) {
                            refreshEvent?.invoke()
                            refreshing = true
                        }
                        scrollAnimation(transY.toInt(), hoverUp, duration = 100L, bounce = false) { v ->
                            recyclerView.translationY = v.toFloat()
                            topView.translationY = v.toFloat() + topOffset
                            transY = v.toFloat()
                        }
                    }
                    //上拉加载
                    Math.abs(transY).toInt() >= hoverDown -> {
                        bottomView.let {
                            it.scaleX = 1f
                            it.scaleY = 1f
                            it.text = "拼命加载中"
                        }
                        if (!refreshing) {
                            loadEvent?.invoke()
                            refreshing = true
                        }
                        scrollAnimation(transY.toInt(), -hoverDown, duration = 100L, bounce = false) { v ->
                            recyclerView.translationY = v.toFloat()
                            transY = v.toFloat()
                        }
                    }
                    else -> resetView()
                }
            }
        }
        return true
    }

    /**
     * 数据加载完成
     */
    private fun resetView() {
        scrollAnimation(transY.toInt(), 0, duration = 300L, bounce = true,
                listener = object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        transY = 0f
                        refreshing = false
                        topView.visibility = View.INVISIBLE
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                    }

                    override fun onAnimationStart(animation: Animator?) {
                        canScrollUp = true
                    }

                })
        { v ->
            recyclerView.translationY = v.toFloat()
            topView.translationY = topOffset + v.toFloat()
        }
    }

    private fun scrollAnimation(vararg position: Int, duration: Long, bounce: Boolean, listener: Animator.AnimatorListener? = null, operation: (Int) -> Unit) {
        val valueAnimator = ValueAnimator()
        valueAnimator.interpolator = if (bounce) {
            AccelerateDecelerateInterpolator()
        } else {
            DecelerateInterpolator()
        }
        valueAnimator.duration = duration
        valueAnimator.setIntValues(position[0], position[1])
        valueAnimator.addUpdateListener { v ->
            val value = v.animatedValue as Int
            operation(value)
        }
        listener?.let {
            valueAnimator.addListener(it)
        }
        valueAnimator.start()
    }

}