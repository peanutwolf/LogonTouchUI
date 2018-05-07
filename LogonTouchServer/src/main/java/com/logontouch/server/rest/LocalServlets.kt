package com.logontouch.server.rest

import com.logontouch.helper.ClientPrivateCertificate
import com.logontouch.helper.HostCertificate
import com.logontouch.helper.PKCS12Helper
import com.logontouch.server.LogonTouchServer
import com.logontouch.server.LogonTouchServer.Companion.LOGONTOUCH_SERVICE_FULL_PATH
import com.logontouch.server.useOutputWithDirs
import com.logontouch.server.useWriterWithDirs
import java.io.File
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("register")
class RegisterClient(private val mSessionContext: SessionContext){

    @POST
    @Path("public_cert")
    @Consumes(MediaType.APPLICATION_JSON)
    fun registerClientPublicCertificate(publicCertificate: HostCertificate): Response{

        mSessionContext.publicCertificate = publicCertificate

        return Response.status(200).build()
    }

    @POST
    @Path("private_cert")
    @Consumes(MediaType.APPLICATION_JSON)
    fun registerClientPrivateCertificate(privateCertificate: ClientPrivateCertificate): Response{
        mSessionContext.privateCertificate = privateCertificate

        return Response.status(200).build()
    }

    @GET
    @Path("status")
    fun getServiceStatus(): Response{
        return when{
            mSessionContext.publicCertificate  == null ||
            mSessionContext.privateCertificate == null -> Response.status(410).build()
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

class SessionContext(private val onCertificateUpload: onCertificateUploadCB){
    val sessionHash: String?
        get() = publicCertificate?.sessionHash

    var publicCertificate: HostCertificate? = null
    var privateCertificate: ClientPrivateCertificate? = null

    fun storeCertificates(): Boolean{
        val public  = publicCertificate
        val private = privateCertificate

        if(public == null || private == null)
            return false

        val publicCertKeystore = PKCS12Helper().deserializeKeyStore(public.publicCertificate, public.publicKeyStoreKey)

        File(LogonTouchServer.CLIENT_PUBLIC_KEYSTORE_FULL_PATH).useOutputWithDirs { fos ->
            publicCertKeystore.store(fos, public.publicKeyStoreKey)
        }
        File(LogonTouchServer.CLIENT_PUBLIC_PASSPHRASE_FULL_PATH).useWriterWithDirs{ fis ->
            fis.write(public.publicKeyStoreKey)
        }

        File(LogonTouchServer.CLIENT_CREDENTIALS_FULL_PATH).useOutputWithDirs {
            it.write(private.cipheredCredentials.toByteArray())
        }

        onCertificateUpload()

        return true
    }
}