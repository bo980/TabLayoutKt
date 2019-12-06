package com.liang.tabs.utils

import android.animation.TimeInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import kotlin.math.roundToInt

object AnimationUtils {
    val LINEAR_INTERPOLATOR: TimeInterpolator = LinearInterpolator()
    val FAST_OUT_SLOW_IN_INTERPOLATOR: TimeInterpolator = FastOutSlowInInterpolator()
    val FAST_OUT_LINEAR_IN_INTERPOLATOR: TimeInterpolator = FastOutLinearInInterpolator()
    val LINEAR_OUT_SLOW_IN_INTERPOLATOR: TimeInterpolator = LinearOutSlowInInterpolator()
    val DECELERATE_INTERPOLATOR: TimeInterpolator = DecelerateInterpolator()

    fun lerp(
        startValue: Float,
        endValue: Float,
        fraction: Float
    ): Float {
        return startValue + fraction * (endValue - startValue)
    }

    fun lerp(startValue: Int, endValue: Int, fraction: Float): Int {
        return startValue + (fraction * (endValue - startValue).toFloat()).roundToInt()
    }
}

