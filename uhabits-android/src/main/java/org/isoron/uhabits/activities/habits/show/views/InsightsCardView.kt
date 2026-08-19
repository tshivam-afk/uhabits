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
package org.isoron.uhabits.activities.habits.show.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import org.isoron.platform.gui.toInt
import org.isoron.platform.time.JavaLocalDateFormatter
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.analytics.Trend
import org.isoron.uhabits.core.ui.screens.habits.show.views.InsightsCardState
import org.isoron.uhabits.databinding.ShowHabitInsightsBinding
import java.util.Locale

class InsightsCardView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private val binding = ShowHabitInsightsBinding.inflate(LayoutInflater.from(context), this)
    private val formatter = JavaLocalDateFormatter(Locale.getDefault())

    init {
        orientation = VERTICAL
    }

    fun setState(state: InsightsCardState) {
        val color = state.theme.color(state.color).toInt()
        val insights = state.insights
        binding.title.setTextColor(color)
        binding.strengthValue.setTextColor(color)
        binding.formValue.setTextColor(color)
        binding.consistencyValue.setTextColor(color)
        binding.recoveryValue.setTextColor(color)
        binding.rate7Value.setTextColor(color)
        binding.rate30Value.setTextColor(color)
        binding.streakValue.setTextColor(color)
        binding.forecastValue.setTextColor(color)
        binding.strengthValue.text = pct(insights.strength)
        binding.formValue.text = pct(insights.form)
        binding.consistencyValue.text = pct(insights.consistency)
        binding.recoveryValue.text = pct(insights.recoveryRate)
        binding.rate7Value.text = pct(insights.rate7.rate)
        binding.rate30Value.text = pct(insights.rate30.rate)
        binding.streakValue.text = insights.currentStreak.toString()
        binding.forecastValue.text = insights.forecastDaysToFull?.toString()
            ?: resources.getString(R.string.em_dash)
        binding.trendLabel.text = when (insights.trend) {
            Trend.RISING -> resources.getString(R.string.trend_rising)
            Trend.FALLING -> resources.getString(R.string.trend_falling)
            Trend.STABLE -> resources.getString(R.string.trend_stable)
        }
        binding.trendLabel.setTextColor(color)
        binding.weekdayBars.setStats(insights.weekdayStats, color)
        val best = insights.bestWeekday?.let { formatter.longWeekdayName(it) }
        binding.bestDayLabel.text = if (best != null) {
            resources.getString(R.string.best_weekday_format, best)
        } else {
            resources.getString(R.string.not_enough_data)
        }
    }

    private fun pct(value: Double): String = String.format(Locale.getDefault(), "%.0f%%", value * 100)
}
