package com.logontouch.server

import com.logontouch.helper.PKCS12Helper
import com.logontouch.helper.checkServerFileExist
import com.logontouch.helper.deleteServerFile
import com.logontouch.helper.getWinRegPathValue
import com.logontouch.mapping.ServerConfig
import com.logontouch.server.rest.*
import com.logontouch.server.rest.utils.GsonProvider
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.server.handler.InetAccessHandler
import org.eclipse.jetty.server.handler.SecuredRedirectHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.servlet.ServletContainer
import sun.security.x509.X500Name
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths


fun <R> File.useOutputWithDirs(block: (FileOutputStream) -> R): R{
    this.parentFile?.mkdirs()
    this.createNewFile()
    return this.outputStream().use(block)
}

fun <R> File.useWriterWithDirs(block: (BufferedWriter) -> R): R{
    this.parentFile?.mkdirs()
    this.createNewFile()
    return this.bufferedWriter().use(block)
}

fun <R> File.useReaderWithDirs(block: (BufferedReader) -> R): R{
    this.parentFile?.mkdirs()
    this.createNewFile()
    return this.bufferedReader().use(block)
}

class LogonTouchServer{
    private val mJettyServer: Server = Server()
    private val httpConfig: HttpConfiguration = HttpConfiguration()

    init {
        mJettyServer.stopAtShutdown = true
        httpConfig.securePort = SERVER_HTTPS_PORT
        httpConfig.secureScheme = "https"
    }

    private fun initServerSSLPrerequisites(): Boolean{
        if(checkServerFileExist(SERVER_PRIVATE_KEYSTORE_FULL_PATH)
                && checkServerFileExist(SERVER_PRIVATE_PASSPHRASE_FULL_PATH)
                && checkServerFileExist(SERVER_PUBLIC_KEYSTORE_FULL_PATH)
                && checkServerFileExist(SERVER_PUBLIC_PASSPHRASE_FULL_PATH)) return true

        deleteServerFile(SERVER_PRIVATE_KEYSTORE_FULL_PATH)
        deleteServerFile(SERVER_PRIVATE_PASSPHRASE_FULL_PATH)
        deleteServerFile(SERVER_PUBLIC_KEYSTORE_FULL_PATH)
        deleteServerFile(SERVER_PUBLIC_PASSPHRASE_FULL_PATH)

        val (publicCert, privateCert) = PKCS12Helper()
                .generatePKCS12CertificatePair(X500Name("CN=ServerSelf, OU=LogonTouchAuth, O=LogonTouch, C=RU"))

        File(SERVER_PRIVATE_KEYSTORE_FULL_PATH).useOutputWithDirs { fos ->
            privateCert.keyStore.store(fos, privateCert.keyPass)
        }
        File(SERVER_PRIVATE_PASSPHRASE_FULL_PATH).useWriterWithDirs { fis ->
            fis.write(privateCert.keyPass)
        }
        File(SERVER_PUBLIC_KEYSTORE_FULL_PATH).useOutputWithDirs { fos ->
            publicCert.keyStore.store(fos, publicCert.keyPass)
        }
        File(SERVER_PUBLIC_PASSPHRASE_FULL_PATH).useWriterWithDirs { fis ->
            fis.write(publicCert.keyPass)
        }
        return true
    }

    private fun createHTTPConnector(jettyServer: Server): ServerConnector{
        val httpConnector = ServerConnector(jettyServer, HttpConnectionFactory(httpConfig))
        httpConnector.port = SERVER_HTTP_PORT
        httpConnector.name = "HTTPApi"

        return httpConnector
    }

