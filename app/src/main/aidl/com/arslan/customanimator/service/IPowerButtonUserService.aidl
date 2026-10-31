package com.arslan.customanimator.service;

import com.arslan.customanimator.service.IPowerButtonListener;

interface IPowerButtonUserService {
    void destroy() = 16777114;
    boolean start(IPowerButtonListener listener, int doublePressWindowMs, in String[] triggers) = 1;
    boolean setGrabbed(boolean interactive) = 2;
}
