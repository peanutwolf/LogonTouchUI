package com.logontouch.ui

import javafx.application.Application
import javafx.scene.image.Image
import javafx.stage.Stage
import tornadofx.App
import tornadofx.addStageIcon

/**``
 * Created by pEANUTwOLF on 26.11.2017.
 */

class LogonTouchUIApp : App(CredentialsGenerateView::class, Styles::class){
    private val mCredentialController: CredentialEntryController by inject()

    override fun start(stage: Stage) {
        super.start(stage)
        stage.sizeToScene()
        stage.centerOnScreen()
        setStageIcons("icons")
        mCredentialController.init()
    }

    override fun stop() {
        mCredentialController.deInit()
        super.stop()
    }

    private fun setStageIcons(resDir: String){
        arrayOf("Icon-App-16x16.png",
                "Icon-App-20x02.png",
                "Icon-App-32x32.png")
                .mapNotNull { this::class.java.classLoader.getResourceAsStream("$resDir/$it") }
                .map { Image(it) }
                .forEach{addStageIcon(it)}
    }
}

fun main(args: Array<String>) {
    Application.launch(LogonTouchUIApp::class.java)
}