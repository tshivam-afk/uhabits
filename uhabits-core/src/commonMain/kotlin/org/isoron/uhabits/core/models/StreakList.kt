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
package org.isoron.uhabits.core.models

import org.isoron.platform.Synchronized
import org.isoron.platform.time.LocalDate
import kotlin.math.min

class StreakList {
    private val list = ArrayList<Streak>()

    @Synchronized
    fun getBest(limit: Int): List<Streak> {
        return list
            .sortedWith { s1: Streak, s2: Streak -> s2.compareLonger(s1) }
            .take(min(list.size, limit))
            .sortedWith { s1: Streak, s2: Streak -> s2.compareNewer(s1) }
    }

    @Synchronized
    fun recompute(
        computedEntries: EntryList,
        from: LocalDate,
        to: LocalDate,
        isNumerical: Boolean,
        targetValue: Double,
        targetType: NumericalHabitType
    ) {
        list.clear()
        val dates = computedEntries
            .getByInterval(from, to)
            .filter { isStreakDay(it.value, isNumerical, targetValue, targetType) }
            .map { it.date }
            .toTypedArray()

        if (dates.isEmpty()) return

        var begin = dates[0]
        var end = dates[0]
        for (i in 1 until dates.size) {
            val current = dates[i]
            if (current == begin.minus(1)) {
                begin = current
            } else {
                list.add(Streak(begin, end))
                begin = current
                end = current
            }
        }
        list.add(Streak(begin, end))
    }

    companion object {
        /**
         * Skip days never break a streak. Numerical "at least" skips used to fail
         * the target check because SKIP is stored as 3 (0.003 after scaling).
         */
        fun isStreakDay(
            value: Int,
            isNumerical: Boolean,
            targetValue: Double,
            targetType: NumericalHabitType
        ): Boolean {
            if (value == Entry.SKIP) return true
            return if (isNumerical) {
                when (targetType) {
                    NumericalHabitType.AT_LEAST -> value / 1000.0 >= targetValue
                    NumericalHabitType.AT_MOST ->
                        value != Entry.UNKNOWN && value / 1000.0 <= targetValue
                }
            } else {
                value > 0
            }
        }
    }
}
