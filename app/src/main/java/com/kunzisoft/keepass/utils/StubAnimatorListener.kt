package com.kunzisoft.keepass.utils

import android.animation.Animator

abstract class StubAnimatorListener : Animator.AnimatorListener{
    override fun onAnimationStart(p0: Animator?) {
        // no-op
    }

    override fun onAnimationEnd(p0: Animator?) {
        // no-op
    }

    override fun onAnimationCancel(p0: Animator?) {
        // no-op
    }

    override fun onAnimationRepeat(p0: Animator?) {
        // no-op
    }
}
