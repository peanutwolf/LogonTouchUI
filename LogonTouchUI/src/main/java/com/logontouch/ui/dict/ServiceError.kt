package com.logontouch.ui.dict

enum class ServiceError{
    OK,
    IDLE,
    WAIT,
    SERVER_FAULT,
    CONFIG_ERROR,
    NOT_REACHABLE,
    ACCESS_DENY;

    var mHTTPServerPort = 8080
    var mHTTPSServerPort = 7779


}