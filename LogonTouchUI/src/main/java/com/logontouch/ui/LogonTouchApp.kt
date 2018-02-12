package com.logontouch.ui

import javafx.application.Application
import javafx.stage.Stage
import tornadofx.App
import tornadofx.singleAssign


/**``
 * Created by pEANUTwOLF on 26.11.2017.
 */

class LogonTouchUIApp : App(CredentialsGenerateView::class, Styles::class){
    private val mCredentialController: CredentialEntryController by inject()

    override fun start(stage: Stage) {
        stage.sizeToScene()
        stage.centerOnScreen()
        mCredentialController.init()
        super.start(stage)
    }

    override fun stop() {
        mCredentialController.deInit()
        super.stop()
    }
}

fun main(args: Array<String>) {
    Application.launch(LogonTouchUIApp::class.java)
}