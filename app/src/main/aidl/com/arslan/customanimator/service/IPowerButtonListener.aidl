package com.arslan.customanimator.service;

oneway interface IPowerButtonListener {
    void onDoublePress();
    void onVolumeTrigger(String trigger);
}
