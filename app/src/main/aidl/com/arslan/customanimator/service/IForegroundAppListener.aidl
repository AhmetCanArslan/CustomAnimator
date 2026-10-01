package com.arslan.customanimator.service;

oneway interface IForegroundAppListener {
    void onForegroundActivitiesChanged(int pid, in String[] packages, boolean foreground);
    void onProcessDied(int pid);
}
