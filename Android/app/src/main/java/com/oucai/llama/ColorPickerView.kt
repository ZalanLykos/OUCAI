package com.oucai.llama

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * A custom View that draws an HSV color picker canvas.
 * Horizontal axis = Hue (0-360), Vertical axis = Saturation (0-100%).
 * Brightness is controlled via a paint color filter for performance.
 */
class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Cache the full-saturation bitmap (brightness=255) - never regenerated
    private var baseBitmap: Bitmap? = null
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val brightnessPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val selectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }
    private val selectorFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.BLACK
    }

    private var brightness = 255
    private var currentHue = 0f
    private var currentSat = 0f

    var onColorChanged: ((color: Int) -> Unit)? = null

    fun setBrightness(value: Int) {
        brightness = value.coerceIn(0, 255)
        // Just update the paint's color filter - no bitmap regeneration
        val scale = brightness / 255f
        val cm = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, 0f,
                0f, scale, 0f, 0f, 0f,
                0f, 0f, scale, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        brightnessPaint.colorFilter = ColorMatrixColorFilter(cm)
        invalidate()
    }

    fun getColor(): Int {
        val hsv = floatArrayOf(currentHue, currentSat, brightness / 255f)
        return Color.HSVToColor(hsv)
    }

    fun setInitialColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        currentHue = hsv[0]
        currentSat = hsv[1]
        brightness = (hsv[2] * 255).toInt().coerceIn(0, 255)
        setBrightness(brightness)
        invalidate()
    }

    private fun buildBaseBitmap() {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        if (w <= 0 || h <= 0) return

        baseBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)

        for (y in 0 until h) {
            val sat = y.toFloat() / h
            for (x in 0 until w) {
                val hue = (x.toFloat() / w) * 360f
                // Always build with full brightness (1.0)
                val hsv = floatArrayOf(hue, sat, 1f)
                pixels[y * w + x] = Color.HSVToColor(hsv)
            }
        }
        baseBitmap?.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildBaseBitmap()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw the cached bitmap with brightness color filter applied
        baseBitmap?.let { canvas.drawBitmap(it, 0f, 0f, brightnessPaint) }

        // Draw selector circle at current position
        val x = (currentHue / 360f) * width
        val y = currentSat * height
        canvas.drawCircle(x, y, 16f, selectorPaint)
        canvas.drawCircle(x, y, 16f, selectorFillPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val x = event.x.coerceIn(0f, width.toFloat())
                val y = event.y.coerceIn(0f, height.toFloat())
                currentHue = (x / width) * 360f
                currentSat = y / height
                onColorChanged?.invoke(getColor())
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