    private fun initLogonTouchSSLContextFactory(): SslContextFactory{
        val sslContextFactory = SslContextFactory()

        sslContextFactory.setIncludeProtocols("TLSv1.2", "TLSv1")
        sslContextFactory.setIncludeCipherSuites("^.*_(SHA|SHA1|SHA256)\$")
        sslContextFactory.setExcludeCipherSuites("^.*_(MD5)\$")

        val serverKey = File(SERVER_PRIVATE_PASSPHRASE_FULL_PATH).useReaderWithDirs {
            it.readLine()
        }

        sslContextFactory.keyStorePath = SERVER_PRIVATE_KEYSTORE_FULL_PATH
        sslContextFactory.setKeyStorePassword(serverKey)
        sslContextFactory.setKeyManagerPassword(serverKey)
        if(checkServerFileExist(CLIENT_PUBLIC_KEYSTORE_FULL_PATH) && checkServerFileExist(CLIENT_PUBLIC_PASSPHRASE_FULL_PATH)){
            File(CLIENT_PUBLIC_PASSPHRASE_FULL_PATH).useReaderWithDirs {
                sslContextFactory.setTrustStorePath(CLIENT_PUBLIC_KEYSTORE_FULL_PATH)
                sslContextFactory.setTrustStorePassword(it.readLine())
            }
        }
        sslContextFactory.needClientAuth = true
        sslContextFactory.wantClientAuth = true

        return sslContextFactory
    }

    private fun createHTTPSConnector(jettyServer: Server, sslContextFactory: SslContextFactory): ServerConnector{
        val https = HttpConfiguration(httpConfig)

        https.addCustomizer(SecureRequestCustomizer())

        val sslConnector = ServerConnector(jettyServer,
                SslConnectionFactory(sslContextFactory, "http/1.1"),
                HttpConnectionFactory(https))
        sslConnector.port = SERVER_HTTPS_PORT
        sslConnector.name = "HTTPSApi"

        return sslConnector
    }

    private fun createGlobalHTTPContext(sessionContext: SessionContext): Handler{
        val httpResConfig = ResourceConfig()
        httpResConfig.register(ProvideClient(sessionContext))

        val httpContext = ServletContextHandler(ServletContextHandler.SESSIONS)
        val contextHolder = ServletHolder(ServletContainer(httpResConfig))

        httpContext.addServlet(contextHolder, "/*")
        httpContext.contextPath = "/external"
        httpContext.virtualHosts = arrayOf("@HTTPApi")

        return httpContext
    }

    private fun createLocalHTTPContext(sslContextFactory: SslContextFactory, sessionContext: SessionContext): Handler{
        val httpResConfig = ResourceConfig()
        httpResConfig.register(RegisterClient(sslContextFactory, sessionContext))
        httpResConfig.register(ServerHTTPStatus())
        httpResConfig.register(GsonProvider::class.java)

        val httpContext = ServletContextHandler(ServletContextHandler.SESSIONS)
        val contextHolder = ServletHolder(ServletContainer(httpResConfig))

        httpContext.addServlet(contextHolder, "/*")
        httpContext.contextPath = "/local"
        httpContext.virtualHosts = arrayOf("@HTTPApi")

        val local = InetAccessHandler()

        local.exclude("0.0.0.0/32")
        local.include("127.0.0.1", "192.168.10.68")
        local.handler = httpContext

        return local
    }

    private fun createHTTPSContext(): ServletContextHandler{
        val httpsResConfig = ResourceConfig()
        httpsResConfig.register(SecureServlets())

        val httpsContext = ServletContextHandler(ServletContextHandler.SESSIONS)
        val contextHolder = ServletHolder(ServletContainer(httpsResConfig))

        httpsContext.addServlet(contextHolder, "/*")
        httpsContext.virtualHosts = arrayOf("@HTTPSApi")

        return httpsContext
    }

    fun serverAssemble(mServerConfig: ServerConfig, onCertificateUpload: onCertificateUploadCB){
        LogonTouchServer.mConfig = mServerConfig

        initServerSSLPrerequisites()
        val sslContextFactory = initLogonTouchSSLContextFactory()

        val mutualSessionContext = SessionContext(onCertificateUpload)
        val httpConnector = createHTTPConnector(mJettyServer)
        val httpsConnector = createHTTPSConnector(mJettyServer, sslContextFactory)
        mJettyServer.addConnector(httpConnector)
        mJettyServer.addConnector(httpsConnector)

        val redirectHandler = ContextHandler()
        redirectHandler.contextPath = "/secure"
        redirectHandler.handler = SecuredRedirectHandler()
        redirectHandler.virtualHosts = arrayOf("@HTTPApi")

        val handlerList = ContextHandlerCollection()
        handlerList.handlers = arrayOf(
                redirectHandler,
                createLocalHTTPContext(sslContextFactory, mutualSessionContext),
                createGlobalHTTPContext(mutualSessionContext),
                createHTTPSContext()
        )
        mJettyServer.handler = handlerList
    }

