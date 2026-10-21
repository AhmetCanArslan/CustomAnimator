package com.arslan.customanimator.service;

import com.arslan.customanimator.service.IPowerButtonListener;

interface IPowerButtonUserService {
    void destroy() = 16777114;
    boolean start(IPowerButtonListener listener, int multiPressTimeoutMs) = 1;
    boolean setGrabbed(boolean grabbed) = 2;
}
