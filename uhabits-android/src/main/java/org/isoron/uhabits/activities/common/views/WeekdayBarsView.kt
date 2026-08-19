/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.activities.common.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.JavaLocalDateFormatter
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.analytics.WeekdayStat
import org.isoron.uhabits.utils.sres
import java.util.Locale
import kotlin.math.max

class WeekdayBarsView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var stats: List<WeekdayStat> = emptyList()
    private var color: Int = sres.getColor(R.attr.contrast80)
    private var displayRates = FloatArray(7)
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.tinyTextSize)
    }
    private val rect = RectF()
    private val formatter = JavaLocalDateFormatter(Locale.getDefault())
    private var animator: ValueAnimator? = null

    init {
        trackPaint.color = sres.getColor(R.attr.contrast20)
        textPaint.color = sres.getColor(R.attr.contrast60)
        if (isInEditMode) {
            stats = DayOfWeek.entries.mapIndexed { index, day ->
                WeekdayStat(day, index, 7, index / 6.0)
            }
            displayRates = stats.map { it.rate.toFloat() }.toFloatArray()
        }
    }

    fun setStats(stats: List<WeekdayStat>, color: Int, animate: Boolean = true) {
        this.stats = stats
        this.color = color
        val target = FloatArray(7) { i -> stats.getOrNull(i)?.rate?.toFloat() ?: 0f }
        animator?.cancel()
        if (!animate || !isAttachedToWindow) {
            displayRates = target
            invalidate()
            return
        }
        val start = displayRates.copyOf()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val t = animation.animatedValue as Float
                for (i in 0 until 7) {
                    val from = start.getOrElse(i) { 0f }
                    displayRates[i] = from + (target[i] - from) * t
                }
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (stats.isEmpty()) return
        val count = stats.size
        val gap = width / (count * 4f)
        val barWidth = (width - gap * (count + 1)) / count
        val labelSpace = textPaint.textSize * 1.8f
        val chartHeight = height - labelSpace
        val radius = barWidth / 2.2f
        barPaint.color = color
        for (i in 0 until count) {
            val left = gap + i * (barWidth + gap)
            val rate = displayRates.getOrElse(i) { 0f }.coerceIn(0f, 1f)
            val barHeight = max(radius, chartHeight * rate)
            rect.set(left, chartHeight - barHeight, left + barWidth, chartHeight)
            canvas.drawRoundRect(rect, radius, radius, trackPaint)
            rect.top = chartHeight - barHeight
            canvas.drawRoundRect(rect, radius, radius, barPaint)
            val label = formatter.shortWeekdayName(stats[i].day).take(1).uppercase(Locale.getDefault())
            canvas.drawText(label, rect.centerX(), height - textPaint.descent(), textPaint)
        }
    }
}
