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
package org.isoron.uhabits.security

import android.app.Activity
import android.app.Application
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.activities.intro.IntroActivity
import org.isoron.uhabits.core.preferences.PrivacyLockState
import org.isoron.uhabits.notifications.SnoozeDelayPickerActivity

/**
 * Process-wide privacy lock. Unlock state is kept only in memory, so closing
 * the app from Recents always requires authentication on the next launch.
 */
object PrivacyLock {
    val state = PrivacyLockState()

    fun unlock() = state.unlock()

    fun lock() = state.lock()

    fun ignoreNextBackground() = state.ignoreNextBackground()

    fun isDeviceSecure(context: Context): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguard.isDeviceSecure
    }

    fun isEnabled(): Boolean {
        if (HabitsApplication.isTestMode()) return false
        return runCatching {
            HabitsApplication.component.preferences.isPrivacyLockEnabled
        }.getOrDefault(false)
    }

    fun lockWhenMinimized(): Boolean {
        return runCatching {
            HabitsApplication.component.preferences.isLockWhenMinimizedEnabled
        }.getOrDefault(true)
    }

    fun shouldBlock(activity: Activity): Boolean {
        if (activity is LockActivity) return false
        if (activity is IntroActivity) return false
        if (activity is SnoozeDelayPickerActivity) return false
        return state.requiresAuth(isEnabled())
    }

    fun applySecureFlag(activity: Activity) {
        if (!isEnabled()) return
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    fun register(application: Application) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                state.onAppBackgrounded(lockWhenMinimized())
            }
        })
        application.registerActivityLifecycleCallbacks(Callbacks())
    }

    private class Callbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            applySecureFlag(activity)
        }

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityResumed(activity: Activity) {
            if (shouldBlock(activity)) {
                activity.window.decorView.visibility = View.INVISIBLE
                activity.startActivity(
                    Intent(activity, LockActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    }
                )
            } else if (activity !is LockActivity) {
                activity.window.decorView.visibility = View.VISIBLE
            }
        }

        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}
