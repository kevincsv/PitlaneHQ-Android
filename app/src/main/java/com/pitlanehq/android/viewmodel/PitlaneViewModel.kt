package com.pitlanehq.android.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pitlanehq.android.data.*
import com.pitlanehq.android.model.*
import com.pitlanehq.android.ui.I18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A lap next to the lap it is compared with. */
data class LapAnalysis(val lap: CloudLap, val trace: Trace?, val ref: Trace?, val refLabel: String, val refTime: Double?, val refSectors: List<Double>)

enum class RefKind { MY_BEST, COMMUNITY }

class PitlaneViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AccountRepository(app)
    private val live = PitWallRepository(repo)

    private val _account = MutableStateFlow(repo.storedState())
    val account = _account.asStateFlow()
    val liveState = live.state
    val demo = MutableStateFlow(repo.demo)
    val online = MutableStateFlow(true)
    val update = MutableStateFlow<AppUpdate?>(null)

    val races = MutableStateFlow(Loadable<List<Race>>())
    val sessions = MutableStateFlow(Loadable<List<CloudSession>>())
    val bests = MutableStateFlow(Loadable<List<PersonalBest>>())
    val laps = MutableStateFlow(Loadable<List<CloudLap>>())
    val analysis = MutableStateFlow(Loadable<LapAnalysis>())
    val combos = MutableStateFlow(Loadable<List<Combo>>())
    val board = MutableStateFlow(Loadable<List<CommunityLap>>())
    val reports = MutableStateFlow(Loadable<List<SharedReport>>())
    val setups = MutableStateFlow(Loadable<List<SharedSetup>>())
    val devices = MutableStateFlow(Loadable<List<Device>>())

    // what the detail screens show
    var session: CloudSession? = null
    var combo: Combo? = null
    var race: Race? = null

    private val cm = app.getSystemService(ConnectivityManager::class.java)
    private val netCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val was = online.value
            online.value = true
            if (!was) refreshStale()
        }

        override fun onLost(network: Network) {
            online.value = cm?.activeNetwork != null && cm.activeNetwork != network
        }
    }

    init {
        I18n.choice = repo.language
        online.value = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } ?: false
        runCatching { cm?.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), netCallback) }
        loadRaces()
        checkUpdate()
    }

    /** A newer version on GitHub: shown as a banner on top. */
    fun checkUpdate() {
        viewModelScope.launch(Dispatchers.IO) { Updates.check(com.pitlanehq.android.BuildConfig.VERSION_NAME)?.let { update.value = it } }
    }

    // back online: load again what was shown from the phone's saved copy
    private fun refreshStale() {
        if (!_account.value.signedIn) return
        if (sessions.value.stale || sessions.value.error != null) loadSessions()
        if (bests.value.stale || bests.value.error != null) loadBests()
        if (combos.value.stale || combos.value.error != null) loadCombos()
        if (reports.value.stale || reports.value.error != null) loadReports()
        if (setups.value.stale || setups.value.error != null) loadSetups()
        if (_account.value.error != null) sync()
    }

    private fun errKey(e: Throwable) = (e as? AppError)?.message ?: e.message ?: "server_down"

    // any call that finds the session gone signs the app out
    private fun <T> load(target: MutableStateFlow<Loadable<T>>, block: () -> Got<T>) {
        target.value = target.value.copy(loading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching(block)
                .onSuccess { target.value = Loadable(data = it.data, stale = it.stale) }
                .onFailure { e ->
                    target.value = target.value.copy(loading = false, error = errKey(e))
                    if (e is SignedOut) signedOut()
                }
        }
    }

    private fun signedOut() {
        live.stop()
        demo.value = false
        _account.value = AccountState(error = "signed_out")
    }

    fun login(email: String, password: String) {
        _account.value = AccountState(email = email, busy = true)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.login(email, password) }
                .onSuccess { _account.value = it; demo.value = repo.demo; loadRaces() }
                .onFailure { _account.value = AccountState(email = email, error = errKey(it)) }
        }
    }

    fun sync() {
        _account.value = _account.value.copy(busy = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.sync() }
                .onSuccess { _account.value = it; demo.value = repo.demo; loadRaces() }
                .onFailure { e -> if (e is SignedOut) signedOut() else _account.value = _account.value.copy(busy = false, error = errKey(e)) }
        }
    }

    fun logout() {
        live.stop()
        listOf(races, sessions, bests, laps, analysis, combos, board, reports, setups, devices).forEach { (it as MutableStateFlow<Loadable<*>>).value = Loadable<Any>() }
        viewModelScope.launch(Dispatchers.IO) {
            repo.logout()
            demo.value = false
            _account.value = AccountState()
        }
    }

    fun setDemo(on: Boolean) {
        if (on && !_account.value.admin) return
        repo.demo = on
        demo.value = on
        live.stop()
        listOf(sessions, bests, laps, analysis, combos, board, reports, setups).forEach { (it as MutableStateFlow<Loadable<*>>).value = Loadable<Any>() }
        loadRaces()
    }

    fun setLanguage(l: String) {
        repo.language = l
        I18n.choice = l
    }

    fun loadRaces() = load(races) { Got(repo.races()) }
    fun loadSessions() = load(sessions) { repo.sessions() }
    fun loadBests() = load(bests) { repo.bests() }
    fun loadLaps(id: String) { laps.value = Loadable(); load(laps) { repo.laps(id) } }
    fun loadCombos() = load(combos) { repo.combos() }
    fun loadBoard(c: Combo) { board.value = Loadable(); load(board) { repo.leaderboard(c.trackId, c.carId) } }
    fun loadReports() = load(reports) { repo.reports() }
    fun loadSetups() = load(setups) { repo.setups() }
    fun loadDevices() = load(devices) { Got(repo.devices()) }

    fun revoke(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.revoke(id) }.onFailure { devices.value = devices.value.copy(error = errKey(it)) }
            loadDevices()
        }
    }

    /** Opens a lap and the lap it is compared with: your best lap of the session, or the community's fastest. */
    fun analyse(s: CloudSession, lap: CloudLap, all: List<CloudLap>, kind: RefKind) {
        analysis.value = Loadable(loading = true)
        load(analysis) {
            val tr = repo.lapTrace(lap.id)
            var ref: Trace? = null
            var label = ""
            var refTime: Double? = null
            var refSec: List<Double> = emptyList()
            if (kind == RefKind.MY_BEST) {
                val best = all.filter { it.valid && it.time > 0 && it.id != lap.id }.minByOrNull { it.time }
                if (best != null) {
                    ref = runCatching { repo.lapTrace(best.id) }.getOrNull()
                    label = "L${best.n}"
                    refTime = best.time
                    refSec = best.sectors
                }
            } else {
                // the community leaderboard of this track and car, matched by name
                val c = runCatching { repo.combos().data }.getOrNull()?.firstOrNull { it.track.equals(s.track, true) && it.car.equals(s.car, true) }
                    ?: runCatching { repo.combos().data }.getOrNull()?.firstOrNull { s.track.startsWith(it.track, true) && it.car.equals(s.car, true) }
                val top = c?.let { runCatching { repo.leaderboard(it.trackId, it.carId).data }.getOrNull() }?.firstOrNull { it.hasTrace }
                if (top != null) {
                    ref = runCatching { repo.communityTrace(top.id) }.getOrNull()
                    label = top.alias
                    refTime = top.time
                    refSec = top.sectors
                }
            }
            Got(LapAnalysis(lap, tr, ref, label, refTime, refSec))
        }
    }

    // only while the live screen is open: the PC streams only while somebody watches
    fun startLive() { if (_account.value.signedIn) live.start() }
    fun stopLive() = live.stop()
    fun setDrinks(on: Boolean, guest: String, guestAuto: Boolean) = live.setDrinks(on, guest, guestAuto)

    override fun onCleared() {
        live.stop()
        runCatching { cm?.unregisterNetworkCallback(netCallback) }
        super.onCleared()
    }
}
