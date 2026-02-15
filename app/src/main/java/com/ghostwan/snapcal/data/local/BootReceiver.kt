package com.ghostwan.snapcal.data.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val manager = MealReminderManager(context)
        if (manager.enabled) {
            manager.scheduleAll()
        }
    }
}
