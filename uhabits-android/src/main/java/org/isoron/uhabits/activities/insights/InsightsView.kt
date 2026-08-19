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
package org.isoron.uhabits.activities.insights

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.RingView
import org.isoron.uhabits.activities.common.views.WeekdayBarsView
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.analytics.HabitSnapshot
import org.isoron.uhabits.core.models.analytics.PortfolioInsights
import org.isoron.uhabits.core.models.analytics.Trend
import org.isoron.uhabits.intents.IntentFactory
import org.isoron.uhabits.utils.applyBottomInset
import org.isoron.uhabits.utils.buildToolbar
import org.isoron.uhabits.utils.currentTheme
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.playCardEnter
import org.isoron.uhabits.utils.setupToolbar
import org.isoron.uhabits.utils.sres
import org.isoron.uhabits.utils.str
import java.util.Locale

class InsightsView(context: Context) : LinearLayout(context) {

    private val toolbar = buildToolbar()
    private val content = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(8f).toInt(), dp(8f).toInt(), dp(8f).toInt(), dp(24f).toInt())
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(sres.getColor(R.attr.windowBackgroundColor))
        val theme = currentTheme()
        setupToolbar(
            toolbar = toolbar,
            title = str(R.string.insights),
            color = PaletteColor(11),
            theme = theme
        )
        addView(toolbar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            applyBottomInset()
        }
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    fun bind(insights: PortfolioInsights, intents: IntentFactory) {
        content.removeAllViews()
        val accent = currentTheme().color(PaletteColor(11)).toInt()
        content.addView(todayCard(insights, accent))
        content.addView(ratesCard(insights, accent))
        content.addView(weekdayCard(insights, accent))
        content.addView(habitListCard(R.string.top_habits, insights.topHabits, intents, accent))
        content.addView(
            habitListCard(R.string.needs_attention, insights.attentionHabits, intents, accent)
        )
        for (i in 0 until content.childCount) {
            content.getChildAt(i).playCardEnter(i * 45L)
        }
    }

    private fun todayCard(insights: PortfolioInsights, accent: Int): LinearLayout {
        val card = card()
        card.addView(header(str(R.string.today), accent))
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val done = insights.completedToday
        val total = insights.totalHabits
        val pct = if (total == 0) 0f else done.toFloat() / total
        val ring = RingView(context).apply {
            val size = dp(72f).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(20f).toInt()
            }
            setColor(accent)
            setThickness(dp(8f))
            setTextSize(sp(14f))
            setText(if (total == 0) "0" else "$done/$total")
            setPercentage(pct)
        }
        row.addView(ring)
        val labels = LinearLayout(context).apply { orientation = VERTICAL }
        labels.addView(
            metricLine(
                resources.getString(R.string.remaining_today_format, insights.remainingToday),
                accent
            )
        )
        labels.addView(
            metricLine(
                resources.getString(R.string.skipped_today_format, insights.skippedToday),
                sres.getColor(R.attr.contrast80)
            )
        )
        labels.addView(
            metricLine(
                resources.getString(R.string.perfect_days_format, insights.perfectDays30),
                sres.getColor(R.attr.contrast80)
            )
        )
        row.addView(labels, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        card.addView(row)
        return card
    }

    private fun ratesCard(insights: PortfolioInsights, accent: Int): LinearLayout {
        val card = card()
        card.addView(header(str(R.string.portfolio), accent))
        val row = LinearLayout(context).apply { orientation = HORIZONTAL }
        row.addView(statColumn(pct(insights.averageScore), str(R.string.avg_score), accent))
        row.addView(statColumn(pct(insights.averageStrength), str(R.string.habit_strength), accent))
        row.addView(statColumn(pct(insights.rate7), str(R.string.last_7_days), accent))
        row.addView(statColumn(pct(insights.rate30), str(R.string.last_30_days), accent))
        card.addView(row)
        val trend = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, dp(12f).toInt(), 0, 0)
        }
        trend.addView(
            metricLine(
                resources.getString(R.string.rising_count_format, insights.risingCount),
                accent
            )
        )
        trend.addView(
            metricLine(
                resources.getString(R.string.falling_count_format, insights.fallingCount),
                sres.getColor(R.attr.contrast80)
            ).apply {
                setPadding(dp(16f).toInt(), 0, 0, 0)
            }
        )
        card.addView(trend)
        card.addView(
            metricLine(
                resources.getString(R.string.total_checkins_format, insights.totalCheckins),
                sres.getColor(R.attr.contrast80)
            ).apply { setPadding(0, dp(8f).toInt(), 0, 0) }
        )
        return card
    }

    private fun weekdayCard(insights: PortfolioInsights, accent: Int): LinearLayout {
        val card = card()
        card.addView(header(str(R.string.weekday_strength), accent))
        val bars = WeekdayBarsView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(88f).toInt())
            setStats(insights.weekdayRates, accent)
        }
        card.addView(bars)
        return card
    }

    private fun habitListCard(
        title: Int,
        habits: List<HabitSnapshot>,
        intents: IntentFactory,
        accent: Int
    ): LinearLayout {
        val card = card()
        card.addView(header(str(title), accent))
        if (habits.isEmpty()) {
            card.addView(
                metricLine(str(R.string.not_enough_data), sres.getColor(R.attr.contrast60))
            )
            return card
        }
        val activity = context as AppCompatActivity
        val habitList = (activity.application as org.isoron.uhabits.HabitsApplication)
            .component.habitList
        for (snap in habits) {
            card.addView(habitRow(snap, habitList, intents))
        }
        return card
    }

    private fun habitRow(
        snap: HabitSnapshot,
        habitList: HabitList,
        intents: IntentFactory
    ): LinearLayout {
        val color = currentTheme().color(snap.color).toInt()
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10f).toInt(), 0, dp(10f).toInt())
            isClickable = true
            isFocusable = true
            val out = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, out, true)
            setBackgroundResource(out.resourceId)
            setOnClickListener {
                val habit = snap.id?.let { habitList.getById(it) } ?: return@setOnClickListener
                context.startActivity(intents.startShowHabitActivity(context, habit))
            }
        }
        val name = TextView(context).apply {
            text = snap.name
            setTextColor(color)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        val meta = TextView(context).apply {
            text = "${pct(snap.strength)} · ${trendLabel(snap.trend)}"
            setTextColor(sres.getColor(R.attr.contrast60))
            textSize = 12f
        }
        row.addView(name)
        row.addView(meta)
        return row
    }

    private fun card(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setBackgroundResource(R.drawable.card_rounded)
            elevation = dp(2f)
            val pad = dp(18f).toInt()
            setPadding(pad, pad, pad, pad)
            val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            val m = dp(6f).toInt()
            lp.setMargins(m, m, m, m)
            layoutParams = lp
        }
    }

    private fun header(text: String, color: Int): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(color)
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(12f).toInt())
        }
    }

    private fun statColumn(value: String, label: String, color: Int): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            addView(
                TextView(context).apply {
                    this.text = value
                    setTextColor(color)
                    textSize = 18f
                    setTypeface(typeface, Typeface.BOLD)
                }
            )
            addView(
                TextView(context).apply {
                    this.text = label
                    setTextColor(sres.getColor(R.attr.contrast60))
                    textSize = 10f
                }
            )
        }
    }

    private fun metricLine(text: String, color: Int): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(color)
            textSize = 14f
        }
    }

    private fun trendLabel(trend: Trend): String {
        return when (trend) {
            Trend.RISING -> str(R.string.trend_rising)
            Trend.FALLING -> str(R.string.trend_falling)
            Trend.STABLE -> str(R.string.trend_stable)
        }
    }

    private fun pct(value: Double): String =
        String.format(Locale.getDefault(), "%.0f%%", value * 100)

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            value,
            resources.displayMetrics
        )
    }
}
