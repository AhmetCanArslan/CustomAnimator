package com.arslan.customanimator.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process
import android.provider.Telephony
import android.telecom.TelecomManager

object DebloatCatalog {

    enum class Risk { RECOMMENDED, ADVANCED, EXPERT, UNSAFE }

    enum class Group {
        GOOGLE, ASSISTANT, OEM, CARRIER, ADS, DIAGNOSTICS, PRINTING, AR,
        ACCESSIBILITY, BACKUP, PARTNER, THEMES, BROWSER, MEDIA, PAYMENT, SYSTEM
    }

    data class Entry(val risk: Risk, val group: Group)

    private val CRITICAL_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.shell",
        "com.android.keychain",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.providers.settings",
        "com.android.providers.telephony",
        "com.android.providers.contacts",
        "com.android.providers.calendar",
        "com.android.providers.downloads",
        "com.android.providers.media",
        "com.android.providers.media.module",
        "com.google.android.providers.media.module",
        "com.android.externalstorage",
        "com.android.mtp",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.android.se",
        "com.android.inputdevices",
        "com.android.location.fused",
        "com.android.wifi.resources",
        "com.android.networkstack",
        "com.android.networkstack.tethering",
        "com.android.networkstack.inprocess",
        "com.google.android.networkstack",
        "com.google.android.networkstack.tethering",
        "com.android.dynsystem",
        "com.android.localtransport",
        "com.android.sharedstoragebackup",
        "com.android.wallpaperbackup",
        "com.android.ons",
        "com.android.ims",
        "com.android.ims.rcsservice",
        "com.android.telephony.resources",
        "com.android.intentresolver",
        "com.android.credentialmanager",
        "com.android.devicelockcontroller",
        "com.android.adservices.api",
        "com.google.android.adservices.api",
        "com.android.role.notes.enabled",
        "com.android.wifi.dialog",
        "com.samsung.android.providers.media",
        "com.samsung.android.providers.contacts",
        "com.sec.android.provider.badge",
        "com.sec.android.app.launcher.provider",
        "com.miui.core",
        "com.miui.rom",
        "com.xiaomi.xmsf",
        "com.miui.securitycenter",
        "com.android.incallui",
        "com.samsung.android.incallui",
        "com.samsung.android.app.telephonyui",
        "com.sec.imsservice",
        "com.sec.epdg",
        "com.sec.sve"
    )

    private val CRITICAL_SHARED_UIDS = setOf(
        "android.uid.system",
        "android.uid.systemui",
        "android.uid.phone",
        "android.uid.bluetooth",
        "android.uid.nfc",
        "android.uid.se",
        "android.uid.shell",
        "android.uid.networkstack",
        "android.uid.telephony",
        "android.media",
        "com.android.providers.telephony",
        "com.samsung.android.providers.telephony"
    )

    private val GOOGLE_ENTRIES = mapOf(
        "com.google.android.gms" to Entry(Risk.UNSAFE, Group.GOOGLE),
        "com.google.android.gsf" to Entry(Risk.UNSAFE, Group.GOOGLE),
        "com.android.vending" to Entry(Risk.UNSAFE, Group.GOOGLE),
        "com.google.android.webview" to Entry(Risk.UNSAFE, Group.SYSTEM),
        "com.android.webview" to Entry(Risk.UNSAFE, Group.SYSTEM),
        "com.android.documentsui" to Entry(Risk.UNSAFE, Group.SYSTEM),
        "com.google.android.documentsui" to Entry(Risk.UNSAFE, Group.SYSTEM),
        "com.google.android.setupwizard" to Entry(Risk.EXPERT, Group.SYSTEM),
        "com.google.android.configupdater" to Entry(Risk.EXPERT, Group.SYSTEM),
        "com.google.android.as" to Entry(Risk.EXPERT, Group.SYSTEM),
        "com.google.android.as.oss" to Entry(Risk.EXPERT, Group.SYSTEM),
        "com.google.android.ext.services" to Entry(Risk.EXPERT, Group.SYSTEM),
        "com.android.statementservice" to Entry(Risk.EXPERT, Group.SYSTEM),
        "com.google.android.googlequicksearchbox" to Entry(Risk.ADVANCED, Group.ASSISTANT),
        "com.google.android.apps.googleassistant" to Entry(Risk.ADVANCED, Group.ASSISTANT),
        "com.google.android.apps.turbo" to Entry(Risk.ADVANCED, Group.SYSTEM),
        "com.google.android.dialer" to Entry(Risk.ADVANCED, Group.GOOGLE),
        "com.google.android.contacts" to Entry(Risk.ADVANCED, Group.GOOGLE),
        "com.google.android.apps.messaging" to Entry(Risk.ADVANCED, Group.GOOGLE),
        "com.google.android.deskclock" to Entry(Risk.ADVANCED, Group.GOOGLE),
        "com.google.android.apps.photos" to Entry(Risk.ADVANCED, Group.MEDIA),
        "com.google.android.apps.maps" to Entry(Risk.ADVANCED, Group.GOOGLE),
        "com.google.android.gm" to Entry(Risk.ADVANCED, Group.GOOGLE),
        "com.google.android.youtube" to Entry(Risk.ADVANCED, Group.MEDIA),
        "com.android.chrome" to Entry(Risk.ADVANCED, Group.BROWSER),
        "com.google.android.GoogleCamera" to Entry(Risk.ADVANCED, Group.MEDIA),
        "com.google.android.apps.walletnfcrel" to Entry(Risk.ADVANCED, Group.PAYMENT),
        "com.google.android.apps.wallpaper" to Entry(Risk.ADVANCED, Group.THEMES),
        "com.google.android.apps.adm" to Entry(Risk.ADVANCED, Group.SYSTEM),
        "com.google.android.backuptransport" to Entry(Risk.ADVANCED, Group.BACKUP),
        "com.google.android.syncadapters.contacts" to Entry(Risk.ADVANCED, Group.BACKUP),
        "com.google.android.syncadapters.calendar" to Entry(Risk.ADVANCED, Group.BACKUP),
        "com.google.android.partnersetup" to Entry(Risk.ADVANCED, Group.DIAGNOSTICS),
        "com.google.android.ims" to Entry(Risk.ADVANCED, Group.CARRIER),
        "com.google.android.marvin.talkback" to Entry(Risk.ADVANCED, Group.ACCESSIBILITY),
        "com.android.cellbroadcastreceiver" to Entry(Risk.EXPERT, Group.CARRIER),
        "com.google.android.cellbroadcastreceiver" to Entry(Risk.EXPERT, Group.CARRIER),
        "com.android.emergency" to Entry(Risk.EXPERT, Group.SYSTEM),
        "com.google.android.calendar" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.keep" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.apps.docs" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.apps.docs.editors.docs" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.apps.docs.editors.sheets" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.apps.docs.editors.slides" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.apps.magazines" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.apps.tachyon" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.videos" to Entry(Risk.RECOMMENDED, Group.MEDIA),
        "com.google.android.music" to Entry(Risk.RECOMMENDED, Group.MEDIA),
        "com.google.android.apps.youtube.music" to Entry(Risk.RECOMMENDED, Group.MEDIA),
        "com.google.android.apps.books" to Entry(Risk.RECOMMENDED, Group.MEDIA),
        "com.google.android.apps.podcasts" to Entry(Risk.RECOMMENDED, Group.MEDIA),
        "com.google.android.apps.recorder" to Entry(Risk.RECOMMENDED, Group.MEDIA),
        "com.google.android.apps.nbu.files" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.apps.subscriptions.red" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.apps.wellbeing" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.apps.safetyhub" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.apps.tips" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.apps.customization.pixel" to Entry(Risk.RECOMMENDED, Group.THEMES),
        "com.google.android.apps.restore" to Entry(Risk.RECOMMENDED, Group.BACKUP),
        "com.google.android.apps.pixelmigrate" to Entry(Risk.RECOMMENDED, Group.BACKUP),
        "com.google.android.projection.gearhead" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.apps.wearables.maestro.companion" to Entry(Risk.RECOMMENDED, Group.GOOGLE),
        "com.google.android.feedback" to Entry(Risk.RECOMMENDED, Group.DIAGNOSTICS),
        "com.google.mainline.telemetry" to Entry(Risk.RECOMMENDED, Group.DIAGNOSTICS),
        "com.google.android.onetimeinitializer" to Entry(Risk.RECOMMENDED, Group.SYSTEM),
        "com.google.android.printservice.recommendation" to Entry(Risk.RECOMMENDED, Group.PRINTING),
        "com.google.ar.core" to Entry(Risk.RECOMMENDED, Group.AR),
        "com.google.ar.lens" to Entry(Risk.RECOMMENDED, Group.AR),
        "com.google.android.apps.carrier.carrierwifi" to Entry(Risk.RECOMMENDED, Group.CARRIER),
        "com.google.audio.hearing.visualization.accessibility.scribe" to Entry(Risk.RECOMMENDED, Group.ACCESSIBILITY),
        "com.google.android.apps.accessibility.voiceaccess" to Entry(Risk.RECOMMENDED, Group.ACCESSIBILITY),
        "com.google.android.hotwordenrollment.okgoogle" to Entry(Risk.RECOMMENDED, Group.ASSISTANT),
        "com.google.android.hotwordenrollment.xgoogle" to Entry(Risk.RECOMMENDED, Group.ASSISTANT)
    )

    private val AOSP_ENTRIES = mapOf(
        "com.android.bips" to Entry(Risk.RECOMMENDED, Group.PRINTING),
        "com.android.printspooler" to Entry(Risk.RECOMMENDED, Group.PRINTING),
        "com.android.wallpaper.livepicker" to Entry(Risk.RECOMMENDED, Group.THEMES),
        "com.android.wallpapercropper" to Entry(Risk.RECOMMENDED, Group.THEMES),
        "com.android.dreams.basic" to Entry(Risk.RECOMMENDED, Group.THEMES),
        "com.android.dreams.phototable" to Entry(Risk.RECOMMENDED, Group.THEMES),
        "com.android.bookmarkprovider" to Entry(Risk.RECOMMENDED, Group.BROWSER),
        "com.android.traceur" to Entry(Risk.RECOMMENDED, Group.DIAGNOSTICS),
        "com.android.egg" to Entry(Risk.RECOMMENDED, Group.SYSTEM),
        "com.android.musicfx" to Entry(Risk.RECOMMENDED, Group.MEDIA),
        "com.android.soundrecorder" to Entry(Risk.RECOMMENDED, Group.MEDIA),
        "com.android.stk" to Entry(Risk.RECOMMENDED, Group.CARRIER),
        "com.android.stk2" to Entry(Risk.RECOMMENDED, Group.CARRIER),
        "com.android.simappdialog" to Entry(Risk.RECOMMENDED, Group.CARRIER),
        "com.android.smspush" to Entry(Risk.RECOMMENDED, Group.CARRIER),
        "com.android.cts.ctsshim" to Entry(Risk.RECOMMENDED, Group.SYSTEM),
        "com.android.cts.priv.ctsshim" to Entry(Risk.RECOMMENDED, Group.SYSTEM),
        "com.android.bluetoothmidiservice" to Entry(Risk.RECOMMENDED, Group.SYSTEM),
        "com.android.apps.tag" to Entry(Risk.RECOMMENDED, Group.SYSTEM),
        "com.android.retaildemo" to Entry(Risk.RECOMMENDED, Group.SYSTEM),
        "com.android.protips" to Entry(Risk.RECOMMENDED, Group.SYSTEM),
        "com.android.calllogbackup" to Entry(Risk.ADVANCED, Group.BACKUP),
        "com.android.managedprovisioning" to Entry(Risk.ADVANCED, Group.SYSTEM),
        "com.android.companiondevicemanager" to Entry(Risk.ADVANCED, Group.SYSTEM),
        "com.android.htmlviewer" to Entry(Risk.ADVANCED, Group.BROWSER),
        "com.android.settings.intelligence" to Entry(Risk.ADVANCED, Group.SYSTEM),
        "com.android.carrierdefaultapp" to Entry(Risk.ADVANCED, Group.CARRIER),
        "com.android.carrierconfig" to Entry(Risk.EXPERT, Group.CARRIER),
        "com.android.hotspot2.osulogin" to Entry(Risk.EXPERT, Group.CARRIER),
        "com.android.wifi.dialog" to Entry(Risk.EXPERT, Group.SYSTEM)
    )

    private val SAMSUNG_ENTRIES = mapOf(
        "com.samsung.android.bixby.agent" to Entry(Risk.RECOMMENDED, Group.ASSISTANT),
        "com.samsung.android.bixby.wakeup" to Entry(Risk.RECOMMENDED, Group.ASSISTANT),
        "com.samsung.android.bixbyvision.framework" to Entry(Risk.RECOMMENDED, Group.ASSISTANT),
        "com.samsung.android.visionintelligence" to Entry(Risk.RECOMMENDED, Group.ASSISTANT),
        "com.samsung.android.app.spage" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.arzone" to Entry(Risk.RECOMMENDED, Group.AR),
        "com.samsung.android.aremoji" to Entry(Risk.RECOMMENDED, Group.AR),
        "com.samsung.android.livestickers" to Entry(Risk.RECOMMENDED, Group.AR),
        "com.samsung.android.app.tips" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.kidsinstaller" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.app.kidshome" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.smartswitchassistant" to Entry(Risk.RECOMMENDED, Group.BACKUP),
        "com.sec.android.easyMover" to Entry(Risk.RECOMMENDED, Group.BACKUP),
        "com.sec.android.easyMover.Agent" to Entry(Risk.RECOMMENDED, Group.BACKUP),
        "com.samsung.android.app.watchmanager" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.voc" to Entry(Risk.RECOMMENDED, Group.DIAGNOSTICS),
        "com.samsung.android.rubin.app" to Entry(Risk.RECOMMENDED, Group.ADS),
        "com.samsung.android.dqagent" to Entry(Risk.RECOMMENDED, Group.DIAGNOSTICS),
        "com.sec.android.diagmonagent" to Entry(Risk.RECOMMENDED, Group.DIAGNOSTICS),
        "com.samsung.android.themestore" to Entry(Risk.RECOMMENDED, Group.THEMES),
        "com.samsung.android.forest" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.mdx" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.mdx.kit" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.sec.android.app.chromecustomizations" to Entry(Risk.RECOMMENDED, Group.BROWSER),
        "com.samsung.android.beaconmanager" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.app.social" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.svoiceime" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.game.gamehome" to Entry(Risk.ADVANCED, Group.OEM),
        "com.samsung.android.game.gametools" to Entry(Risk.ADVANCED, Group.OEM),
        "com.samsung.android.game.gos" to Entry(Risk.ADVANCED, Group.OEM),
        "com.samsung.android.scloud" to Entry(Risk.ADVANCED, Group.BACKUP),
        "com.sec.android.app.samsungapps" to Entry(Risk.ADVANCED, Group.OEM),
        "com.samsung.android.messaging" to Entry(Risk.ADVANCED, Group.OEM),
        "com.samsung.android.dialer" to Entry(Risk.ADVANCED, Group.OEM),
        "com.samsung.android.spay" to Entry(Risk.ADVANCED, Group.PAYMENT),
        "com.samsung.android.spayfw" to Entry(Risk.ADVANCED, Group.PAYMENT),
        "com.samsung.android.samsungpass" to Entry(Risk.ADVANCED, Group.PAYMENT),
        "com.samsung.android.samsungpassautofill" to Entry(Risk.ADVANCED, Group.PAYMENT),
        "com.samsung.android.honeyboard" to Entry(Risk.EXPERT, Group.OEM),
        "com.samsung.android.smartsuggestions" to Entry(Risk.EXPERT, Group.OEM),
        "com.samsung.android.mapsagent" to Entry(Risk.EXPERT, Group.OEM)
    )

    private val XIAOMI_ENTRIES = mapOf(
        "com.miui.analytics" to Entry(Risk.RECOMMENDED, Group.ADS),
        "com.miui.msa.global" to Entry(Risk.RECOMMENDED, Group.ADS),
        "com.miui.systemAdSolution" to Entry(Risk.RECOMMENDED, Group.ADS),
        "com.xiaomi.mipicks" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.xiaomi.glgm" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.miui.player" to Entry(Risk.RECOMMENDED, Group.MEDIA),
        "com.miui.video" to Entry(Risk.RECOMMENDED, Group.MEDIA),
        "com.miui.videoplayer" to Entry(Risk.RECOMMENDED, Group.MEDIA),
        "com.mi.globalbrowser" to Entry(Risk.RECOMMENDED, Group.BROWSER),
        "com.miui.bugreport" to Entry(Risk.RECOMMENDED, Group.DIAGNOSTICS),
        "com.miui.yellowpage" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.miui.hybrid" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.miui.hybrid.accessory" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.xiaomi.payment" to Entry(Risk.RECOMMENDED, Group.PAYMENT),
        "com.mipay.wallet" to Entry(Risk.RECOMMENDED, Group.PAYMENT),
        "com.miui.daemon" to Entry(Risk.ADVANCED, Group.DIAGNOSTICS),
        "com.miui.cloudservice" to Entry(Risk.ADVANCED, Group.BACKUP),
        "com.miui.cloudbackup" to Entry(Risk.ADVANCED, Group.BACKUP),
        "com.miui.backup" to Entry(Risk.ADVANCED, Group.BACKUP)
    )

    private val PARTNER_ENTRIES = mapOf(
        "com.facebook.katana" to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.facebook.appmanager" to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.facebook.system" to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.facebook.services" to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.netflix.partner.activation" to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.netflix.mediaclient" to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.linkedin.android" to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.amazon.appmanager" to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.amazon.mShop.android.shopping" to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.microsoft.appmanager" to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.microsoft.skydrive" to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.microsoft.office.outlook" to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.spotify.music" to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.booking" to Entry(Risk.RECOMMENDED, Group.PARTNER)
    )

    private val SERVICE_ENTRIES = mapOf(
        "com.google.android.tts" to Entry(Risk.ADVANCED, Group.ACCESSIBILITY),
        "com.google.android.gms.location.history" to Entry(Risk.RECOMMENDED, Group.DIAGNOSTICS),
        "com.google.android.apps.setupwizard.searchselector" to Entry(Risk.RECOMMENDED, Group.SYSTEM),
        "com.google.android.apps.work.oobconfig" to Entry(Risk.RECOMMENDED, Group.SYSTEM),
        "com.google.android.apps.wallpaper.nexus" to Entry(Risk.RECOMMENDED, Group.THEMES),
        "com.google.android.modulemetadata" to Entry(Risk.EXPERT, Group.SYSTEM),
        "com.google.android.hotwordenrollment.okgoogle.tgl" to Entry(Risk.RECOMMENDED, Group.ASSISTANT),
        "com.samsung.android.bixby.service" to Entry(Risk.RECOMMENDED, Group.ASSISTANT),
        "com.samsung.android.bixby.es.globalaction" to Entry(Risk.RECOMMENDED, Group.ASSISTANT),
        "com.samsung.android.app.settings.bixby" to Entry(Risk.RECOMMENDED, Group.ASSISTANT),
        "com.samsung.SMT" to Entry(Risk.ADVANCED, Group.ACCESSIBILITY),
        "com.samsung.android.app.routines" to Entry(Risk.ADVANCED, Group.OEM),
        "com.samsung.android.smartmirroring" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.app.sharelive" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.aircommandmanager" to Entry(Risk.ADVANCED, Group.OEM),
        "com.samsung.android.service.peoplestripe" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.service.livedrawing" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.app.appsedge" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.app.edgetouch" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.easysetup" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.net.wifi.wifiguider" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.bbc.bbcagent" to Entry(Risk.RECOMMENDED, Group.DIAGNOSTICS),
        "com.sec.android.sdhms" to Entry(Risk.RECOMMENDED, Group.DIAGNOSTICS),
        "com.sec.android.app.billing" to Entry(Risk.RECOMMENDED, Group.PAYMENT),
        "com.samsung.android.app.reminder" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.oneconnect" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.stickercenter" to Entry(Risk.RECOMMENDED, Group.THEMES),
        "com.samsung.android.themecenter" to Entry(Risk.RECOMMENDED, Group.THEMES),
        "com.samsung.android.privateshare" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.samsung.android.sm.devicesecurity" to Entry(Risk.ADVANCED, Group.OEM),
        "com.samsung.android.calendar" to Entry(Risk.ADVANCED, Group.OEM),
        "com.samsung.android.app.notes" to Entry(Risk.ADVANCED, Group.OEM),
        "com.xiaomi.joyose" to Entry(Risk.RECOMMENDED, Group.DIAGNOSTICS),
        "com.xiaomi.mircs" to Entry(Risk.RECOMMENDED, Group.DIAGNOSTICS),
        "com.miui.miservice" to Entry(Risk.RECOMMENDED, Group.DIAGNOSTICS),
        "com.miui.huanji" to Entry(Risk.RECOMMENDED, Group.BACKUP),
        "com.miui.personalassistant" to Entry(Risk.RECOMMENDED, Group.OEM),
        "com.miui.micloudsync" to Entry(Risk.ADVANCED, Group.BACKUP),
        "com.xiaomi.simactivate.service" to Entry(Risk.ADVANCED, Group.CARRIER),
        "com.xiaomi.account" to Entry(Risk.EXPERT, Group.SYSTEM)
    )

    private val PREFIX_ENTRIES = listOf(
        "com.samsung.android.knox." to Entry(Risk.EXPERT, Group.OEM),
        "com.sec.enterprise." to Entry(Risk.EXPERT, Group.OEM),
        "com.qualcomm." to Entry(Risk.EXPERT, Group.SYSTEM),
        "com.qti." to Entry(Risk.EXPERT, Group.SYSTEM),
        "com.mediatek." to Entry(Risk.EXPERT, Group.SYSTEM),
        "com.facebook." to Entry(Risk.RECOMMENDED, Group.PARTNER),
        "com.verizon." to Entry(Risk.RECOMMENDED, Group.CARRIER),
        "com.vzw." to Entry(Risk.RECOMMENDED, Group.CARRIER),
        "com.att." to Entry(Risk.RECOMMENDED, Group.CARRIER),
        "com.tmobile." to Entry(Risk.RECOMMENDED, Group.CARRIER),
        "com.sprint." to Entry(Risk.RECOMMENDED, Group.CARRIER),
        "com.vodafone." to Entry(Risk.RECOMMENDED, Group.CARRIER),
        "com.turkcell." to Entry(Risk.RECOMMENDED, Group.CARRIER),
        "com.samsung.android.app.omcagent" to Entry(Risk.EXPERT, Group.CARRIER),
        "android.auto_generated_rro" to Entry(Risk.EXPERT, Group.THEMES),
        "com.android.theme." to Entry(Risk.EXPERT, Group.THEMES),
        "com.android.systemui.theme." to Entry(Risk.EXPERT, Group.THEMES),
        "com.google.android.overlay." to Entry(Risk.EXPERT, Group.THEMES),
        "com.samsung.android.overlay." to Entry(Risk.EXPERT, Group.THEMES)
    )

    private val ENTRIES: Map<String, Entry> =
        GOOGLE_ENTRIES + AOSP_ENTRIES + SAMSUNG_ENTRIES + XIAOMI_ENTRIES + PARTNER_ENTRIES + SERVICE_ENTRIES

    fun classify(packageName: String, isSystemApp: Boolean): Entry {
        ENTRIES[packageName]?.let { return it }
        PREFIX_ENTRIES.firstOrNull { packageName.startsWith(it.first) }?.let { return it.second }
        return if (isSystemApp) Entry(Risk.UNSAFE, Group.SYSTEM) else Entry(Risk.RECOMMENDED, Group.PARTNER)
    }

    fun protectedPackages(context: Context): Set<String> {
        val blocked = InstalledAppsProvider.getUnsafeToKillPackages(context).toMutableSet()
        blocked.addAll(CRITICAL_PACKAGES)
        blocked.add(context.packageName)
        defaultDialer(context)?.let { blocked.add(it) }
        defaultSms(context)?.let { blocked.add(it) }
        return blocked
    }

    data class Protection(
        val packageName: String,
        val uid: Int,
        val flags: Int,
        val sharedUserId: String?
    )

    fun isProtected(protection: Protection, blocked: Set<String>): Boolean {
        if (protection.packageName in blocked) return true
        if (protection.uid in 0 until Process.FIRST_APPLICATION_UID) return true
        if (protection.sharedUserId in CRITICAL_SHARED_UIDS) return true
        return (protection.flags and ApplicationInfo.FLAG_PERSISTENT) != 0
    }

    private fun defaultDialer(context: Context): String? {
        return try {
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
        } catch (e: Exception) {
            null
        }
    }

    private fun defaultSms(context: Context): String? {
        return try {
            Telephony.Sms.getDefaultSmsPackage(context)
        } catch (e: Exception) {
            null
        }
    }
}
