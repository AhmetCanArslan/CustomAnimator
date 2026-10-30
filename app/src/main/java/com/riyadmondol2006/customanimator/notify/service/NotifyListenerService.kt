package com.riyadmondol2006.customanimator.notify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.riyadmondol2006.customanimator.R
import com.riyadmondol2006.customanimator.notify.data.FlashPattern
import com.riyadmondol2006.customanimator.notify.data.IgnoreManager
import com.riyadmondol2006.customanimator.notify.data.IgnoreMatcher
import com.riyadmondol2006.customanimator.notify.data.LoggingManager
import com.riyadmondol2006.customanimator.notify.data.LoggingPreferences
import com.riyadmondol2006.customanimator.notify.data.MatchedRuleInfo
import com.riyadmondol2006.customanimator.notify.data.RuleAction
import com.riyadmondol2006.customanimator.notify.data.RuleType
import com.riyadmondol2006.customanimator.notify.data.NotificationGate
import com.riyadmondol2006.customanimator.notify.data.RuleMatcher
import com.riyadmondol2006.customanimator.notify.data.RulesManager
import com.riyadmondol2006.customanimator.notify.data.ScreenFlashColor
import com.riyadmondol2006.customanimator.notify.data.WidgetNotification
import com.riyadmondol2006.customanimator.notify.data.WidgetNotificationStore
import com.riyadmondol2006.customanimator.notify.widget.NotificationWidgetProvider
import com.riyadmondol2006.customanimator.utils.OtpCopierManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class NotifyListenerService : NotificationListenerService() {

    private class PostedNotification(
        val packageName: String,
        val title: String,
        val text: String,
        val bigText: String,
    ) {
        val body: String = bigText.ifBlank { text }
    }

    private class RingerState(val isVibration: Boolean, val isSilent: Boolean, val isDND: Boolean)

    private lateinit var rulesManager: RulesManager
    private lateinit var ignoreManager: IgnoreManager
    private lateinit var loggingManager: LoggingManager
    private lateinit var loggingPreferences: LoggingPreferences
    private val flashManager = lazy { FlashManager(this) }
    private val screenWakeManager = lazy { ScreenWakeManager(this) }
    private lateinit var aodManager: AodManager
    private val screenFlashManager = lazy { ScreenFlashManager(this) }
    private val appLabels = ConcurrentHashMap<String, String>()
    private var lastPurgeTime = 0L
    @Volatile
    private var persistentAodKey: String? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        rulesManager = RulesManager(this)
        ignoreManager = IgnoreManager(this)
        loggingManager = LoggingManager.getInstance(this)
        loggingPreferences = LoggingPreferences(this)
        aodManager = AodManager(this)
        aodManager.recoverStaleState()
        startPersistentNotification()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        appLabels.clear()
        NotificationWidgetProvider.updateAll(this)
        WidgetNotificationStore.notifyWidgets(this)
    }

    override fun onDestroy() {
        ioScope.cancel()
        loggingManager.flushPending()
        if (flashManager.isInitialized()) flashManager.value.stop()
        if (screenWakeManager.isInitialized()) screenWakeManager.value.stop()
        aodManager.stop()
        if (screenFlashManager.isInitialized()) screenFlashManager.value.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { requestRebind(ComponentName(this, NotifyListenerService::class.java)) }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) handlePosted(sbn)
        super.onNotificationPosted(sbn)
    }

    private fun handlePosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val posted = PostedNotification(
            packageName = sbn.packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "",
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "",
        )
        val isOwnPackage = posted.packageName == packageName

        if (!isOwnPackage && OtpCopierManager.isEnabled(this)) {
            OtpCopierManager.onNotification(this, sbn.key, "${posted.title}\n${posted.body}")
        }

        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val shouldProcess = NotificationGate.shouldProcess(
            isServiceEnabled = isPrimeNotifyServiceEnabled(this),
            isOwnServiceNotification = isOwnPackage && isPersistentChannel(sbn),
        )
        if (!shouldProcess) return

        val ignoredBy = ignoreManager.rulesFor(posted.packageName).filter {
            IgnoreMatcher.matches(it, posted.packageName, posted.title, posted.body)
        }
        if (ignoredBy.isNotEmpty()) {
            if (ignoredBy.any { it.blocksNotification }) cancelNotification(sbn.key)
            return
        }

        logNotification(posted, runRules(sbn.key, posted))
    }

    private fun isPersistentChannel(sbn: StatusBarNotification): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && sbn.notification.channelId == PERSISTENT_CHANNEL_ID

    private fun runRules(key: String, posted: PostedNotification): List<MatchedRuleInfo> {
        val candidates = rulesManager.enabledRulesFor(posted.packageName)
        if (candidates.isEmpty()) return emptyList()

        val content = RuleMatcher.Content(posted.title, posted.text, posted.bigText)
        val ringer by lazy(LazyThreadSafetyMode.NONE) { readRingerState() }
        val matched = ArrayList<MatchedRuleInfo>()

        for (rule in candidates) {
            if (!RuleMatcher.matchesKeywords(rule, content)) continue

            val shouldExecute = RuleMatcher.shouldExecute(
                rule = rule,
                isThrottled = rulesManager.shouldThrottleRule(rule.id),
                isVibration = ringer.isVibration,
                isSilent = ringer.isSilent,
                isDND = ringer.isDND,
            )

            if (shouldExecute) rulesManager.updateRuleExecutionTime(rule.id)

            val ruleLabel = buildRuleLabel(rule.appNames, rule.keywords, rule.titleKeywords, rule.bodyKeywords)

            for (action in rule.actions) {
                if (shouldExecute) execute(action, rule.id, key, posted)
                matched.add(
                    MatchedRuleInfo(
                        ruleId = rule.id,
                        ruleName = ruleLabel,
                        ruleType = action.type,
                        wasExecuted = shouldExecute,
                    )
                )
            }
        }
        return matched
    }

    private fun readRingerState(): RingerState {
        val ringerMode = (getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode
        val interruptionFilter =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).currentInterruptionFilter
        return RingerState(
            isVibration = ringerMode == AudioManager.RINGER_MODE_VIBRATE,
            isSilent = ringerMode == AudioManager.RINGER_MODE_SILENT,
            isDND = interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL,
        )
    }

    private fun execute(action: RuleAction, ruleId: String, key: String, posted: PostedNotification) {
        when (action.type) {
            RuleType.FLASH -> flash(action)
            RuleType.WAKE_UP -> screenWakeManager.value.wakeScreen(
                durationSeconds = action.screenDurationSeconds ?: 10,
                pocketModeEnabled = action.pocketModeEnabled ?: true,
            )
            RuleType.AOD -> {
                val duration = action.aodDurationSeconds ?: 10
                persistentAodKey = if (duration == -1) key else null
                aodManager.triggerAod(durationSeconds = duration)
            }
            RuleType.FLASH_SCREEN -> screenFlashManager.value.triggerFlash(
                color = ScreenFlashColor.fromName(action.screenFlashColor),
                durationSeconds = action.screenFlashDurationSeconds ?: 5,
            )
            RuleType.WIDGET -> addToWidget(action, ruleId, posted)
        }
    }

    private fun flash(action: RuleAction) {
        val customPattern = action.customPatternId?.let { id ->
            rulesManager.getCustomPatterns().find { it.id == id }
        }
        if (customPattern != null) {
            flashManager.value.executeCustomPattern(customPattern.intervals)
        } else {
            flashManager.value.executePattern(action.flashPattern ?: FlashPattern.HEARTBEAT)
        }
    }

    private fun addToWidget(action: RuleAction, ruleId: String, posted: PostedNotification) {
        val timestamp = System.currentTimeMillis()
        ioScope.launch {
            val item = WidgetNotification(
                timestamp = timestamp,
                packageName = posted.packageName,
                appName = appLabel(posted.packageName),
                title = posted.title,
                body = if (action.widgetKeepBody != false) posted.body else "",
                ruleId = ruleId,
            )
            WidgetNotificationStore.add(applicationContext, item)
            WidgetNotificationStore.notifyWidgets(applicationContext)
        }
    }

    private fun logNotification(posted: PostedNotification, matchedRules: List<MatchedRuleInfo>) {
        val now = System.currentTimeMillis()
        val retentionDays = loggingPreferences.autoDeleteDays
        val shouldPurge = retentionDays > 0 && now - lastPurgeTime > PURGE_INTERVAL_MS
        if (shouldPurge) lastPurgeTime = now
        ioScope.launch {
            if (shouldPurge) loggingManager.purgeOlderThan(retentionDays)
            loggingManager.logNotification(
                packageName = posted.packageName,
                appName = appLabel(posted.packageName),
                title = posted.title,
                body = posted.body,
                matchedRules = matchedRules,
            )
        }
    }

    private fun appLabel(packageName: String): String {
        appLabels[packageName]?.let { return it }
        val label = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            return packageName
        }
        appLabels[packageName] = label
        return label
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn != null && sbn.key == persistentAodKey) {
            persistentAodKey = null
            aodManager.stopAodForReason(-1)
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

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, PERSISTENT_NOTIFICATION_ID, notification, type)
    }

    companion object {
        private const val PERSISTENT_CHANNEL_ID = "notify_listener_channel"
        private const val PERSISTENT_CHANNEL_GROUP_ID = "notify_listener_service_group"
        private const val PERSISTENT_NOTIFICATION_ID = 4501
        private const val PURGE_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }
}
