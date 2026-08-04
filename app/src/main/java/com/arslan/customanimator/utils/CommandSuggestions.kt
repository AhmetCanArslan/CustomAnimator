package com.arslan.customanimator.utils

object CommandSuggestions {

    data class Suggestion(val token: String, val description: String)

    private const val PACKAGES = "%packages%"

    private val separators = charArrayOf(';', '|', '&', '\n')

    private val aliases: List<Pair<List<String>, List<String>>> = listOf(
        listOf("cmd", "package") to listOf("pm"),
        listOf("cmd", "activity") to listOf("am")
    )

    private val options: Map<String, List<String>> = mapOf(
        "pm" to listOf(
            "list", "path", "dump", "install", "uninstall", "clear", "enable", "disable",
            "disable-user", "hide", "unhide", "suspend", "unsuspend", "default-state",
            "grant", "revoke", "reset-permissions", "compile", "bg-dexopt-job",
            "trim-caches", "get-app-links", "set-home-activity", "get-install-location"
        ),
        "pm list" to listOf(
            "packages", "permissions", "permission-groups", "features",
            "instrumentation", "libraries", "users", "staged-sessions"
        ),
        "pm list packages" to listOf(
            "-s", "-3", "-d", "-e", "-u", "-f", "-i", "-U", "--user",
            "--show-versioncode", "--apex-only", "--uid"
        ),
        "pm list permissions" to listOf("-g", "-f", "-s", "-u", "-d"),
        "pm install" to listOf("-r", "-t", "-d", "-g", "-i", "--user", "--install-location"),
        "pm uninstall" to listOf("-k", "--user", "--versionCode"),
        "pm clear" to listOf("--user"),
        "pm path" to listOf("--user"),
        "pm enable" to listOf("--user"),
        "pm disable" to listOf("--user"),
        "pm disable-user" to listOf("--user"),
        "pm hide" to listOf("--user"),
        "pm unhide" to listOf("--user"),
        "pm suspend" to listOf("--user"),
        "pm unsuspend" to listOf("--user"),
        "pm grant" to listOf("--user"),
        "pm revoke" to listOf("--user"),
        "pm compile" to listOf("-m", "-r", "-f", "-c", "--reset", "-a", "--secondary-dex"),
        "pm get-app-links" to listOf("--user"),

        "am" to listOf(
            "start", "start-activity", "start-service", "start-foreground-service",
            "stop-service", "broadcast", "force-stop", "stop-app", "kill", "kill-all",
            "crash", "instrument", "set-debug-app", "clear-debug-app", "monitor",
            "hang", "restart", "idle-maintenance", "screen-compat", "get-current-user",
            "switch-user", "send-trim-memory", "trace-ipc", "dumpheap"
        ),
        "am start" to listOf(
            "-n", "-a", "-d", "-t", "-c", "-e", "--es", "--ei", "--ez", "--el", "--ef",
            "--eu", "--user", "-W", "-S", "-D", "-R", "--activity-clear-top",
            "--activity-single-top", "--activity-new-task"
        ),
        "am start-activity" to listOf("-n", "-a", "-d", "-t", "-c", "--user", "-W", "-S"),
        "am start-service" to listOf("-n", "-a", "--user", "--es", "--ei", "--ez"),
        "am start-foreground-service" to listOf("-n", "-a", "--user", "--es", "--ei", "--ez"),
        "am broadcast" to listOf(
            "-a", "-n", "-p", "-d", "-c", "--user", "--es", "--ei", "--ez", "--receiver-foreground"
        ),
        "am force-stop" to listOf("--user"),
        "am stop-app" to listOf("--user"),
        "am kill" to listOf("--user"),
        "am instrument" to listOf("-w", "-r", "-e", "-p", "--user", "--no-window-animation"),
        "am screen-compat" to listOf("on", "off"),
        "am monitor" to listOf("--gdb"),

        "cmd" to listOf(
            "-l", "package", "activity", "appops", "settings", "statusbar", "notification",
            "power", "wifi", "connectivity", "display", "media_session", "jobscheduler",
            "device_config", "shortcut", "role", "uimode", "overlay", "battery",
            "deviceidle", "thermalservice", "input_method", "audio", "location",
            "netpolicy", "user", "window", "alarm", "telecom", "vibrator", "sensorservice"
        ),
        "cmd statusbar" to listOf(
            "expand-notifications", "expand-settings", "collapse", "add-tile", "remove-tile",
            "click-tile", "check-support", "get-status-icons", "send-disable-flag"
        ),
        "cmd power" to listOf(
            "set-mode", "set-adaptive-power-saver-enabled", "set-fixed-performance-mode-enabled"
        ),
        "cmd power set-mode" to listOf("0", "1", "2"),
        "cmd notification" to listOf(
            "list", "post", "allow_listener", "disallow_listener", "allow_assistant"
        ),
        "cmd deviceidle" to listOf(
            "enable", "disable", "step", "force-idle", "unforce", "whitelist",
            "tempwhitelist", "get"
        ),
        "cmd deviceidle get" to listOf("light", "deep", "force", "screen", "charging", "network"),
        "cmd battery" to listOf("set", "unplug", "reset", "suspend_input"),
        "cmd battery set" to listOf(
            "level", "status", "ac", "usb", "wireless", "present", "temp", "counter", "invalid"
        ),
        "cmd overlay" to listOf(
            "list", "enable", "disable", "enable-exclusive", "set-priority", "lookup", "dump"
        ),
        "cmd uimode" to listOf("night", "car"),
        "cmd uimode night" to listOf("yes", "no", "auto", "custom"),
        "cmd uimode car" to listOf("yes", "no"),
        "cmd display" to listOf(
            "set-match-content-frame-rate-pref", "get-match-content-frame-rate-pref",
            "set-brightness", "reset-brightness-configuration"
        ),
        "cmd shortcut" to listOf("reset-throttling", "reset-all-throttling", "clear-shortcuts"),
        "cmd role" to listOf(
            "get-role-holders", "add-role-holder", "remove-role-holder", "clear-role-holders"
        ),
        "cmd jobscheduler" to listOf("run", "timeout", "cancel", "monitor-battery"),
        "cmd wifi" to listOf(
            "status", "set-wifi-enabled", "list-scan-results", "start-scan",
            "list-networks", "forget-network", "set-scan-always-available"
        ),
        "cmd wifi set-wifi-enabled" to listOf("enabled", "disabled"),
        "cmd user" to listOf("list", "create-user", "remove-user", "get-max-users"),
        "cmd alarm" to listOf("set-time", "set-timezone", "get-config-version"),
        "cmd input_method" to listOf("list", "enable", "disable", "set", "reset"),

        "settings" to listOf("--user", "get", "put", "delete", "list", "reset"),
        "settings get" to listOf("global", "system", "secure"),
        "settings put" to listOf("global", "system", "secure"),
        "settings delete" to listOf("global", "system", "secure"),
        "settings list" to listOf("global", "system", "secure"),
        "settings reset" to listOf("global", "secure"),

        "dumpsys" to listOf(
            "-l", "activity", "package", "power", "battery", "batterystats", "window",
            "display", "meminfo", "cpuinfo", "gfxinfo", "input", "notification", "alarm",
            "jobscheduler", "media_session", "audio", "wifi", "connectivity", "netstats",
            "deviceidle", "appops", "usagestats", "sensorservice", "thermalservice",
            "statusbar", "SurfaceFlinger", "telephony.registry", "location", "procstats"
        ),
        "dumpsys activity" to listOf(
            "activities", "services", "providers", "broadcasts", "intents", "processes",
            "recents", "top", "starter", "lru"
        ),
        "dumpsys package" to listOf(
            "packages", "permissions", "permission-groups", "libraries", "features",
            "preferred-xml", "install-sessions"
        ),
        "dumpsys meminfo" to listOf("-a", "-d", "-c", "-s", "--oom"),
        "dumpsys window" to listOf("windows", "displays", "policy", "tokens", "visible-apps"),
        "dumpsys battery" to listOf("set", "unplug", "reset"),
        "dumpsys battery set" to listOf(
            "level", "status", "ac", "usb", "wireless", "present", "temp"
        ),
        "dumpsys deviceidle" to listOf(
            "enable", "disable", "step", "force-idle", "unforce", "whitelist", "get"
        ),
        "dumpsys batterystats" to listOf("--reset", "--checkin", "--charged", "--history"),
        "dumpsys procstats" to listOf("--hours", "--full-details", "--current"),

        "svc" to listOf("power", "wifi", "data", "usb", "nfc", "bluetooth"),
        "svc power" to listOf("stayon", "reboot", "shutdown"),
        "svc power stayon" to listOf("true", "false", "usb", "ac", "wireless"),
        "svc wifi" to listOf("enable", "disable"),
        "svc data" to listOf("enable", "disable"),
        "svc nfc" to listOf("enable", "disable"),
        "svc bluetooth" to listOf("enable", "disable"),
        "svc usb" to listOf("setFunctions", "getFunctions", "setScreenUnlockedFunctions"),

        "wm" to listOf(
            "size", "density", "scaling", "dismiss-keyguard", "user-rotation", "folded-area"
        ),
        "wm size" to listOf("reset", "-d"),
        "wm density" to listOf("reset", "-d"),
        "wm scaling" to listOf("off", "auto"),
        "wm user-rotation" to listOf("free", "lock", "-d"),

        "input" to listOf(
            "text", "keyevent", "tap", "swipe", "draganddrop", "press", "roll",
            "motionevent", "keycombination"
        ),
        "input keyevent" to listOf("--longpress"),

        "ime" to listOf("list", "enable", "disable", "set", "reset"),
        "ime list" to listOf("-a", "-s"),
        "ime enable" to listOf("--user"),
        "ime disable" to listOf("--user"),
        "ime set" to listOf("--user"),

        "appops" to listOf(
            "get", "set", "reset", "list", "start", "stop", "query-op",
            "write-settings", "read-settings"
        ),

        "device_config" to listOf(
            "list", "get", "put", "delete", "reset",
            "set_sync_disabled_for_tests", "get_sync_disabled_for_tests"
        ),

        "logcat" to listOf(
            "-d", "-c", "-v", "-b", "-t", "-s", "-e", "-m", "-f", "-g", "-T",
            "--pid", "*:E", "*:W", "*:I", "*:S"
        ),
        "logcat -b" to listOf("main", "system", "crash", "events", "radio", "all", "default"),
        "logcat -v" to listOf(
            "brief", "long", "process", "raw", "tag", "thread", "threadtime", "time", "color"
        ),

        "service" to listOf("list", "check", "call"),

        "bmgr" to listOf(
            "enabled", "enable", "backupnow", "restore", "list", "transport", "run",
            "wipe", "fullbackup"
        ),
        "bmgr backupnow" to listOf("--all"),
        "bmgr list" to listOf("transports", "sets"),

        "monkey" to listOf(
            "-p", "-v", "-s", "--throttle", "--pct-touch", "--pct-motion",
            "--ignore-crashes", "--ignore-timeouts"
        ),

        "content" to listOf("query", "insert", "update", "delete", "call", "read", "write"),
        "content query" to listOf("--uri", "--user", "--projection", "--where", "--sort"),
        "content insert" to listOf("--uri", "--user", "--bind"),
        "content update" to listOf("--uri", "--user", "--bind", "--where"),
        "content delete" to listOf("--uri", "--user", "--where"),

        "screencap" to listOf("-p", "-d"),
        "screenrecord" to listOf(
            "--size", "--bit-rate", "--time-limit", "--verbose", "--bugreport", "--rotate"
        ),
        "reboot" to listOf("recovery", "bootloader", "-p"),
        "locksettings" to listOf(
            "set-pin", "set-password", "set-pattern", "clear", "verify",
            "get-disabled", "set-disabled", "require-strong-auth"
        ),
        "locksettings clear" to listOf("--old"),
        "locksettings set-pin" to listOf("--old"),
        "locksettings set-password" to listOf("--old"),
        "locksettings set-pattern" to listOf("--old"),

        "sm" to listOf("list-disks", "list-volumes", "has-adoptable", "get-primary-storage-uuid"),
        "dmesg" to listOf("-w", "-T", "-c"),

        "ls" to listOf("-l", "-a", "-la", "-lh", "-R", "-t", "-S", "-d", "-Z"),
        "ps" to listOf("-A", "-e", "-f", "-ef", "-o", "-p", "-u", "-T"),
        "top" to listOf("-n", "-m", "-d", "-b", "-H", "-q", "-s"),
        "df" to listOf("-h", "-k", "-a"),
        "du" to listOf("-h", "-s", "-sh", "-a", "-d"),
        "grep" to listOf("-i", "-v", "-r", "-n", "-E", "-c", "-l", "-w", "-o", "-A", "-B"),
        "find" to listOf(
            "-name", "-iname", "-type", "-maxdepth", "-mindepth", "-size", "-mtime",
            "-delete", "-exec", "-print"
        ),
        "rm" to listOf("-r", "-f", "-rf", "-v", "-d"),
        "cp" to listOf("-r", "-f", "-p", "-a", "-v"),
        "mv" to listOf("-f", "-n", "-v"),
        "mkdir" to listOf("-p", "-m"),
        "chmod" to listOf("-R", "644", "755", "777", "+x", "-x"),
        "chown" to listOf("-R"),
        "kill" to listOf("-9", "-15", "-l"),
        "killall" to listOf("-9", "-l", "-v"),
        "mount" to listOf("-o", "-t", "-r", "-w", "remount"),
        "ping" to listOf("-c", "-i", "-s", "-w", "-W"),
        "ip" to listOf("-4", "-6", "addr", "route", "link", "rule", "neigh"),
        "ip addr" to listOf("show", "add", "del", "flush"),
        "ip route" to listOf("show", "add", "del", "get", "flush"),
        "ip link" to listOf("show", "set"),
        "head" to listOf("-n", "-c"),
        "tail" to listOf("-n", "-c", "-f"),
        "wc" to listOf("-l", "-w", "-c"),
        "sort" to listOf("-r", "-n", "-u", "-k", "-f"),
        "uniq" to listOf("-c", "-d", "-u", "-i"),
        "cut" to listOf("-d", "-f", "-c"),
        "sed" to listOf("-n", "-e", "-i", "-E"),
        "tar" to listOf("-c", "-x", "-t", "-z", "-v", "-f", "-czf", "-xzf"),
        "unzip" to listOf("-l", "-o", "-d", "-q"),
        "xargs" to listOf("-n", "-I", "-0"),
        "stat" to listOf("-c", "-f", "-L"),
        "setenforce" to listOf("0", "1", "Enforcing", "Permissive"),
        "date" to listOf("-u", "-s", "+%Y-%m-%d", "+%H:%M:%S", "+%s"),
        "free" to listOf("-h", "-m", "-b"),
        "netstat" to listOf("-a", "-n", "-t", "-u", "-l", "-p"),
        "getevent" to listOf("-l", "-t", "-p", "-i")
    )

