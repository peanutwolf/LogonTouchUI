package com.logontouch.ui

import com.logontouch.ui.dict.ServiceError
import com.logontouch.ui.dict.ServiceError.*
import com.sun.xml.internal.bind.v2.model.core.ID
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import tornadofx.*
import java.io.ByteArrayInputStream

class CredentialsGenerateView: View(){
    override val root = VBox()
    var mUserText: TextField by singleAssign()
    var mPasswordText: PasswordField by singleAssign()

    private var mStatusFragment: StatusFragment = find(StatusFragment::class)
    private val mCredentialEntryPane = inflateCredentialEntryView()
    private val mStatusPane = inflateStatusPane()
    private val mCredentialController: CredentialEntryController by inject()

    init {
        title = "LogonTouchUI"

        root += mCredentialEntryPane
        root += mStatusPane

        mCredentialEntryPane.isDisable = true
    }

    private fun inflateStatusPane(): Node{
        val parentLayout = Form()
        parentLayout += mStatusFragment
        parentLayout += button{
            text = "Restart service"
            onAction = EventHandler {
                hideCredentialQR()
                mCredentialController.init()
            }
        }
        return parentLayout
    }

    private fun inflateCredentialEntryView(): Node {
        val parentLayout = Form()

        parentLayout.fieldset {
            field ("Username"){
                mUserText = textfield()
            }
        }

        parentLayout.fieldset {
            field ("Password"){
                mPasswordText = passwordfield()
            }
        }

        parentLayout.button{
            text = "Generate QR"
            enableWhen { mUserText.textProperty().isEmpty.not().and(mPasswordText.textProperty().isEmpty.not()) }
            onAction = EventHandler {
                mCredentialController.onCredentialEntry(mUserText.text, mPasswordText.text)
            }
        }

        return parentLayout
    }


    fun showServiceStatus(error: ServiceError) {
        when(error){
            OK -> mCredentialEntryPane.isDisable = false
            else -> mCredentialEntryPane.isDisable = true
        }
        mStatusFragment.setServerIdleMode(error)
        FX.primaryStage.sizeToScene()
    }

    fun showCredentialQR(qrArray: ByteArray) {
        val qrImage = ByteArrayInputStream(qrArray).use {
            return@use Image(it)
        }

        mStatusFragment.mCredentialQR.isVisible = true
        mStatusFragment.mCredentialQR.image = qrImage
        FX.primaryStage.sizeToScene()
        FX.primaryStage.centerOnScreen()
    }

    fun hideCredentialQR(){
        mStatusFragment.mCredentialQR.image = null
        mStatusFragment.mCredentialQR.isVisible = false

        FX.primaryStage.sizeToScene()
        FX.primaryStage.centerOnScreen()
    }

}