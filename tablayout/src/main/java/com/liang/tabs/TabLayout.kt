package com.liang.tabs

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorListenerAdapter
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.database.DataSetObserver
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.liang.tabs.indicator.DefIndicatorEvaluator
import com.liang.tabs.indicator.Indicator
import com.liang.tabs.indicator.IndicatorPoint
import com.liang.tabs.indicator.TransitionIndicatorEvaluator
import com.liang.tabs.utils.*
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/**
 * TODO: document your custom view class.
 */

@Suppress("NAME_SHADOWING")
class TabLayout : HorizontalScrollView {

    companion object {
        const val MODE_SCROLLABLE = 0
        const val MODE_FIXED = 1
        const val INDICATOR_TIER_BACK = 0
        const val INDICATOR_TIER_FRONT = 1
    }

    private var currentVpSelectedListener: OnTabSelectedListener? = null
    private var contentInsetStart = 0
    var tabSelectedIndicator: Drawable? = null
        set(value) {
            if (field !== value) {
                field = value
                ViewCompat.postInvalidateOnAnimation(slidingTabIndicator)
            }
        }
    var tabIndicatorGravity = 0
        set(value) {
            if (field != value) {
                field = value
                ViewCompat.postInvalidateOnAnimation(slidingTabIndicator)
            }
        }
    var tabIndicatorFullWidth = false
        set(value) {
            if (field != value) {
                field = value
                ViewCompat.postInvalidateOnAnimation(slidingTabIndicator)
            }
        }
    var tabIndicatorTransitionScroll = false
    var tabScaleTransitionScroll = 0f
    var tabColorTransitionScroll = false
    var unboundedRipple = false
        set(value) {
            if (field != value) {
                field = value
                repeat(slidingTabIndicator.childCount) {
                    val child = slidingTabIndicator.getChildAt(it)
                    if (child is Tab) {
                        child.tabUnboundedRipple = field
                    }
                }
            }
        }

    var tabTextColors: ColorStateList? = null
        set(value) {
            if (field != value) {
                field = value

                repeat(slidingTabIndicator.childCount) {
                    val child = slidingTabIndicator.getChildAt(it)
                    if (child is Tab) {
                        child.tabTitleColors = field
                    }
                }
            }
        }


    var tabIconTint: ColorStateList? = null
        set(value) {
            if (field != value) {
                field = value
                repeat(slidingTabIndicator.childCount) {
                    val child = slidingTabIndicator.getChildAt(it)
                    if (child is Tab) {
                        child.tabIconTint = field
                    }
                }
            }
        }
    var tabRippleColor: Int = 0
        set(value) {
            if (field != value) {
                field = value
                repeat(slidingTabIndicator.childCount) {
                    val child = slidingTabIndicator.getChildAt(it)
                    if (child is Tab) {
                        child.tabRippleColor = field
                    }
                }
            }
        }
    private var tabIconTintMode: PorterDuff.Mode? = null