    private val args: Map<String, List<List<String>>> = mapOf(
        "pm path" to listOf(listOf(PACKAGES)),
        "pm dump" to listOf(listOf(PACKAGES)),
        "pm clear" to listOf(listOf(PACKAGES)),
        "pm uninstall" to listOf(listOf(PACKAGES)),
        "pm enable" to listOf(listOf(PACKAGES)),
        "pm disable" to listOf(listOf(PACKAGES)),
        "pm disable-user" to listOf(listOf(PACKAGES)),
        "pm hide" to listOf(listOf(PACKAGES)),
        "pm unhide" to listOf(listOf(PACKAGES)),
        "pm suspend" to listOf(listOf(PACKAGES)),
        "pm unsuspend" to listOf(listOf(PACKAGES)),
        "pm default-state" to listOf(listOf(PACKAGES)),
        "pm reset-permissions" to listOf(listOf(PACKAGES)),
        "pm get-app-links" to listOf(listOf(PACKAGES)),
        "pm compile" to listOf(listOf(PACKAGES)),
        "pm grant" to listOf(listOf(PACKAGES), runtimePermissions()),
        "pm revoke" to listOf(listOf(PACKAGES), runtimePermissions()),
        "pm trim-caches" to listOf(listOf("128M", "256M", "512M", "1G", "2G")),
        "pm compile -m" to listOf(listOf("verify", "speed-profile", "speed", "everything")),
        "pm compile -r" to listOf(
            listOf("first-boot", "boot-after-ota", "install", "bg-dexopt", "cmdline")
        ),
        "pm list packages --user" to listOf(listOf("0")),
        "pm install --install-location" to listOf(listOf("0", "1", "2")),

        "am force-stop" to listOf(listOf(PACKAGES)),
        "am stop-app" to listOf(listOf(PACKAGES)),
        "am kill" to listOf(listOf(PACKAGES)),
        "am crash" to listOf(listOf(PACKAGES)),
        "am start -n" to listOf(listOf(PACKAGES)),
        "am start -a" to listOf(intentActions()),
        "am broadcast -a" to listOf(intentActions()),
        "am broadcast -p" to listOf(listOf(PACKAGES)),
        "am switch-user" to listOf(listOf("0")),
        "am send-trim-memory" to listOf(
            listOf(PACKAGES),
            listOf("HIDDEN", "RUNNING_MODERATE", "BACKGROUND", "RUNNING_LOW", "MODERATE",
                "RUNNING_CRITICAL", "COMPLETE")
        ),
        "am set-debug-app" to listOf(listOf(PACKAGES)),

        "settings get global" to listOf(globalKeys()),
        "settings delete global" to listOf(globalKeys()),
        "settings put global" to listOf(globalKeys()),
        "settings get secure" to listOf(secureKeys()),
        "settings delete secure" to listOf(secureKeys()),
        "settings put secure" to listOf(secureKeys()),
        "settings get system" to listOf(systemKeys()),
        "settings delete system" to listOf(systemKeys()),
        "settings put system" to listOf(systemKeys()),
        "settings reset global" to listOf(
            listOf("untrusted_defaults", "untrusted_clear", "trusted_defaults")
        ),
        "settings reset secure" to listOf(
            listOf("untrusted_defaults", "untrusted_clear", "trusted_defaults")
        ),
        "settings put global window_animation_scale" to listOf(animationScales()),
        "settings put global transition_animation_scale" to listOf(animationScales()),
        "settings put global animator_duration_scale" to listOf(animationScales()),
        "settings put global airplane_mode_on" to listOf(boolInts()),
        "settings put global adb_enabled" to listOf(boolInts()),
        "settings put global development_settings_enabled" to listOf(boolInts()),
        "settings put global stay_on_while_plugged_in" to listOf(listOf("0", "1", "2", "3", "7")),
        "settings put global auto_time" to listOf(boolInts()),
        "settings put global auto_time_zone" to listOf(boolInts()),
        "settings put global heads_up_notifications_enabled" to listOf(boolInts()),
        "settings put system accelerometer_rotation" to listOf(boolInts()),
        "settings put system haptic_feedback_enabled" to listOf(boolInts()),
        "settings put system sound_effects_enabled" to listOf(boolInts()),
        "settings put system vibrate_when_ringing" to listOf(boolInts()),
        "settings put system screen_brightness_mode" to listOf(listOf("0", "1")),
        "settings put system screen_off_timeout" to listOf(
            listOf("15000", "30000", "60000", "120000", "600000", "1800000")
        ),
        "settings put secure ui_night_mode" to listOf(listOf("1", "2")),
        "settings put secure doze_always_on" to listOf(boolInts()),
        "settings put secure navigation_mode" to listOf(listOf("0", "1", "2")),

        "dumpsys package" to listOf(listOf(PACKAGES)),
        "dumpsys gfxinfo" to listOf(listOf(PACKAGES)),
        "dumpsys meminfo" to listOf(listOf(PACKAGES)),
        "dumpsys procstats" to listOf(listOf(PACKAGES)),
        "dumpsys battery set" to listOf(
            listOf("level", "status", "ac", "usb", "wireless", "present", "temp")
        ),
        "cmd battery set" to listOf(
            listOf("level", "status", "ac", "usb", "wireless", "present", "temp")
        ),
        "cmd deviceidle whitelist" to listOf(listOf(PACKAGES)),
        "dumpsys deviceidle whitelist" to listOf(listOf(PACKAGES)),
        "cmd overlay enable" to listOf(listOf(PACKAGES)),
        "cmd overlay disable" to listOf(listOf(PACKAGES)),
        "cmd shortcut clear-shortcuts" to listOf(listOf(PACKAGES)),

        "appops get" to listOf(listOf(PACKAGES), appOps()),
        "appops set" to listOf(listOf(PACKAGES), appOps(), appOpModes()),
        "appops reset" to listOf(listOf(PACKAGES)),
        "appops query-op" to listOf(appOps(), appOpModes()),

        "device_config list" to listOf(deviceConfigNamespaces()),
        "device_config get" to listOf(deviceConfigNamespaces()),
        "device_config put" to listOf(deviceConfigNamespaces()),
        "device_config delete" to listOf(deviceConfigNamespaces()),
        "device_config reset" to listOf(
            listOf("untrusted_defaults", "untrusted_clear", "trusted_defaults"),
            deviceConfigNamespaces()
        ),

        "monkey -p" to listOf(listOf(PACKAGES)),
        "bmgr backupnow" to listOf(listOf(PACKAGES)),
        "am instrument -w" to listOf(listOf(PACKAGES)),

        "svc power reboot" to listOf(listOf("recovery", "bootloader")),
        "wm size" to listOf(listOf("1080x2400", "1440x3200", "720x1600")),
        "wm density" to listOf(listOf("320", "420", "480", "560", "640")),
        "wm user-rotation lock" to listOf(listOf("0", "1", "2", "3")),
        "input keyevent" to listOf(keyEvents()),
        "input text" to listOf(emptyList()),

        "getprop" to listOf(readableProps()),
        "setprop" to listOf(writableProps()),
        "setprop debug.hwui.renderer" to listOf(listOf("skiagl", "skiavk")),
        "setprop debug.hwui.overdraw" to listOf(listOf("show", "false")),
        "setprop persist.sys.usb.config" to listOf(listOf("mtp", "ptp", "none")),

        "service check" to listOf(serviceNames()),
        "service call" to listOf(serviceNames()),
        "pidof" to listOf(listOf(PACKAGES)),
        "killall" to listOf(listOf(PACKAGES)),
        "cat" to listOf(
            listOf(
                "/proc/cpuinfo", "/proc/meminfo", "/proc/version", "/proc/uptime",
                "/proc/stat", "/sys/class/power_supply/battery/capacity",
                "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"
            )
        ),
        "sleep" to listOf(listOf("1", "3", "5", "10")),
        "setenforce" to listOf(listOf("0", "1")),
        "logcat -b" to listOf(
            listOf("main", "system", "crash", "events", "radio", "all", "default")
        ),
        "logcat -v" to listOf(
            listOf("brief", "long", "process", "raw", "tag", "thread", "threadtime", "time", "color")
        ),
        "logcat -t" to listOf(listOf("50", "100", "500", "1000")),
        "screenrecord --size" to listOf(listOf("720x1280", "1080x1920")),
        "screenrecord --time-limit" to listOf(listOf("30", "60", "120", "180")),
        "screenrecord --bit-rate" to listOf(listOf("4000000", "8000000", "16000000"))
    )

