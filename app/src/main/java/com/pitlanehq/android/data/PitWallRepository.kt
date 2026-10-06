package com.pitlanehq.android.data

import com.pitlanehq.android.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PitWallRepository {
    private val client=OkHttpClient.Builder().pingInterval(15,TimeUnit.SECONDS).build()
    private var socket:WebSocket?=null
    private val _connection=MutableStateFlow(PitWallConnection())
    val connection=_connection.asStateFlow()
    private val _telemetry=MutableStateFlow(TelemetryFrame())
    val telemetry=_telemetry.asStateFlow()

    fun connect(host:String,port:Int=8080) {
        socket?.cancel()
        _connection.value=PitWallConnection(ConnectionState.CONNECTING,host,port)
        val request=Request.Builder().url("ws://$host:$port/api/ws").build()
        socket=client.newWebSocket(request,object:WebSocketListener(){
            override fun onOpen(webSocket:WebSocket,response:Response){_connection.value=PitWallConnection(ConnectionState.LIVE,host,port)}
            override fun onMessage(webSocket:WebSocket,text:String){parseTelemetry(text)}
            override fun onFailure(webSocket:WebSocket,t:Throwable,response:Response?){_connection.value=_connection.value.copy(state=ConnectionState.ERROR,message=t.message)}
            override fun onClosed(webSocket:WebSocket,code:Int,reason:String){_connection.value=_connection.value.copy(state=ConnectionState.OFFLINE,message=reason)}
        })
    }
    fun disconnect(){socket?.close(1000,"User disconnected");socket=null;_connection.value=_connection.value.copy(state=ConnectionState.OFFLINE)}
    private fun parseTelemetry(text:String){
        runCatching {
            val o=JSONObject(text)
            _telemetry.value=TelemetryFrame(
                speedKph=o.optDouble("Speed",Double.NaN).takeUnless{it.isNaN()}?.times(3.6),
                rpm=o.optDouble("RPM",Double.NaN).takeUnless{it.isNaN()},
                gear=o.optInt("Gear").takeIf{o.has("Gear")},
                fuelLitres=o.optDouble("FuelLevel",Double.NaN).takeUnless{it.isNaN()},
                lap=o.optInt("Lap").takeIf{o.has("Lap")}
            )
            _connection.value=_connection.value.copy(state=ConnectionState.LIVE,message=null)
        }
    }
}
