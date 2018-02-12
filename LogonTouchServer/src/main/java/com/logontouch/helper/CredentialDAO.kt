package com.logontouch.helper

import java.util.*


data class HostCertificate(
        var sessionHash: String,
        var publicCertificate: ByteArray,
        var publicKeyStoreKey: CharArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HostCertificate

        if (sessionHash != other.sessionHash) return false
        if (!Arrays.equals(publicCertificate, other.publicCertificate)) return false
        if (!Arrays.equals(publicKeyStoreKey, other.publicKeyStoreKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sessionHash.hashCode()
        result = 31 * result + Arrays.hashCode(publicCertificate)
        result = 31 * result + Arrays.hashCode(publicKeyStoreKey)
        return result
    }
}

data class ClientCertificate(private val reqHash: String, val cert: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClientCertificate

        if (reqHash != other.reqHash) return false
        if (!Arrays.equals(cert, other.cert)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = reqHash.hashCode()
        result = 31 * result + Arrays.hashCode(cert)
        return result
    }
}

data class ClientPrivateCertificate (
    val sessionHash: String,
    val privateCertificate: ByteArray,
    val cipheredCredentials: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClientPrivateCertificate

        if (sessionHash != other.sessionHash) return false
        if (!Arrays.equals(privateCertificate, other.privateCertificate)) return false
        if (cipheredCredentials != other.cipheredCredentials) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sessionHash.hashCode()
        result = 31 * result + Arrays.hashCode(privateCertificate)
        result = 31 * result + cipheredCredentials.hashCode()
        return result
    }
}
