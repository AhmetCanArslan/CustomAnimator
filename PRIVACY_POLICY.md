# Privacy Policy

**App:** Custom Animator M3 (`com.riyadmondol2006.customanimator`), a fork of [CustomAnimator](https://github.com/AhmetCanArslan/CustomAnimator)
**Maintainer:** [riyadmondol2006](https://github.com/riyadmondol2006)
**Last Updated:** October 2026

## Summary

Custom Animator M3 does not collect, store, or transmit your personal data. There is no account, no login, no analytics, no crash reporting, and no telemetry of any kind. Everything the app creates — presets, rules, notification logs, ignore lists — is written to the app's own private storage on your device and stays there.

No feature of the app sends your data over the network. The app contains **no ads, no in-app purchases and no third-party SDKs**. The only network requests it makes are downloads of public data files, described in "Network Use" below.

## What the App Stores

All of the following is kept in the app's private storage (`SharedPreferences`), readable only by the app:

| Data | Purpose |
| --- | --- |
| Animation, width, battery and terminal presets | Your saved configurations |
| Notification rules and custom flash patterns | Which notifications trigger which action |
| Notification log (app name, package name, title, body, timestamp, matched rules) | The History screen |
| Ignore list (app, title/body patterns) | Notifications you chose to suppress |
| App preferences (last screen, onboarding consent, service toggles) | Restoring the app's state |

None of this is uploaded, shared, sold, or backed up to any server operated by the developer. You can delete the notification log at any time from **History → Log Settings**, set it to auto-delete after 1/3/7/30 days, or remove everything at once by clearing the app's data or uninstalling it.

## Notification Access

The app includes a notification listener service. When you enable it, Android delivers the content of incoming notifications to the app so it can match them against the rules you created and trigger a flash, a screen wake, AOD or a screen flash.

- Notification content is processed **on your device only**, in memory, at the moment the notification arrives.
- A copy is written to the local notification log so the History screen can show it. That log never leaves your device.
- Notification content is never sent over the network, and never shared with the developer or any third party.
- The listener can be turned off at any time from the master switch on the Notifications tab or in Android's system settings, and disabling it stops all processing immediately.

## Permissions

Each permission is used only for the feature named next to it. No permission is used for profiling, tracking, or data collection.

| Permission | Why it is needed |
| --- | --- |
| `WRITE_SECURE_SETTINGS`, `SET_ANIMATION_SCALE` | Change animation scales, screen density and always-on display. Must be granted via ADB or Shizuku; it cannot be granted from inside the app. |
| `WRITE_SETTINGS` | Apply system-level display settings you choose. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Read incoming notifications for the rules engine (see above). |
| `POST_NOTIFICATIONS` | Show the app's own foreground-service and test notifications. |
| `CAMERA` | Control the camera flash LED for flash patterns. The camera is never opened for capture; no photo or video is ever taken or recorded. |
| `WAKE_LOCK` | Wake the screen when a rule asks for it. |
| `SYSTEM_ALERT_WINDOW` | Draw the FPS counter and the full-screen notification flash over other apps. |
| `PACKAGE_USAGE_STATS` | Detect which app is in the foreground for Auto Force-Stop. Usage statistics are read live and are not stored or transmitted. |
| `QUERY_ALL_PACKAGES` | List installed apps so you can pick which ones a rule or exclusion applies to. The list is only rendered on screen; it is never uploaded. |
| `READ_PHONE_STATE` | Read the carrier name for the carrier-name feature. |
| `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE` | Show saved Wi-Fi networks and their credentials **on your device's screen only**. They are read locally through a privileged shell and are never copied off the device. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the notification listener and other background features alive. |
| `RECEIVE_BOOT_COMPLETED` | Restart enabled services after a reboot. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Ask you to exempt the app so battery saver does not kill the listener. |
| `TURN_SCREEN_ON` | Turn the screen on when a notification rule asks for it. |
| `INTERNET` | Download the public lists described in "Network Use". |

## Shizuku and Shell Access

Some features (secure settings, Wi-Fi passwords, the built-in terminal) run shell commands through Shizuku or ADB. These commands execute locally on your device. The app does not send command output anywhere; it is only displayed in the app.

## Network Use

The app makes plain, anonymous HTTPS GET requests for public data files, and only when you use the feature that needs them:

- **Debloater**: the package description list from the [Universal Android Debloater](https://github.com/Universal-Debloater-Alliance) project on `raw.githubusercontent.com`.
- **Link cleaner**: the public tracking-parameter rule list from [ClearURLs](https://rules2.clearurls.xyz).

Nothing about you or your device is sent with these requests apart from what any HTTPS request inherently reveals (your IP address) to those hosts. Links to Shizuku's setup guide and to this project's GitHub page open in your browser only when you tap them.

## Third-Party Services

None. Unlike the upstream Play Store build, this fork has removed Google AdMob, the User Messaging Platform consent SDK and Google Play Billing entirely, and it does not declare the `AD_ID` permission.

## Changes to This Policy

This policy is updated whenever the app's functionality changes in a way that affects it. The "Last Updated" date above always reflects the current version.

## Contact

For questions about this privacy policy, open an issue at https://github.com/riyadmondol2006/CustomAnimator-M3/issues.
