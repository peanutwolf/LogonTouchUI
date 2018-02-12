package com.logontouch.helper

import com.google.gson.Gson
import com.logontouch.mapping.ApplicationConfig
import com.logontouch.mapping.ServerConfig
import java.nio.file.Path


class LogonTouchConfigParser(private val mConfigPath: Path){
    private var mApplicationConfig: ApplicationConfig? = null

    fun parseConfig(){
        mApplicationConfig = Gson().fromJson(mConfigPath.toFile().reader(), ApplicationConfig::class.java)
    }

    fun getServerConfig(): ServerConfig? = mApplicationConfig?.serverConfig

}