    fun serverStart(onStart: ()->Unit, onError: (ex: Throwable)->Unit){
        try{
            mJettyServer.start().also {
                when(mJettyServer.isStarted){
                    true -> onStart()
                    else -> onError(Exception("Failed to Start JettyServer"))
                }
            }
            mJettyServer.join()
        }catch (ex : Exception){
            onError(ex)
        }
    }

    fun serverStop(){
        mJettyServer.stop()
    }

    companion object {
        val LOGONTOUCH_SERVICE_FULL_PATH = getWinRegPathValue("SOFTWARE\\LogonTouch").toString()
        var mConfig: ServerConfig = ServerConfig()
        set(value) {
            field = value
            SERVER_PRIVATE_KEYSTORE_FULL_PATH =
                    Paths.get(LOGONTOUCH_SERVICE_FULL_PATH, value.keysDirFolder, value.serverKeysDirFolder, value.serverPrivateStore).toString()
            SERVER_PRIVATE_PASSPHRASE_FULL_PATH =
                    Paths.get(LOGONTOUCH_SERVICE_FULL_PATH, value.keysDirFolder, value.serverKeysDirFolder, value.serverPrivatePass).toString()
            SERVER_PUBLIC_KEYSTORE_FULL_PATH =
                    Paths.get(LOGONTOUCH_SERVICE_FULL_PATH, value.keysDirFolder, value.serverKeysDirFolder, value.serverPublicStore).toString()
            SERVER_PUBLIC_PASSPHRASE_FULL_PATH =
                    Paths.get(LOGONTOUCH_SERVICE_FULL_PATH, value.keysDirFolder, value.serverKeysDirFolder, value.serverPublicPass).toString()
            CLIENT_PUBLIC_KEYSTORE_FULL_PATH =
                    Paths.get(LOGONTOUCH_SERVICE_FULL_PATH, value.keysDirFolder, value.clientKeysDirFolder, value.clientPublicStore).toString()
            CLIENT_PUBLIC_PASSPHRASE_FULL_PATH =
                    Paths.get(LOGONTOUCH_SERVICE_FULL_PATH, value.keysDirFolder, value.clientKeysDirFolder, value.clientPublicPass).toString()
            CLIENT_CREDENTIALS_FULL_PATH =
                    Paths.get(LOGONTOUCH_SERVICE_FULL_PATH, value.keysDirFolder, value.clientKeysDirFolder, value.clientCredentials).toString()
        }

        init {
            mConfig = defaultServerConfig()
        }

        var SERVER_PRIVATE_KEYSTORE_FULL_PATH   = ""
        var SERVER_PRIVATE_PASSPHRASE_FULL_PATH = ""
        var SERVER_PUBLIC_KEYSTORE_FULL_PATH    = ""
        var SERVER_PUBLIC_PASSPHRASE_FULL_PATH  = ""

        var CLIENT_PUBLIC_KEYSTORE_FULL_PATH    = ""
        var CLIENT_PUBLIC_PASSPHRASE_FULL_PATH  = ""
        var CLIENT_CREDENTIALS_FULL_PATH        = ""

        val SERVER_HTTP_PORT: Int
            get() = mConfig.httpPort
        val SERVER_HTTPS_PORT: Int
            get() =  mConfig.httpsPort

        private fun defaultServerConfig(): ServerConfig{
            val config = ServerConfig()
            config.httpPort = 8080
            config.httpsPort = 7779
            config.version   = "0.1"
            config.keysDir.path = ""
            config.keysDir.clientKeysDir.path = ""
            config.keysDir.clientKeysDir.publicStore = "clientstore.pkcs12"
            config.keysDir.clientKeysDir.publicPass = "clientstore.key"
            config.keysDir.clientKeysDir.credentials = "clientcred.cip"
            config.keysDir.serverKeysDir.path = ""
            config.keysDir.serverKeysDir.privateStore = "privatestore.pkcs12"
            config.keysDir.serverKeysDir.privatePass = "privatestore.key"
            config.keysDir.serverKeysDir.publicStore = "publicstore.pkcs12"
            config.keysDir.serverKeysDir.publicPass = "publicstore.key"
            return config
        }
    }
}

