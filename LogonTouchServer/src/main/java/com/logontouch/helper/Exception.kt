package com.logontouch.helper

open class InitException(msg: String? = null, reason: Throwable? = null): Exception(msg, reason)

class RegValueNotFoundException(val regValue: String): InitException("Cannot find registry value=[$regValue]")
class ConfigFileNotFoundException(val filePath: String): InitException("Cannot find config file in path=[$filePath]")
class ConfigParseException: InitException("Cannot parse application configuration file")
