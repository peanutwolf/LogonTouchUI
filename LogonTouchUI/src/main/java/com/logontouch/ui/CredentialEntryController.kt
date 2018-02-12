package com.logontouch.ui

import com.google.gson.Gson
import com.logontouch.helper.*

import com.logontouch.server.LogonTouchServer
import com.logontouch.ui.dict.ServiceError
import javafx.application.Platform
import net.glxn.qrgen.javase.QRCode
import tornadofx.Controller
import java.util.*
import java.util.concurrent.Callable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class CredentialEntryController: Controller(){
    private lateinit var mLogonTouchServer: LogonTouchServer
    private val mCredentialModel: CredentialModel by CredentialModelDelegate()
    private val mCredentialView: CredentialsGenerateView by inject()
    private val mSessionHashMock = "1234567"

    init {
        val current = getWinLogonAccount()
        if(current != null){
            mCredentialView.mUserText.text = current
        }
    }

    fun init() {
        mCredentialView.showServiceStatus(ServiceError.WAIT)

        deInit()

        mLogonTouchServer = LogonTouchServer()

        val cfgPath = getWinRegPathValue("SOFTWARE\\LogonTouch","Config")
        if (cfgPath == null){
            mCredentialView.showServiceStatus(ServiceError.CONFIG_ERROR)
            return
        }
        val serverCfg = LogonTouchConfigParser(cfgPath)
                .also { it.parseConfig() }
                .getServerConfig()

        if (serverCfg == null){
            mCredentialView.showServiceStatus(ServiceError.CONFIG_ERROR)
            return
        }

        mLogonTouchServer.serverAssemble(serverCfg, this::onCertificateUpload)
        Thread{
            mLogonTouchServer.serverStart({
                Platform.runLater{
                    checkServiceAvailable()
                }
            }, {
                mLogonTouchServer.serverStop()
                Platform.runLater {
                    mCredentialView.showServiceStatus(ServiceError.SERVER_FAULT)
                }
            })
        }.start()

    }

    fun deInit(){
        if(this::mLogonTouchServer.isInitialized.not())
            return
        mLogonTouchServer.serverStop()
    }


    private fun onCertificateUpload() {
        runAsync {
            deInit()
        }ui {
            mCredentialView.showServiceStatus(ServiceError.IDLE)
            mCredentialView.hideCredentialQR()
        }
    }

    fun onCredentialEntry(username: String, password: String) {
        runAsync {
            val (domain, login ) = username.let {
                return@let Pair(it.substringBefore('\\', ""),
                                 it.substringAfter('\\'))
            }
            return@runAsync processUserCredentials(domain, login, password).let {
                genSecretKeysQR(it)
            }
        } ui {
            mCredentialView.showCredentialQR(it)
        }
    }

    private fun processUserCredentials(domain: String, username: String, password: String): ClientSecretKeys{
        val credsHolder = mCredentialModel.generateCredentialSecret(domain, username, password)
        val certs = mCredentialModel.generateCredentialCertificates()

        //send public certificate to server for client verification
        var res = with(HostCertificate(mSessionHashMock, certs.publicCertificate, certs.publicKeyStoreKey)){
            mCredentialModel.postCredentialCertificate(this, CredentialModel.REGISTER_PUBLIC_CERTIFICATE)
        }

        println("Post public certs res=[$res]")

        //send private certificate and ciphered credentials to server for client to fetch it from
        res = with(ClientPrivateCertificate(mSessionHashMock,
                certs.privateCertificate,
                credsHolder.encodedCreds)) {
            mCredentialModel.postCredentialCertificate(this, CredentialModel.REGISTER_PRIVATE_CERTIFICATE)
        }

        println("Post private certs res=[$res]")

        return ClientSecretKeys(mSessionHashMock,
                certs.privateKeyStoreKey.joinToString(separator = ""),
                credsHolder.encodedSecretKey,
                credsHolder.encodedIV)
    }

    private fun genSecretKeysQR(secretKeys: ClientSecretKeys): ByteArray{

        val jsonSecret = Gson().toJson(secretKeys)
        val qrArray = QRCode.from(jsonSecret).withSize(300, 300).stream().toByteArray()

        return qrArray
    }

    private fun checkServiceAvailable(){
        runAsync {
            mCredentialModel.getCredentialServiceStatus()
        } ui { status ->
            when(status){
                200 -> mCredentialView.showServiceStatus(ServiceError.OK)
                else ->  mCredentialView.showServiceStatus(ServiceError.NOT_REACHABLE)
            }
        }
    }

}

class LogonTouchServerDelegate: ReadOnlyProperty<Any?, LogonTouchServer> {
    fun updateInstance(){
        model = LogonTouchServer()
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): LogonTouchServer {
        return model
    }

    companion object {
        private var model = LogonTouchServer()
    }
}