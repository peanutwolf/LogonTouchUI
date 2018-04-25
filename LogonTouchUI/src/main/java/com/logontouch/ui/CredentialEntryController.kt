package com.logontouch.ui

import com.google.gson.Gson
import com.logontouch.helper.*
import com.logontouch.server.LogonTouchServer
import com.logontouch.helper.ConfigFileNotFoundException
import com.logontouch.helper.ConfigParseException
import com.logontouch.helper.RegValueNotFoundException
import com.logontouch.ui.dict.ServiceError
import javafx.application.Platform
import net.glxn.qrgen.javase.QRCode
import org.apache.commons.lang3.RandomStringUtils
import org.apache.logging.log4j.LogManager
import tornadofx.Controller
import java.io.FileNotFoundException
import java.net.InetAddress
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class CredentialEntryController: Controller(){
    private lateinit var mLogonTouchServer: LogonTouchServer
    private val mCredentialModel: CredentialModel by CredentialModelDelegate()
    private val mCredentialView: CredentialsGenerateView by inject()
    private val logger = LogManager.getLogger(CredentialEntryController::class.java)

    init {
        val current = getWinLogonAccount()
        if(current != null){
            mCredentialView.mUserText.text = current
        }
    }

    private fun tryInit(){
        mCredentialView.showServiceStatus(ServiceError.WAIT)

        deInit()

        mLogonTouchServer = LogonTouchServer()

        val cfgPath = getWinRegPathValue("SOFTWARE\\LazyGravity\\LogonTouchUI","Config")
                ?: throw RegValueNotFoundException("Config")

        val serverCfg = LogonTouchConfigParser(cfgPath)
                .also {
                    try {
                        it.parseConfig()
                    }catch(fileNotFound: FileNotFoundException){
                        throw ConfigFileNotFoundException(cfgPath.toString())
                    }
                }
                .apply {
                    ServiceError.configPath = cfgPath.toString()
                }
                .getServerConfig()
                ?.apply {
                    ServiceError.serverIpAddress = InetAddress.getLocalHost().hostAddress
                    ServiceError.httpServerPort  = httpPort
                    ServiceError.httpsServerPort = httpsPort
                    mCredentialModel.logonTouchPort = httpPort
                } ?: throw ConfigParseException()

        mLogonTouchServer.serverAssemble(serverCfg, this::onCertificateUpload)
        Thread{
            mLogonTouchServer.serverStart({ Platform.runLater{
                checkServiceAvailable()
            }
            }, {
                logger.error("Failed to start LogonTouchServer\n", it)
                mLogonTouchServer.serverStop()
                Platform.runLater {
                    mCredentialView.showServiceStatus(ServiceError.SERVER_FAULT)
                }
            })
        }.start()
    }

    fun init() {

        try {
            tryInit()
        }catch (ex: Throwable){
            logger.error("Failed to initialize CredentialEntryController\n", ex)
            when(ex){
                is RegValueNotFoundException -> mCredentialView.showServiceStatus(ServiceError.CONFIG_ERROR)
                is ConfigFileNotFoundException -> {
                    ServiceError.configPath = ex.filePath
                    mCredentialView.showServiceStatus(ServiceError.CONFIG_NOT_FOUND_ERROR)
                }
                is ConfigParseException -> mCredentialView.showServiceStatus(ServiceError.CONFIG_ERROR)
                else -> mCredentialView.showServiceStatus(ServiceError.CONFIG_ERROR)
            }
        }


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
            mCredentialView.showServiceStatus(ServiceError.CERT_UPLOADED)
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

        val sessionHash = RandomStringUtils.random(10, true, true)

        //send public certificate to server for client verification
        with(HostCertificate(sessionHash, certs.publicCertificate, certs.publicKeyStoreKey)){
            mCredentialModel.postCredentialCertificate(this, CredentialModel.REGISTER_PUBLIC_CERTIFICATE)
        }

        //send private certificate and ciphered credentials to server for client to fetch it from
        with(ClientPrivateCertificate(sessionHash,
                certs.privateCertificate,
                credsHolder.encodedCreds)) {
            mCredentialModel.postCredentialCertificate(this, CredentialModel.REGISTER_PRIVATE_CERTIFICATE)
        }

        return ClientSecretKeys(sessionHash,
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