    private val topLevel: List<String> = listOf(
        "pm", "am", "cmd", "settings", "dumpsys", "getprop", "setprop", "svc", "wm",
        "input", "ime", "content", "appops", "device_config", "logcat", "service",
        "bmgr", "monkey", "screencap", "screenrecord", "reboot", "locksettings", "sm",
        "bugreport", "dmesg", "getevent", "ls", "cat", "ps", "top", "df", "du", "grep",
        "find", "rm", "cp", "mv", "mkdir", "rmdir", "chmod", "chown", "kill", "killall",
        "mount", "umount", "ping", "ip", "ifconfig", "netstat", "head", "tail", "wc",
        "sort", "uniq", "cut", "sed", "awk", "echo", "touch", "sleep", "date", "uptime",
        "whoami", "id", "getenforce", "setenforce", "pidof", "free", "sync", "stop",
        "start", "printenv", "which", "sh", "toybox", "tar", "unzip", "stat", "md5sum",
        "xargs", "basename", "dirname", "ln", "mkfifo", "wm"
    ).distinct().sorted()

    private val descriptions: Map<String, String> = mapOf(
        "pm" to "Package manager",
        "am" to "Activity manager",
        "cmd" to "Call a system service directly",
        "settings" to "Read and write system settings",
        "dumpsys" to "Dump system service state",
        "getprop" to "Read system properties",
        "setprop" to "Write a system property",
        "svc" to "Toggle power, wifi, data, usb, nfc",
        "wm" to "Window manager: size, density, rotation",
        "input" to "Inject taps, swipes, keys and text",
        "ime" to "Manage input methods",
        "content" to "Query content providers",
        "appops" to "Read and set app operation modes",
        "device_config" to "Read and write feature flags",
        "logcat" to "Read the system log",
        "service" to "List and call binder services",
        "bmgr" to "Backup manager",
        "monkey" to "Send pseudo-random UI events",
        "screencap" to "Take a screenshot to a file",
        "screenrecord" to "Record the screen to a file",
        "reboot" to "Restart the device",
        "locksettings" to "Manage the lock screen credential",
        "sm" to "Storage manager",
        "bugreport" to "Generate a bug report",
        "dmesg" to "Kernel log",
        "getevent" to "Raw input events",
        "ls" to "List directory contents",
        "cat" to "Print a file",
        "ps" to "List running processes",
        "top" to "Live process and CPU usage",
        "df" to "Free space per filesystem",
        "du" to "Disk usage of files and folders",
        "grep" to "Filter lines by pattern",
        "find" to "Search for files",
        "rm" to "Delete files",
        "cp" to "Copy files",
        "mv" to "Move or rename files",
        "mkdir" to "Create a directory",
        "rmdir" to "Remove an empty directory",
        "chmod" to "Change file permissions",
        "chown" to "Change file owner",
        "kill" to "Send a signal to a pid",
        "killall" to "Kill processes by name",
        "mount" to "Mount or remount a filesystem",
        "umount" to "Unmount a filesystem",
        "ping" to "Test network reachability",
        "ip" to "Network addresses and routes",
        "ifconfig" to "Network interfaces",
        "netstat" to "Network connections",
        "head" to "First lines of a file",
        "tail" to "Last lines of a file",
        "wc" to "Count lines, words and bytes",
        "sort" to "Sort lines",
        "uniq" to "Collapse repeated lines",
        "cut" to "Extract columns from lines",
        "sed" to "Stream editor",
        "awk" to "Text processing language",
        "echo" to "Print text",
        "touch" to "Create a file or update its timestamp",
        "sleep" to "Wait for seconds",
        "date" to "Show or set the time",
        "uptime" to "How long the device has been up",
        "whoami" to "Current shell user",
        "id" to "Current uid, gid and groups",
        "getenforce" to "SELinux enforcing state",
        "setenforce" to "Set SELinux enforcing or permissive",
        "pidof" to "Pid of a process by name",
        "free" to "Memory usage",
        "sync" to "Flush pending writes to storage",
        "stop" to "Stop the Android runtime",
        "start" to "Start the Android runtime",
        "printenv" to "Print environment variables",
        "which" to "Locate an executable",
        "sh" to "Run a shell",
        "toybox" to "List built-in toybox applets",
        "tar" to "Create or extract an archive",
        "unzip" to "Extract a zip archive",
        "stat" to "File metadata",
        "md5sum" to "MD5 checksum of a file",
        "xargs" to "Build commands from input",
        "basename" to "Strip the directory from a path",
        "dirname" to "Directory part of a path",
        "ln" to "Create a link",

        "pm list" to "List packages, permissions, users",
        "pm list packages" to "List installed packages",
        "pm path" to "APK path of a package",
        "pm dump" to "Dump everything about a package",
        "pm install" to "Install an APK from a device path",
        "pm uninstall" to "Uninstall a package",
        "pm clear" to "Delete a package's data and caches",
        "pm enable" to "Enable a package or component",
        "pm disable" to "Disable a package (needs system rights)",
        "pm disable-user" to "Disable a package for this user",
        "pm hide" to "Hide a package",
        "pm unhide" to "Unhide a package",
        "pm suspend" to "Suspend a package",
        "pm unsuspend" to "Unsuspend a package",
        "pm default-state" to "Reset a package to its default state",
        "pm grant" to "Grant a runtime permission",
        "pm revoke" to "Revoke a runtime permission",
        "pm reset-permissions" to "Reset all runtime permissions",
        "pm compile" to "Recompile a package's dex code",
        "pm bg-dexopt-job" to "Run the background dexopt job now",
        "pm trim-caches" to "Free caches until this much space is free",
        "pm get-app-links" to "Show verified app link domains",
        "pm list packages -s" to "System packages only",
        "pm list packages -3" to "Third-party packages only",
        "pm list packages -d" to "Disabled packages only",
        "pm list packages -e" to "Enabled packages only",
        "pm list packages -f" to "Show the APK file path",
        "pm list packages -i" to "Show the installer package",
        "pm list packages -u" to "Include uninstalled packages",
        "pm install -r" to "Reinstall, keeping data",
        "pm install -d" to "Allow a version downgrade",
        "pm install -g" to "Grant all permissions",
        "pm install -t" to "Allow test APKs",
        "pm uninstall -k" to "Keep data and cache directories",
        "pm compile -m" to "Compilation filter",
        "pm compile -r" to "Compilation reason",
        "pm compile -f" to "Force recompilation",
        "pm compile -a" to "All packages",

        "am start" to "Start an activity",
        "am start-activity" to "Start an activity",
        "am start-service" to "Start a service",
        "am start-foreground-service" to "Start a foreground service",
        "am stop-service" to "Stop a service",
        "am broadcast" to "Send a broadcast intent",
        "am force-stop" to "Force stop a package",
        "am stop-app" to "Stop an app without resetting its state",
        "am kill" to "Kill background processes of a package",
        "am kill-all" to "Kill all background processes",
        "am crash" to "Force a crash in a package",
        "am instrument" to "Run an instrumentation test",
        "am monitor" to "Watch for crashes and ANRs",
        "am get-current-user" to "Current user id",
        "am switch-user" to "Switch to another user",
        "am send-trim-memory" to "Ask a process to release memory",
        "am idle-maintenance" to "Trigger idle maintenance now",
        "am start -n" to "Component: package/.Activity",
        "am start -a" to "Intent action",
        "am start -d" to "Intent data URI",
        "am start -W" to "Wait for the launch to finish",
        "am broadcast -a" to "Intent action",
        "am broadcast -p" to "Target package",

        "settings get" to "Read a setting",
        "settings put" to "Write a setting",
        "settings delete" to "Remove a setting",
        "settings list" to "List every setting in a namespace",
        "settings reset" to "Reset settings changed by apps",
        "settings get global" to "Device-wide namespace",
        "settings put global" to "Device-wide namespace",
        "settings get secure" to "Read-only-to-apps namespace",
        "settings put secure" to "Read-only-to-apps namespace",
        "settings get system" to "User preference namespace",
        "settings put system" to "User preference namespace",
        "settings put global window_animation_scale" to "Window open and close animation speed",
        "settings put global transition_animation_scale" to "Activity transition animation speed",
        "settings put global animator_duration_scale" to "In-app animator speed",

        "svc power" to "Screen stay-on, reboot, shutdown",
        "svc power stayon" to "Keep the screen on while charging",
        "svc wifi" to "Turn Wi-Fi on or off",
        "svc data" to "Turn mobile data on or off",
        "svc usb" to "USB functions",
        "wm size" to "Get or set the resolution",
        "wm density" to "Get or set the DPI",
        "wm scaling" to "Force GPU scaling off or auto",
        "wm dismiss-keyguard" to "Unlock the screen",
        "wm user-rotation" to "Lock or free the screen rotation",
        "input text" to "Type a string into the focused field",
        "input keyevent" to "Send a key",
        "input tap" to "Tap at x y",
        "input swipe" to "Swipe from x1 y1 to x2 y2",

        "cmd -l" to "List every available service",
        "cmd statusbar" to "Expand or collapse the status bar",
        "cmd deviceidle" to "Doze state and battery whitelist",
        "cmd battery" to "Fake the battery state",
        "cmd battery reset" to "Restore the real battery state",
        "cmd overlay" to "Manage runtime resource overlays",
        "cmd uimode night" to "Force dark or light mode",
        "cmd power set-mode" to "0 none, 1 low power, 2 performance",

        "dumpsys -l" to "List every dumpable service",
        "dumpsys battery" to "Battery state",
        "dumpsys battery reset" to "Restore the real battery state",
        "dumpsys meminfo" to "Memory per process",
        "dumpsys gfxinfo" to "Frame rendering timings",
        "dumpsys cpuinfo" to "CPU usage per process",
        "dumpsys activity" to "Activity manager state",
        "dumpsys window" to "Window manager state",
        "dumpsys package" to "Package manager state",
        "dumpsys batterystats" to "Historical battery usage",

        "appops get" to "Read a package's app op modes",
        "appops set" to "Set an app op mode",
        "appops reset" to "Reset a package's app ops",
        "device_config put" to "Write a flag: namespace key value",
        "logcat -d" to "Dump the log and exit",
        "logcat -c" to "Clear the log buffers",
        "logcat -b" to "Choose a log buffer",
        "logcat -v" to "Output format",
        "logcat -t" to "Print only the last N lines",
        "service list" to "List all binder services",
        "bmgr backupnow" to "Back up now",
        "monkey -p" to "Restrict events to a package"
    )

