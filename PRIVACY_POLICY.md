# Privacy Policy

**App:** CustomAnimator (`com.arslan.customanimator`)
**Last Updated:** November 2026

## Summary

CustomAnimator does not collect, store, or transmit your personal data. There is no account, no login, no analytics, no crash reporting, and no telemetry of any kind. Everything the app creates — presets, rules, notification logs, ignore lists — is written to the app's own private storage on your device and stays there.

No feature of the app sends your data over the network. The app uses the network only for the optional in-app purchase and to download the public Debloater app list from GitHub; both are described in "Third-Party Services" below.

## What the App Stores

All of the following is kept in the app's private storage (`SharedPreferences`), readable only by CustomAnimator:

| Data | Purpose |
| --- | --- |
| Animation, width, battery and terminal presets | Your saved configurations |
| Notification rules and custom flash patterns | Which notifications trigger which action |
| Notification log (app name, package name, title, body, timestamp, matched rules) | The History screen |
| Ignore list (app, title/body patterns) | Notifications you chose to suppress |
| App preferences (last screen, onboarding consent, service toggles) | Restoring the app's state |

None of this is uploaded, shared, or sold, and none of it is sent to any server operated by the developer. If Android Auto Backup is enabled on your device, Google may include the app's preferences in your Google account backup under Google's own privacy policy; the developer has no access to it. You can delete the notification log at any time from **History → Log Settings**, set it to auto-delete after 1/3/7/30 days, or remove everything at once by clearing the app's data or uninstalling it.

## Notification Access

CustomAnimator includes a notification listener service. When you enable it, Android delivers the content of incoming notifications to the app so it can match them against the rules you created and trigger a flash, a screen wake, AOD or a screen flash.

- Notification content is processed **on your device only**, in memory, at the moment the notification arrives.
- A copy is written to the local notification log so the History screen can show it. That log never leaves your device.
- Notification content is never sent over the network, and never shared with the developer or any third party.
- The listener can be turned off at any time from the master switch on the Notifications tab or in Android's system settings, and disabling it stops all processing immediately.

## Permissions

Each permission is used only for the feature named next to it. No permission is used for profiling, tracking, or data collection.

| Permission | Why it is needed |
| --- | --- |
| `WRITE_SECURE_SETTINGS`, `SET_ANIMATION_SCALE` | Change animation scales, screen density and always-on display. Must be granted via ADB or Shizuku; it cannot be granted from inside the app. |
| `WRITE_SETTINGS`, `WRITE_SYNC_SETTINGS` | Apply system-level display and sync settings you choose. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Read incoming notifications for the rules engine (see above). |
| `POST_NOTIFICATIONS`, `POST_PROMOTED_NOTIFICATIONS` | Show the app's own foreground-service, test and Live Update notifications. |
| `WAKE_LOCK` | Wake the screen when a rule asks for it. |
| `SYSTEM_ALERT_WINDOW` | Draw the FPS counter and the full-screen notification flash over other apps. |
| `READ_PHONE_STATE` | Read the carrier name for the carrier-name feature. |
| `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE` | Show saved Wi-Fi networks and their credentials **on your device's screen only**. They are read locally through a privileged shell and are never copied off the device. |
| `INTERNET` | Download the public Debloater app list from GitHub and let Google Play Billing process the optional Premium purchase. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the notification listener, the foreground app watcher and other background features alive. |
| `RECEIVE_BOOT_COMPLETED` | Restart enabled services after a reboot. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Ask you to exempt the app so battery saver does not kill the listener. |
| `WRITE_EXTERNAL_STORAGE` (Android 9 and lower only) | Save exported files to the Download folder. |

The app does not declare `QUERY_ALL_PACKAGES`, `CAMERA` or Usage Access. The list of launchable apps shown when you pick apps for a rule comes from the standard launcher query and is only rendered on screen; it is never uploaded. The flashlight is controlled without the camera permission and no photo or video is ever taken. The foreground app is detected through a Shizuku service on your device, and nothing about it is stored or transmitted.

## Files You Choose

APK extraction and installation, the WhatsApp cleaner and the screenshot actions work on files on your device, through Shizuku shell commands or folders you grant access to. Extracted APKs are saved to the Download/CustomAnimator folder or a folder you choose. Nothing is uploaded.

## Shizuku and Shell Access

Some features (secure settings, Wi-Fi passwords, the built-in terminal) run shell commands through Shizuku or ADB. These commands execute locally on your device. CustomAnimator does not send command output anywhere; it is only displayed in the app.

## Third-Party Services

The app shows no ads and uses no analytics, crash reporting, or tracking SDKs. Two third-party services are involved, and the developer does not control either:

- **Google Play Billing** handles the optional one-time Premium purchase under the Google Privacy Policy — https://policies.google.com/privacy. The developer receives no personal data from it.
- **GitHub** (`raw.githubusercontent.com`) serves the public Universal Android Debloater list used by the Debloater. The app only downloads this file and sends no data about you or your apps; GitHub, like any web server, sees the IP address of the request under GitHub's privacy statement — https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement


## Changes to This Policy

This policy is updated whenever the app's functionality changes in a way that affects it. The "Last Updated" date above always reflects the current version.

## Contact

For questions about this privacy policy, contact the developer through the app store listing or the contact options in the app's settings.
