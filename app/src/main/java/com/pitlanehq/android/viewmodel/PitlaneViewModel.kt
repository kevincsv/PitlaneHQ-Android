package com.pitlanehq.android.viewmodel
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pitlanehq.android.data.AccountRepository
import com.pitlanehq.android.data.AccountState
import com.pitlanehq.android.data.PitWallRepository
import com.pitlanehq.android.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PitlaneViewModel(app:Application):AndroidViewModel(app){
 private val repo=PitWallRepository()
 private val accountRepo=AccountRepository(app)
 val telemetry=repo.telemetry.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),TelemetryFrame())
 val connection=repo.connection.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),PitWallConnection())
 private val _account=MutableStateFlow(accountRepo.storedState())
 val account=_account.stateIn(viewModelScope,SharingStarted.Eagerly,accountRepo.storedState())
 fun login(email:String,password:String){viewModelScope.launch(Dispatchers.IO){_account.value=AccountState(email=email,syncing=true);runCatching{accountRepo.login(email,password)}.onSuccess{_account.value=it}.onFailure{_account.value=accountRepo.storedState().copy(error=it.message)}}}
 fun sync(){viewModelScope.launch(Dispatchers.IO){_account.value=_account.value.copy(syncing=true,error=null);runCatching{accountRepo.sync()}.onSuccess{_account.value=it}.onFailure{_account.value=_account.value.copy(syncing=false,error=it.message)}}}
 fun logout(){viewModelScope.launch(Dispatchers.IO){accountRepo.logout();_account.value=AccountState()}}
 fun connect(host:String)=repo.connect(host)
 fun disconnect()=repo.disconnect()
 override fun onCleared(){repo.disconnect();super.onCleared()}
}