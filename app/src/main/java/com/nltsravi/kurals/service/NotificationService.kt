package com.nltsravi.kurals.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nltsravi.kurals.MainActivity
import com.nltsravi.kurals.R
import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.data.KuralRepository
import com.nltsravi.kurals.data.PreferencesAndCollectionStorage
import com.nltsravi.kurals.model.NotificationSettings
import com.nltsravi.kurals.util.TextUtils
import java.util.Calendar
import kotlin.random.Random

object NotificationService {

    const val CHANNEL_ID = "daily-thirukkural"
    const val CHANNEL_NAME = "தினசரி திருக்குறள் (Daily Thirukkural)"
    const val ACTION_DAILY_ALARM = "com.nltsravi.kurals.ACTION_DAILY_KURAL_ALARM"
    const val EXTRA_KURAL_NUMBER = "extra_kural_number"
    private const val ALARM_REQUEST_CODE = 1330

    /**
     * Format hour & minute into readable 12-hour string (e.g., "8:00 AM" / "காலை 8:00")
     * Matches `formatNotificationTime` in `notificationService.ts`.
     */
    fun formatNotificationTime(hour: Int, minute: Int, isTamil: Boolean = false): String {
        val periodEn = if (hour >= 12) "PM" else "AM"
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        val displayMin = minute.toString().padStart(2, '0')

        if (!isTamil) {
            return "$displayHour:$displayMin $periodEn"
        }

        val periodTa = when {
            hour in 4..11 -> "காலை"
            hour in 12..15 -> "மதியம்"
            hour in 16..19 -> "மாலை"
            else -> "இரவு"
        }
        return "$periodTa $displayHour:$displayMin"
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily Thirukkural wisdom notifications"
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyNotifications(context: Context, settings: NotificationSettings): Boolean {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyKuralAlarmReceiver::class.java).apply {
            action = ACTION_DAILY_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!settings.enabled) {
            alarmManager.cancel(pendingIntent)
            return false
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.hour)
            set(Calendar.MINUTE, settings.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            return true
        } catch (_: Exception) {
            return false
        }
    }

    fun postKuralNotification(context: Context, kuralNumber: Int? = null): Boolean {
        return try {
            ensureNotificationChannel(context)
            KuralRepository.initialize(context)
            val targetNum = kuralNumber ?: (Random.nextInt(AppConstants.TOTAL_KURALS) + 1)
            val kural = KuralRepository.getKuralByNumber(targetNum) ?: KuralRepository.getRandomKural()
            val (line1, line2) = TextUtils.formatKuralCouplet(kural.line1, kural.line2)

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_KURAL_NUMBER, kural.number)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                kural.number,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "📜 குறள் ${kural.number} (${kural.chapterNameTamil}) • Kural ${kural.number}"
            val shortBody = "$line1\n$line2"
            val fullBody = "$line1\n$line2\n\n💡 பொருள்: ${kural.mv}\n🌐 ${kural.translation}"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(shortBody)
                .setStyle(NotificationCompat.BigTextStyle().bigText(fullBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(kural.number, notification)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}

class DailyKuralAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val storage = PreferencesAndCollectionStorage(context)
        val settings = storage.loadNotificationSettings()
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            if (settings.enabled) {
                NotificationService.scheduleDailyNotifications(context, settings)
            }
            return
        }
        if (settings.enabled) {
            NotificationService.postKuralNotification(context)
        }
    }
}

