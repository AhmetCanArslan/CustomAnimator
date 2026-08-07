package com.arslan.customanimator.notify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import android.media.AudioManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.arslan.customanimator.R
import com.arslan.customanimator.notify.data.FlashPattern
import com.arslan.customanimator.notify.data.IgnoreManager
import com.arslan.customanimator.notify.data.LoggingManager
import com.arslan.customanimator.notify.data.LoggingPreferences
import com.arslan.customanimator.notify.data.MatchedRuleInfo
import com.arslan.customanimator.notify.data.RuleType
import com.arslan.customanimator.notify.data.NotificationGate
import com.arslan.customanimator.notify.data.RuleMatcher
import com.arslan.customanimator.notify.data.RulesManager
import com.arslan.customanimator.notify.data.ScreenFlashColor
import com.arslan.customanimator.notify.data.WidgetNotification
import com.arslan.customanimator.notify.data.WidgetNotificationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotifyListenerService : NotificationListenerService() {

    private lateinit var rulesManager: RulesManager
    private lateinit var ignoreManager: IgnoreManager
    private lateinit var flashManager: FlashManager
    private lateinit var screenWakeManager: ScreenWakeManager
    private lateinit var aodManager: AodManager
    private lateinit var screenFlashManager: ScreenFlashManager
    private lateinit var loggingManager: LoggingManager
    private lateinit var loggingPreferences: LoggingPreferences
    private var lastPurgeTime = 0L
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        rulesManager = RulesManager(this)
        ignoreManager = IgnoreManager(this)
        flashManager = FlashManager(this)
        screenWakeManager = ScreenWakeManager(this)
        aodManager = AodManager(this)
        screenFlashManager = ScreenFlashManager(this)
        loggingManager = LoggingManager.getInstance(this)
        loggingPreferences = LoggingPreferences(this)
        startPersistentNotification()
    }

    override fun onDestroy() {
        ioScope.cancel()
        flashManager.stop()
        screenWakeManager.stop()
        aodManager.stop()
        screenFlashManager.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val bodyRaw = bigText.ifBlank { text }

        val isOwnServiceNotification = packageName == this.packageName &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            sbn.notification.channelId == PERSISTENT_CHANNEL_ID

        val shouldProcess = NotificationGate.shouldProcess(
            isServiceEnabled = isPrimeNotifyServiceEnabled(this),
            isOwnServiceNotification = isOwnServiceNotification,
        )
        if (!shouldProcess) {
            super.onNotificationPosted(sbn)
            return
        }

        if (ignoreManager.isIgnored(packageName, title, bodyRaw)) {
            super.onNotificationPosted(sbn)
            return
        }

        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val ringerMode = am.ringerMode
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val interruptionFilter = nm.currentInterruptionFilter

        val isVibration = ringerMode == AudioManager.RINGER_MODE_VIBRATE
        val isSilent = ringerMode == AudioManager.RINGER_MODE_SILENT
        val isDND = interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        val allLoggedRules = mutableListOf<MatchedRuleInfo>()

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        for (rule in rulesManager.getRules().filter { it.isEnabled }) {
            if (!RuleMatcher.matches(rule, packageName, title, text, bigText)) continue

            val shouldExecute = RuleMatcher.shouldExecute(
                rule = rule,
                isThrottled = rulesManager.shouldThrottleRule(rule.id),
                isVibration = isVibration,
                isSilent = isSilent,
                isDND = isDND,
            )

            if (shouldExecute) rulesManager.updateRuleExecutionTime(rule.id)

            val ruleLabel = buildRuleLabel(rule.appNames, rule.keywords, rule.titleKeywords, rule.bodyKeywords)

            for (action in rule.actions) {
                if (shouldExecute) {
                    when (action.type) {
                        RuleType.FLASH -> {
                            val customPattern = action.customPatternId?.let { id ->
                                rulesManager.getCustomPatterns().find { it.id == id }
                            }
                            if (customPattern != null) {
                                flashManager.executeCustomPattern(customPattern.intervals)
                            } else {
                                flashManager.executePattern(action.flashPattern ?: FlashPattern.HEARTBEAT)
                            }
                        }
                        RuleType.WAKE_UP -> {
                            screenWakeManager.wakeScreen(
                                durationSeconds = action.screenDurationSeconds ?: 10,
                                pocketModeEnabled = action.pocketModeEnabled ?: true,
                            )
                        }
                        RuleType.AOD -> {
                            aodManager.triggerAod(durationSeconds = action.aodDurationSeconds ?: 10)
                        }
                        RuleType.FLASH_SCREEN -> {
                            val color = ScreenFlashColor.fromName(action.screenFlashColor)
                            screenFlashManager.triggerFlash(
                                color = color,
                                durationSeconds = action.screenFlashDurationSeconds ?: 5,
                            )
                        }
                        RuleType.WIDGET -> {
                            val item = WidgetNotification(
                                packageName = packageName,
                                appName = appName,
                                title = title,
                                body = if (action.widgetKeepBody != false) bodyRaw else "",
                                ruleId = rule.id,
                            )
                            ioScope.launch {
                                WidgetNotificationStore.add(applicationContext, item)
                                WidgetNotificationStore.notifyWidgets(applicationContext)
                            }
                        }
                    }
                }
                allLoggedRules.add(
                    MatchedRuleInfo(
                        ruleId = rule.id,
                        ruleName = ruleLabel,
                        ruleType = action.type,
                        wasExecuted = shouldExecute,
                    )
                )
            }
        }

        val now = System.currentTimeMillis()
        val shouldPurge = loggingPreferences.autoDeleteDays > 0 &&
            now - lastPurgeTime > 24 * 60 * 60 * 1000L
        if (shouldPurge) lastPurgeTime = now
        val retentionDays = loggingPreferences.autoDeleteDays
        ioScope.launch {
            if (shouldPurge) loggingManager.purgeOlderThan(retentionDays)
            loggingManager.logNotification(
                packageName = packageName,
                appName = appName,
                title = title,
                body = bodyRaw,
                matchedRules = allLoggedRules,
            )
        }

        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn != null && isPrimeNotifyServiceEnabled(this)) {
            val matched = rulesManager.getRules().filter { it.isEnabled }.firstOrNull { rule ->
                rule.packageNames.contains(sbn.packageName) &&
                    rule.actions.any { it.type == RuleType.AOD && it.aodDurationSeconds == -1 }
            }
            if (matched != null) aodManager.stopAodForReason(-1)
        }
    }

    private fun buildRuleLabel(appNames: List<String>, keywords: List<String>, titleKeywords: List<String>, bodyKeywords: List<String>): String {
        val appsLabel = appNames.joinToString(", ").ifBlank { "?" }
        val parts = mutableListOf<String>()
        if (keywords.isNotEmpty()) parts.add(keywords.joinToString(", "))
        if (titleKeywords.isNotEmpty()) parts.add("title: ${titleKeywords.joinToString("|")}")
        if (bodyKeywords.isNotEmpty()) parts.add("body: ${bodyKeywords.joinToString("|")}")
        return if (parts.isEmpty()) appsLabel else "$appsLabel \u2013 ${parts.joinToString("; ")}"
    }

    private fun startPersistentNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannelGroup(
                NotificationChannelGroup(
                    PERSISTENT_CHANNEL_GROUP_ID,
                    getString(R.string.pn_service_channel_group)
                )
            )
            val channel = NotificationChannel(
                PERSISTENT_CHANNEL_ID,
                getString(R.string.pn_service_title),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.pn_notification_persistent_text)
                group = PERSISTENT_CHANNEL_GROUP_ID
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            nm.createNotificationChannel(channel)
        }

        val settingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, PERSISTENT_CHANNEL_ID)
            }
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            this,
            0,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, PERSISTENT_CHANNEL_ID)
            .setContentTitle(getString(R.string.pn_service_title))
            .setContentText(getString(R.string.pn_notification_persistent_text))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        getString(R.string.pn_notification_persistent_text) + "\n" +
                            getString(R.string.pn_notification_persistent_settings_hint)
                    )
            )
            .setSmallIcon(R.drawable.ic_notification_prime)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setShowWhen(false)
            .setSilent(true)
            .setContentIntent(settingsPendingIntent)
            .build()

        startForeground(PERSISTENT_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val PERSISTENT_CHANNEL_ID = "notify_listener_channel"
        private const val PERSISTENT_CHANNEL_GROUP_ID = "notify_listener_service_group"
        private const val PERSISTENT_NOTIFICATION_ID = 4501
    }
}
