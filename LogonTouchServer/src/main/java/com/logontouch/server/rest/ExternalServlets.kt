package com.logontouch.server.rest

import com.google.gson.Gson
import com.logontouch.helper.ClientCertificate
import com.logontouch.helper.HostCertificate
import com.logontouch.server.LogonTouchServer
import org.apache.logging.log4j.LogManager
import java.io.File
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("register")
class ProvideClient(private val mSessionContext: SessionContext){
    private val logger = LogManager.getLogger(ProvideClient::class.java)

    @GET
    @Path("private_cert")
    @Produces(MediaType.APPLICATION_JSON)
    fun requestCertificate(@DefaultValue("") @QueryParam("hash") reqHash: String): Response {
        when{
            mSessionContext.publicCertificate == null ||
            mSessionContext.privateCertificate == null ||
            reqHash != mSessionContext.sessionHash   -> {
                logger.info("[requestCertificate] Response with status=403. Certificate not registered.")
                return Response.status(403).build()
            }
        }

        return Response.status(200).apply {
            val json = Gson().toJson(ClientCertificate(mSessionContext.sessionHash!!, mSessionContext.privateCertificate?.privateCertificate!!))
            entity(json)
            type(MediaType.APPLICATION_JSON_TYPE)
        }.build()
    }

    @GET
    @Path("public_cert")
    @Produces(MediaType.APPLICATION_JSON)
    fun requestServerPublicCertificate(@DefaultValue("") @QueryParam("hash") reqHash: String): Response {
        when{
            mSessionContext.sessionHash == null    ||
            reqHash != mSessionContext.sessionHash -> return Response.status(403).build()
        }

        val publicCertBytes = File(LogonTouchServer.SERVER_PUBLIC_KEYSTORE_FULL_PATH).readBytes()
        val publicKeyChars = File(LogonTouchServer.SERVER_PUBLIC_PASSPHRASE_FULL_PATH).readText().toCharArray()

        return Response.status(200).apply {
            val json = Gson().toJson(HostCertificate(mSessionContext.sessionHash!!, publicCertBytes, publicKeyChars))
            entity(json)
            type(MediaType.APPLICATION_JSON_TYPE)
        }.build()
    }

    @GET
    @Path("status")
    fun getServiceStatus(@DefaultValue("") @QueryParam("hash") reqHash: String,
                         @DefaultValue("") @QueryParam("result") reqResult: String): Response{

        return when{
            mSessionContext.publicCertificate == null  ||
            mSessionContext.privateCertificate == null                     -> Response.status(410).build()
            reqHash == mSessionContext.sessionHash && reqResult == "true"  ->{
                logger.info("[getServiceStatus] Received positive bind status from client")
                if(mSessionContext.storeCertificates())
                    Response.status(205).build()
                else
                    Response.status(410).build()
            }
            else -> Response.status(200).build()
        }
    }
}