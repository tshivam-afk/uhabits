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
package org.isoron.uhabits.core.preferences

/**
 * In-memory unlock state for the privacy lock.
 *
 * The flag lives only in the current process. Swiping the app from Recents
 * kills the process, so the next launch always starts locked.
 */
class PrivacyLockState {
    @Volatile
    var isUnlocked: Boolean = false
        private set

    @Volatile
    private var ignoreNextBackground: Boolean = false

    fun unlock() {
        isUnlocked = true
    }

    fun lock() {
        isUnlocked = false
    }

    /**
     * Skip the next background event. Used when the app launches an external
     * picker (SAF, ringtone) so that "lock when minimized" does not fire.
     */
    fun ignoreNextBackground() {
        ignoreNextBackground = true
    }

    /**
     * Called when the process goes to the background (no started activities).
     *
     * If [lockWhenMinimized] is true, lock immediately. If false, stay
     * unlocked until the process is killed from Recents.
     */
    fun onAppBackgrounded(lockWhenMinimized: Boolean) {
        if (ignoreNextBackground) {
            ignoreNextBackground = false
            return
        }
        if (lockWhenMinimized) {
            lock()
        }
    }

    fun requiresAuth(privacyLockEnabled: Boolean): Boolean {
        return privacyLockEnabled && !isUnlocked
    }
}
