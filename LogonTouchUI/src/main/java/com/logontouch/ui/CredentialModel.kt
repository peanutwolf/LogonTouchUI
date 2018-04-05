package com.logontouch.ui

import com.google.gson.Gson
import com.logontouch.helper.AESCipherHelper
import com.logontouch.helper.PKCS12Helper
import sun.security.x509.X500Name
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class CredentialModel{
    private val mCredentialHelper: PKCS12Helper = PKCS12Helper()
    private val mCipherHelper: AESCipherHelper = AESCipherHelper()

    fun getCredentialServiceStatus(): Int{
        var resp = 403

        try {
            val url = URL("http://$LOGONTOUCH_SERVICE_IP:$LOGONTOUCH_SERVICE_PORT/$REQUEST_STATUS_PATH")
            val con = (url.openConnection() as HttpURLConnection).apply {
                this.requestMethod = "GET"
            }
            resp = when(con.responseCode) {
                200  -> 200
                else -> 403
            }

            con.inputStream.close()
            con.disconnect()
        }catch (ex: Exception){
            //any exception leads to unavailable service
        }
        return resp
    }

    fun <T> postCredentialCertificate(certificate: T, postPath: String): Int{
        var resp = 403

        try {
            val url = URL("http://$LOGONTOUCH_SERVICE_IP:$LOGONTOUCH_SERVICE_PORT/$postPath")
            val con = (url.openConnection() as HttpURLConnection).apply {
                this.requestMethod = "POST"
                this.setRequestProperty("Content-Type", "application/json")
                this.doOutput = true
            }

            val jsonCert = Gson().toJson(certificate)
            BufferedOutputStream(con.outputStream).use {
                it.write(jsonCert.toByteArray(Charset.defaultCharset()))
                it.flush()
            }
            resp = con.responseCode

            con.outputStream.close()
            con.disconnect()
        }catch (ex: Exception){
            //any exception leads to unavailable service
        }
        return resp
    }

    fun generateCredentialSecret(domain: String, username: String, password: String): CipheredCredentialHolder{
        val secKey = mCipherHelper.genSecretKey()
        val iv = mCipherHelper.genIV()
        val creds = mCipherHelper.cipherCredentials(domain, username, password, secKey, iv)
        val encodedKey = Base64.getEncoder().encodeToString(secKey)
        val encodedIV = Base64.getEncoder().encodeToString(iv)
        val encodedCreds = Base64.getEncoder().encodeToString(creds)
        return CipheredCredentialHolder(encodedCreds, encodedKey, encodedIV)
    }

    fun generateCredentialCertificates(): ClientCredentialCertificate {
        val (publicKeyEntry, privateKeyEntry) = mCredentialHelper.generatePKCS12CertificatePair(X500Name("CN=logontouch"))

        val publicKeystorePass  = mCredentialHelper.generateRandomPassphrase(20)
        val privateKeystorePass = privateKeyEntry.keyPass

        val publicCertBytes  = mCredentialHelper.serializeKeyStore(publicKeyEntry.keyStore, publicKeystorePass)
        val privateCertBytes = mCredentialHelper.serializeKeyStore(privateKeyEntry.keyStore, privateKeyEntry.keyPass)

        return ClientCredentialCertificate(publicCertBytes, publicKeystorePass, privateCertBytes, privateKeystorePass)
    }

    companion object {
        val LOGONTOUCH_SERVICE_IP = "127.0.0.1"
        val LOGONTOUCH_SERVICE_PORT = "55470"

        val REQUEST_STATUS_PATH = "local/status"
        val REGISTER_PUBLIC_CERTIFICATE  = "local/register/public_cert"
        val REGISTER_PRIVATE_CERTIFICATE = "local/register/private_cert"
    }
}


class CredentialModelDelegate: ReadOnlyProperty<Any?, CredentialModel> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): CredentialModel {
        return model
    }

    companion object {
        private val model = CredentialModel()
    }
}