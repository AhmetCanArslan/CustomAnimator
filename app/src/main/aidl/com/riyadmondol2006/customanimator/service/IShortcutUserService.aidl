package com.riyadmondol2006.customanimator.service;

import android.app.PendingIntent;

interface IShortcutUserService {
    void destroy() = 16777114;
    String getShortcutsJson(String packageName) = 1;
    ParcelFileDescriptor openShortcutIcon(String packageName, String shortcutId) = 2;
    PendingIntent getShortcutIntent(String packageName, String shortcutId) = 3;
}
