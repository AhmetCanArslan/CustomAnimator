package com.riyadmondol2006.customanimator.service;

interface IHotspotUserService {
    void destroy() = 16777114;
    String getStateJson() = 1;
    String applyConfig(String configJson) = 2;
    String setHotspotEnabled(boolean enabled) = 3;
}
