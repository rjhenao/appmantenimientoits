package com.uvrp.itsmantenimientoapp

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Zoom por pellizco anclado al punto focal (donde están los dedos).
 * Transformación: posición = contenido × escala + traslación (pivote en esquina superior izquierda).
 */
class PinchZoomLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val minScale = 1f
    private val maxScale = 5f

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                zoomAt(detector.focusX, detector.focusY, detector.scaleFactor)
                return true
            }
        }
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (scaleFactor > minScale + 0.05f) {
                    resetZoom()
                } else {
                    zoomAt(e.x, e.y, 2.5f / scaleFactor)
                }
                return true
            }
        }
    )

    init {
        isClickable = true
        clipChildren = false
        clipToPadding = false
    }

    fun resetZoom() {
        scaleFactor = 1f
        translateX = 0f
        translateY = 0f
        applyTransform()
    }

    /** Escala relativa manteniendo fijo el punto (focusX, focusY) bajo los dedos. */
    private fun zoomAt(focusX: Float, focusY: Float, scaleMultiplier: Float) {
        val previousScale = scaleFactor
        val newScale = (scaleFactor * scaleMultiplier).coerceIn(minScale, maxScale)
        if (abs(newScale - previousScale) < 0.001f) return

        if (newScale <= minScale) {
            resetZoom()
            return
        }

        // Coordenada en el contenido (sin escala) que está bajo el punto focal
        val contentX = (focusX - translateX) / previousScale
        val contentY = (focusY - translateY) / previousScale

        scaleFactor = newScale
        // Mantener ese punto del contenido en la misma posición en pantalla
        translateX = focusX - contentX * scaleFactor
        translateY = focusY - contentY * scaleFactor

        applyTransform()
    }

    private fun applyTransform() {
        val child = getChildAt(0) ?: return
        ensurePivotTopLeft(child)
        child.scaleX = scaleFactor
        child.scaleY = scaleFactor
        child.translationX = translateX
        child.translationY = translateY
    }

    private fun ensurePivotTopLeft(child: View) {
        if (child.pivotX != 0f || child.pivotY != 0f) {
            child.pivotX = 0f
            child.pivotY = 0f
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        getChildAt(0)?.let { ensurePivotTopLeft(it) }
        if (changed) applyTransform()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.pointerCount > 1) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }
        if (scaleFactor > minScale && ev.actionMasked == MotionEvent.ACTION_MOVE) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        var handled = scaleDetector.onTouchEvent(event)

        if (scaleFactor > minScale && event.pointerCount == 1 && !scaleDetector.isInProgress) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    handled = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        if (abs(dx) > 1f || abs(dy) > 1f) {
                            translateX += dx
                            translateY += dy
                            lastTouchX = event.x
                            lastTouchY = event.y
                            applyTransform()
                        }
                        handled = true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
        }

        if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
            handled = true
        }

        return handled || super.onTouchEvent(event)
    }
}
