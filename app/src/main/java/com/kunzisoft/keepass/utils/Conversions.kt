package com.kunzisoft.keepass.utils

import android.content.res.Resources

sealed class Dimension(
    protected val value: Float
) : Comparable<Dimension> {


    abstract fun toDp(): Float
    abstract fun toPx(): Float
    abstract fun toSp(): Float

    val intPx: Int get() = toPx().toInt()

    override fun compareTo(other: Dimension): Int {
        return toPx().compareTo(other.toPx())
    }

    class Dp(value: Float): Dimension(value) {
        override fun toDp(): Float {
            return value
        }

        override fun toPx(): Float {
            return value * Resources.getSystem().displayMetrics.density
        }

        override fun toSp(): Float {
            return Px(toPx()).toSp()
        }
    }

    class Px(value: Float): Dimension(value) {
        override fun toDp(): Float {
            return value / Resources.getSystem().displayMetrics.density
        }

        override fun toPx(): Float {
            return value
        }

        override fun toSp(): Float {
            return value / Resources.getSystem().displayMetrics.scaledDensity
        }
    }

    class Sp(value: Float): Dimension(value) {
        override fun toDp(): Float {
            return Px(toPx()).toDp()
        }

        override fun toPx(): Float {
            return value * Resources.getSystem().displayMetrics.scaledDensity
        }

        override fun toSp(): Float {
            return value
        }
    }
}

val Float.dp get() = Dimension.Dp(this)
val Int.dp get() = toFloat().dp

val Float.sp get() = Dimension.Sp(this)
val Int.sp get() = toFloat().sp
