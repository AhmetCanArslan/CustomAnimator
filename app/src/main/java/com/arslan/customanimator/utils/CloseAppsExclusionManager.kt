package com.arslan.customanimator.utils

import android.content.Context

/** Packages the user never wants "Close background apps" to force-stop. */
class CloseAppsExclusionManager(context: Context) : SelectedAppsManager(context, "close_apps_exclusions")
