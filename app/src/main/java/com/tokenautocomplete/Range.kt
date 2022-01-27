package com.tokenautocomplete

import java.util.*

class Range(start: Int, end: Int) {
    @JvmField
    val start: Int
    @JvmField
    val end: Int
    fun length(): Int {
        return end - start
    }

    override fun equals(other: Any?): Boolean {
        if (null == other || other !is Range) {
            return false
        }
        return other.start == start && other.end == end
    }

    override fun toString(): String {
        return String.format(Locale.US, "[%d..%d]", start, end)
    }

    override fun hashCode(): Int {
        var result = start
        result = 31 * result + end
        return result
    }

    init {
        require(start <= end) {
            String.format(
                Locale.ENGLISH,
                "Start (%d) cannot be greater than end (%d)", start, end
            )
        }
        this.start = start
        this.end = end
    }
}