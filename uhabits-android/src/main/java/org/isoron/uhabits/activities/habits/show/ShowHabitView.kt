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

package org.isoron.uhabits.activities.habits.show

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.isoron.uhabits.core.ui.screens.habits.show.ShowHabitPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.ShowHabitState
import org.isoron.uhabits.databinding.ShowHabitBinding
import org.isoron.uhabits.utils.applyBottomInset
import org.isoron.uhabits.utils.applyToolbarInsets
import org.isoron.uhabits.utils.playCardEnter
import org.isoron.uhabits.utils.setupToolbar

class ShowHabitView(context: Context) : FrameLayout(context) {
    private val binding = ShowHabitBinding.inflate(LayoutInflater.from(context))
    private var didAnimateCards = false

    init {
        binding.toolbar.applyToolbarInsets()
        addView(binding.root)
    }

    fun setState(data: ShowHabitState) {
        setupToolbar(
            binding.toolbar,
            title = data.title,
            color = data.color,
            theme = data.theme
        )
        binding.subtitleCard.setState(data.subtitle)
        binding.overviewCard.setState(data.overview)
        binding.insightsCard.setState(data.insights)
        binding.notesCard.setState(data.notes)
        binding.targetCard.setState(data.target)
        binding.streakCard.setState(data.streaks)
        binding.scoreCard.setState(data.scores)
        binding.frequencyCard.setState(data.frequency)
        binding.historyCard.setState(data.history)
        binding.barCard.setState(data.bar)
        if (data.isNumerical) {
            binding.overviewCard.visibility = GONE
        } else {
            binding.targetCard.visibility = GONE
        }
        binding.linearLayout.applyBottomInset()
        if (!didAnimateCards) {
            didAnimateCards = true
            animateCards()
        }
    }

    private fun animateCards() {
        val layout = binding.linearLayout
        var delay = 0L
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child.visibility != VISIBLE) continue
            child.playCardEnter(delay)
            delay += 40L
        }
    }

    fun setListener(presenter: ShowHabitPresenter) {
        binding.scoreCard.setListener(presenter.scoreCardPresenter)
        binding.historyCard.setListener(presenter.historyCardPresenter)
        binding.barCard.setListener(presenter.barCardPresenter)
    }
}
