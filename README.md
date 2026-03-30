# Custom Animator

Custom Animator is an Android application designed to simplify fine-tuning your device's animation scales and display density (DPI). Instead of navigating through deep developer settings and being limited to standard presets, this app allows you to set custom values and create your own presets.

<div align="center">
  <div style="border: 1px solid #d0d7de; border-radius: 12px; padding: 24px; display: inline-block; max-width: 400px;">
    <h3 style="margin-top: 0;">App Preview</h3>
    <img src="art/customAnimator.gif" width="220" style="border-radius: 8px; margin-bottom: 20px;">
    <br>
    <a href="https://play.google.com/store/apps/details?id=com.arslan.customanimator">
      <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="300">
    </a>
  </div>
</div>

## Features

- **Precise Control**: Set animation scales to any value, not just the standard 0.5x, 1x, 1.5x, etc.
- **Slider Mode**: Quick adjustment with intuitive sliders for Window, Transition and Animator scales.
- **Manual Input Mode**: Control with numeric input fields.
- **Preset System**: Save your favorite configurations and switch between them instantly.
- **DPI Changer**: Adjust display density. Note: Changing DPI is not allowed on some OEMS, so i've added shizuku fallback for that. If you have shizuku, you can change DPI on any device.
- **Shizuku Support**: Automatically grant necessary permissions for once if Shizuku is running.

## Getting Started

To function correctly, the app requires the `WRITE_SECURE_SETTINGS` permission only once to modify system animation scales and DPI settings. **No other permissions are required.**

### Option 1: Shizuku (Recommended - One Time Setup)
If you have [Shizuku](https://shizuku.rikka.app/) installed and running, the app can automatically request and grant the required permission. Shizuku will only grant the permission. It doesn't do anything else with it.

### Option 2: ADB (One Time Setup)
If you don't use Shizuku, you can grant the permission manually via ADB once:

```bash
adb shell pm grant com.arslan.customanimator android.permission.WRITE_SECURE_SETTINGS
```

## Support

- **Buy Me a Coffee**: [Support Development](https://buymeacoffee.com/ahmetcanarslan)

## Privacy Policy

Custom Animator does not collect any personal data or usage analytics. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for full details.

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

---

**Note**: This app modifies system-level animation and display settings. Use with caution and test thoroughly on your device.
