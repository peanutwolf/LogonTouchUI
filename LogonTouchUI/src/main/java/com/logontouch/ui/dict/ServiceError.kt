package com.logontouch.ui.dict

enum class ServiceError{
    OK,
    IDLE,
    WAIT,
    SERVER_FAULT,
    CONFIG_ERROR,
    CONFIG_NOT_FOUND_ERROR,
    NOT_REACHABLE,
    ACCESS_DENY;

    companion object {
        var serverIpAddress = ""
        var httpServerPort = 55470
        var httpsServerPort = 55471
        var configPath = ""
    }
}