package com.arslan.customanimator.service;

interface IShortcutUserService {
    void destroy() = 16777114;
    String getShortcutsJson(String packageName) = 1;
    ParcelFileDescriptor openShortcutIcon(String packageName, String shortcutId) = 2;
    String startShortcut(String packageName, String shortcutId) = 3;
}
