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

import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PortfolioInsightsTest : BaseUnitTest() {

    @Test
    fun emptyList() {
        val insights = PortfolioInsights.compute(habitList)
        assertEquals(0, insights.totalHabits)
        assertEquals(0, insights.completedToday)
        assertEquals(0.0, insights.averageScore, 1e-9)
    }

    @Test
    fun todayCountsAndIgnoresArchived() {
        val today = getToday()
        val done = fixtures.createEmptyHabit("Done")
        done.frequency = Frequency.DAILY
        done.originalEntries.add(Entry(today, Entry.YES_MANUAL))
        done.recompute()
        habitList.add(done)

        val leftover = fixtures.createEmptyHabit("Left")
        leftover.frequency = Frequency.DAILY
        leftover.recompute()
        habitList.add(leftover)

        val archived = fixtures.createEmptyHabit("Old")
        archived.isArchived = true
        archived.recompute()
        habitList.add(archived)

        val insights = PortfolioInsights.compute(habitList)
        assertEquals(2, insights.totalHabits)
        assertEquals(1, insights.completedToday)
        assertEquals(1, insights.remainingToday)
        assertEquals(0, insights.skippedToday)
        assertTrue(insights.topHabits.isNotEmpty())
        assertTrue(insights.attentionHabits.any { it.name == "Left" })
    }

    @Test
    fun perfectDayWhenEveryHabitSucceeds() {
        val today = getToday()
        repeat(2) { index ->
            val habit = fixtures.createEmptyHabit("H$index")
            habit.frequency = Frequency.DAILY
            habit.originalEntries.add(Entry(today, Entry.YES_MANUAL))
            habit.originalEntries.add(Entry(today.minus(1), Entry.NO))
            habit.recompute()
            habitList.add(habit)
        }
        val insights = PortfolioInsights.compute(habitList)
        assertEquals(1, insights.perfectDays30)
        assertEquals(2, insights.completedToday)
    }
}
