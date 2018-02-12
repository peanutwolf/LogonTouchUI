package com.logontouch.helper

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import org.apache.commons.lang3.RandomStringUtils
import sun.security.tools.keytool.CertAndKeyGen
import sun.security.x509.X500Name
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.security.Key
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


enum class KeyStoreEntryType{
    PUBLIC_KEYSTORE,
    PRIVATE_KEYSTORE,
}

data class KeyStoreEntry(val type: KeyStoreEntryType, val keyStore: KeyStore, val keyPass: CharArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyStoreEntry

        if (keyStore != other.keyStore) return false
        if (!Arrays.equals(keyPass, other.keyPass)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyStore.hashCode()
        result = 31 * result + Arrays.hashCode(keyPass)
        return result
    }
}

class AESCipherHelper{
    private val ALGORITHM = "AES"
    private val AES_CBS_PADDING = "AES/CBC/PKCS5Padding"
    private val mKeyGenerator =  KeyGenerator.getInstance(ALGORITHM).also { it.init(128) }

    fun genSecretKey(): ByteArray = mKeyGenerator.generateKey().encoded

    fun genIV(): ByteArray = mKeyGenerator.generateKey().encoded

    fun cipherCredentials(domain: String, username: String, password: String, secKey: ByteArray, iv: ByteArray): ByteArray{
        val aesCipher = Cipher.getInstance(AES_CBS_PADDING)
        val secKeySpec = SecretKeySpec(secKey, ALGORITHM)
        val ivSpec = IvParameterSpec(iv)
        aesCipher.init(Cipher.ENCRYPT_MODE, secKeySpec, ivSpec)
        val jsonView = """
            {
            "domain"  : "$domain",
            "username": "$username",
            "password": "$password"
            }
        """.trimIndent().toByteArray(Charset.defaultCharset())

        return aesCipher.doFinal(jsonView)
    }
    
}

class PKCS12Helper{

    fun generatePKCS12CertificatePair(x500Name: X500Name): Pair<KeyStoreEntry, KeyStoreEntry> {
        val p12keystorePublic  = KeyStore.getInstance("PKCS12")
        val p12keystorePrivate = KeyStore.getInstance("PKCS12")

        val certChainGen = CertAndKeyGen("RSA", "SHA256withRSA").apply {
            this.generate(2048)
        }

        val publicStorePass = RandomStringUtils.random(20, true, true).toCharArray()
        val privateStorePass = "keystore.key".toCharArray()//RandomStringUtils.random(20, true, true).toCharArray()
        
        val certTmp: X509Certificate = certChainGen.getSelfCertificate(x500Name, 365*24*3600)

        p12keystorePublic.load(null, null)
        p12keystorePrivate.load(null, null)

        p12keystorePublic.setCertificateEntry("${x500Name.commonName}-public", certTmp)
        p12keystorePrivate.setKeyEntry("${x500Name.commonName}-private", certChainGen.privateKey, privateStorePass, arrayOf(certTmp))

        val publicEntry  = KeyStoreEntry(KeyStoreEntryType.PUBLIC_KEYSTORE , p12keystorePublic, publicStorePass)
        val privateEntry = KeyStoreEntry(KeyStoreEntryType.PRIVATE_KEYSTORE, p12keystorePrivate, privateStorePass)

        return Pair(publicEntry, privateEntry)
    }

    fun serializeKeyStore(keyStore: KeyStore, password: CharArray): ByteArray{
        return ByteArrayOutputStream().let {
            keyStore.store(it, password)
            it.toByteArray()
        }
    }

    fun deserializeKeyStore(bytes: ByteArray, password: CharArray): KeyStore{
        val p12Keystore = KeyStore.getInstance("PKCS12")
        ByteArrayInputStream(bytes).use {
            p12Keystore.load(it, password)
        }
        return p12Keystore
    }

    fun generateRandomPassphrase(len: Int): CharArray =
        RandomStringUtils.random(len, true, true).toCharArray()

}