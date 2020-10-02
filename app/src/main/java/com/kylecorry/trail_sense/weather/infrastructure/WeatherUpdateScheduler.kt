package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherUpdateReceiver
import com.kylecorry.trail_sense.weather.infrastructure.services.WeatherUpdateService
import com.kylecorry.trailsensecore.infrastructure.system.AlarmUtils
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import java.time.Duration
import java.time.LocalDateTime

object WeatherUpdateScheduler {
    fun start(context: Context) {
        AlarmUtils.cancel(context, WeatherUpdateReceiver.pendingIntent(context))
        AlarmUtils.set(
            context,
            LocalDateTime.now(),
            WeatherUpdateReceiver.pendingIntent(context),
            exact = true,
            allowWhileIdle = true
        )
    }

    fun stop(context: Context) {
        NotificationUtils.cancel(context, WeatherNotificationService.WEATHER_NOTIFICATION_ID)
        AlarmUtils.cancel(context, WeatherUpdateReceiver.pendingIntent(context))
        context.stopService(WeatherUpdateService.intent(context))
    }
}