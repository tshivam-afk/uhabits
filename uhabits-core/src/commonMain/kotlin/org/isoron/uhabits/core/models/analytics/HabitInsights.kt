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
import org.isoron.uhabits.core.models.Score
import kotlin.math.sqrt

enum class Trend {
    RISING,
    STABLE,
    FALLING
}

data class WindowRate(
    val days: Int,
    val successes: Int,
    val eligible: Int,
    val expected: Double,
    val rate: Double
) {
    companion object {
        val EMPTY = WindowRate(0, 0, 0, 0.0, 0.0)
    }
}

data class WeekdayStat(
    val day: DayOfWeek,
    val successes: Int,
    val eligible: Int,
    val rate: Double
)

data class MonthlyRate(
    val start: LocalDate,
    val successes: Int,
    val eligible: Int,
    val rate: Double
)

/**
 * Advanced analytics for a single habit. Built on top of the classic score
 * engine without changing its formula.
 */
data class HabitInsights(
    val scoreToday: Double,
    val form: Double,
    val strength: Double,
    val trend: Trend,
    val momentum7: Double,
    val momentum30: Double,
    val rate7: WindowRate,
    val rate30: WindowRate,
    val rate90: WindowRate,
    val rate365: WindowRate,
    val currentStreak: Int,
    val longestStreak: Int,
    val weekdayStats: List<WeekdayStat>,
    val bestWeekday: DayOfWeek?,
    val worstWeekday: DayOfWeek?,
    val consistency: Double,
    val recoveryRate: Double,
    val skipRate: Double,
    val perfectWeeks: Int,
    val volatility: Double,
    val forecastDaysToHalf: Int?,
    val forecastDaysToFull: Int?,
    val monthlyRates: List<MonthlyRate>,
    val totalCheckins: Int,
    val notesCount: Int,
    val adherence: Double
) {
    companion object {
        private const val TREND_THRESHOLD = 0.03
        private const val MAX_FORECAST_DAYS = 3650

        fun compute(habit: Habit, today: LocalDate = getToday()): HabitInsights {
            val known = habit.originalEntries.getKnown()
            val first = known.lastOrNull()?.date
            val scoreToday = habit.scores[today].value
            val score7 = habit.scores[today.minus(7)].value
            val score30 = habit.scores[today.minus(30)].value
            val momentum7 = scoreToday - score7
            val momentum30 = scoreToday - score30

            if (first == null || first.isNewerThan(today)) {
                return empty(scoreToday, momentum7, momentum30)
            }

            val historyFrom = first
            val history = habit.computedEntries.getByInterval(historyFrom, today).asReversed()
            val rate7 = windowRate(habit, history, today, 7)
            val rate30 = windowRate(habit, history, today, 30)
            val rate90 = windowRate(habit, history, today, 90)
            val rate365 = windowRate(habit, history, today, 365)
            val form = recentForm(habit, history, today)
            val weekdayStats = weekdayStats(habit, history, today)
            val rankedDays = weekdayStats
                .filter { it.eligible > 0 }
                .sortedWith(compareByDescending<WeekdayStat> { it.rate }.thenByDescending { it.eligible })
            val bestWeekday = rankedDays.firstOrNull()?.day
            val worstWeekday = rankedDays.minWithOrNull(
                compareBy<WeekdayStat> { it.rate }.thenByDescending { it.eligible }
            )?.day
            val consistency = consistency(habit, history, today)
            val recovery = recoveryRate(habit, history)
            val skip = skipRate(history)
            val perfectWeeks = perfectWeeks(habit, history, today)
            val volatility = volatility(habit, today)
            val currentStreak = habit.streaks.getCurrent(today)?.length ?: 0
            val longestStreak = habit.streaks.getLongest()?.length ?: 0
            val monthly = monthlyRates(habit, history, today)
            val totalCheckins = known.count { it.value == Entry.YES_MANUAL || it.value > Entry.SKIP }
            val notesCount = known.count { it.notes.isNotBlank() }
            val lifetimeDays = historyFrom.daysUntil(today) + 1
            val lifetime = windowRate(habit, history, today, lifetimeDays)
            val strength = strength(
                score = scoreToday,
                rate30 = rate30.rate,
                form = form,
                consistency = consistency,
                streak = currentStreak,
                recovery = recovery
            )
            return HabitInsights(
                scoreToday = scoreToday,
                form = form,
                strength = strength,
                trend = trend(momentum7),
                momentum7 = momentum7,
                momentum30 = momentum30,
                rate7 = rate7,
                rate30 = rate30,
                rate90 = rate90,
                rate365 = rate365,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                weekdayStats = weekdayStats,
                bestWeekday = bestWeekday,
                worstWeekday = worstWeekday,
                consistency = consistency,
                recoveryRate = recovery,
                skipRate = skip,
                perfectWeeks = perfectWeeks,
                volatility = volatility,
                forecastDaysToHalf = forecastDays(habit, scoreToday, rate30.rate, 0.5),
                forecastDaysToFull = forecastDays(habit, scoreToday, rate30.rate, 1.0),
                monthlyRates = monthly,
                totalCheckins = totalCheckins,
                notesCount = notesCount,
                adherence = lifetime.rate
            )
        }

        fun empty(
            scoreToday: Double = 0.0,
            momentum7: Double = 0.0,
            momentum30: Double = 0.0
        ): HabitInsights {
            val emptyDays = DayOfWeek.entries.map { WeekdayStat(it, 0, 0, 0.0) }
            return HabitInsights(
                scoreToday = scoreToday,
                form = 0.0,
                strength = 0.0,
                trend = trend(momentum7),
                momentum7 = momentum7,
                momentum30 = momentum30,
                rate7 = WindowRate.EMPTY,
                rate30 = WindowRate.EMPTY,
                rate90 = WindowRate.EMPTY,
                rate365 = WindowRate.EMPTY,
                currentStreak = 0,
                longestStreak = 0,
                weekdayStats = emptyDays,
                bestWeekday = null,
                worstWeekday = null,
                consistency = 0.0,
                recoveryRate = 0.0,
                skipRate = 0.0,
                perfectWeeks = 0,
                volatility = 0.0,
                forecastDaysToHalf = if (scoreToday >= 0.5) 0 else null,
                forecastDaysToFull = if (scoreToday >= 1.0) 0 else null,
                monthlyRates = emptyList(),
                totalCheckins = 0,
                notesCount = 0,
                adherence = 0.0
            )
        }

        fun trend(momentum7: Double): Trend {
            return when {
                momentum7 > TREND_THRESHOLD -> Trend.RISING
                momentum7 < -TREND_THRESHOLD -> Trend.FALLING
                else -> Trend.STABLE
            }
        }

        internal fun strength(
            score: Double,
            rate30: Double,
            form: Double,
            consistency: Double,
            streak: Int,
            recovery: Double
        ): Double {
            val streakFactor = (streak / 30.0).coerceIn(0.0, 1.0)
            return (
                0.30 * score.coerceIn(0.0, 1.0) +
                    0.25 * rate30.coerceIn(0.0, 1.0) +
                    0.15 * form.coerceIn(0.0, 1.0) +
                    0.15 * consistency.coerceIn(0.0, 1.0) +
                    0.10 * streakFactor +
                    0.05 * recovery.coerceIn(0.0, 1.0)
                ).coerceIn(0.0, 1.0)
        }

        internal fun forecastDays(
            habit: Habit,
            scoreToday: Double,
            dailySuccessRate: Double,
            target: Double
        ): Int? {
            if (scoreToday >= target) return 0
            if (dailySuccessRate <= 0.0) return null
            val freq = habit.frequency.toDouble()
            var score = scoreToday
            var days = 0
            while (score < target && days < MAX_FORECAST_DAYS) {
                score = Score.compute(freq, score, dailySuccessRate)
                days++
            }
            return if (score >= target) days else null
        }

        private fun windowRate(
            habit: Habit,
            history: List<Entry>,
            today: LocalDate,
            days: Int
        ): WindowRate {
            val from = today.minus(days - 1)
            var successes = 0
            var eligible = 0
            var skips = 0
            for (entry in history) {
                if (entry.date.isOlderThan(from)) continue
                if (entry.value == Entry.SKIP) {
                    skips++
                    continue
                }
                eligible++
                if (habit.isSuccessful(entry)) successes++
            }
            val calendarDays = days - skips
            val expected = calendarDays.coerceAtLeast(0) * habit.frequency.toDouble()
            val rate = if (eligible == 0) 0.0 else successes.toDouble() / eligible
            return WindowRate(days, successes, eligible, expected, rate)
        }

        private fun recentForm(
            habit: Habit,
            history: List<Entry>,
            today: LocalDate
        ): Double {
            val from = today.minus(59)
            val freq = habit.frequency.toDouble()
            var score = 0.0
            for (entry in history) {
                if (entry.date.isOlderThan(from)) continue
                if (entry.value == Entry.SKIP) continue
                val check = if (habit.isSuccessful(entry)) 1.0 else 0.0
                score = Score.computeRecentForm(freq, score, check)
            }
            return score
        }

        private fun weekdayStats(
            habit: Habit,
            history: List<Entry>,
            today: LocalDate
        ): List<WeekdayStat> {
            val from = today.minus(364)
            val successes = IntArray(7)
            val eligible = IntArray(7)
            for (entry in history) {
                if (entry.date.isOlderThan(from)) continue
                if (entry.value == Entry.SKIP) continue
                val idx = entry.date.dayOfWeek.daysSinceSunday
                eligible[idx]++
                if (habit.isSuccessful(entry)) successes[idx]++
            }
            return DayOfWeek.entries.map { day ->
                val e = eligible[day.daysSinceSunday]
                val s = successes[day.daysSinceSunday]
                WeekdayStat(day, s, e, if (e == 0) 0.0 else s.toDouble() / e)
            }
        }

        private fun consistency(
            habit: Habit,
            history: List<Entry>,
            today: LocalDate
        ): Double {
            val rates = ArrayList<Double>(12)
            for (week in 0 until 12) {
                val weekEnd = today.minus(week * 7)
                val weekStart = weekEnd.minus(6)
                var successes = 0
                var eligible = 0
                for (entry in history) {
                    if (entry.date.isOlderThan(weekStart) || entry.date.isNewerThan(weekEnd)) continue
                    if (entry.value == Entry.SKIP) continue
                    eligible++
                    if (habit.isSuccessful(entry)) successes++
                }
                if (eligible > 0) rates.add(successes.toDouble() / eligible)
            }
            if (rates.size < 2) return if (rates.firstOrNull() == 1.0) 1.0 else 0.0
            val mean = rates.average()
            if (mean <= 0.0) return 0.0
            val variance = rates.sumOf { (it - mean) * (it - mean) } / rates.size
            val cv = sqrt(variance) / mean
            return (1.0 - cv).coerceIn(0.0, 1.0)
        }

        private fun recoveryRate(habit: Habit, history: List<Entry>): Double {
            var misses = 0
            var recovered = 0
            var pendingMiss = false
            for (entry in history) {
                if (entry.value == Entry.SKIP) continue
                if (habit.isSuccessful(entry)) {
                    if (pendingMiss) {
                        recovered++
                        pendingMiss = false
                    }
                } else if (!pendingMiss) {
                    misses++
                    pendingMiss = true
                }
            }
            if (misses == 0) return if (history.any { habit.isSuccessful(it) }) 1.0 else 0.0
            return recovered.toDouble() / misses
        }

        private fun skipRate(history: List<Entry>): Double {
            if (history.isEmpty()) return 0.0
            val skips = history.count { it.value == Entry.SKIP }
            return skips.toDouble() / history.size
        }

        private fun perfectWeeks(
            habit: Habit,
            history: List<Entry>,
            today: LocalDate
        ): Int {
            var count = 0
            for (week in 0 until 12) {
                val weekEnd = today.minus(week * 7)
                val weekStart = weekEnd.minus(6)
                var successes = 0
                var misses = 0
                for (entry in history) {
                    if (entry.date.isOlderThan(weekStart) || entry.date.isNewerThan(weekEnd)) continue
                    if (entry.value == Entry.SKIP) continue
                    if (habit.isSuccessful(entry)) successes++ else misses++
                }
                if (successes > 0 && misses == 0) count++
            }
            return count
        }

        private fun volatility(habit: Habit, today: LocalDate): Double {
            val scores = habit.scores.getByInterval(today.minus(29), today).map { it.value }
            if (scores.size < 2) return 0.0
            val mean = scores.average()
            val variance = scores.sumOf { (it - mean) * (it - mean) } / scores.size
            return sqrt(variance)
        }

        private fun monthlyRates(
            habit: Habit,
            history: List<Entry>,
            today: LocalDate
        ): List<MonthlyRate> {
            val result = ArrayList<MonthlyRate>(6)
            var cursor = today.startOfMonth()
            repeat(6) {
                val start = cursor
                val end = if (cursor.year == today.year && cursor.month == today.month) {
                    today
                } else {
                    cursor.plus(cursor.monthLength - 1)
                }
                var successes = 0
                var eligible = 0
                for (entry in history) {
                    if (entry.date.isOlderThan(start) || entry.date.isNewerThan(end)) continue
                    if (entry.value == Entry.SKIP) continue
                    eligible++
                    if (habit.isSuccessful(entry)) successes++
                }
                val rate = if (eligible == 0) 0.0 else successes.toDouble() / eligible
                result.add(MonthlyRate(start, successes, eligible, rate))
                cursor = start.minus(1).startOfMonth()
            }
            return result
        }
    }
}
