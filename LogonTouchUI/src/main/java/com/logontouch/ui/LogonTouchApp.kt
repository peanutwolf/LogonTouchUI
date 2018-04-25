package com.logontouch.ui

import com.logontouch.helper.getWinRegPathValue
import com.logontouch.ui.logging.CustomConfigurationFactory
import javafx.application.Application
import javafx.scene.image.Image
import javafx.stage.Stage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.ConfigurationFactory
import tornadofx.App
import tornadofx.addStageIcon

/**``
 * Created by pEANUTwOLF on 26.11.2017.
 */

class LogonTouchUIApp : App(CredentialsGenerateView::class, Styles::class){
    private val mCredentialController: CredentialEntryController by inject()

    init {
        configureLogger()
    }

    private val logger = LogManager.getLogger(LogonTouchUIApp::class.java)

    override fun start(stage: Stage) {
        super.start(stage)
        logger.info("[start] /***************************************************************************/")
        logger.info("[start] Starting LogonTouchUIApp")

        stage.sizeToScene()
        stage.centerOnScreen()
        setStageIcons("icons")
        mCredentialController.init()
    }

    override fun stop() {
        logger.info("[stop] Stopping LogonTouchUIApp")
        logger.info("[stop] /***************************************************************************/")
        mCredentialController.deInit()
        super.stop()
    }

    private fun configureLogger(){
        try {
            val userPath = getWinRegPathValue("SOFTWARE\\LazyGravity\\LogonTouchUI","")
            val customFactory = CustomConfigurationFactory()
                    .apply { logFilePath = userPath?.resolve("logontouchui.log")?.toString()}
            ConfigurationFactory.setConfigurationFactory(customFactory)
        }catch (ex: Exception){
            //Logger not initialized
        }
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