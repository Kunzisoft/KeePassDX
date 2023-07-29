/*
 * Loop 1.2.1 created by Issei Aoki modified by Jeremy JAMET
 * https://github.com/igreenwood/loupe
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Issei Aoki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.igreenwood.loupe

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.ImageView
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


class Loupe(imageView: ImageView, container: ViewGroup) : View.OnTouchListener,
    View.OnLayoutChangeListener {

    companion object {
        const val DEFAULT_MAX_ZOOM = 5.0f
        const val DEFAULT_ANIM_DURATION = 250L
        const val DEFAULT_ANIM_DURATION_LONG = 375L
        const val DEFAULT_VIEW_DRAG_FRICTION = 1f
        const val DEFAULT_DRAG_DISMISS_DISTANCE_IN_VIEW_HEIGHT_RATIO = 0.5f
        const val DEFAULT_DRAG_DISMISS_DISTANCE_IN_DP = 96
        const val MAX_FLING_VELOCITY = 8000f
        const val MIN_FLING_VELOCITY = 1500f
        const val DEFAULT_DOUBLE_TAP_ZOOM_SCALE = 0.5f
        val DEFAULT_INTERPOLATOR = DecelerateInterpolator()

        fun create(
                imageView: ImageView,
                container: ViewGroup,
                config: Loupe.() -> Unit = { }
        ): Loupe {
            return Loupe(imageView, container).apply(config)
        }
    }

    interface OnViewTranslateListener {
        fun onStart(view: ImageView)
        fun onViewTranslate(view: ImageView, amount: Float)
        fun onDismiss(view: ImageView)
        fun onRestore(view: ImageView)
    }

    interface OnScaleChangedListener {
        fun onScaleChange(scaleFactor: Float, focusX: Float, focusY: Float)
    }

    // max zoom(> 1f)
    var maxZoom = DEFAULT_MAX_ZOOM
    // use fling gesture for dismiss
    var useFlingToDismissGesture = true
    // flag to enable or disable drag to dismiss
    var useDragToDismiss = true
    // duration millis for dismiss animation
    var dismissAnimationDuration = DEFAULT_ANIM_DURATION
    // duration millis for restore animation
    var restoreAnimationDuration = DEFAULT_ANIM_DURATION
    // duration millis for image animation
    var flingAnimationDuration = DEFAULT_ANIM_DURATION
    // duration millis for double tap scale animation
    var scaleAnimationDuration = DEFAULT_ANIM_DURATION_LONG
    // duration millis for over scale animation
    var overScaleAnimationDuration = DEFAULT_ANIM_DURATION_LONG
    // duration millis for over scrolling animation
    var overScrollAnimationDuration = DEFAULT_ANIM_DURATION
    // view drag friction for swipe to dismiss(1f : drag distance == view move distance. Smaller value, view is moving more slower)
    var viewDragFriction = DEFAULT_VIEW_DRAG_FRICTION
    // drag distance threshold in dp for swipe to dismiss
    var dragDismissDistanceInDp = DEFAULT_DRAG_DISMISS_DISTANCE_IN_DP
    // on view touched
    var onViewTouchedListener: View.OnTouchListener? = null
    // on view translate listener
    var onViewTranslateListener: OnViewTranslateListener? = null
    // on scale changed
    var onScaleChangedListener: OnScaleChangedListener? = null

    var dismissAnimationInterpolator: Interpolator = DEFAULT_INTERPOLATOR

    var restoreAnimationInterpolator: Interpolator = DEFAULT_INTERPOLATOR

    var flingAnimationInterpolator: Interpolator = DEFAULT_INTERPOLATOR

    var doubleTapScaleAnimationInterpolator: Interpolator = AccelerateDecelerateInterpolator()

    var overScaleAnimationInterpolator: Interpolator = DEFAULT_INTERPOLATOR

    var overScrollAnimationInterpolator: Interpolator = DEFAULT_INTERPOLATOR

    var doubleTapZoomScale: Float = DEFAULT_DOUBLE_TAP_ZOOM_SCALE // 0f~1f

    var minimumFlingVelocity: Float = MIN_FLING_VELOCITY

    private var flingAnimator: Animator = ValueAnimator()

    // bitmap matrix
    private var transfrom = Matrix()
    // bitmap scale
    private var scale = 1f
    // is ready for drawing bitmap
    private var isReadyToDraw = false
    // view rect - padding (recalculated on size changed)
    private var canvasBounds = RectF()
    // bitmap drawing rect (move on scroll, recalculated on scale changed)
    private var bitmapBounds = RectF()
    // displaying bitmap rect (does not move, recalculated on scale changed)
    private var viewport = RectF()
    // minimum scale of bitmap
    private var minScale = 1f
    // maximum scale of bitmap
    private var maxScale = 1f
    // bitmap (decoded) width
    private var imageWidth = 0f
    // bitmap (decoded) height
    private var imageHeight = 0f

    private val scroller: OverScroller
    private var originalViewBounds = Rect()
    private var dragToDismissThreshold = 0f
    private var dragDismissDistanceInPx = 0f
    private var isViewTranslateAnimationRunning = false
    private var isVerticalScrollEnabled = true
    private var isHorizontalScrollEnabled = true
    private var isBitmapTranslateAnimationRunning = false
    private var isBitmapScaleAnimationRunninng = false
    private var initialY = 0f
    // scaling helper
    private var scaleGestureDetector: ScaleGestureDetector? = null
    // translating helper
    private var gestureDetector: GestureDetector? = null
    private val onScaleGestureListener: ScaleGestureDetector.OnScaleGestureListener =
        object : ScaleGestureDetector.OnScaleGestureListener {

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (isDragging() || isBitmapTranslateAnimationRunning || isBitmapScaleAnimationRunninng) {
                    return false
                }

                val scaleFactor = detector.scaleFactor
                val focalX = detector.focusX
                val focalY = detector.focusY

                if (detector.scaleFactor == 1.0f) {
                    // scale is not changing
                    return true
                }

                zoomToTargetScale(calcNewScale(scaleFactor), focalX, focalY)

                return true
            }

            override fun onScaleBegin(p0: ScaleGestureDetector): Boolean = true

            override fun onScaleEnd(p0: ScaleGestureDetector) {}

        }

    private val onGestureListener: GestureDetector.OnGestureListener =
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e2.pointerCount != 1) {
                    return true
                }

                if (scale > minScale) {
                    processScroll(distanceX, distanceY)
                } else if (useDragToDismiss && scale == minScale) {
                    processDrag(distanceY)
                }
                return true
            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (scale > minScale) {
                    processFlingBitmap(velocityX, velocityY)
                } else {
                    processFlingToDismiss(velocityY)
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isBitmapScaleAnimationRunninng) {
                    return true
                }

                if (scale > minScale) {
                    zoomOutToMinimumScale()
                } else {
                    zoomInToTargetScale(e)
                }
                return true
            }
        }

    init {
        container.apply {
            background.alpha = 255
            setOnTouchListener(this@Loupe)
            addOnLayoutChangeListener(this@Loupe)
            scaleGestureDetector = ScaleGestureDetector(context, onScaleGestureListener)
            gestureDetector = GestureDetector(context, onGestureListener)
            scroller = OverScroller(context)
            dragDismissDistanceInPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dragDismissDistanceInDp.toFloat(),
                    resources.displayMetrics
            )
        }
        imageView.apply {
            imageMatrix = null
            y = 0f
            animate().cancel()
            scaleType = ImageView.ScaleType.MATRIX
        }
    }

    private var imageViewRef: WeakReference<ImageView> = WeakReference(imageView)
    private var containerRef: WeakReference<ViewGroup> = WeakReference(container)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        onViewTouchedListener?.onTouch(view, event)

        event ?: return false
        val imageView = imageViewRef.get() ?: return false
        val container = containerRef.get() ?: return false

        container.parent.requestDisallowInterceptTouchEvent(scale != minScale)

        if (!imageView.isEnabled) {
            return false
        }

        if (isViewTranslateAnimationRunning) {
            return false
        }

        val scaleEvent = scaleGestureDetector?.onTouchEvent(event)
        val isScaleAnimationIsRunning = scale < minScale
        if (scaleEvent != scaleGestureDetector?.isInProgress && !isScaleAnimationIsRunning) {
            // handle single touch gesture when scaling process is not running
            gestureDetector?.onTouchEvent(event)
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                flingAnimator.cancel()
            }
            MotionEvent.ACTION_UP -> {
                when {
                    scale == minScale -> {
                        if (!isViewTranslateAnimationRunning) {
                            dismissOrRestoreIfNeeded()
                        }
                    }
                    scale > minScale -> {
                        constrainBitmapBounds(true)
                    }
                    else -> {
                        zoomOutToMinimumScale(true)
                    }
                }
            }
        }

        setTransform()
        imageView.postInvalidate()
        return true
    }

    override fun onLayoutChange(
            view: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
    ) {
        val imageView = imageViewRef.get() ?: return
        val container = containerRef.get() ?: return

        imageView.run {
            setupLayout(left, top, right, bottom)
            initialY = y
            if (useFlingToDismissGesture) {
                setDragToDismissDistance(DEFAULT_DRAG_DISMISS_DISTANCE_IN_VIEW_HEIGHT_RATIO)
            } else {
                setDragToDismissDistance(DEFAULT_DRAG_DISMISS_DISTANCE_IN_DP)
            }
            container.background.alpha = 255
            setTransform()
            postInvalidate()
        }
    }

    private fun startVerticalTranslateAnimation(velY: Float) {
        val imageView = imageViewRef.get() ?: return

        isViewTranslateAnimationRunning = true


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            imageView.run {
                val translationY = if (velY > 0) {
                    originalViewBounds.top + height - top
                } else {
                    originalViewBounds.top - height - top
                }
                animate()
                        .setDuration(dismissAnimationDuration)
                        .setInterpolator(dismissAnimationInterpolator)
                        .translationY(translationY.toFloat())
                        .setUpdateListener {
                            val amount = calcTranslationAmount()
                            changeBackgroundAlpha(amount)
                            onViewTranslateListener?.onViewTranslate(imageView, amount)
                        }
                        .setListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(p0: Animator) {

                            }

                            override fun onAnimationEnd(p0: Animator) {
                                isViewTranslateAnimationRunning = false
                                onViewTranslateListener?.onDismiss(imageView)
                                cleanup()
                            }

                            override fun onAnimationCancel(p0: Animator) {
                                isViewTranslateAnimationRunning = false
                            }

                            override fun onAnimationRepeat(p0: Animator) {
                                // no op
                            }
                        })
            }
        } else {
            ObjectAnimator.ofFloat(imageView, View.TRANSLATION_Y, if (velY > 0) {
                originalViewBounds.top + imageView.height - imageView.top
            } else {
                originalViewBounds.top - imageView.height - imageView.top
            }.toFloat()).apply {
                duration = dismissAnimationDuration
                interpolator = dismissAnimationInterpolator
                addUpdateListener {
                    val amount = calcTranslationAmount()
                    changeBackgroundAlpha(amount)
                    onViewTranslateListener?.onViewTranslate(imageView, amount)
                }
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(p0: Animator) {
                        // no op
                    }

                    override fun onAnimationEnd(p0: Animator) {
                        isViewTranslateAnimationRunning = false
                        onViewTranslateListener?.onDismiss(imageView)
                        cleanup()
                    }

                    override fun onAnimationCancel(p0: Animator) {
                        isViewTranslateAnimationRunning = false
                    }

                    override fun onAnimationRepeat(p0: Animator) {
                        // no op
                    }
                })
                start()
            }
        }
    }

    private fun processFlingBitmap(velocityX: Float, velocityY: Float) {
        val imageView = imageViewRef.get() ?: return

        var (velX, velY) = velocityX / scale to velocityY / scale

        if (velX == 0f && velY == 0f) {
            return
        }

        if (velX > MAX_FLING_VELOCITY) {
            velX = MAX_FLING_VELOCITY
        }

        if (velY > MAX_FLING_VELOCITY) {
            velY = MAX_FLING_VELOCITY
        }

        val (fromX, fromY) = bitmapBounds.left to bitmapBounds.top

        scroller.forceFinished(true)
        scroller.fling(
                fromX.roundToInt(),
                fromY.roundToInt(),
                velX.roundToInt(),
                velY.roundToInt(),
                (viewport.right - bitmapBounds.width()).roundToInt(),
                viewport.left.roundToInt(),
                (viewport.bottom - bitmapBounds.height()).roundToInt(),
                viewport.top.roundToInt()
        )

        ViewCompat.postInvalidateOnAnimation(imageView)

        val toX = scroller.finalX.toFloat()
        val toY = scroller.finalY.toFloat()

        flingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = flingAnimationDuration
            interpolator = flingAnimationInterpolator
            addUpdateListener {
                val amount = it.animatedValue as Float
                val newLeft = lerp(amount, fromX, toX)
                val newTop = lerp(amount, fromY, toY)
                bitmapBounds.offsetTo(newLeft, newTop)
                ViewCompat.postInvalidateOnAnimation(imageView)
                setTransform()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {
                    isBitmapTranslateAnimationRunning = true
                }

                override fun onAnimationEnd(p0: Animator) {
                    isBitmapTranslateAnimationRunning = false
                    constrainBitmapBounds()
                }

                override fun onAnimationCancel(p0: Animator) {
                    isBitmapTranslateAnimationRunning = false
                }

                override fun onAnimationRepeat(p0: Animator) {
                    // no op
                }
            })
        }
        flingAnimator.start()
    }

    private fun processScroll(distanceX: Float, distanceY: Float) {
        val distX = if (isHorizontalScrollEnabled) {
            -distanceX
        } else {
            0f
        }
        val distY = if (isVerticalScrollEnabled) {
            -distanceY
        } else {
            0f
        }
        offsetBitmap(distX, distY)
        setTransform()
    }

    private fun zoomInToTargetScale(e: MotionEvent) {
        val imageView = imageViewRef.get() ?: return
        val startScale = scale
        val endScale = minScale * maxZoom * doubleTapZoomScale
        val focalX = e.x
        val focalY = e.y
        ValueAnimator.ofFloat(startScale, endScale).apply {
            duration = scaleAnimationDuration
            interpolator = doubleTapScaleAnimationInterpolator
            addUpdateListener {
                zoomToTargetScale(it.animatedValue as Float, focalX, focalY)
                ViewCompat.postInvalidateOnAnimation(imageView)
                setTransform()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {
                    isBitmapScaleAnimationRunninng = true
                }

                override fun onAnimationEnd(p0: Animator) {
                    isBitmapScaleAnimationRunninng = false
                    if (endScale == minScale) {
                        zoomToTargetScale(minScale, focalX, focalY)
                        imageView.postInvalidate()
                    }
                }

                override fun onAnimationCancel(p0: Animator) {
                    isBitmapScaleAnimationRunninng = false
                }

                override fun onAnimationRepeat(p0: Animator) {
                    // no op
                }
            })
        }.start()
    }

    private fun zoomOutToMinimumScale(isOverScaling: Boolean = false) {
        val imageView = imageViewRef.get() ?: return
        val startScale = scale
        val endScale = minScale
        val startLeft = bitmapBounds.left
        val startTop = bitmapBounds.top
        val endLeft = canvasBounds.centerX() - imageWidth * minScale * 0.5f
        val endTop = canvasBounds.centerY() - imageHeight * minScale * 0.5f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (isOverScaling) {
                overScaleAnimationDuration
            } else {
                scaleAnimationDuration
            }
            interpolator = if (isOverScaling) {
                overScaleAnimationInterpolator
            } else {
                doubleTapScaleAnimationInterpolator
            }
            addUpdateListener {
                val value = it.animatedValue as Float
                scale = lerp(value, startScale, endScale)
                val newLeft = lerp(value, startLeft, endLeft)
                val newTop = lerp(value, startTop, endTop)
                calcBounds()
                bitmapBounds.offsetTo(newLeft, newTop)
                constrainBitmapBounds()
                ViewCompat.postInvalidateOnAnimation(imageView)
                setTransform()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {
                    isBitmapScaleAnimationRunninng = true
                }

                override fun onAnimationEnd(p0: Animator) {
                    isBitmapScaleAnimationRunninng = false
                    if (endScale == minScale) {
                        scale = minScale
                        calcBounds()
                        constrainBitmapBounds()
                        imageView.postInvalidate()
                    }
                }

                override fun onAnimationCancel(p0: Animator) {
                    isBitmapScaleAnimationRunninng = false
                }

                override fun onAnimationRepeat(p0: Animator) {
                    // no op
                }
            })
        }.start()
    }

    private var lastDistY = Float.NaN

    private fun processDrag(distanceY: Float) {
        val imageView = imageViewRef.get() ?: return

        if (lastDistY.isNaN()) {
            lastDistY = distanceY
            return
        }

        if (imageView.y == initialY) {
            onViewTranslateListener?.onStart(imageView)
        }

        imageView.y -= distanceY * viewDragFriction // if viewDragRatio is 1.0f, view translation speed is equal to user scrolling speed.
        val amount = calcTranslationAmount()
        changeBackgroundAlpha(amount)
        onViewTranslateListener?.onViewTranslate(imageView, amount)
    }

    private fun dismissOrRestoreIfNeeded() {
        if (!isDragging() || isViewTranslateAnimationRunning) {
            return
        }
        dismissOrRestore()
    }

    private fun dismissOrRestore() {
        val imageView = imageViewRef.get() ?: return

        if (shouldTriggerDragToDismissAnimation()) {
            if (useFlingToDismissGesture) {
                startDragToDismissAnimation()
            } else {
                onViewTranslateListener?.onDismiss(imageView)
                cleanup()
            }
        } else {
            restoreViewTransform()
        }
    }

    private fun shouldTriggerDragToDismissAnimation() = dragDistance() > dragToDismissThreshold

    private fun restoreViewTransform() {
        val imageView = imageViewRef.get() ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            imageView.run {
                animate()
                        .setDuration(restoreAnimationDuration)
                        .setInterpolator(restoreAnimationInterpolator)
                        .translationY((originalViewBounds.top - top).toFloat())
                        .setUpdateListener {
                            val amount = calcTranslationAmount()
                            changeBackgroundAlpha(amount)
                            onViewTranslateListener?.onViewTranslate(this, amount)
                        }
                        .setListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(p0: Animator) {
                                // no op
                            }

                            override fun onAnimationEnd(p0: Animator) {
                                onViewTranslateListener?.onRestore(imageView)
                            }

                            override fun onAnimationCancel(p0: Animator) {
                                // no op
                            }

                            override fun onAnimationRepeat(p0: Animator) {
                                // no op
                            }
                        })
            }
        } else {
            ObjectAnimator.ofFloat(imageView, View.TRANSLATION_Y, (originalViewBounds.top - imageView.top).toFloat()).apply {
                duration = restoreAnimationDuration
                interpolator = restoreAnimationInterpolator
                addUpdateListener {
                    val amount = calcTranslationAmount()
                    changeBackgroundAlpha(amount)
                    onViewTranslateListener?.onViewTranslate(imageView, amount)
                }
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(p0: Animator) {
                        // no op
                    }

                    override fun onAnimationEnd(p0: Animator) {
                        onViewTranslateListener?.onRestore(imageView)
                    }

                    override fun onAnimationCancel(p0: Animator) {
                        // no op
                    }

                    override fun onAnimationRepeat(p0: Animator) {
                        // no op
                    }
                })
                start()
            }
        }
    }

    private fun startDragToDismissAnimation() {
        val imageView = imageViewRef.get() ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            imageView.run {
                val translationY = if (y - initialY > 0) {
                    originalViewBounds.top + height - top
                } else {
                    originalViewBounds.top - height - top
                }
                animate()
                        .setDuration(dismissAnimationDuration)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .translationY(translationY.toFloat())
                        .setUpdateListener {
                            val amount = calcTranslationAmount()
                            changeBackgroundAlpha(amount)
                            onViewTranslateListener?.onViewTranslate(this, amount)
                        }
                        .setListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(p0: Animator) {
                                isViewTranslateAnimationRunning = true
                            }

                            override fun onAnimationEnd(p0: Animator) {
                                isViewTranslateAnimationRunning = false
                                onViewTranslateListener?.onDismiss(imageView)
                                cleanup()
                            }

                            override fun onAnimationCancel(p0: Animator) {
                                isViewTranslateAnimationRunning = false
                            }

                            override fun onAnimationRepeat(p0: Animator) {
                                // no op
                            }
                        })
            }
        } else {
            ObjectAnimator.ofFloat(imageView, View.TRANSLATION_Y, imageView.translationY).apply {
                duration = dismissAnimationDuration
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    val amount = calcTranslationAmount()
                    changeBackgroundAlpha(amount)
                    onViewTranslateListener?.onViewTranslate(imageView, amount)
                }
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(p0: Animator) {
                        isViewTranslateAnimationRunning = true
                    }

                    override fun onAnimationEnd(p0: Animator) {
                        isViewTranslateAnimationRunning = false
                        onViewTranslateListener?.onDismiss(imageView)
                        cleanup()
                    }

                    override fun onAnimationCancel(p0: Animator) {
                        isViewTranslateAnimationRunning = false
                    }

                    override fun onAnimationRepeat(p0: Animator) {
                        // no op
                    }
                })
                start()
            }
        }
    }

    private fun processFlingToDismiss(velocityY: Float) {
        if (useFlingToDismissGesture && !isViewTranslateAnimationRunning) {
            if (abs(velocityY) < minimumFlingVelocity) {
                return
            }
            startVerticalTranslateAnimation(velocityY)
        }
    }

    private fun calcTranslationAmount() =
        constrain(
                0f,
                norm(dragDistance(), 0f, originalViewBounds.height().toFloat()),
                1f
        )

    private fun dragDistance() = abs(viewOffsetY())

    private fun isDragging() = dragDistance() > 0f

    private fun viewOffsetY() = imageViewRef.get()?.y ?: 0 - initialY

    /**
     * targetScale: new scale
     * focalX: focal x in current bitmapBounds
     * focalY: focal y in current bitmapBounds
     */
    private fun zoomToTargetScale(targetScale: Float, focalX: Float, focalY: Float) {
        scale = targetScale
        val lastBounds = RectF(bitmapBounds)
        // scale has changed, recalculate bitmap bounds
        calcBounds()
        // offset to focalPoint
        offsetToZoomFocalPoint(focalX, focalY, lastBounds, bitmapBounds)
        onScaleChangedListener?.onScaleChange(targetScale, focalX, focalY)
    }

    private fun setTransform() {
        val imageView = imageViewRef.get() ?: return
        transfrom.apply {
            reset()
            postTranslate(-imageWidth / 2, -imageHeight / 2)
            postScale(scale, scale)
            postTranslate(bitmapBounds.centerX(), bitmapBounds.centerY())
        }
        imageView.imageMatrix = transfrom
    }

    /**
     * setup layout
     */
    private fun setupLayout(left: Int, top: Int, right: Int, bottom: Int) {
        val imageView = imageViewRef.get() ?: return
        originalViewBounds.set(left, top, right, bottom)
        imageView.run {
            val drawable = imageViewRef.get()?.drawable
            val bitmap = (drawable as? BitmapDrawable)?.bitmap
            if (width == 0 || height == 0 || drawable == null) return
            imageWidth = (bitmap?.width ?: drawable.intrinsicWidth).toFloat()
            imageHeight = (bitmap?.height ?: drawable.intrinsicHeight).toFloat()
            val canvasWidth = (width - paddingLeft - paddingRight).toFloat()
            val canvasHeight = (height - paddingTop - paddingBottom).toFloat()

            calcScaleRange(canvasWidth, canvasHeight, imageWidth, imageHeight)
            calcBounds()
            constrainBitmapBounds()
            isReadyToDraw = true
            invalidate()
        }
    }

    private fun constrainBitmapBounds(animate: Boolean = false) {
        val imageView = imageViewRef.get() ?: return

        if (isBitmapTranslateAnimationRunning || isBitmapScaleAnimationRunninng) {
            return
        }

        val offset = PointF()

        // constrain viewport inside bitmap bounds
        if (viewport.left < bitmapBounds.left) {
            offset.x += viewport.left - bitmapBounds.left
        }

        if (viewport.top < bitmapBounds.top) {
            offset.y += viewport.top - bitmapBounds.top
        }

        if (viewport.right > bitmapBounds.right) {
            offset.x += viewport.right - bitmapBounds.right
        }

        if (viewport.bottom > bitmapBounds.bottom) {
            offset.y += viewport.bottom - bitmapBounds.bottom
        }

        if (offset.equals(0f, 0f)) {
            return
        }

        if (animate) {
            if (!isVerticalScrollEnabled) {
                bitmapBounds.offset(0f, offset.y)
                offset.y = 0f
            }

            if (!isHorizontalScrollEnabled) {
                bitmapBounds.offset(offset.x, 0f)
                offset.x = 0f
            }

            val start = RectF(bitmapBounds)
            val end = RectF(bitmapBounds).apply {
                offset(offset.x, offset.y)
            }
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = overScrollAnimationDuration
                interpolator = overScrollAnimationInterpolator
                addUpdateListener {
                    val amount = it.animatedValue as Float
                    val newLeft = lerp(amount, start.left, end.left)
                    val newTop = lerp(amount, start.top, end.top)
                    bitmapBounds.offsetTo(newLeft, newTop)
                    ViewCompat.postInvalidateOnAnimation(imageView)
                    setTransform()
                }
            }.start()
        } else {
            bitmapBounds.offset(offset.x, offset.y)
        }
    }

    /**
     * calc canvas/bitmap bounds
     */
    private fun calcBounds() {
        val imageView = imageViewRef.get() ?: return

        imageView.run {
            // calc canvas bounds
            canvasBounds = RectF(
                    paddingLeft.toFloat(),
                    paddingTop.toFloat(),
                    width - paddingRight.toFloat(),
                    height - paddingBottom.toFloat()
            )
        }
        // calc bitmap bounds
        bitmapBounds = RectF(
                canvasBounds.centerX() - imageWidth * scale * 0.5f,
                canvasBounds.centerY() - imageHeight * scale * 0.5f,
                canvasBounds.centerX() + imageWidth * scale * 0.5f,
                canvasBounds.centerY() + imageHeight * scale * 0.5f
        )
        // calc viewport
        viewport = RectF(
                max(canvasBounds.left, bitmapBounds.left),
                max(canvasBounds.top, bitmapBounds.top),
                min(canvasBounds.right, bitmapBounds.right),
                min(canvasBounds.bottom, bitmapBounds.bottom)
        )
        // check scroll availability
        isHorizontalScrollEnabled = true
        isVerticalScrollEnabled = true

        if (bitmapBounds.width() < canvasBounds.width()) {
            isHorizontalScrollEnabled = false
        }

        if (bitmapBounds.height() < canvasBounds.height()) {
            isVerticalScrollEnabled = false
        }
    }

    private fun offsetBitmap(offsetX: Float, offsetY: Float) {
        bitmapBounds.offset(offsetX, offsetY)
    }

    /**
     * calc min/max scale and set initial scale
     */
    private fun calcScaleRange(
            canvasWidth: Float,
            canvasHeight: Float,
            bitmapWidth: Float,
            bitmapHeight: Float
    ) {
        val canvasRatio = canvasHeight / canvasWidth
        val bitmapRatio = bitmapHeight / bitmapWidth
        minScale = if (canvasRatio > bitmapRatio) {
            canvasWidth / bitmapWidth
        } else {
            canvasHeight / bitmapHeight
        }
        scale = minScale
        maxScale = minScale * maxZoom
    }


    private fun calcNewScale(newScale: Float): Float {
        return min(maxScale, newScale * scale)
    }

    private fun constrain(min: Float, value: Float, max: Float): Float {
        return max(min(value, max), min)
    }

    private fun offsetToZoomFocalPoint(
            focalX: Float,
            focalY: Float,
            oldBounds: RectF,
            newBounds: RectF
    ) {
        val oldX = constrain(viewport.left, focalX, viewport.right)
        val oldY = constrain(viewport.top, focalY, viewport.bottom)
        val newX = map(oldX, oldBounds.left, oldBounds.right, newBounds.left, newBounds.right)
        val newY = map(oldY, oldBounds.top, oldBounds.bottom, newBounds.top, newBounds.bottom)
        offsetBitmap(oldX - newX, oldY - newY)
    }

    private fun map(
            value: Float,
            srcStart: Float,
            srcStop: Float,
            dstStart: Float,
            dstStop: Float
    ): Float {
        if (srcStop - srcStart == 0f) {
            return 0f
        }
        return ((value - srcStart) * (dstStop - dstStart) / (srcStop - srcStart)) + dstStart
    }

    private fun lerp(amt: Float, start: Float, stop: Float): Float {
        return start + (stop - start) * amt
    }

    private fun norm(value: Float, start: Float, stop: Float): Float {
        return value / (stop - start)
    }

    private fun changeBackgroundAlpha(amount: Float) {
        val container = containerRef.get() ?: return
        val newAlpha = ((1.0f - amount) * 255).roundToInt()
        container.background.mutate().alpha = newAlpha
    }

    fun setDragToDismissDistance(distance: Int) {
        val imageView = imageViewRef.get() ?: return
        dragToDismissThreshold = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                distance.toFloat(),
                imageView.context.resources.displayMetrics
        )
    }

    fun setDragToDismissDistance(heightRatio: Float) {
        val imageView = imageViewRef.get() ?: return
        dragToDismissThreshold = imageView.height * heightRatio
    }

    fun dismiss() {
        // Animate down offscreen (the finish listener will call the cleanup method)
        startVerticalTranslateAnimation(MIN_FLING_VELOCITY)
    }

    fun cleanup() {
        containerRef.get()?.apply {
            setOnTouchListener(null)
            removeOnLayoutChangeListener(null)
        }
        imageViewRef.clear()
        containerRef.clear()
    }
}
