package com.pitlanehq.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pitlanehq.android.data.AccountRepository
import com.pitlanehq.android.data.AccountState
import com.pitlanehq.android.data.PitWallRepository
import com.pitlanehq.android.data.SignedOut
import com.pitlanehq.android.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PitlaneViewModel(app: Application) : AndroidViewModel(app) {
    private val accountRepo = AccountRepository(app)
    private val live = PitWallRepository(accountRepo)

    private val _account = MutableStateFlow(accountRepo.storedState())
    val account = _account.asStateFlow()
    val liveState = live.state

    val sessions = MutableStateFlow(Loadable<List<CloudSession>>())
    val laps = MutableStateFlow(Loadable<List<CloudLap>>())
    val combos = MutableStateFlow(Loadable<List<Combo>>())
    val board = MutableStateFlow(Loadable<List<CommunityLap>>())

    // what the detail screens show
    var session: CloudSession? = null
    var combo: Combo? = null

    // any call that finds the session gone signs the app out
    private fun <T> load(target: MutableStateFlow<Loadable<T>>, block: () -> T) {
        target.value = target.value.copy(loading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching(block)
                .onSuccess { target.value = Loadable(data = it) }
                .onFailure { e ->
                    target.value = target.value.copy(loading = false, error = e.message)
                    if (e is SignedOut) signedOut()
                }
        }
    }

    private fun signedOut() {
        live.stop()
        _account.value = AccountState(error = "Signed out: sign in again")
    }

    fun login(email: String, password: String) {
        _account.value = AccountState(email = email, busy = true)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { accountRepo.login(email, password) }
                .onSuccess { _account.value = it }
                .onFailure { _account.value = AccountState(email = email, error = it.message) }
        }
    }

    fun sync() {
        _account.value = _account.value.copy(busy = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { accountRepo.sync() }
                .onSuccess { _account.value = it }
                .onFailure { e -> if (e is SignedOut) signedOut() else _account.value = _account.value.copy(busy = false, error = e.message) }
        }
    }

    fun logout() {
        live.stop()
        sessions.value = Loadable(); laps.value = Loadable(); combos.value = Loadable(); board.value = Loadable()
        viewModelScope.launch(Dispatchers.IO) {
            accountRepo.logout()
            _account.value = AccountState()
        }
    }

    fun loadSessions() = load(sessions) { accountRepo.sessions() }
    fun loadLaps(id: String) { laps.value = Loadable(); load(laps) { accountRepo.laps(id) } }
    fun loadCombos() = load(combos) { accountRepo.combos() }
    fun loadBoard(c: Combo) { board.value = Loadable(); load(board) { accountRepo.leaderboard(c) } }

    // only while the live screen is open: the PC streams only while somebody watches
    fun startLive() { if (_account.value.signedIn) live.start() }
    fun stopLive() = live.stop()

    override fun onCleared() {
        live.stop()
        super.onCleared()
    }
}
