package com.kylecorry.trail_sense.weather.infrastructure.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.weather.infrastructure.services.WeatherUpdateService

class WeatherUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        WeatherUpdateService.start(context)
    }

    companion object {

        const val PI_ID = 283095423

        fun intent(context: Context): Intent {
            return Intent(context, WeatherUpdateReceiver::class.java)
        }

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context, PI_ID, intent(context), PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

}