package com.devapps.aquatraking.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.devapps.aquatraking.R

class CustomWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private val amplitude = 20f
    private val wavePath = Path()
    private var waveOffset = 0f
    private val waveSpeed = 5f

    // Puntos originales (en dp)
    private val originalPoints = listOf(
        PointF(54f, 24f), PointF(180f, 24f), PointF(234f, 56f),
        PointF(234f, 270f), PointF(213f, 298f), PointF(127f, 306f),
        PointF(106f, 306f), PointF(21f, 298f), PointF(0f, 270f),
        PointF(0f, 56f), PointF(54f, 24f), PointF(54f, 0f), PointF(180f, 0f),
        PointF(180f, 24f)
    )

    // Dimensiones de la figura (en dp)
    private val figureWidth = 234f // Ancho máximo de la figura
    private val figureHeight = 306f // Alto máximo de la figura

    init {
        setupPaint()
    }

    private fun setupPaint() {
        paint.apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f.dpToPx()
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            pathEffect = CornerPathEffect(8f.dpToPx())
        }
        wavePaint.apply {
            color = ContextCompat.getColor(context, R.color.chart_gradient)
            style = Paint.Style.FILL
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Ajusta el tamaño de la vista al tamaño de la figura
        val width = figureWidth.dpToPx().toInt()
        val height = figureHeight.dpToPx().toInt()
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        createPath()
    }

    private fun createPath() {
        path.reset()
        val scaledPoints = originalPoints.map {
            PointF(it.x.dpToPx(), it.y.dpToPx())
        }
        path.moveTo(scaledPoints[0].x, scaledPoints[0].y)
        scaledPoints.forEach { point ->
            path.lineTo(point.x, point.y)
        }
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.clipPath(path)
        drawWave(canvas)
        canvas.restore()
        canvas.drawPath(path, paint)
    }

    private fun drawWave(canvas: Canvas) {
        val waveHeight = (progress / 100f) * (height - 56f.dpToPx())
        val waveStartY = height - waveHeight
        wavePath.reset()
        wavePath.moveTo(0f, waveStartY)
        val waveWidth = width / 4f

        for (i in 0..3) {
            val startX = i * waveWidth * 2 - waveOffset
            val endX = startX + 2 * waveWidth
            val controlY = if (i % 2 == 0) waveStartY - amplitude else waveStartY + amplitude
            wavePath.quadTo((startX + endX) / 2, controlY, endX, waveStartY)
        }

        wavePath.lineTo(width.toFloat(), height.toFloat())
        wavePath.lineTo(0f, height.toFloat())
        wavePath.close()
        canvas.drawPath(wavePath, wavePaint)
        waveOffset = (waveOffset + waveSpeed) % (2 * waveWidth)
        postInvalidateDelayed(10)
    }

    fun setProgress(progress: Float) {
        this.progress = progress.coerceIn(0f, 100f)
        invalidate()
    }

    private fun Float.dpToPx(): Float {
        return this * resources.displayMetrics.density
    }
}