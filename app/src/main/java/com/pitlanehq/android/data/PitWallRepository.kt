package com.pitlanehq.android.data

import com.pitlanehq.android.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PitWallRepository {
    private val client=OkHttpClient.Builder().pingInterval(15,TimeUnit.SECONDS).build()
    private var socket:WebSocket?=null
    private var streamJob:Job?=null
    private val _connection=MutableStateFlow(PitWallConnection())
    val connection=_connection.asStateFlow()
    private val _telemetry=MutableStateFlow(TelemetryFrame())
    val telemetry=_telemetry.asStateFlow()

    fun connect(host:String,port:Int=8080) {
        disconnect()
        _connection.value=PitWallConnection(ConnectionState.CONNECTING,host,port)
        streamJob=CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            runCatching {
                val request=Request.Builder().url("http://$host:$port/api/stream?vars=*&hz=30").header("Accept","text/event-stream").build()
                client.newCall(request).execute().use { response ->
                    if(!response.isSuccessful) error("PitWall HTTP "+response.code)
                    _connection.value=PitWallConnection(ConnectionState.LIVE,host,port)
                    val reader=response.body?.charStream()?.buffered() ?: error("Empty stream")
                    var line:String?
                    while(isActive && reader.readLine().also{line=it}!=null){
                        val raw=line ?: continue
                        if(raw.startsWith("data:")) parseTelemetry(raw.removePrefix("data:").trim())
                    }
                }
            }.onFailure { _connection.value=_connection.value.copy(state=ConnectionState.ERROR,message=it.message) }
        }
    }
    fun disconnect(){streamJob?.cancel();streamJob=null;socket?.close(1000,"User disconnected");socket=null;_connection.value=_connection.value.copy(state=ConnectionState.OFFLINE)}
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
