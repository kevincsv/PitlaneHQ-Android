package com.pitlanehq.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pitlanehq.android.data.PitWallRepository
import com.pitlanehq.android.model.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class PitlaneViewModel:ViewModel(){
    private val repo=PitWallRepository()
    val telemetry=repo.telemetry.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),TelemetryFrame())
    val connection=repo.connection.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),PitWallConnection())
    fun connect(host:String)=repo.connect(host)
    fun disconnect()=repo.disconnect()
    override fun onCleared(){repo.disconnect();super.onCleared()}
}
