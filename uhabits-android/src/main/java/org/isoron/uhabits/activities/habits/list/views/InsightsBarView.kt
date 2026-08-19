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
package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.analytics.PortfolioInsights
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.sres
import java.util.Locale

class InsightsBarView(context: Context) : LinearLayout(context) {

    private val summary = TextView(context)
    private val detail = TextView(context)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padH = dp(16f).toInt()
        val padV = dp(10f).toInt()
        setPadding(padH, padV, padH, padV)
        setBackgroundColor(sres.getColor(R.attr.headerBackgroundColor))
        isClickable = true
        isFocusable = true
        val outValue = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        foreground = context.getDrawable(outValue.resourceId)

        val texts = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        summary.apply {
            setTextColor(sres.getColor(R.attr.contrast100))
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
        }
        detail.apply {
            setTextColor(sres.getColor(R.attr.contrast60))
            textSize = 12f
        }
        texts.addView(summary)
        texts.addView(detail)
        addView(texts)
        addView(
            TextView(context).apply {
                text = "›"
                textSize = 22f
                setTextColor(sres.getColor(R.attr.contrast60))
            }
        )
        hide()
    }

    fun bind(insights: PortfolioInsights) {
        if (insights.totalHabits == 0) {
            hide()
            return
        }
        visibility = VISIBLE
        summary.text = resources.getString(
            R.string.insights_bar_today,
            insights.completedToday,
            insights.totalHabits
        )
        detail.text = resources.getString(
            R.string.insights_bar_detail,
            pct(insights.averageScore),
            pct(insights.rate7)
        )
    }

    fun hide() {
        visibility = GONE
    }

    private fun pct(value: Double): String =
        String.format(Locale.getDefault(), "%.0f%%", value * 100)
}
