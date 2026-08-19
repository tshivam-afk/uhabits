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
package org.isoron.uhabits.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import me.tatarka.inject.annotations.Component
import org.isoron.platform.time.computeToday
import org.isoron.platform.time.setToday
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.core.ui.widgets.WidgetBehavior
import org.isoron.uhabits.inject.HabitsApplicationComponent
import org.isoron.uhabits.intents.IntentParser.CheckmarkIntentData

@ReceiverScope
@Component
internal abstract class WidgetComponent(
    @Component val parent: HabitsApplicationComponent
) {
    abstract val widgetController: WidgetBehavior
}

/**
 * The Android BroadcastReceiver for Loop Habit Tracker.
 *
 *
 * All broadcast messages are received and processed by this class.
 */
class WidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as HabitsApplication
        val component = WidgetComponent::class.create(app.component)
        val parser = app.component.intentParser
        val controller = component.widgetController
        val prefs = app.component.preferences
        val widgetUpdater = app.component.widgetUpdater
        Log.i(TAG, String.format("Received intent: %s", intent.toString()))
        lastReceivedIntent = intent
        try {
            // Intent extras are parceled by AlarmManager, so compare actions by value, not identity.
            val action = intent.action
            var data: CheckmarkIntentData? = null
            if (action != ACTION_UPDATE_WIDGETS_VALUE) {
                data = parser.parseCheckmarkIntent(intent)
            }
            when (action) {
                ACTION_ADD_REPETITION -> {
                    Log.d(
                        TAG,
                        String.format(
                            "onAddRepetition habit=%d date=%s",
                            data!!.habit.id,
                            data.date
                        )
                    )
                    controller.onAddRepetition(
                        data.habit,
                        data.date
                    )
                }
                ACTION_TOGGLE_REPETITION -> {
                    Log.d(
                        TAG,
                        String.format(
                            "onToggleRepetition habit=%d date=%s",
                            data!!.habit.id,
                            data.date
                        )
                    )
                    controller.onToggleRepetition(
                        data.habit,
                        data.date
                    )
                }
                ACTION_REMOVE_REPETITION -> {
                    Log.d(
                        TAG,
                        String.format(
                            "onRemoveRepetition habit=%d date=%s",
                            data!!.habit.id,
                            data.date
                        )
                    )
                    controller.onRemoveRepetition(
                        data.habit,
                        data.date
                    )
                }
                ACTION_SET_NUMERICAL_VALUE -> {
                    val value = readNumericalValue(intent)
                    if (value == null || data == null) {
                        Log.e(TAG, "Missing habit or value for ACTION_SET_NUMERICAL_VALUE")
                        return
                    }
                    val milliValue = (value * 1000).toInt()
                    val notes = data.habit.originalEntries.get(data.date).notes
                    Log.d(
                        TAG,
                        String.format(
                            "onSetNumericalValue habit=%d date=%s value=%s",
                            data.habit.id,
                            data.date,
                            value
                        )
                    )
                    controller.setValue(data.habit, data.date, milliValue, notes)
                }
                ACTION_UPDATE_WIDGETS_VALUE -> {
                    setToday(computeToday(prefs.midnightDelayHours, 0))
                    widgetUpdater.updateWidgets()
                    widgetUpdater.scheduleStartDayWidgetUpdate()
                }
            }
        } catch (e: RuntimeException) {
            Log.e("WidgetReceiver", "could not process intent", e)
        }
    }

    private fun readNumericalValue(intent: Intent): Double? {
        val extras = intent.extras ?: return null
        if (!extras.containsKey(EXTRA_NUMERICAL_VALUE)) return null
        return when (val raw = extras.get(EXTRA_NUMERICAL_VALUE)) {
            is Number -> raw.toDouble()
            is String -> raw.replace(',', '.').toDoubleOrNull()
            else -> null
        }
    }

    companion object {
        const val ACTION_ADD_REPETITION = "org.isoron.uhabits.ACTION_ADD_REPETITION"
        const val ACTION_DISMISS_REMINDER = "org.isoron.uhabits.ACTION_DISMISS_REMINDER"
        const val ACTION_REMOVE_REPETITION = "org.isoron.uhabits.ACTION_REMOVE_REPETITION"
        const val ACTION_TOGGLE_REPETITION = "org.isoron.uhabits.ACTION_TOGGLE_REPETITION"
        const val ACTION_SET_NUMERICAL_VALUE = "org.isoron.uhabits.ACTION_SET_NUMERICAL_VALUE"
        const val ACTION_UPDATE_WIDGETS_VALUE = "org.isoron.uhabits.ACTION_UPDATE_WIDGETS_VALUE"
        const val EXTRA_NUMERICAL_VALUE = "value"
        private const val TAG = "WidgetReceiver"
        var lastReceivedIntent: Intent? = null
            private set

        fun clearLastReceivedIntent() {
            lastReceivedIntent = null
        }
    }
}
