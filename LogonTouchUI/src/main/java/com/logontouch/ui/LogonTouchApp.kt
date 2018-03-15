package com.logontouch.ui

import javafx.application.Application
import javafx.scene.image.Image
import javafx.stage.Stage
import tornadofx.App
import tornadofx.addStageIcon
import java.io.File
import java.io.FileInputStream


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
        val resURL = this::class.java.classLoader.getResource(resDir) ?: return
        File(resURL.toURI())
                .takeIf { it.isDirectory }
                ?.listFiles()
                ?.map { Image(FileInputStream(it)) }
                ?.forEach{
                    addStageIcon(it)
                }
    }
}

fun main(args: Array<String>) {
    Application.launch(LogonTouchUIApp::class.java)
}