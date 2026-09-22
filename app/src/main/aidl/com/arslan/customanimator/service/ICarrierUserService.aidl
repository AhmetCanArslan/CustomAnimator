package com.arslan.customanimator.service;

interface ICarrierUserService {
    void destroy() = 16777114;
    String setCarrierName(int subId, String name) = 1;
    String resetCarrierName(int subId) = 2;
}
