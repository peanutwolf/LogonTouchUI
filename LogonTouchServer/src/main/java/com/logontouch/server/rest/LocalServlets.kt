package com.logontouch.server.rest



import com.logontouch.helper.ClientPrivateCertificate
import com.logontouch.helper.HostCertificate
import com.logontouch.helper.PKCS12Helper
import com.logontouch.server.LogonTouchServer
import com.logontouch.server.LogonTouchServer.Companion.LOGONTOUCH_SERVICE_FULL_PATH
import com.logontouch.server.useOutputWithDirs
import com.logontouch.server.useWriterWithDirs
import org.eclipse.jetty.util.ssl.SslContextFactory
import java.io.File
import java.io.FileOutputStream
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("register")
class RegisterClient(private val sslContextFactory: SslContextFactory, private val mSessionContext: SessionContext){

    @POST
    @Path("public_cert")
    @Consumes(MediaType.APPLICATION_JSON)
    fun registerClientPublicCertificate(publicCertificate: HostCertificate): Response{
        val publicCertKeystore = PKCS12Helper().deserializeKeyStore(publicCertificate.publicCertificate, publicCertificate.publicKeyStoreKey)

        File(LogonTouchServer.CLIENT_PUBLIC_KEYSTORE_FULL_PATH).useOutputWithDirs { fos ->
            publicCertKeystore.store(fos, publicCertificate.publicKeyStoreKey)
        }
        File(LogonTouchServer.CLIENT_PUBLIC_PASSPHRASE_FULL_PATH).useWriterWithDirs{ fis ->
            fis.write(publicCertificate.publicKeyStoreKey)
        }

        sslContextFactory.reload {
            it.setTrustStorePath(LogonTouchServer.CLIENT_PUBLIC_KEYSTORE_FULL_PATH)
            it.setTrustStorePassword(String(publicCertificate.publicKeyStoreKey))
        }

        mSessionContext.mSessionHash = publicCertificate.sessionHash

        println("Received public certs")

        return Response.status(200).build()
    }

    @POST
    @Path("private_cert")
    @Consumes(MediaType.APPLICATION_JSON)
    fun registerClientPrivateCertificate(privateCertificate: ClientPrivateCertificate): Response{
        mSessionContext.mClientCertificate = privateCertificate.privateCertificate

        FileOutputStream(File("client_test.p12")).use {
            it.write(privateCertificate.privateCertificate)
        }

        File(LogonTouchServer.CLIENT_CREDENTIALS_FULL_PATH).useOutputWithDirs {
            it.write(privateCertificate.cipheredCredentials.toByteArray())
        }

        println("Received private certs")

        return Response.status(200).build()
    }

    @GET
    @Path("status")
    fun getServiceStatus(): Response{
        return when{
            mSessionContext.mSessionHash == null       -> Response.status(410).build()
            mSessionContext.mClientCertificate == null -> Response.status(410).build()
            else                                       -> Response.status(200).build()
        }
    }
}

@Path("status")
class ServerHTTPStatus{

    @GET
    fun getServerStatus(): Response{
        return Response.status(200).build()
    }

    @GET
    @Path("{param}")
    fun getServerParam(@PathParam("param") param: String): Response{

        return with(Response.status(200)){
            when(param){
                "srv_path" -> this.entity(LOGONTOUCH_SERVICE_FULL_PATH)
                else    -> this.status(Response.Status.BAD_REQUEST)
            }
        }.build()

    }
}

typealias onCertificateUploadCB = () -> Unit

class SessionContext(val onCertificateUpload: onCertificateUploadCB){
    var mSessionHash: String? = null
    var mClientCertificate: ByteArray? = null
}