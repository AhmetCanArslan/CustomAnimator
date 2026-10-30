package com.riyadmondol2006.customanimator.service;

oneway interface IForegroundAppListener {
    void onForegroundActivitiesChanged(int pid, in String[] packages, boolean foreground);
    void onProcessDied(int pid);
}