    var tabTextSize = 0
        set(value) {
            if (field != value) {
                field = value
                repeat(slidingTabIndicator.childCount) {
                    val child = slidingTabIndicator.getChildAt(it)
                    if (child is Tab) {
                        child.tabTitleSize = field.toFloat()
                    }
                }
            }
        }
    private var tabBackgroundResId = 0
    private var tabPaddingStart = 0
    private var tabPaddingTop = 0
    private var tabPaddingEnd = 0
    private var tabPaddingBottom = 0
    private var tabTextBold = false
    var inlineLabel: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                repeat(slidingTabIndicator.childCount) {
                    val child = slidingTabIndicator.getChildAt(it)
                    if (child is Tab) {
                        child.inlineLabel = value
                    }
                }
                applyModeAndGravity()
            }
        }
    private var selectedTab: Tab? = null
    private var requestedTabMinWidth = -1
    private var requestedTabMaxWidth = 0
    private var scrollableTabMinWidth = 200
    var tabMaxWidth = 2147483647
        set(value) {
            if (field != value) {
                field = value
                applyModeAndGravity()
            }
        }

    private var tabGravity: Int = 0
    var mode: Int = MODE_SCROLLABLE
        set(value) {
            if (field != value) {
                field = value
                applyModeAndGravity()
            }
        }
    private val tabs: ArrayList<Tab> = arrayListOf()
    private val slidingTabIndicator = SlidingTabIndicator(context)
    private val selectedListeners: ArrayList<OnTabSelectedListener> = arrayListOf()
    private var tabIndicatorAnimationDuration = 0
    private val tabViewContentBounds = RectF()

    var tabIndicatorTier = 0
        set(value) {
            field = value
            ViewCompat.postInvalidateOnAnimation(slidingTabIndicator)
        }
    var tabIndicatorWidth = 0
        set(value) {
            field = value
            ViewCompat.postInvalidateOnAnimation(slidingTabIndicator)
        }
    var tabIndicatorMargin = 0
        set(value) {
            field = value
            ViewCompat.postInvalidateOnAnimation(slidingTabIndicator)
        }
    var tabIndicatorWidthScale = 0f
        set(value) {
            field = value
            ViewCompat.postInvalidateOnAnimation(slidingTabIndicator)
        }
    private var selectedPosition = -1

    private var viewPager: ViewPager? = null
    private var pagerAdapter: PagerAdapter? = null
    private var pagerAdapterObserver: DataSetObserver? = null
    private var pageChangeListener: TabLayoutOnPageChangeListener? = null
    private var adapterChangeListener: AdapterChangeListener? = null
    private var setupViewPagerImplicitly = false

    private var scrollAnimator: ValueAnimator? = null
        get() {
            if (field == null) {
                field = ValueAnimator().apply {
                    interpolator = AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR
                    duration = tabIndicatorAnimationDuration.toLong()
                    addUpdateListener { animator ->
                        scrollTo(
                            (animator.animatedValue as Int),
                            0
                        )
                    }
                }
            }
            return field
        }

    private var scaleAnimator: ValueAnimator? = null

    private var tabDividerWidth = 0
    private var tabDividerHeight = 0
    private var tabDividerColor = 0

    private var itemDecoration: ItemDecoration? = null

    fun addItemDecoration(itemDecoration: ItemDecoration?) {
        this.itemDecoration = itemDecoration
        repeat(slidingTabIndicator.childCount) {
            val child = slidingTabIndicator.getChildAt(it)
            if (child is Tab) {
                child.tabDecoration = child.tabDecoration ?: itemDecoration
            }
        }

        applyModeAndGravity()
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }


    private fun init(attrs: AttributeSet?, defStyle: Int) {
        isHorizontalScrollBarEnabled = false
        super.addView(slidingTabIndicator, 0, LayoutParams(-2, -1))
        val typedArray = context.obtainStyledAttributes(
            attrs, R.styleable.TabLayout,
            defStyle, 0
        )
        slidingTabIndicator.setSelectedIndicatorHeight(
            typedArray.getDimensionPixelSize(
                R.styleable.TabLayout_tabIndicatorHeight,
                -1
            )
        )
        slidingTabIndicator.setSelectedIndicatorColor(
            typedArray.getColor(
                R.styleable.TabLayout_tabIndicatorColor,
                0
            )
        )
        this.tabSelectedIndicator = context.getDrawable(
            typedArray,
            R.styleable.TabLayout_tabIndicator
        )
        this.tabIndicatorGravity = typedArray.getInt(
            R.styleable.TabLayout_tabIndicatorGravity,
            0
        )

        tabPaddingStart = typedArray.getDimensionPixelSize(R.styleable.TabLayout_tabPadding, 0)
            .also { tabPaddingBottom = it }.also { tabPaddingEnd = it }
            .also { tabPaddingTop = it }
        tabPaddingStart = typedArray.getDimensionPixelSize(
            R.styleable.TabLayout_tabPaddingStart,
            tabPaddingStart
        )
        tabPaddingTop = typedArray.getDimensionPixelSize(
            R.styleable.TabLayout_tabPaddingTop,
            tabPaddingTop
        )
        tabPaddingEnd = typedArray.getDimensionPixelSize(
            R.styleable.TabLayout_tabPaddingEnd,
            tabPaddingEnd
        )
        tabPaddingBottom = typedArray.getDimensionPixelSize(
            R.styleable.TabLayout_tabPaddingBottom,
            tabPaddingBottom
        )


        this.tabIndicatorTier = typedArray.getInt(R.styleable.TabLayout_tabIndicatorTier, 0)
        this.tabIndicatorWidth =
            typedArray.getDimensionPixelSize(R.styleable.TabLayout_tabIndicatorWidth, 0)
        this.tabIndicatorMargin =
            typedArray.getDimensionPixelSize(R.styleable.TabLayout_tabIndicatorMargin, 0)
        this.tabIndicatorWidthScale =
            typedArray.getFloat(R.styleable.TabLayout_tabIndicatorWidthScale, 0f)

        if (typedArray.hasValue(R.styleable.TabLayout_tabTextColor)) {
            tabTextColors = context.getColorStateList(
                typedArray,
                R.styleable.TabLayout_tabTextColor
            )
        }

        tabTextSize = typedArray.getDimensionPixelSize(R.styleable.TabLayout_tabTextSize, 0)
        tabIconTint = context.getColorStateList(
            typedArray,
            R.styleable.TabLayout_tabIconTint
        )
        tabIconTintMode = parseTintMode(
            typedArray.getInt(R.styleable.TabLayout_tabIconTintMode, -1),
            null as PorterDuff.Mode?
        )
        tabIndicatorFullWidth =
            typedArray.getBoolean(R.styleable.TabLayout_tabIndicatorFullWidth, false)
        tabIndicatorTransitionScroll =
            typedArray.getBoolean(R.styleable.TabLayout_tabIndicatorTransitionScroll, false)
        tabScaleTransitionScroll =
            typedArray.getFloat(R.styleable.TabLayout_tabScaleTransitionScroll, 1.0f)
        tabColorTransitionScroll =
            typedArray.getBoolean(R.styleable.TabLayout_tabTextColorTransitionScroll, false)
        tabTextBold = typedArray.getBoolean(R.styleable.TabLayout_tabTextBold, false)

        tabRippleColor = typedArray.getColor(
            R.styleable.TabLayout_tabRippleColor, 0
        )
        tabIndicatorAnimationDuration =
            typedArray.getInt(R.styleable.TabLayout_tabIndicatorAnimationDuration, 300)
        requestedTabMinWidth =
            typedArray.getDimensionPixelSize(R.styleable.TabLayout_tabMinWidth, -1)
        requestedTabMaxWidth =
            typedArray.getDimensionPixelSize(R.styleable.TabLayout_tabMaxWidth, -1)
        tabBackgroundResId = typedArray.getResourceId(R.styleable.TabLayout_tabBackground, 0)
        contentInsetStart =
            typedArray.getDimensionPixelSize(R.styleable.TabLayout_tabContentStart, 0)
        mode = typedArray.getInt(R.styleable.TabLayout_tabMode, 0)
        tabGravity = typedArray.getInt(R.styleable.TabLayout_tabGravity, 0)
        inlineLabel = typedArray.getBoolean(R.styleable.TabLayout_tabInlineLabel, false)
        unboundedRipple =
            typedArray.getBoolean(R.styleable.TabLayout_tabUnboundedRipple, false)


        tabDividerWidth =
            typedArray.getDimensionPixelSize(R.styleable.TabLayout_tabDividerWidth, 0)
        tabDividerHeight =
            typedArray.getDimensionPixelSize(R.styleable.TabLayout_tabDividerHeight, 0)
        tabDividerColor = typedArray.getColor(R.styleable.TabLayout_tabDividerColor, 0)

        itemDecoration =
            if (tabDividerWidth > 0) TabImp.DefItemDecoration(
                tabDividerWidth,
                tabDividerHeight
            ).apply {
                color = tabDividerColor
            } else null

        typedArray.recycle()

        val res = this.resources
        scrollableTabMinWidth =
            res.getDimensionPixelSize(R.dimen.design_tab_scrollable_min_width)
        applyModeAndGravity()

    }

    fun addOnTabSelectedListener(listener: OnTabSelectedListener) {
        if (!selectedListeners.contains(listener)) {
            selectedListeners.add(listener)
        }
    }

    fun removeOnTabSelectedListener(listener: OnTabSelectedListener) {
        selectedListeners.remove(listener)
    }

    fun clearOnTabSelectedListeners() {
        selectedListeners.clear()
    }

    fun setIndicator(indicator: Indicator) {
        tabIndicatorWidth = indicator.width
        tabIndicatorMargin = indicator.margin
        tabIndicatorWidthScale = indicator.widthScale
        tabIndicatorFullWidth = indicator.isFullWidth
        tabIndicatorGravity = indicator.gravity
        tabIndicatorTransitionScroll = indicator.isTransitionScroll
        tabSelectedIndicator = indicator.getDrawable()
        slidingTabIndicator.setSelectedIndicatorHeight(indicator.height)
        slidingTabIndicator.setSelectedIndicatorColor(indicator.color)

    }

    fun setSelectedTabIndicatorColor(@ColorInt color: Int) {
        slidingTabIndicator.setSelectedIndicatorColor(color)
    }

    /**
     * Show Badge
     *
     * @param position
     */
    fun showBadgeMsg(position: Int) {
        showBadgeMsg(position, "", true)
    }

    /**
     * Show Badge
     *
     * @param position
     * @param count
     */
    fun showBadgeMsg(position: Int, count: Int) {
        showBadgeMsg(position, count.toString() + "", count > 0)
    }

    fun showBadgeMsg(position: Int, msg: String) {
        showBadgeMsg(position, msg, msg.trim { it <= ' ' }.isNotEmpty())
    }

    /**
     * Show Badge
     *
     * @param position
     * @param msg
     * @param showDot
     */
    fun showBadgeMsg(
        position: Int,
        msg: String?,
        showDot: Boolean
    ) {
        val tab = slidingTabIndicator.getChildAt(position)
        if (tab is Tab) {
            if (showDot) {
                tab.showBadge(msg)
            } else {
                tab.hideBadge()
            }
        }
    }

    /**
     * Setting the font color of Badge
     *
     * @param color
     */
    fun setBadgeTextColor(@ColorInt color: Int) {
        repeat(slidingTabIndicator.childCount) {
            setBadgeTextColor(it, color)
        }
    }

    /**
     * Set the font color of the specified Badge
     *
     * @param position
     * @param color
     */
    fun setBadgeTextColor(position: Int, @ColorInt color: Int) {
        val tab = slidingTabIndicator.getChildAt(position)
        if (tab is Tab) {
            tab.setBadgeTextColor(color)
        }
    }

    /**
     * Set the font size of the specified Badge
     *
     * @param position
     * @param textSize
     */
    fun setBadgeTextSize(position: Int, textSize: Int) {
        val tab = slidingTabIndicator.getChildAt(position)
        if (tab is Tab) {
            tab.setBadgeTextSize(textSize.toFloat())
        }
    }

    fun setBadgeTextSize(textSize: Int) {
        repeat(slidingTabIndicator.childCount) {
            setBadgeTextSize(it, textSize)
        }
    }

    /**
     * Setting the background color of Badge
     *
     * @param color
     */
    fun setBadgeColor(@ColorInt color: Int) {
        repeat(slidingTabIndicator.childCount) {
            setBadgeColor(it, color)
        }
    }

    /**
     * Set the background color of the specified Badge
     *
     * @param position
     * @param color
     */
    fun setBadgeColor(position: Int, @ColorInt color: Int) {
        val tab = slidingTabIndicator.getChildAt(position)
        if (tab is Tab) {
            tab.setBadgeBackgroundColor(color)
        }
    }

    /**
     * Set the border and color of the Badge
     *
     * @param width
     * @param color
     */
    fun setBadgeStroke(width: Int, @ColorInt color: Int) {
        repeat(slidingTabIndicator.childCount) {
            setBadgeStroke(it, width, color)
        }
    }

    /**
     * Set the border and color of the specified Badge
     *
     * @param width
     * @param color
     */
    fun setBadgeStroke(position: Int, width: Int, @ColorInt color: Int) {
        val tab = slidingTabIndicator.getChildAt(position)
        if (tab is Tab) {
            tab.setBadgeStroke(width, color)
        }
    }


    fun setTabGravity(gravity: Int) {
        if (tabGravity != gravity) {
            tabGravity = gravity
            applyModeAndGravity()
        }
    }


    fun setTabTextColors(normalColor: Int, selectedColor: Int) {
        this.tabTextColors = createColorStateList(normalColor, selectedColor)
    }


    fun setTabIconTintResource(@ColorRes iconTintResourceId: Int) {
        this.tabIconTint = AppCompatResources.getColorStateList(
            this.context,
            iconTintResourceId
        )
    }


    fun setTabRippleColorResource(@ColorRes tabRippleColorId: Int) {
        this.tabRippleColor = resources.getColor(tabRippleColorId)
    }

    fun setSelectedTabIndicator(@DrawableRes tabSelectedIndicatorResourceId: Int) {
        if (tabSelectedIndicatorResourceId != 0) {
            this.tabSelectedIndicator = AppCompatResources.getDrawable(
                this.context,
                tabSelectedIndicatorResourceId
            )
        } else {
            this.tabSelectedIndicator = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (viewPager == null) {
            val vp = this.parent
            if (vp is ViewPager) {
                setupWithViewPager(vp, autoRefresh = true, implicitSetup = true)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (setupViewPagerImplicitly) {
            setupWithViewPager(null as ViewPager?)
            setupViewPagerImplicitly = false
        }
    }

    @SuppressLint("WrongConstant", "SwitchIntDef")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasureSpec = heightMeasureSpec
        val idealHeight: Int =
            this.dpToPx(this.defaultHeight) + this.paddingTop + this.paddingBottom
        when (MeasureSpec.getMode(heightMeasureSpec)) {
            -2147483648 -> heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                idealHeight.coerceAtMost(MeasureSpec.getSize(heightMeasureSpec)), 1073741824
            )
            0 -> heightMeasureSpec = MeasureSpec.makeMeasureSpec(idealHeight, 1073741824)
        }
        val specWidth = MeasureSpec.getSize(widthMeasureSpec)
        if (MeasureSpec.getMode(widthMeasureSpec) != 0) {
            this.tabMaxWidth =
                if (requestedTabMaxWidth > 0) requestedTabMaxWidth else specWidth - this.dpToPx(
                    56
                )
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (this.childCount == 1) {
            val child = getChildAt(0)
            var remeasure = false
            when (mode) {
                0 -> remeasure = child.measuredWidth < this.measuredWidth
                1 -> remeasure = child.measuredWidth != this.measuredWidth
            }
            if (remeasure) {
                val childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(
                    heightMeasureSpec,
                    this.paddingTop + this.paddingBottom,
                    child.layoutParams.height
                )
                val childWidthMeasureSpec =
                    MeasureSpec.makeMeasureSpec(this.measuredWidth, 1073741824)
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
            }
        }
    }

    override fun draw(canvas: Canvas?) {
        repeat(slidingTabIndicator.childCount) {
            val child = slidingTabIndicator.getChildAt(it)
            if (child is Tab) {
                child.drawBackground(canvas!!)
            }
        }
        super.draw(canvas)
    }

    @get:Dimension(unit = 0)
    private val defaultHeight: Int
        get() {
            var hasIconAndText = false
            var i = 0
            val count = tabs.size
            while (i < count) {
                val tab: Tab = tabs[i]
                if (tab.defaultIcon != null && !tab.title.isNullOrEmpty() && !tab.inlineLabel) {
                    hasIconAndText = true
                    break
                }
                ++i
            }
            return if (hasIconAndText) 72 else 48
        }


    private fun applyModeAndGravity() {
        var paddingStart = 0
        if (mode == 0) {
            paddingStart = 0.coerceAtLeast(contentInsetStart - tabPaddingStart)
        }
        ViewCompat.setPaddingRelative(slidingTabIndicator, paddingStart, 0, 0, 0)
        when (mode) {
            0 -> slidingTabIndicator.gravity = 8388611
            1 -> slidingTabIndicator.gravity = 1
        }
        updateTabViews()
    }

    @JvmOverloads
    fun addTab(
        tab: Tab,
        position: Int = tabs.size,
        setSelected: Boolean = tabs.isEmpty()
    ) {

        if (tab.tabLayout !== this) {
            tab.tabLayout = this@TabLayout
        }

        this.configureTab(tab, position)
        this.addTabView(tab)
        if (setSelected) {
            tab.select()
        }

        updateTabViews()
    }


    override fun addView(child: View) {
        addViewInternal(child)
    }

    override fun addView(child: View, index: Int) {
        addViewInternal(child)
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams?) {
        addViewInternal(child)
    }

    override fun addView(
        child: View,
        index: Int,
        params: ViewGroup.LayoutParams?
    ) {
        addViewInternal(child)
    }

    private fun addViewInternal(child: View) {
        if (child is Tab) {
            addTab((child as Tab))
        } else {
            throw IllegalArgumentException("Only Tab instances can be added to TabLayout")
        }
    }

    fun removeTab(tab: Tab) {
        removeTabAt(tab.position)
    }

    fun removeTabAt(position: Int) {
        val selectedTabPosition = selectedTab?.position ?: 0
        this.removeTabViewAt(position)
        tabs.removeAt(position).reset()
        val newTabCount = tabs.size
        for (i in position until newTabCount) {
            tabs[i].position = i
        }
        if (selectedTabPosition == position) {
            if (tabs.isNotEmpty()) {
                selectTab(
                    tabs[0.coerceAtLeast(position - 1)]
                )
            }

        }
    }

    fun removeAllTabs() {
        for (i in slidingTabIndicator.childCount - 1 downTo 0) {
            this.removeTabViewAt(i)
        }
        val i: MutableIterator<*> = tabs.iterator()
        while (i.hasNext()) {
            var any = i.next()
            i.remove()
            if (any is Tab) {
                any.reset()
            }
        }
        selectedTab = null
        selectedPosition = -1
    }


    private fun removeTabViewAt(position: Int) {
        val view = slidingTabIndicator.getChildAt(position)
        slidingTabIndicator.removeViewAt(position)
        if (view is Tab) {
            view.reset()
        }
        requestLayout()
    }


    private fun configureTab(
        tab: Tab,
        position: Int
    ) {
        tab.position = position

        tab.tabTitleColors = tab.tabTitleColors ?: tabTextColors

        tab.tabIconTint = tab.tabIconTint ?: tabIconTint

        tab.tabIconTintMode = tab.tabIconTintMode ?: tabIconTintMode

        tab.tabTitleSize = if (tab.tabTitleSize != 0f) tab.tabTitleSize else tabTextSize.toFloat()

        tab.tabBackgroundResId =
            if (tab.tabBackgroundResId != 0) tab.tabBackgroundResId else tabBackgroundResId

        tab.tabRippleColor = if (tab.tabRippleColor != 0) tab.tabRippleColor else tabRippleColor

        tab.inlineLabel = inlineLabel

        tab.isTabTitleBold = tabTextBold

        tab.setTabPadding(
            tabPaddingStart,
            tabPaddingTop,
            tabPaddingEnd,
            tabPaddingBottom
        )

        if (position > 0) {
            tab.tabDecoration = tab.tabDecoration ?: itemDecoration
        }

        tabs.add(position, tab)
        val count = tabs.size
        for (i in position + 1 until count) {
            tabs[i].apply {
                this.position = i
            }
        }
    }


    private fun addTabView(tab: Tab) {
        val tabView = tab.view
        slidingTabIndicator.addView(
            tabView,
            tab.position,
            this.createLayoutParamsForTabs(tab)
        )
    }

    fun newTab(): Tab {
        return this.createTabView()
    }

    private fun createTabView(): Tab {
        val tabView = TabImp(context)
        tabView.view.isFocusable = true
        tabView.view.minimumWidth = this.tabMinWidth
        return tabView
    }

    fun getTabCount(): Int {
        return tabs.size
    }

    fun getTabAt(index: Int): Tab? {
        return if (index >= 0 && index < getTabCount()) tabs[index] else null
    }

    private fun createLayoutParamsForTabs(tab: Tab): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(-2, -1)
        this.updateTabViewLayoutParams(lp, tab)
        return lp
    }

    private fun updateTabViewLayoutParams(lp: LinearLayout.LayoutParams, tab: Tab) {
        lp.leftMargin = tab.tabDecoration?.width ?: 0
        if (this.mode == 1 && this.tabGravity == 0) {
            lp.width = 0
            lp.weight = 1.0f
        } else {
            lp.width = -2
            lp.weight = 0.0f
        }
    }


    private fun updateTabViews() {
        repeat(slidingTabIndicator.childCount) {
            slidingTabIndicator.getChildAt(it).apply {
                minimumWidth = tabMinWidth
                updateTabViewLayoutParams(
                    (layoutParams as LinearLayout.LayoutParams),
                    this as Tab
                )
            }.requestLayout()
        }
    }

    private val tabMinWidth: Int
        get() = if (requestedTabMinWidth != -1) {
            requestedTabMinWidth
        } else {
            if (mode == 0) scrollableTabMinWidth else 0
        }

    @JvmOverloads
    fun selectTab(position: Int, updateIndicator: Boolean = true, isCallback: Boolean = true) {
        val child = slidingTabIndicator.getChildAt(position)
        if (child is Tab) {
            selectTab(child, updateIndicator, isCallback)
        }
    }

    @JvmOverloads
    fun selectTab(tab: Tab, updateIndicator: Boolean = true, isCallback: Boolean = true) {
        Log.e("Tab", "selectTab...")
        val currentTab = selectedTab
        if (currentTab === tab) {
            dispatchTabReselected(tab)
            animateToTab(tab.position)
        } else {
            val newPosition = tab.position
            if (updateIndicator) {
                if ((currentTab == null || currentTab.position == -1) && newPosition != -1) {
                    this.setScrollPosition(newPosition, 0.0f, true)
                } else {
                    animateToTab(newPosition)
                }
                if (newPosition != -1) {
                    this.setSelectedTabView(newPosition)
                    updateScaleOrColorPosition(newPosition, 1f)
                }
            }
            selectedTab = tab
            if (currentTab != null && isCallback) {
                dispatchTabUnselected(currentTab)
            }
            if (isCallback) {
                dispatchTabSelected(tab)
            }
        }
    }

    fun getSelectedTabPosition(): Int {
        return selectedTab?.position ?: -1
    }

    private fun setSelectedTabView(
        position: Int,
        isViewpagerScroll: Boolean = false
    ) {
        if (selectedPosition == position) {
            return
        }
        val selectedChild = slidingTabIndicator.getChildAt(selectedPosition)
        val newChild = slidingTabIndicator.getChildAt(position)
        selectedPosition = position
        if (isViewpagerScroll) {
            updateTab(selectedChild, newChild, false, tabColorTransitionScroll)
            return
        }
        val isScale = tabScaleTransitionScroll > 1f
        if (!isScale && !tabColorTransitionScroll) {
            updateTab(selectedChild, newChild, false, false)
            return
        }

        scaleAnimator?.let {
            if (it.isRunning) {
                it.cancel()
            }
        }

        val selectedScaleValues =
            PropertyValuesHolder.ofFloat("selectedScale", tabScaleTransitionScroll, 1.0f)
        val newScaleValues =
            PropertyValuesHolder.ofFloat("newScale", 1.0f, tabScaleTransitionScroll)
        val selectedColorValues =
            PropertyValuesHolder.ofFloat("selectedColor", 1.0f, .0f)
        val newColorValues =
            PropertyValuesHolder.ofFloat("newColor", .0f, 1.0f)

        scaleAnimator = ValueAnimator.ofPropertyValuesHolder(
            selectedScaleValues,
            newScaleValues,
            selectedColorValues,
            newColorValues
        ).apply {
            this.duration = 200
            this.interpolator = AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR
            this.addUpdateListener { animation ->
                val selectedScale =
                    animation.getAnimatedValue("selectedScale") as Float
                val newScale = animation.getAnimatedValue("newScale") as Float
                val selectedColor =
                    animation.getAnimatedValue("selectedColor") as Float
                val newColor = animation.getAnimatedValue("newColor") as Float
                if (selectedChild is Tab) { //                    selectedChild.setPivotX(100);
                    //                    selectedChild.setPivotY(selectedChild.getBottom());
                    if (isScale) {
                        (selectedChild as Tab).updateScale(selectedScale)
                    }
                    if (tabColorTransitionScroll) {
                        (selectedChild as Tab).updateColor(selectedColor)
                    }
                }
                if (newChild is Tab) {
                    if (isScale) {
                        (newChild as Tab).updateScale(newScale)
                    }
                    if (tabColorTransitionScroll) {
                        (newChild as Tab).updateColor(newColor)
                    }
                }
            }
            this.addListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    updateTab(selectedChild, newChild, isScale, tabColorTransitionScroll)
                }

                override fun onAnimationCancel(animation: Animator) {
                    updateTab(selectedChild, newChild, isScale, tabColorTransitionScroll)
                }

                override fun onAnimationRepeat(animation: Animator) {}
            })
        }?.also {
            it.start()
        }
    }

    private fun updateTab(
        selectedChild: View?,
        newChild: View?,
        isScale: Boolean,
        isColorTransitionScroll: Boolean
    ) {
        if (selectedChild != null) {
            selectedChild.isSelected = false
            selectedChild.isActivated = false
        }
        if (newChild != null) {
            newChild.isSelected = true
            newChild.isActivated = true
        }
        if (selectedChild is Tab) {
            if (isScale) {
                (selectedChild as Tab).updateScale(1.0f)
            }
            if (isColorTransitionScroll) {
                (selectedChild as Tab).updateColor(.0f)
            }
        }
        if (newChild is Tab) {
            if (isScale) {
                (newChild as Tab).updateScale(tabScaleTransitionScroll)
            }
            if (isColorTransitionScroll) {
                (newChild as Tab).updateColor(1.0f)
            }
        }
    }

    private fun updateScaleOrColorPosition(position: Int, positionOffset: Float) {
        if (position < 0 || position > getTabCount() - 1) {
            return
        }
        if (selectedTab != null && position == selectedTab?.position) {
            return
        }
        if (selectedTab != null && selectedTab?.position!! > -1) {
            (slidingTabIndicator.getChildAt(selectedTab?.position!!) as Tab).updateScale(1.0f)
        }
        val scale = tabScaleTransitionScroll - 1.0f
        (slidingTabIndicator.getChildAt(position) as Tab).updateScale(1.0f + scale * positionOffset)
    }

    private fun dispatchTabSelected(tab: Tab) {
        selectedListeners.forEach {
            it.onTabSelected(tab)
        }
    }

    private fun dispatchTabUnselected(tab: Tab) {
        selectedListeners.forEach {
            it.onTabUnselected(tab)
        }
    }

    private fun dispatchTabReselected(tab: Tab) {
        selectedListeners.forEach {
            it.onTabReselected(tab)
        }
    }


    private fun animateToTab(newPosition: Int) {
        if (newPosition != -1) {
            if (this.windowToken != null && ViewCompat.isLaidOut(this) && !slidingTabIndicator.childrenNeedLayout) {
                val startScrollX = this.scrollX
                val targetScrollX: Int = this.calculateScrollXForTab(newPosition, 0.0f)
                if (startScrollX != targetScrollX) {
                    this.scrollAnimator?.setIntValues(startScrollX, targetScrollX)
                    this.scrollAnimator?.start()
                }
                slidingTabIndicator.animateIndicatorToPosition(
                    newPosition,
                    this.tabIndicatorAnimationDuration
                )
            } else {
                this.setScrollPosition(newPosition, 0.0f, true)
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun calculateScrollXForTab(position: Int, positionOffset: Float): Int {
        return if (mode == 0) {
            val selectedChild = slidingTabIndicator.getChildAt(position)
            val nextChild = slidingTabIndicator.getChildAt(position + 1)
            val selectedWidth = selectedChild?.width ?: 0
            val nextWidth = nextChild?.width ?: 0
            val selectedDividerWidth =
                if (selectedChild is Tab) selectedChild.tabDecoration?.width ?: 0 else 0
            val nextDividerWidth =
                if (nextChild is Tab) nextChild.tabDecoration?.width ?: 0 else 0
            val scrollBase =
                selectedChild.left - selectedDividerWidth / 2 + (selectedWidth + selectedDividerWidth) / 2 - this.width / 2
            val scrollOffset =
                ((selectedWidth + nextWidth + selectedDividerWidth + nextDividerWidth) * 0.5f * positionOffset).toInt()
            if (ViewCompat.getLayoutDirection(this) == 0) scrollBase + scrollOffset else scrollBase - scrollOffset
        } else 0
    }

    private fun setScrollPosition(
        position: Int,
        positionOffset: Float,
        updateSelectedText: Boolean,
        updateIndicatorPosition: Boolean = true
    ) {
        val roundedPosition = (position.toFloat() + positionOffset).roundToInt()
        if (roundedPosition >= 0 && roundedPosition < slidingTabIndicator.childCount) {
            if (updateIndicatorPosition) {
                slidingTabIndicator.setIndicatorPositionFromTabPosition(
                    position,
                    positionOffset
                )
            }
            if (scrollAnimator != null && scrollAnimator!!.isRunning) {
                scrollAnimator!!.cancel()
            }

            val scrollXForTab = calculateScrollXForTab(position, positionOffset)
//            Log.e("setScrollPosition", "scrollXForTab: $scrollXForTab")

            scrollTo(scrollXForTab, 0)

            if (updateSelectedText) {
                setSelectedTabView(roundedPosition, true)
            }
            if (roundedPosition >= 0 && roundedPosition < slidingTabIndicator.childCount) {
                if (positionOffset > 0 && position + 1 < slidingTabIndicator.childCount) {
                    if (tabScaleTransitionScroll > 1) {
                        val scale = tabScaleTransitionScroll - 1.0f
                        (slidingTabIndicator.getChildAt(position) as Tab).updateScale(1.0f + scale * (1.0f - positionOffset))
                        (slidingTabIndicator.getChildAt(position + 1) as Tab).updateScale(1.0f + scale * positionOffset)
                    }
                    if (tabColorTransitionScroll) {
                        (slidingTabIndicator.getChildAt(position) as Tab).updateColor(1.0f - positionOffset)
                        (slidingTabIndicator.getChildAt(position + 1) as Tab).updateColor(
                            positionOffset
                        )
                    }
                }
            }
        }
    }

    @JvmOverloads
    fun setupWithViewPager(
        viewPager: ViewPager?, autoRefresh: Boolean = true,
        implicitSetup: Boolean = false
    ) {
        this.viewPager?.let {
            if (pageChangeListener != null) {
                it.removeOnPageChangeListener(pageChangeListener!!)
            }
            if (adapterChangeListener != null) {
                it.removeOnAdapterChangeListener(adapterChangeListener!!)
            }
        }

        currentVpSelectedListener?.let {
            this.removeOnTabSelectedListener(it)
        }
        this.currentVpSelectedListener = null

        if (viewPager != null) {
            this.viewPager = viewPager
            if (pageChangeListener == null) {
                pageChangeListener = TabLayoutOnPageChangeListener(this)
            }
            pageChangeListener?.reset()
            viewPager.addOnPageChangeListener(pageChangeListener!!)
            this.currentVpSelectedListener = ViewPagerOnTabSelectedListener(viewPager)
            this.addOnTabSelectedListener(this.currentVpSelectedListener!!)
            val adapter = viewPager.adapter
            if (adapter != null) {
                setPagerAdapter(adapter, autoRefresh)
            }
            if (adapterChangeListener == null) {
                adapterChangeListener = AdapterChangeListener()
            }
            adapterChangeListener?.setAutoRefresh(autoRefresh)
            viewPager.addOnAdapterChangeListener(adapterChangeListener!!)
            setScrollPosition(viewPager.currentItem, 0.0f, true)
        } else {
            this.viewPager = null
            setPagerAdapter(null, false)
        }
        setupViewPagerImplicitly = implicitSetup
    }

    fun setPagerAdapter(adapter: PagerAdapter?, addObserver: Boolean) {
        if (pagerAdapter != null && pagerAdapterObserver != null) {
            pagerAdapter?.unregisterDataSetObserver(pagerAdapterObserver!!)
        }
        pagerAdapter = adapter
        if (addObserver && adapter != null) {
            if (pagerAdapterObserver == null) {
                pagerAdapterObserver = PagerAdapterObserver()
            }
            adapter.registerDataSetObserver(pagerAdapterObserver!!)
        }
        populateFromPagerAdapter()
    }

    private inner class PagerAdapterObserver internal constructor() : DataSetObserver() {
        override fun onChanged() {
            populateFromPagerAdapter()
        }

        override fun onInvalidated() {
            populateFromPagerAdapter()
        }
    }

    fun populateFromPagerAdapter() {
        this.removeAllTabs()
        pagerAdapter?.let { pagerAdapter ->
            val adapterCount = pagerAdapter.count
            var curItem: Int
            curItem = 0
            while (curItem < adapterCount) {
                if (pagerAdapter is TabAdapter) {
                    val tab: Tab = (pagerAdapter as TabAdapter).getTab(curItem)
                    addTab(
                        tab.setTitle(pagerAdapter.getPageTitle(curItem).toString()),
                        setSelected = false
                    )
                } else {
                    addTab(
                        this.newTab().setTitle(pagerAdapter.getPageTitle(curItem).toString()),
                        setSelected = false
                    )
                }
                ++curItem
            }
            if (viewPager != null && adapterCount > 0) {
                curItem = viewPager!!.currentItem
                if (curItem != getSelectedTabPosition() && curItem < getTabCount()) {
                    selectTab(getTabAt(curItem)!!)
                }
            }
        }

    }


    private inner class ViewPagerOnTabSelectedListener(private val viewPager: ViewPager) :
        OnTabSelectedListener {

        override fun onTabSelected(tab: Tab) {
            viewPager.currentItem = tab.position
        }

        override fun onTabUnselected(item: Tab) {
        }

        override fun onTabReselected(item: Tab) {
        }


    }

    private inner class SlidingTabIndicator(context: Context) : LinearLayout(context) {

        private var selectedIndicatorHeight = 0
        private val selectedIndicatorPaint: Paint = Paint()
        private val defaultSelectionIndicator: GradientDrawable = GradientDrawable()
        var selectedPosition = -1
        var selectionOffset = 0f
        private val direction = -1
        private var indicatorAnimator: ValueAnimator? = null
        private val indicatorPoint = IndicatorPoint()

        init {
            setWillNotDraw(false)
        }


        @SuppressLint("WrongConstant")
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            if (MeasureSpec.getMode(widthMeasureSpec) == 1073741824) {
                if (mode == 1 && tabGravity == 1) {
                    val count = this.childCount
                    var largestTabWidth = 0
                    var gutter = 0
                    while (gutter < count) {
                        val child = this.getChildAt(gutter)
                        if (child.visibility == 0) {
                            largestTabWidth =
                                Math.max(largestTabWidth, child.measuredWidth)
                        }
                        ++gutter
                    }
                    if (largestTabWidth <= 0) {
                        return
                    }
                    gutter = dpToPx(16)
                    var remeasure = false
                    if (largestTabWidth * count > this.measuredWidth - gutter * 2) {
                        tabGravity = 0
                        updateTabViews()
                        remeasure = true
                    } else {
                        for (i in 0 until count) {
                            val lp =
                                this.getChildAt(i).layoutParams as LayoutParams
                            if (lp.width != largestTabWidth || lp.weight != 0.0f) {
                                lp.width = largestTabWidth
                                lp.weight = 0.0f
                                remeasure = true
                            }
                        }
                    }
                    if (remeasure) {
                        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                    }
                }
            }
        }

        override fun onLayout(
            changed: Boolean,
            l: Int,
            t: Int,
            r: Int,
            b: Int
        ) {
            super.onLayout(changed, l, t, r, b)
            if (indicatorAnimator != null && indicatorAnimator!!.isRunning) {
                indicatorAnimator!!.cancel()
                val duration = indicatorAnimator!!.duration
                animateIndicatorToPosition(
                    selectedPosition,
                    ((1.0f - indicatorAnimator!!.animatedFraction) * duration.toFloat()).roundToInt()
                )
            } else {
                updateIndicatorPosition()
            }
        }

        override fun draw(canvas: Canvas) {
            drawDivider(canvas)
            if (tabIndicatorTier == INDICATOR_TIER_BACK) {
                drawIndicator(canvas)
                super.draw(canvas)
            } else {
                super.draw(canvas)
                drawIndicator(canvas)
            }
        }

        private fun drawDivider(canvas: Canvas) {
            repeat(childCount) {
                val child = getChildAt(it)
                if (child is Tab) {
                    child.tabDecoration?.let { itemDecoration ->
                        val itemDecorationHeight =
                            if (itemDecoration.height > 0) itemDecoration.height else height
                        val rect = Rect(
                            child.left - itemDecoration.width,
                            (height - itemDecorationHeight) / 2,
                            child.left,
                            (height - itemDecorationHeight) / 2 + itemDecorationHeight
                        )
                        itemDecoration.onDraw(canvas, rect)
                    }

                }
            }
        }

        val childrenNeedLayout: Boolean
            get() {
                var i = 0
                val z = this.childCount
                while (i < z) {
                    val child = getChildAt(i)
                    if (child.width <= 0) {
                        return true
                    }
                    ++i
                }
                return false
            }

        fun animateIndicatorToPosition(position: Int, duration: Int) {
            indicatorAnimator?.let {
                if (it.isRunning) {
                    it.cancel()
                }
            }

            val targetView = this.getChildAt(position)
            if (targetView == null) {
                updateIndicatorPosition()
            } else {
                val target = IndicatorPoint()
                target.left = targetView.left.toFloat()
                target.right = targetView.right.toFloat()
                if (!tabIndicatorFullWidth && targetView is Tab) {
                    calculateTabViewContentBounds(
                        (targetView as Tab),
                        this@TabLayout.tabViewContentBounds
                    )
                    target.left = this@TabLayout.tabViewContentBounds.left
                    target.right = this@TabLayout.tabViewContentBounds.right
                }
                if (indicatorPoint != target) {
                    indicatorAnimator = ValueAnimator.ofObject(
                        if (tabIndicatorTransitionScroll) TransitionIndicatorEvaluator() else DefIndicatorEvaluator(),
                        indicatorPoint,
                        target
                    ).apply {
                        this.interpolator = AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR
                        this.duration = duration.toLong()
//                    animator.setFloatValues(new float[]{0.0F, 1.0F});
//                    final int finalTargetLeft = targetLeft;
//                    final int finalTargetRight = targetRight;
                        this.addUpdateListener { animator ->
                            val p: IndicatorPoint = animator.animatedValue as IndicatorPoint
                            setIndicatorPosition(p.left, p.right)
                        }
                        this.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animator: Animator) {
                                selectedPosition = position
                                selectionOffset = 0.0f
                            }
                        })
                    }.also { it.start() }

                }
            }
        }

        private fun updateIndicatorPosition() {
            val selectedTab = this.getChildAt(selectedPosition)
            var left: Int
            var right: Int
            if (selectedTab != null && selectedTab.width > 0) {
                left = selectedTab.left
                right = selectedTab.right
                if (!this@TabLayout.tabIndicatorFullWidth && selectedTab is Tab) {
                    calculateTabViewContentBounds(
                        (selectedTab as Tab),
                        this@TabLayout.tabViewContentBounds
                    )
                    left = this@TabLayout.tabViewContentBounds.left.toInt()
                    right = this@TabLayout.tabViewContentBounds.right.toInt()
                }
                if (selectionOffset > 0.0f && selectedPosition < this.childCount - 1) {
                    val nextTitle = this.getChildAt(selectedPosition + 1)
                    var nextTitleLeft = nextTitle.left
                    var nextTitleRight = nextTitle.right
                    if (!this@TabLayout.tabIndicatorFullWidth && nextTitle is Tab) {
                        calculateTabViewContentBounds(
                            (nextTitle as Tab),
                            this@TabLayout.tabViewContentBounds
                        )
                        nextTitleLeft = this@TabLayout.tabViewContentBounds.left.toInt()
                        nextTitleRight = this@TabLayout.tabViewContentBounds.right.toInt()
                    }
                    if (tabIndicatorTransitionScroll) {
                        var offR = selectionOffset * 2 - 1
                        var offL = selectionOffset * 2
                        if (selectedPosition + 1 < selectedPosition && selectionOffset > 0) {
                            if (offR < 0) {
                                offR = 0f
                            }
                            if (1 - offL < 0) {
                                offL = 1f
                            }
                        } else {
                            offL = selectionOffset * 2 - 1
                            offR = selectionOffset * 2
                            if (offL < 0) {
                                offL = 0f
                            }
                            if (1 - offR < 0) {
                                offR = 1f
                            }
                        }
                        left += ((nextTitleLeft - left) * offL).toInt()
                        right += ((nextTitleRight - right) * offR).toInt()
                    } else {
                        left =
                            (selectionOffset * nextTitleLeft.toFloat() + (1.0f - selectionOffset) * left.toFloat()).toInt()
                        right =
                            (selectionOffset * nextTitleRight.toFloat() + (1.0f - selectionOffset) * right.toFloat()).toInt()
                    }
                }
            } else {
                right = -1
                left = -1
            }

            setIndicatorPosition(left.toFloat(), right.toFloat())
        }

        private fun calculateTabViewContentBounds(
            tab: Tab,
            contentBounds: RectF
        ) {
            if (tabIndicatorWidth > 0 || tabIndicatorWidthScale > 0) {
                val left: Int = tab.view.left
                val right: Int = tab.view.right
                val tabWidth = right - left
                tabIndicatorWidth =
                    tabWidth.coerceAtMost(if (tabIndicatorWidth > 0) tabIndicatorWidth else (tabWidth * tabIndicatorWidthScale).toInt())
                val tabIndicatorInt = 0.coerceAtLeast(tabWidth - tabIndicatorWidth) / 2
                val contentLeftBounds = left + tabIndicatorInt
                val contentRightBounds = contentLeftBounds + tabIndicatorWidth
                contentBounds[contentLeftBounds.toFloat(), 0.0f, contentRightBounds.toFloat()] =
                    0.0f
            } else {
                var tabViewContentWidth: Int = tab.contentWidth
                if (tabViewContentWidth < dpToPx(24)) {
                    tabViewContentWidth = dpToPx(24)
                }
                val tabViewCenter: Int =
                    (tab.view.left + tab.view.right) / 2
                val contentLeftBounds = tabViewCenter - tabViewContentWidth / 2
                val contentRightBounds = tabViewCenter + tabViewContentWidth / 2
                contentBounds[contentLeftBounds.toFloat(), 0.0f, contentRightBounds.toFloat()] =
                    0.0f
            }
        }

        private fun setIndicatorPosition(left: Float, right: Float) {
            if (left != indicatorPoint.left || right != indicatorPoint.right) {
                indicatorPoint.left = left
                indicatorPoint.right = right
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }

        fun setIndicatorPositionFromTabPosition(
            position: Int,
            positionOffset: Float
        ) {
            if (indicatorAnimator != null && indicatorAnimator!!.isRunning) {
                indicatorAnimator!!.cancel()
            }
            selectedPosition = position
            selectionOffset = positionOffset
            updateIndicatorPosition()
        }

        fun setSelectedIndicatorHeight(height: Int) {
            if (selectedIndicatorHeight != height) {
                selectedIndicatorHeight = height
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }

        fun setSelectedIndicatorColor(color: Int) {
            if (selectedIndicatorPaint.color != color) {
                selectedIndicatorPaint.color = color
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }

        private fun drawIndicator(canvas: Canvas) {
            var indicatorHeight = 0
            if (this@TabLayout.tabSelectedIndicator != null) {
                indicatorHeight = this@TabLayout.tabSelectedIndicator?.intrinsicHeight ?: 0
            }
            if (selectedIndicatorHeight >= 0) {
                indicatorHeight = selectedIndicatorHeight
            }
            var indicatorTop = 0
            var indicatorBottom = 0
            when (this@TabLayout.tabIndicatorGravity) {
                0 -> {
                    indicatorTop = this.height - indicatorHeight - tabIndicatorMargin
                    indicatorBottom = this.height - tabIndicatorMargin
                }
                1 -> {
                    indicatorTop = (this.height - indicatorHeight) / 2
                    indicatorBottom = (this.height + indicatorHeight) / 2
                }
                2 -> {
                    indicatorTop = tabIndicatorMargin
                    indicatorBottom = indicatorHeight
                }
                3 -> {
                    indicatorTop = tabIndicatorMargin
                    indicatorBottom = this.height - tabIndicatorMargin
                }
            }
            if (indicatorPoint.left >= 0 && indicatorPoint.right > indicatorPoint.left) {
                val selectedIndicator: Drawable =
                    DrawableCompat.wrap(tabSelectedIndicator ?: defaultSelectionIndicator)
                selectedIndicator.setBounds(
                    indicatorPoint.left.toInt(),
                    indicatorTop,
                    indicatorPoint.right.toInt(),
                    indicatorBottom
                )
                if (selectedIndicatorPaint.color != 0) {
                    if (Build.VERSION.SDK_INT == 21) {
                        selectedIndicator.setColorFilter(
                            selectedIndicatorPaint.color,
                            PorterDuff.Mode.SRC_IN
                        )
                    } else {
                        DrawableCompat.setTint(
                            selectedIndicator,
                            selectedIndicatorPaint.color
                        )
                    }
                }
                selectedIndicator.draw(canvas)
            }
        }
    }


    interface OnTabSelectedListener {
        fun onTabSelected(item: Tab)
        fun onTabUnselected(item: Tab)
        fun onTabReselected(item: Tab)
    }

    @Dimension(unit = 1)
    fun dpToPx(@Dimension(unit = 0) dps: Int): Int {
        return (this.resources.displayMetrics.density * dps.toFloat()).roundToInt()
    }

    private inner class TabLayoutOnPageChangeListener(tabLayout: TabLayout) :
        ViewPager.OnPageChangeListener {
        private val tabLayoutRef: WeakReference<TabLayout> = WeakReference(tabLayout)
        private var previousScrollState = 0
        private var scrollState = 0
        override fun onPageScrollStateChanged(state: Int) {
            previousScrollState = scrollState
            scrollState = state
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            val tabLayout = tabLayoutRef.get()
            tabLayout?.let {
                val updateText =
                    scrollState != 2 || previousScrollState == 1
                val updateIndicator =
                    scrollState != 2 || previousScrollState != 0
                it.setScrollPosition(position, positionOffset, updateText, updateIndicator)
            }
        }

        override fun onPageSelected(position: Int) {
            val tabLayout = tabLayoutRef.get()
            tabLayout?.let {
                if (it.getSelectedTabPosition() != position && position < it.getTabCount()) {
                    val updateIndicator =
                        scrollState == 0 || scrollState == 2 && previousScrollState == 0
                    it.selectTab(it.getTabAt(position)!!, updateIndicator, true)
                }
            }
        }

        fun reset() {
            scrollState = 0
            previousScrollState = scrollState
        }

    }

    private inner class AdapterChangeListener : ViewPager.OnAdapterChangeListener {
        private var autoRefresh = false
        override fun onAdapterChanged(
            viewPager: ViewPager,
            oldAdapter: PagerAdapter?,
            newAdapter: PagerAdapter?
        ) {
            if (this@TabLayout.viewPager === viewPager) {
                this@TabLayout.setPagerAdapter(newAdapter, autoRefresh)
            }
        }

        fun setAutoRefresh(autoRefresh: Boolean) {
            this.autoRefresh = autoRefresh
        }
    }


    class TabIndicator : Indicator() {
        var type = TYPE_LINE
        var radius = 0

        fun type(type: Int): TabIndicator {
            this.type = type
            return this
        }

        fun radius(radius: Int): TabIndicator {
            this.radius = radius
            return this
        }


        override fun getDrawable(): Drawable? {
            return createGradientDrawable()
        }

        private fun createGradientDrawable(): Drawable {
            val indicatorDrawable = GradientDrawable()
            indicatorDrawable.setColor(color)
            if (type == TYPE_TRIANGLE) {
            } else {
                indicatorDrawable.setColor(color)
                if (type == TYPE_LINE || type == TYPE_RECT) {
                    indicatorDrawable.shape = GradientDrawable.RECTANGLE
                    indicatorDrawable.cornerRadius = radius.toFloat()
                }
                if (type == TYPE_OVAL) {
                    indicatorDrawable.shape = GradientDrawable.OVAL
                }
                if (type == TYPE_RING) {
                    indicatorDrawable.shape = GradientDrawable.RING
                }
            }
            return indicatorDrawable
        }

        companion object {
            /**
             * Type is a line
             */
            const val TYPE_LINE = 0
            /**
             * Type is a rect
             */
            const val TYPE_RECT = 1
            /**
             * Type is a triangle
             */
            const val TYPE_TRIANGLE = 2
            /**
             * Type is a ring.
             */
            const val TYPE_RING = 3
            /**
             * Type is an ellipse
             */
            const val TYPE_OVAL = 4
        }

    }

}
