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
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.Score
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HabitInsightsTest : BaseUnitTest() {

    @Test
    fun emptyHabitHasZeroedInsights() {
        val habit = fixtures.createEmptyHabit()
        habit.recompute()
        val insights = HabitInsights.compute(habit)
        assertEquals(0.0, insights.scoreToday, 1e-9)
        assertEquals(0.0, insights.strength, 1e-9)
        assertEquals(0, insights.currentStreak)
        assertEquals(0, insights.rate30.eligible)
        assertEquals(Trend.STABLE, insights.trend)
        assertNull(insights.bestWeekday)
    }

    @Test
    fun dailySuccessStreakAndRate() {
        val habit = fixtures.createEmptyHabit()
        habit.frequency = Frequency.DAILY
        val today = getToday()
        for (i in 0 until 14) {
            habit.originalEntries.add(Entry(today.minus(i), Entry.YES_MANUAL))
        }
        habit.recompute()
        val insights = HabitInsights.compute(habit)
        assertEquals(14, insights.currentStreak)
        assertEquals(14, insights.longestStreak)
        assertEquals(7, insights.rate7.successes)
        assertEquals(7, insights.rate7.eligible)
        assertEquals(1.0, insights.rate7.rate, 1e-9)
        assertEquals(14, insights.rate30.successes)
        assertTrue(insights.scoreToday > 0.4)
        assertTrue(insights.strength > 0.4)
        assertEquals(1.0, insights.recoveryRate, 1e-9)
        assertEquals(0.0, insights.skipRate, 1e-9)
        assertTrue(insights.perfectWeeks >= 1)
        assertEquals(0, insights.forecastDaysToHalf)
    }

    @Test
    fun skipDaysAreExcludedFromRates() {
        val habit = fixtures.createEmptyHabit()
        habit.frequency = Frequency.DAILY
        val today = getToday()
        habit.originalEntries.add(Entry(today, Entry.YES_MANUAL))
        habit.originalEntries.add(Entry(today.minus(1), Entry.SKIP))
        habit.originalEntries.add(Entry(today.minus(2), Entry.YES_MANUAL))
        habit.recompute()
        val insights = HabitInsights.compute(habit)
        assertEquals(2, insights.rate7.successes)
        assertEquals(2, insights.rate7.eligible)
        assertEquals(1.0, insights.rate7.rate, 1e-9)
        assertTrue(insights.skipRate > 0.0)
        assertEquals(3, insights.currentStreak)
    }

    @Test
    fun missThenRecovery() {
        val habit = fixtures.createEmptyHabit()
        habit.frequency = Frequency.DAILY
        val today = getToday()
        habit.originalEntries.add(Entry(today, Entry.YES_MANUAL))
        habit.originalEntries.add(Entry(today.minus(1), Entry.NO))
        habit.originalEntries.add(Entry(today.minus(2), Entry.YES_MANUAL))
        habit.recompute()
        val insights = HabitInsights.compute(habit)
        assertEquals(1.0, insights.recoveryRate, 1e-9)
        assertEquals(2, insights.rate7.successes)
        assertEquals(3, insights.rate7.eligible)
        assertEquals(1, insights.currentStreak)
    }

    @Test
    fun weekdayStrengthPicksBestDay() {
        val habit = fixtures.createEmptyHabit()
        habit.frequency = Frequency.DAILY
        val today = getToday()
        for (i in 0 until 28) {
            val date = today.minus(i)
            val value = if (date.dayOfWeek == DayOfWeek.MONDAY) {
                Entry.YES_MANUAL
            } else {
                Entry.NO
            }
            habit.originalEntries.add(Entry(date, value))
        }
        habit.recompute()
        val insights = HabitInsights.compute(habit)
        assertEquals(DayOfWeek.MONDAY, insights.bestWeekday)
        val monday = insights.weekdayStats.first { it.day == DayOfWeek.MONDAY }
        assertEquals(1.0, monday.rate, 1e-9)
        assertTrue(insights.worstWeekday != DayOfWeek.MONDAY)
    }

    @Test
    fun numericalSkipIsNotSuccess() {
        val habit = fixtures.createEmptyNumericalHabit(NumericalHabitType.AT_LEAST)
        habit.frequency = Frequency.DAILY
        val today = getToday()
        habit.originalEntries.add(Entry(today, 2000))
        habit.originalEntries.add(Entry(today.minus(1), Entry.SKIP))
        habit.originalEntries.add(Entry(today.minus(2), 500))
        habit.recompute()
        assertTrue(habit.isSuccessful(habit.computedEntries.get(today)))
        assertTrue(!habit.isSuccessful(habit.computedEntries.get(today.minus(1))))
        assertTrue(!habit.isSuccessful(habit.computedEntries.get(today.minus(2))))
        val insights = HabitInsights.compute(habit)
        assertEquals(1, insights.rate7.successes)
        assertEquals(2, insights.rate7.eligible)
        assertEquals(3, insights.currentStreak)
    }

    @Test
    fun forecastFromZeroNeedsTime() {
        val habit = fixtures.createEmptyHabit()
        habit.frequency = Frequency.DAILY
        habit.recompute()
        val days = HabitInsights.forecastDays(habit, 0.0, 1.0, 0.5)
        assertTrue(days != null && days > 0)
        assertEquals(0, HabitInsights.forecastDays(habit, 0.8, 0.2, 0.5))
        assertNull(HabitInsights.forecastDays(habit, 0.1, 0.0, 0.5))
    }

    @Test
    fun recentFormReactsFasterThanClassicScore() {
        val habit = fixtures.createEmptyHabit()
        habit.frequency = Frequency.DAILY
        val today = getToday()
        for (i in 30 downTo 8) {
            habit.originalEntries.add(Entry(today.minus(i), Entry.YES_MANUAL))
        }
        for (i in 7 downTo 0) {
            habit.originalEntries.add(Entry(today.minus(i), Entry.NO))
        }
        habit.recompute()
        val insights = HabitInsights.compute(habit)
        assertTrue(insights.form < insights.scoreToday)
        assertEquals(Trend.FALLING, insights.trend)
        assertTrue(insights.momentum7 < 0.0)
    }

    @Test
    fun strengthIsBounded() {
        assertEquals(0.0, HabitInsights.strength(0.0, 0.0, 0.0, 0.0, 0, 0.0), 1e-9)
        val full = HabitInsights.strength(1.0, 1.0, 1.0, 1.0, 90, 1.0)
        assertEquals(1.0, full, 1e-9)
    }

    @Test
    fun recentFormFormulaDiffersFromClassic() {
        val classic = Score.compute(1.0, 0.0, 1.0)
        val form = Score.computeRecentForm(1.0, 0.0, 1.0)
        assertTrue(form > classic)
        assertEquals(classic, Score.computeWithHalfLife(1.0, 0.0, 1.0, Score.CLASSIC_HALF_LIFE), 1e-12)
    }
}
