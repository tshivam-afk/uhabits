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

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.core.models.analytics.PortfolioInsights
import org.isoron.uhabits.utils.applyRootViewInsets

class InsightsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as HabitsApplication
        AndroidThemeSwitcher(this, app.component.preferences).apply()
        val view = InsightsView(this)
        view.applyRootViewInsets()
        setContentView(view)
        view.bind(
            PortfolioInsights.compute(app.component.habitList),
            app.component.intentFactory
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
