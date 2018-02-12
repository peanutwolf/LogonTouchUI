package com.logontouch.server.rest

import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

@Path("credential")
class SecureServlets {

    @GET
    @Path("status")
    fun serverStatus(): Response {
        return Response.status(200).build()
    }

    @GET
    @Path("provide")
    fun provideCredentialKey(@QueryParam("key") credentialKey: String): Response{
        println("Received client credential key=[$credentialKey]")
        return Response.status(200).build()
    }

}