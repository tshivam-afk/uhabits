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
package org.isoron.uhabits.core.models.analytics

import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.PaletteColor

data class HabitSnapshot(
    val id: Long?,
    val name: String,
    val color: PaletteColor,
    val score: Double,
    val strength: Double,
    val trend: Trend,
    val completedToday: Boolean,
    val enteredToday: Boolean,
    val currentStreak: Int
)

data class PortfolioInsights(
    val totalHabits: Int,
    val completedToday: Int,
    val remainingToday: Int,
    val skippedToday: Int,
    val averageScore: Double,
    val averageStrength: Double,
    val rate7: Double,
    val rate30: Double,
    val perfectDays30: Int,
    val weekdayRates: List<WeekdayStat>,
    val topHabits: List<HabitSnapshot>,
    val attentionHabits: List<HabitSnapshot>,
    val totalCheckins: Int,
    val risingCount: Int,
    val fallingCount: Int
) {
    companion object {
        fun compute(
            habitList: HabitList,
            today: LocalDate = getToday()
        ): PortfolioInsights {
            return compute(habitList.filter { !it.isArchived }, today)
        }

        fun compute(
            habits: Iterable<Habit>,
            today: LocalDate = getToday()
        ): PortfolioInsights {
            val active = habits.toList()
            if (active.isEmpty()) return empty()

            val snapshots = ArrayList<HabitSnapshot>(active.size)
            val insights = ArrayList<HabitInsights>(active.size)
            var completed = 0
            var remaining = 0
            var skipped = 0
            var totalCheckins = 0
            var rate7Success = 0
            var rate7Eligible = 0
            var rate30Success = 0
            var rate30Eligible = 0
            val weekdaySuccess = IntArray(7)
            val weekdayEligible = IntArray(7)

            for (habit in active) {
                val insight = HabitInsights.compute(habit, today)
                insights.add(insight)
                val todayEntry = habit.computedEntries.get(today)
                val completedToday = habit.isCompletedToday()
                val skippedToday = todayEntry.value == Entry.SKIP
                if (skippedToday) skipped++
                else if (completedToday) completed++
                else remaining++
                totalCheckins += insight.totalCheckins
                rate7Success += insight.rate7.successes
                rate7Eligible += insight.rate7.eligible
                rate30Success += insight.rate30.successes
                rate30Eligible += insight.rate30.eligible
                for (stat in insight.weekdayStats) {
                    weekdaySuccess[stat.day.daysSinceSunday] += stat.successes
                    weekdayEligible[stat.day.daysSinceSunday] += stat.eligible
                }
                snapshots.add(
                    HabitSnapshot(
                        id = habit.id,
                        name = habit.name,
                        color = habit.color,
                        score = insight.scoreToday,
                        strength = insight.strength,
                        trend = insight.trend,
                        completedToday = completedToday,
                        enteredToday = habit.isEnteredToday(),
                        currentStreak = insight.currentStreak
                    )
                )
            }

            val perfectDays = perfectDays(active, today, 30)
            val weekdayRates = DayOfWeek.entries.map { day ->
                val e = weekdayEligible[day.daysSinceSunday]
                val s = weekdaySuccess[day.daysSinceSunday]
                WeekdayStat(day, s, e, if (e == 0) 0.0 else s.toDouble() / e)
            }
            val top = snapshots.sortedByDescending { it.strength }.take(3)
            val attention = snapshots
                .filter { snap ->
                    !snap.completedToday || snap.trend == Trend.FALLING || snap.strength < 0.4
                }
                .sortedWith(
                    compareBy<HabitSnapshot> { it.completedToday }
                        .thenBy { it.strength }
                )
                .take(3)
            return PortfolioInsights(
                totalHabits = active.size,
                completedToday = completed,
                remainingToday = remaining,
                skippedToday = skipped,
                averageScore = insights.map { it.scoreToday }.average(),
                averageStrength = insights.map { it.strength }.average(),
                rate7 = if (rate7Eligible == 0) 0.0 else rate7Success.toDouble() / rate7Eligible,
                rate30 = if (rate30Eligible == 0) 0.0 else rate30Success.toDouble() / rate30Eligible,
                perfectDays30 = perfectDays,
                weekdayRates = weekdayRates,
                topHabits = top,
                attentionHabits = attention,
                totalCheckins = totalCheckins,
                risingCount = snapshots.count { it.trend == Trend.RISING },
                fallingCount = snapshots.count { it.trend == Trend.FALLING }
            )
        }

        fun empty(): PortfolioInsights {
            return PortfolioInsights(
                totalHabits = 0,
                completedToday = 0,
                remainingToday = 0,
                skippedToday = 0,
                averageScore = 0.0,
                averageStrength = 0.0,
                rate7 = 0.0,
                rate30 = 0.0,
                perfectDays30 = 0,
                weekdayRates = DayOfWeek.entries.map { WeekdayStat(it, 0, 0, 0.0) },
                topHabits = emptyList(),
                attentionHabits = emptyList(),
                totalCheckins = 0,
                risingCount = 0,
                fallingCount = 0
            )
        }

        private fun perfectDays(
            habits: List<Habit>,
            today: LocalDate,
            days: Int
        ): Int {
            if (habits.isEmpty()) return 0
            var count = 0
            for (offset in 0 until days) {
                val date = today.minus(offset)
                var allGood = true
                var anyEligible = false
                for (habit in habits) {
                    val entry = habit.computedEntries.get(date)
                    if (entry.value == Entry.SKIP) continue
                    anyEligible = true
                    if (!habit.isSuccessful(entry)) {
                        allGood = false
                        break
                    }
                }
                if (allGood && anyEligible) count++
            }
            return count
        }
    }
}
