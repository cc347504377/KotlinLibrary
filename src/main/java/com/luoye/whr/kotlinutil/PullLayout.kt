package com.example.whr.sensordemo

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.TextView
import com.luoye.whr.kotlinutil.BaseAdapter
import com.luoye.whr.kotlinutil.timer

/**
 * Created by whr on 10/18/17.
 */
class PullLayout : FrameLayout {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private val recyclerView: RecyclerView
    private val textView: TextView
    private var canScrollUp = true
    private var canScrollDown = true
    private var refreshEvent: (() -> Unit)? = null
    private var loadEvent: (() -> Unit)? = null
    private val handle = Handler()

    init {
        iniEvent()
        setBackgroundColor(Color.GRAY)
        recyclerView = RecyclerView(context).apply {
            this.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            this.layoutManager = LinearLayoutManager(context)
            this.setBackgroundColor(Color.WHITE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.translationZ = 50f
            }
            this.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    canScrollDown = recyclerView!!.canScrollVertically(-1)
                    canScrollUp = recyclerView!!.canScrollVertically(1)
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    canScrollDown = recyclerView!!.canScrollVertically(-1)
                    canScrollUp = recyclerView!!.canScrollVertically(1)
                }
            })
            this.adapter = object : BaseAdapter<String>(context, arrayListOf("哈哈", "恩恩", "Hello"), android.R.layout.simple_list_item_1) {

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
                    val viewHolder = holder as ViewHolder
                    viewHolder.textView.text = data[position]
                }

                override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder = ViewHolder(getView(parent))

                inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                    val textView = view.findViewById(android.R.id.text1) as TextView
                }
            }
        }
        textView = TextView(context).apply {
            this.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL).apply {
                this.setMargins(0, 100, 0, 0)
            }
            this.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18f)
            this.setTextColor(Color.WHITE)
            this.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            this.text = "哇哈哈"
        }
        addView(textView)
        addView(recyclerView)
    }

    private fun iniEvent() {
        refreshEvent = {
            timer(1000) {
                handle.post { refreshFinish() }
                refreshing = false
            }
        }
        loadEvent = {
            timer(1000) {
                handle.post { refreshFinish() }
                refreshing = false
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
                val scroll = event.y - lastY
                if (!canScrollUp && scroll < 0) {
                    return true
                }
                if (!canScrollDown && scroll > 0) {
                    return true
                }
            }
        }
        return false
    }

    //悬停距离
    private val scrollSize = 384
    private var refreshing = false
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event!!.action) {
            MotionEvent.ACTION_MOVE -> {
                //不能上滑
                val scroll = event.y - lastY
                if (!canScrollUp && scroll < 0) transY += scroll
                //不能下拉
                if (!canScrollDown && scroll > 0) transY += scroll
                lastY = event.y
                recyclerView.translationY = transY
            }
            MotionEvent.ACTION_UP -> {
                if (Math.abs(transY).toInt() >= scrollSize) {
                    val rawSize = if (transY > 0) {
                        if (!refreshing) {
                            refreshEvent?.invoke()
                            refreshing = true
                        }
                        scrollSize
                    } else {
                        if (!refreshing) {
                            loadEvent?.invoke()
                            refreshing = true
                        }
                        -scrollSize
                    }
                    scrollAnimation(transY.toInt(), rawSize, duration = 100L, bounce = false) { v ->
                        recyclerView.translationY = v.toFloat()
                        transY = v.toFloat()
                    }
                } else {
                    refreshFinish()
                }
            }
        }
        return true
    }

    private fun refreshFinish() {
        scrollAnimation(transY.toInt(), 0, duration = 500L, bounce = true) { v ->
            recyclerView.translationY = v.toFloat()
        }
        transY = 0f
    }

    private fun scrollAnimation(vararg position: Int, duration: Long, bounce: Boolean, operation: (Int) -> Unit) {
        val valueAnimator = ValueAnimator()
        valueAnimator.interpolator = if (bounce) {
            BounceInterpolator()
        } else {
            AccelerateInterpolator()
        }
        valueAnimator.duration = duration
        valueAnimator.setIntValues(position[0], position[1])
        valueAnimator.addUpdateListener { v ->
            val value = v.animatedValue as Int
            operation(value)
        }
        valueAnimator.start()
    }

}