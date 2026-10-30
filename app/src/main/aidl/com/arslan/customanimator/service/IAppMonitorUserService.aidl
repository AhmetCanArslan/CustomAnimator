package com.arslan.customanimator.service;

import com.arslan.customanimator.service.IForegroundAppListener;

interface IAppMonitorUserService {
    void destroy() = 16777114;
    boolean setListener(IForegroundAppListener listener) = 1;
}
