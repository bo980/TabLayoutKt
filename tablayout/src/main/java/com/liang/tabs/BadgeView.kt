package com.liang.tabs


import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.floor

class BadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr), Badge {
    private var padding = 1
    private var mStroke: Int
    private var mStrokeColor: Int
    private var mBackgroundColor: Int
    private var mInitBackgroundFlag = false
    var dragListener: OnDragListener? = null

    interface OnDragListener {
        fun onDragOut()
    }

    fun setPadding(padding: Int) {
        this.padding = padding
    }

    private fun initBadge() {
        gravity = Gravity.CENTER
        visibility = View.GONE
        viewTreeObserver.addOnPreDrawListener(OnPreDrawListener {
            if (!mInitBackgroundFlag) {
                setBackgroundDrawable(createStateListDrawable())
                refreshPadding()
                mInitBackgroundFlag = true
                return@OnPreDrawListener false
            }
            true
        })
    }

    private fun refreshPadding() {
        val width = measuredWidth
        val height = measuredHeight
        val defPadding = dip2px(context, padding.toFloat())
        val length = text.length
        if (length == 1) {
            val mix = width - height
            val ipa = floor(mix / 2.0f.toDouble()).toInt()
            if (mix < 0) {
                setPadding(defPadding - ipa, defPadding, defPadding - ipa, defPadding)
            } else {
                setPadding(
                    paddingLeft.coerceAtLeast(defPadding),
                    defPadding,
                    paddingRight.coerceAtLeast(defPadding),
                    defPadding
                )
            }
        }
        if (length > 1) {
            setPadding(
                (defPadding + textSize / 2).toInt(),
                defPadding,
                (defPadding + textSize / 2).toInt(),
                defPadding
            )
        }
    }

    /**
     * Setting the background color of BadgeView
     *
     * @param color
     */
    override fun setBackgroundColor(@ColorInt color: Int) {
        mBackgroundColor = color
        setBackgroundDrawable(createStateListDrawable())
    }

    override fun setText(text: CharSequence, type: BufferType) {
        super.setText(text, type)
        mInitBackgroundFlag = false
    }

    /**
     * Setting the border of BadgeView
     *
     * @param width
     * @param color
     */
    fun setStroke(width: Int, @ColorInt color: Int) {
        mStroke = width
        mStrokeColor = color
        setBackgroundDrawable(createStateListDrawable())
    }

    private fun createStateListDrawable(): StateListDrawable {
        val bg =
            StateListDrawable()
        val gradientStateNormal = GradientDrawable()
        gradientStateNormal.setColor(mBackgroundColor)
        gradientStateNormal.shape = GradientDrawable.RECTANGLE
        gradientStateNormal.cornerRadius = 50f
        gradientStateNormal.setStroke(mStroke, mStrokeColor)
        bg.addState(View.EMPTY_STATE_SET, gradientStateNormal)
        return bg
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (text.toString().trim { it <= ' ' }.isEmpty()) {
            val pointWidth = dip2px(context, 10f)
            setMeasuredDimension(pointWidth, pointWidth)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * 显示BadgeView
     *
     * @param msg
     */
    override fun show(msg: String?) {
        text = msg
        if (visibility == View.VISIBLE) {
            return
        }
        visibility = View.VISIBLE
        val animation =
            AnimationUtils.loadAnimation(context, R.anim.badge_view_show)
        animation.interpolator = OvershootInterpolator()
        startAnimation(animation)
    }

    /**
     * 隐藏BadgeView
     */
    override fun hide() {
        if (visibility == View.GONE) {
            return
        }
        val animation =
            AnimationUtils.loadAnimation(context, R.anim.badge_view_hide)
        startAnimation(animation)
        animation.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    override fun setBadgeTextSize(sp: Float) {
        textSize = sp
    }

    override fun setBadgeBackgroundColor(color: Int) {
        mBackgroundColor = color
        setBackgroundDrawable(createStateListDrawable())
    }

    override fun setBadgeStroke(width: Int, color: Int) {
        mStroke = width
        mStrokeColor = color
        setBackgroundDrawable(createStateListDrawable())
    }

    override fun setBadgeTextColor(color: Int) {
        setTextColor(color)
    }

    fun dip2px(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            context.resources.displayMetrics
        ).toInt()
    }


    init {
        val typedArray = context.obtainStyledAttributes(
            attrs, R.styleable.BadgeView,
            defStyleAttr, 0
        )
        mStrokeColor = typedArray.getColor(
            R.styleable.BadgeView_badgeStrokeColor,
            Color.WHITE
        )
        mStroke = typedArray.getDimensionPixelSize(
            R.styleable.BadgeView_badgeStrokeWidth,
            dip2px(getContext(), 1f)
        )
        mBackgroundColor = typedArray.getColor(
            R.styleable.BadgeView_badgeBackgroundColor,
            Color.RED
        )
        typedArray.recycle()
        initBadge()
    }
}