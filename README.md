# Custom Animator

Custom Animator is an Android tweaking app for things Android normally buries in developer settings. It started as a way to fine-tune animation scales and display density (DPI) with custom values and presets, and has grown into a small toolbox of per-app and system-level tweaks — all without root.

<div align="center">
  <a href="https://play.google.com/store/apps/details?id=com.arslan.customanimator">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="300">
  </a>
</div>

## Animation & Display

- **Precise Control**: Set animation scales to any value, not just the standard 0.5x, 1x, 1.5x steps.
- **Slider Mode**: Quick adjustment with sliders for Window, Transition and Animator scales.
- **Manual Input Mode**: Type exact values into numeric fields.
- **Preset System**: Save your favorite configurations and switch between them instantly.
- **DPI Changer**: Adjust display density. Some OEMs block this, so there's a Shizuku fallback — with Shizuku you can change DPI on any device.
- **Width Presets**: Save and restore "smallest width" (dp) configurations.

## Developer Tools

- **Developer Options**: Toggle ADB debugging (USB / wireless), "Don't Keep Activities" and "Limit Background Processes" from inside the app.
- **Extra Tweaks**: Disable keyboard animations, show seconds in the status bar clock, and force screen rotation.
- **Auto Force-Stop**: Force-stops selected apps as soon as they go to the background, via a background service.
- **Auto Permission Disabler**: Revokes permissions of selected apps when they go to the background, and restores them when you reopen them.
- **Compile Booster**: Runs AOT compilation across installed apps to improve launch and runtime performance, with live progress.
- **Graphics API Override**: Choose ANGLE or the native graphics driver per app.
- **Terminal**: Built-in terminal with saved command presets.

## Getting Started

The app's core features need the `WRITE_SECURE_SETTINGS` permission, granted once, to modify system animation scales, DPI and developer settings.

### Option 1: Shizuku (Recommended — one-time setup)

If [Shizuku](https://shizuku.rikka.app/) is installed and running, the app can request and grant the required permission automatically. Shizuku only grants the permission; it does nothing else with it.

### Option 2: ADB (one-time setup)

Without Shizuku, grant the permission manually over ADB:

```bash
adb shell pm grant com.arslan.customanimator android.permission.WRITE_SECURE_SETTINGS
```

### Other permissions

The background features ask for a few extra permissions only when you enable them:

- `PACKAGE_USAGE_STATS` — detect when a monitored app moves to the background (Auto Force-Stop, Auto Permission Disabler).
- `FOREGROUND_SERVICE` / `POST_NOTIFICATIONS` — keep those background services alive and show their ongoing notification.

## Building

Requires JDK 11+ and Android SDK 36 (minSdk 24). Two product flavors:

| Flavor | Notes |
| --- | --- |
| `github` | Ad-free build, distributed here on GitHub. |
| `playstore` | Play Store build, includes AdMob banners. Needs `admobAppId` / `admobBannerId` in `local.properties`. |

```bash
./gradlew assembleGithubRelease
```


## Support

- **Buy Me a Coffee**: [Support Development](https://buymeacoffee.com/ahmetcanarslan)

## Privacy

Custom Animator does not collect personal data or usage analytics; presets and settings stay on your device. The Play Store build serves AdMob banner ads, which are subject to Google's own data handling. The GitHub build contains no ads or SDKs of that kind. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for details.

## License

Licensed under the GNU General Public License v3.0 — see [LICENSE](LICENSE).

---

**Note**: This app modifies system-level settings and can force-stop or alter other apps. Use with caution and test on your own device first.
