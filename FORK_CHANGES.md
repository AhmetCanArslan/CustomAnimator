# Changes from the original

Custom Animator M3 is a fork of [**CustomAnimator**](https://github.com/AhmetCanArslan/CustomAnimator) by Ahmet Can Arslan. This page lists everything this fork changes compared with the original project (upstream version 3.8, build 198).

To see the raw diff: [compare upstream with this fork](https://github.com/AhmetCanArslan/CustomAnimator/compare/main...riyadmondol2006:CustomAnimator-M3:main).

- [Identity](#identity)
- [Design](#design)
- [Ads and purchases removed](#ads-and-purchases-removed)
- [Animation scales](#animation-scales)
- [Stability and Android 12–17 compatibility](#stability-and-android-1217-compatibility)
- [Tweaks that did nothing](#tweaks-that-did-nothing)
- [Smaller fixes](#smaller-fixes)
- [Build, toolchain and CI](#build-toolchain-and-ci)
- [Not changed](#not-changed)

## Identity

| | Upstream | Custom Animator M3 |
|---|---|---|
| App name | CustomAnimator | **Custom Animator M3** |
| Package | `com.arslan.customanimator` | **`com.riyadmondol2006.customanimator`** (source, AIDL and JNI moved too) |
| Icon | Traced bitmap mark on black | **New icon**: a dot morphing into a rounded square as it moves (a Material 3 shape morph) on an indigo-to-violet gradient, plus a monochrome layer for Android 13+ themed icons |
| Version | 3.8 (198) | 4.0-M3 (200 + CI run number) |
| Distribution | Google Play | Signed APKs on GitHub Releases |
| Feedback | Developer e-mail | GitHub issues |

Because the package name differs, the fork installs alongside the original app, and settings are not shared between the two.

## Design

The whole app moves from following the wallpaper with top tab chips to its own Material 3 Expressive identity.

- **Own colour palettes**: Aurora (default), Ocean, Ember, Meadow and Graphite, each generated from a fixed seed so the app keeps its look on any wallpaper. **Wallpaper** is still available on Android 12+. Pick one in Settings → Appearance; theme changes animate.
- **Pure black dark mode** for OLED screens.
- **Typeface**: Plus Jakarta Sans (SIL OFL 1.1), bundled as one variable font, used for all text.
- **Floating bottom dock** replaces the row of tab chips under the title. Tabs are icons; the selected one expands into a labelled pill. It gives haptic feedback and hides while the keyboard is open.
- **Home header**: "Custom Animator" wordmark with an **M3** tag.
- **Sub-screens**: circular tonal back button.
- **Section titles**: uppercase, letter-spaced labels with an accent dot, the same on every screen.
- **Cards**: softer corners on grouped list cards.
- **Launch screen**: follows light/dark instead of always flashing black.
- **Animation sliders**: one-tap picks for 0×, 0.25×, 0.35×, 0.5×, 0.75× and 1× under each slider.
- **Preset cards**: preset names no longer wrap letter by letter when a preset has a Quick Settings tile.

## Ads and purchases removed

- Removed **Google AdMob** (banner, interstitial and rewarded ads), the **User Messaging Platform** consent form and **Google Play Billing** ("Remove ads").
- Removed the **`AD_ID` permission** and the AdMob app-ID metadata.
- Features that needed you to watch a rewarded ad now just work: **Game Mode**, **Compile Booster**, the **Optimizer's** booster, cleaner and quick actions, and **adding home-screen shortcuts**.
- Removed the "Support development" onboarding page, the remove-ads bottom prompt, the ad info dialog and the **Play Store rating dialog**.
- No third-party SDKs are left. The privacy policy was rewritten to match.

## Animation scales

The original's write path was already correct for all three scales (an earlier upstream commit fixed `applyAllScales` stopping after the first failure). What still made a control look broken was the screen state:

- **Live sync with the system.** The three scales were read once when the app opened. After a Quick Settings tile, a profile, "Revert everything" or Android's Developer options changed them, the sliders showed old values, and pressing Apply wrote those old values back. The screen now watches the three settings and also refreshes when you return to it.
- **Manual input is validated.** Typing something invalid (for example `7`, `abc` or an empty field) used to be ignored silently, and Apply then wrote the previous value while saying "updated". Invalid fields now show an error and Apply stops with an explanation.
- **Simple mode keeps your values.** Switching to Simple mode used to overwrite the transition and animator values on screen; switching back and applying wrote those overwritten values. Simple mode now only decides what is applied at the moment you press Apply.
- **Preset Load** updates the sliders only after the apply succeeds.
- The full range is **0× (off) to 5×** in 0.01 steps, from the slider, the ± buttons, manual input, presets, profiles and tiles.

## Stability and Android 12–17 compatibility

**Crashes and stuck states**
- Foreground services could crash with `ForegroundServiceStartNotAllowedException` when started while the app was in the background. They now stop themselves cleanly instead. This covers the app watcher, FPS overlay, live activity, compile booster, game mode, boost, power button and screenshot watcher services.
- The battery alert called `startForeground` again on every battery-level change, which crashes in the background on Android 12+. It now only updates its notification.
- The live activity overlay switched itself off permanently if Shizuku was a moment late after the process started. It now waits instead.
- The power-button reader could report "active" after its reader thread had died; it now restarts properly. It also retries connecting after the screen turns on, instead of giving up for good.
- Boost and Game Mode widgets could stay stuck on a spinner forever if the process died mid-run. The stuck state now expires.
- The terminal could stay stuck in "running" after a rotation, and the Performance Booster re-ran (force-stopping apps again) after a rotation.
- Corrupt notification-rule data could crash the notification listener on every notification.

**Security**
- Quick Settings tiles that run shell actions (terminal, toggle, profile, width, animation and restart System UI) now ask you to **unlock the phone first** when tapped on the lock screen.

**Notifications**
- The full-screen **notification flash blocked every touch** on the phone for its whole duration (Android 12+ untrusted-touch rules). It now lets touches through.
- The lock-screen flash could not be dismissed. A tap now closes it.
- Added `TURN_SCREEN_ON`, which Android 14+ requires for **wake-screen rules**.
- Pocket detection could be defeated when two notifications arrived together, waking the screen in a pocket.
- Rules fired twice for group summaries; summaries are now skipped.
- "AOD until dismissed" turned off when any notification from that app was removed. Now only the triggering notification counts.
- The notification listener asks to rebind when Android disconnects it.
- The "suggest filter" regex now escapes special characters, so the suggested filter matches its own notification.
- The notifications widget refreshes its list on the periodic update, so relative times no longer go stale.

**Main-thread work and missing screens**
- Granting secure settings through Shizuku (Permissions screen and setup guide) ran shell commands on the main thread. It now runs in the background.
- Reading the background-process limit no longer blocks the UI.
- Opening the notification-access, overlay, battery-optimisation and write-settings screens no longer crashes on devices that lack them; the app falls back to the app-info screen.

**Older Android versions**
- The screenshot actions overlay works on Android 7.x (it used a window type only available from Android 8).
- Pinning shortcuts is disabled on Android 7.x, where the launcher could never open them.

**Native code**
- Added a bounds check to the native key-state reader (`evdev.c`), which could read outside its buffer for out-of-range key codes.

## Tweaks that did nothing

Each of these reported success but had no effect, or misreported its state. Where possible, the broken behaviour was confirmed on a Pixel 6 running Android 17.

| Tweak | Problem | Fix |
|---|---|---|
| Force RTL layout | Wrote `debug_force_rtl` (wrong key) | Uses `debug.force_rtl` and refreshes the system property |
| Limit background processes | The settings key it wrote is ignored on current Android, and turning it off wiped other `activity_manager_constants` overrides | Uses `device_config activity_manager max_cached_processes` |
| Charging sounds tile | Used the Global namespace instead of Secure | Uses `settings … secure` (existing tiles are migrated) |
| Airplane mode tile | Sent a protected broadcast, so the radios never changed | Uses `cmd connectivity airplane-mode` (existing tiles are migrated) |
| Alarm revealer | Looked for `alarmClock`, but the system prints `Alarm clock:` | Matches the real output |
| Memory compaction | `am compact all full` is not valid syntax | Uses `am compact system` |
| App thread priority | `renice -n` adds to the current value, so priorities stacked and Normal never reset | Sets the target value on every thread |
| Game Mode (off) | Reset every app to "active" and unrestricted, wiping your own battery restrictions | Saves and restores each app's standby bucket and background mode |
| Status bar icons | Toggling one icon erased hidden icons the app didn't know about | Keeps unknown entries |
| Hardware overlays | Showed "disabled" after a reboot even though SurfaceFlinger had reset it | Clears the flag when the boot count changes |
| Doze whitelist | Listed `system-excidle` apps as fully whitelisted | Skips them |

## Smaller fixes

- The ADB command shown in the app hard-coded the package name, so it was wrong for debug builds. It now uses the installed package.
- Battery tweaks that failed now say so instead of failing silently.
- Game Mode's on/off button refreshes when you come back to the app (it can be changed from the widget or tile).
- Width changes arm the 15-second "keep this width?" safety revert before running the command, so it can't be lost when the activity restarts.
- Preset lists and modes reload after restoring a backup or removing a tile.
- Back from Screenshot actions returns to Quick Settings tiles when opened from there.
- Editors (profile, rule, pattern) are no longer reopened blank after the app is closed mid-edit.

## Build, toolchain and CI

| | Upstream | Custom Animator M3 |
|---|---|---|
| Gradle | 9.7.1 | 9.8.2 |
| Android Gradle Plugin | 9.3.0 | 9.5.0 |
| Kotlin | 2.4.20 | 2.4.21 |
| NDK / CMake | 28.2 / 3.22.1 | 30.0 / 4.1.2 |
| Compose BOM | 2026.08.00 | 2026.09.00 |
| Libraries | | core-ktx 1.19.1, gson 2.15.0, zxing 3.5.4, robolectric 4.17 |
| Removed dependencies | | play-services-ads, user-messaging-platform, billing-ktx, fragment-ktx (unused) |

- Gradle configuration cache, build cache and parallel execution are enabled.
- Release signing reads `SIGNING_*` values from the environment or `local.properties`. Without them the release APK is built unsigned instead of the build failing on missing AdMob IDs.
- The dependency-info block is no longer embedded in the APK, which is only useful on Google Play.
- **GitHub Actions** ([`build.yml`](.github/workflows/build.yml)) builds debug and release APKs and runs the unit tests on every push and pull request. Pushes to `main` publish a signed GitHub Release with SHA-256 checksums.

## Not changed

- All features, screens and translations from upstream are kept.
- The minimum Android version is still 7.0 (API 24). The target SDK stays at 36.
- License: GPL-3.0, as upstream.
