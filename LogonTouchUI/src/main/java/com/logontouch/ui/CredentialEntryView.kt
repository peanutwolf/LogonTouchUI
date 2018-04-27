package com.logontouch.ui

import com.logontouch.ui.dict.ServiceError
import com.logontouch.ui.dict.ServiceError.*
import com.sun.xml.internal.bind.v2.model.core.ID
import javafx.animation.PauseTransition
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.PasswordField
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.util.Duration
import tornadofx.*
import java.io.ByteArrayInputStream

class CredentialsGenerateView: View(){
    override val root = VBox()
    var mUserText: TextField by singleAssign()
    var mPasswordText: PasswordField by singleAssign()
    var mProgressIndicator: ProgressIndicator by singleAssign()

    private val mCredentialChangeListener = CredentialChangeListener()
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
                mUserText.textProperty().addListener(mCredentialChangeListener)
            }
        }

        parentLayout.fieldset {
            field ("Password"){
                    mPasswordText = passwordfield()
                    mPasswordText.textProperty().addListener(mCredentialChangeListener)

                    mProgressIndicator = progressindicator()
                    mProgressIndicator.isVisible = false
                    mProgressIndicator.setMaxSize(20.0, 20.0)
                    mProgressIndicator.setPrefSize(20.0, 20.0)
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

    fun showCredentialQrLoader(visible: Boolean){
        mProgressIndicator.isVisible = visible
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


    private inner class CredentialChangeListener: ChangeListener<String>{
        private val debounce = PauseTransition(Duration.seconds(1.0))

        init {
            debounce.setOnFinished {
                mCredentialController.onCredentialEntry(mUserText.text, mPasswordText.text)
            }
        }

        override fun changed(observable: ObservableValue<out String>?, oldValue: String?, newValue: String?) {
            if(mUserText.text.isNotEmpty() && mPasswordText.text.isNotEmpty()){
                showCredentialQrLoader(true)
                debounce.playFromStart()
            }else{
                showCredentialQrLoader(false)
                hideCredentialQR()
                debounce.stop()
            }
        }
    }
}