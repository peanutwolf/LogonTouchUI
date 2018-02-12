package com.logontouch.ui

import com.logontouch.ui.dict.ServiceError
import tornadofx.EventBus
import tornadofx.FXEvent

object GetServicePathRequest: FXEvent(EventBus.RunOn.BackgroundThread)
class GetServicePathEvent(val error: ServiceError): FXEvent()

data class CipheredCredentialHolder(val encodedCreds: String, val encodedSecretKey: String, val encodedIV: String)

data class ClientCredentialCertificate(val publicCertificate: ByteArray,
                                       val publicKeyStoreKey: CharArray,
                                       val privateCertificate: ByteArray,
                                       val privateKeyStoreKey: CharArray)
data class  ClientSecretKeys(val sessionHash: String,
                            val privateKeyStoreKey: String,
                            val secretCredentialKey: String,
                             val secretCredentialIV: String)