    private fun runtimePermissions() = listOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",
        "android.permission.READ_PHONE_STATE",
        "android.permission.CALL_PHONE",
        "android.permission.READ_CALL_LOG",
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.BODY_SENSORS",
        "android.permission.ACTIVITY_RECOGNITION",
        "android.permission.BLUETOOTH_CONNECT",
        "android.permission.BLUETOOTH_SCAN",
        "android.permission.BLUETOOTH_ADVERTISE",
        "android.permission.NEARBY_WIFI_DEVICES",
        "android.permission.WRITE_SECURE_SETTINGS",
        "android.permission.DUMP",
        "android.permission.PACKAGE_USAGE_STATS"
    )

    private fun appOps() = listOf(
        "RUN_IN_BACKGROUND", "RUN_ANY_IN_BACKGROUND", "WAKE_LOCK", "SYSTEM_ALERT_WINDOW",
        "WRITE_SETTINGS", "GET_USAGE_STATS", "REQUEST_INSTALL_PACKAGES",
        "MANAGE_EXTERNAL_STORAGE", "START_FOREGROUND", "CAMERA", "RECORD_AUDIO",
        "COARSE_LOCATION", "FINE_LOCATION", "POST_NOTIFICATION", "SCHEDULE_EXACT_ALARM",
        "READ_CLIPBOARD", "WRITE_CLIPBOARD", "BOOT_COMPLETED", "INSTANT_APP_START_FOREGROUND"
    )

    private fun appOpModes() = listOf("allow", "deny", "ignore", "default")

    private fun deviceConfigNamespaces() = listOf(
        "activity_manager", "activity_manager_native_boot", "app_compat", "autofill",
        "connectivity", "content_capture", "device_idle", "game_driver", "input_native_boot",
        "media_native", "netd_native", "privacy", "runtime", "runtime_native",
        "runtime_native_boot", "storage_native_boot", "systemui", "telephony",
        "window_manager", "window_manager_native_boot"
    )

    private fun serviceNames() = listOf(
        "package", "activity", "window", "power", "battery", "statusbar", "notification",
        "phone", "SurfaceFlinger", "audio", "wifi", "connectivity", "clipboard",
        "input_method", "alarm", "media_session", "display", "sensorservice"
    )

    private fun keyEvents() = listOf(
        "KEYCODE_HOME", "KEYCODE_BACK", "KEYCODE_APP_SWITCH", "KEYCODE_POWER",
        "KEYCODE_WAKEUP", "KEYCODE_SLEEP", "KEYCODE_MENU", "KEYCODE_ENTER",
        "KEYCODE_DEL", "KEYCODE_TAB", "KEYCODE_SPACE", "KEYCODE_SEARCH",
        "KEYCODE_VOLUME_UP", "KEYCODE_VOLUME_DOWN", "KEYCODE_VOLUME_MUTE",
        "KEYCODE_BRIGHTNESS_UP", "KEYCODE_BRIGHTNESS_DOWN", "KEYCODE_CAMERA",
        "KEYCODE_MEDIA_PLAY_PAUSE", "KEYCODE_MEDIA_NEXT", "KEYCODE_MEDIA_PREVIOUS",
        "KEYCODE_MEDIA_STOP", "KEYCODE_DPAD_UP", "KEYCODE_DPAD_DOWN", "KEYCODE_DPAD_LEFT",
        "KEYCODE_DPAD_RIGHT", "KEYCODE_DPAD_CENTER", "KEYCODE_NOTIFICATION",
        "KEYCODE_SETTINGS", "KEYCODE_SCREENSHOT"
    )

    private fun intentActions() = listOf(
        "android.intent.action.VIEW",
        "android.intent.action.MAIN",
        "android.intent.action.SEND",
        "android.intent.action.DIAL",
        "android.intent.action.CALL",
        "android.intent.action.BOOT_COMPLETED",
        "android.intent.action.MEDIA_SCANNER_SCAN_FILE",
        "android.settings.SETTINGS",
        "android.settings.APPLICATION_DETAILS_SETTINGS",
        "android.settings.DEVELOPMENT_SETTINGS"
    )

    private fun globalKeys() = listOf(
        "window_animation_scale", "transition_animation_scale", "animator_duration_scale",
        "airplane_mode_on", "development_settings_enabled", "adb_enabled",
        "stay_on_while_plugged_in", "auto_time", "auto_time_zone", "device_name",
        "wifi_sleep_policy", "data_roaming", "low_power", "low_power_trigger_level",
        "heads_up_notifications_enabled", "wifi_scan_always_enabled",
        "ble_scan_always_enabled", "private_dns_mode", "private_dns_specifier"
    )

    private fun secureKeys() = listOf(
        "ui_night_mode", "screensaver_enabled", "location_mode", "default_input_method",
        "enabled_accessibility_services", "enabled_input_methods",
        "immersive_mode_confirmations", "sysui_qs_tiles", "navigation_mode",
        "doze_always_on", "one_handed_mode_enabled", "show_ime_with_hard_keyboard",
        "accessibility_display_daltonizer_enabled", "night_display_activated"
    )

    private fun systemKeys() = listOf(
        "screen_brightness", "screen_brightness_mode", "screen_off_timeout",
        "accelerometer_rotation", "haptic_feedback_enabled", "sound_effects_enabled",
        "peak_refresh_rate", "min_refresh_rate", "font_scale", "vibrate_when_ringing",
        "volume_music", "volume_ring", "volume_alarm", "pointer_speed", "show_touches"
    )

    private fun readableProps() = listOf(
        "ro.build.version.release", "ro.build.version.sdk", "ro.build.version.security_patch",
        "ro.build.fingerprint", "ro.product.model", "ro.product.brand", "ro.product.manufacturer",
        "ro.product.device", "ro.product.cpu.abi", "ro.serialno", "ro.boot.hardware",
        "ro.sf.lcd_density", "sys.boot_completed", "persist.sys.timezone",
        "persist.sys.locale", "debug.hwui.renderer", "gsm.operator.alpha",
        "ro.hardware", "ro.boot.bootloader", "dalvik.vm.heapsize"
    )

    private fun writableProps() = listOf(
        "debug.hwui.renderer", "debug.hwui.overdraw", "debug.layout",
        "persist.sys.timezone", "persist.sys.locale", "persist.sys.usb.config",
        "log.tag.stats_log", "debug.force_rtl"
    )

    private fun animationScales() = listOf("0", "0.25", "0.5", "0.75", "1", "1.5", "2")

    private fun boolInts() = listOf("0", "1")

    fun suggest(
        text: String,
        cursor: Int,
        installedPackages: List<String> = emptyList()
    ): List<Suggestion> {
        val safeCursor = cursor.coerceIn(0, text.length)
        val statement = text.substring(0, safeCursor).let { prefix ->
            val cut = prefix.lastIndexOfAny(separators)
            if (cut >= 0) prefix.substring(cut + 1) else prefix
        }

        val endsWithSpace = statement.isNotEmpty() && statement.last().isWhitespace()
        val tokens = statement.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val partial = if (endsWithSpace) "" else tokens.lastOrNull().orEmpty()
        val preceding = if (endsWithSpace) tokens else tokens.dropLast(1)

        val resolved = if (preceding.isEmpty()) {
            Resolved(topLevel, "")
        } else {
            resolve(preceding) ?: return emptyList()
        }

        val pool = resolved.pool.flatMap { token ->
            if (token == PACKAGES) installedPackages else listOf(token)
        }

        val ordered = if (partial.isEmpty()) {
            pool
        } else {
            val prefixed = pool.filter {
                it.startsWith(partial, ignoreCase = true) && !it.equals(partial, ignoreCase = true)
            }
            val contained = pool.filter {
                !it.startsWith(partial, ignoreCase = true) && it.contains(partial, ignoreCase = true)
            }
            val exact = pool.filter { it.equals(partial, ignoreCase = true) }
            prefixed + contained + exact
        }

        return ordered.distinct().map { token ->
            Suggestion(token = token, description = describe(resolved.path, token))
        }
    }

    private class Resolved(val pool: List<String>, val path: String)

    private fun resolve(precedingRaw: List<String>): Resolved? {
        val preceding = applyAlias(precedingRaw)

        for (size in preceding.size downTo 1) {
            val path = preceding.take(size)
            val key = path.joinToString(" ")
            val opts = options[key]
            val argPools = args[key]
            if (opts == null && argPools == null) continue

            val rest = preceding.drop(size)
            var consumed = 0
            var index = 0
            while (index < rest.size) {
                val token = rest[index]
                if (token.startsWith("-")) {
                    if (takesValue(key, token)) {
                        if (index == rest.size - 1) {
                            val values = args["$key $token"]?.firstOrNull().orEmpty()
                            return if (values.isEmpty()) null else Resolved(values, "$key $token")
                        }
                        index++
                    }
                } else {
                    consumed++
                }
                index++
            }

            val pool = if (consumed == 0) {
                opts.orEmpty() + argPools?.firstOrNull().orEmpty()
            } else {
                argPools?.getOrNull(consumed).orEmpty()
            }
            return if (pool.isEmpty()) null else Resolved(pool, key)
        }
        return null
    }

    private val valueFlags = setOf(
        "--user", "--install-location", "--versionCode", "--namespace", "--uri",
        "--where", "--sort", "--bind", "--projection", "--old", "--throttle",
        "--pct-touch", "--pct-motion", "--time-limit", "--bit-rate", "--size",
        "--rotate", "--pid", "--hours", "--split"
    )

    private fun takesValue(path: String, flag: String): Boolean =
        flag in valueFlags || args.containsKey("$path $flag")

    private fun applyAlias(preceding: List<String>): List<String> {
        aliases.forEach { (from, to) ->
            if (preceding.size >= from.size && preceding.take(from.size) == from) {
                return to + preceding.drop(from.size)
            }
        }
        return preceding
    }

    private fun describe(path: String, token: String): String {
        val key = if (path.isEmpty()) token else "$path $token"
        return descriptions[key].orEmpty()
    }

    fun apply(text: String, cursor: Int, suggestion: String): Pair<String, Int> {
        val safeCursor = cursor.coerceIn(0, text.length)
        val prefix = text.substring(0, safeCursor)
        val start = prefix.indexOfLast { it.isWhitespace() } + 1
        var end = safeCursor
        while (end < text.length && !text[end].isWhitespace()) end++
        val replaced = prefix.substring(0, start) + suggestion + " "
        return (replaced + text.substring(end)) to replaced.length
    }
}
