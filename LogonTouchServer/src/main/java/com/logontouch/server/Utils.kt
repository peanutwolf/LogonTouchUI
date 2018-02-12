package com.logontouch.server

import javax.xml.bind.DatatypeConverter

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()
fun ByteArray.toHex() : CharArray {
    val result = StringBuffer()
    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex])
        result.append(HEX_CHARS[secondIndex])
    }
    return result.toList().toCharArray()
}

fun String.toHexByteArray(): ByteArray = DatatypeConverter.parseHexBinary(this)
