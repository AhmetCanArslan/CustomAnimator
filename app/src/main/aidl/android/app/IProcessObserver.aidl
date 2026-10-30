package android.app;

oneway interface IProcessObserver {
    void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities);
    void onForegroundServicesChanged(int pid, int uid, int serviceTypes);
    void onProcessStateChanged(int pid, int uid, int procState);
    void onProcessStarted(int pid, int processUid, int packageUid, String packageName, String processName);
    void onProcessDied(int pid, int uid);
}
