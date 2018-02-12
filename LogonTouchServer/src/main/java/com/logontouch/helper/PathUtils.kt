package com.logontouch.helper

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

fun checkServerFileExist(filePath: String = "", fileName: String): Boolean =
        checkServerFileExist(filePath + fileName)

fun checkServerFileExist(fileFullPath: String): Boolean =
        File(fileFullPath).let{it.exists() && it.isFile}

fun deleteServerFile(filePath: String = "", fileName: String) =
        deleteServerFile(filePath + fileName)

fun deleteServerFile(fileFullPath: String) =
        File(fileFullPath).takeIf{it.exists() && it.isFile}?.delete()

fun parsePath(path: String): Path {
    return try {
        Paths.get(path)
    } catch (ex: InvalidPathException) {
        Paths.get("")
    } catch (ex: NullPointerException) {
        Paths.get("")
    }
}
//"SOFTWARE\\LogonTouch","Config"  config
//"SOFTWARE\\LogonTouch",""  install
fun getWinRegPathValue(regPath: String, param: String = ""): Path?  {
    val path = Advapi32Util.registryGetStringValue(HKEY_LOCAL_MACHINE, regPath, param)
    return parsePath(path)
}

fun getWinLogonAccount(): String?{
    val name = Advapi32Util.getUserName()
            ?.run {
                Advapi32Util.getAccountByName(this)
            }
            ?.let {
                it.fqn
            }

    return name
}
