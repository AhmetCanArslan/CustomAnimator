package com.riyadmondol2006.customanimator.service;

import com.riyadmondol2006.customanimator.service.IForegroundAppListener;

interface IAppMonitorUserService {
    void destroy() = 16777114;
    boolean setListener(IForegroundAppListener listener) = 1;
}
