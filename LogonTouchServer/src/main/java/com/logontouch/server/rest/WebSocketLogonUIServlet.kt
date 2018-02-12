package com.logontouch.server.rest

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.util.*
import javax.servlet.annotation.WebServlet

@WebServlet(name = "WebSocketLogonUIServlet", urlPatterns = arrayOf(""))
class WebSocketLogonUIServlet: WebSocketServlet(){
    override fun configure(factory: WebSocketServletFactory?) {
        with(LogonUIWebSocket()){
            factory?.setCreator { _, _ ->  this}
        }

    }
}

@WebSocket
class LogonUIWebSocket{

    private var mSession: Session? = null

    @OnWebSocketConnect
    fun onOpen(session: Session){
        mSession = session
    }

    @OnWebSocketClose
    fun onClose(statusCode: Int, reason: String){
        mSession = null
    }

    @OnWebSocketMessage
    fun onMessage(data: String) {
        println(data)
    }

}