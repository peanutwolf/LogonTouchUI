package com.logontouch.ui

import com.logontouch.ui.dict.ServiceError
import com.logontouch.ui.dict.ServiceError.*
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import tornadofx.*

class StatusFragment: Fragment() {
    override val root = VBox()
    private var mStatusLabel: Label by singleAssign()
     var mCredentialQR: ImageView by singleAssign()

    init {

        root += vbox {
            this.alignment = Pos.CENTER

            mCredentialQR = imageview {}
            mCredentialQR.isVisible = false

        }
        root += hbox {
            mStatusLabel  = label("Wait while we check LogonTouch service availability")
        }

    }

    fun setServerIdleMode(error: ServiceError){
        mStatusLabel.text = when(error){
            OK             -> "Service connection is available."
            WAIT           -> "Wait while loading..."
            IDLE           -> "Service is idle. Press button to rebind device"
            SERVER_FAULT   -> "Failed to start REST server. TCP port could be in use..."
            CONFIG_ERROR   -> "Error in configuration occurred while loading :-("
            NOT_REACHABLE  -> "LogonTouch service is not available.\nTry again or reinstall LogonTouch software"
            ACCESS_DENY    -> "Service connection is available but there are some problems in access rights" +
                    "\nTry again or reinstall LogonTouch software"
        }
    }


}
