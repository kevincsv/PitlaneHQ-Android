package com.pitlanehq.android.viewmodel

import android.app.Application
import org.json.JSONObject
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
import kotlinx.coroutines.withContext

/** A lap next to the lap it is compared with. */
data class LapAnalysis(val lap: CloudLap, val trace: Trace?, val ref: Trace?, val refLabel: String, val refTime: Double?, val refSectors: List<Double>, val car: JSONObject? = null,
    val turns: List<Double> = emptyList(), val pit: PitLane? = null)

/** The pit lane of a track (from laps through the pits): [5 m point of a lap of n points, metres to the left of the track]. */
data class PitLane(val n: Int, val pts: List<Pair<Int, Double>>)

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
    val news = MutableStateFlow<List<com.pitlanehq.android.data.AppNews>>(emptyList())
    val inboxSeen = MutableStateFlow(repo.inboxSeen())
    fun inboxMark(ids: Collection<String>) { repo.inboxMark(ids); inboxSeen.value = repo.inboxSeen() }

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
    val profile = MutableStateFlow(Loadable<DriverProfile>())
    private var profileLap: String? = null

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
        viewModelScope.launch(Dispatchers.IO) { news.value = Updates.news(com.pitlanehq.android.data.SERVER, com.pitlanehq.android.ui.I18n.lang) }
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

    /** Second step of the sign-in: the authenticator code. */
    fun loginCode(code: String) {
        val a = _account.value
        _account.value = a.copy(busy = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.loginCode(code) }
                .onSuccess { _account.value = it; demo.value = repo.demo; loadRaces() }
                .onFailure { e -> _account.value = if (e.message == "sign_in_again") AccountState(email = a.email, error = "sign_in_again") else a.copy(busy = false, error = errKey(e)) }
        }
    }

    fun cancelCode() { _account.value = AccountState(email = _account.value.email) }

    // two-step sign-in: the screens wait on these (null = nothing pending)
    val twoFactorSetup = MutableStateFlow<com.pitlanehq.android.data.TwoFactorSetup?>(null)
    val recoveryCodes = MutableStateFlow<List<String>?>(null)
    val twoFactorError = MutableStateFlow<String?>(null)

    fun setup2fa(password: String) {
        twoFactorError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.setup2fa(password) }.onSuccess { twoFactorSetup.value = it }.onFailure { twoFactorError.value = errKey(it) }
        }
    }

    fun enable2fa(code: String) {
        twoFactorError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.enable2fa(code) }
                .onSuccess { twoFactorSetup.value = null; recoveryCodes.value = it; _account.value = repo.storedState().copy(busy = false) }
                .onFailure { twoFactorError.value = errKey(it) }
        }
    }

    fun disable2fa(password: String, code: String, done: () -> Unit) {
        twoFactorError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.disable2fa(password, code) }
                .onSuccess { _account.value = repo.storedState().copy(busy = false); done() }
                .onFailure { twoFactorError.value = errKey(it) }
        }
    }

    /** quiet: by itself (the app opened or came back), without the spinner and without an error on screen. */
    fun sync(quiet: Boolean = false) {
        if (!quiet) _account.value = _account.value.copy(busy = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.sync() }
                .onSuccess {
                    _account.value = it; demo.value = repo.demo; loadRaces()
                    // what is already on screen is read again too
                    if (sessions.value.data != null) loadSessions()
                    if (bests.value.data != null) loadBests()
                    // your profile's recent races (your own result in each), when they changed
                    runCatching { repo.publishProfileRaces() }
                }
                .onFailure { e -> if (e is SignedOut) signedOut() else if (!quiet) _account.value = _account.value.copy(busy = false, error = errKey(e)) }
        }
    }

    private var lastAuto = 0L

    /**
     * The app came to the screen (it opened, or came back from the background): your account, public
     * name, races and laps are read again by themselves, at most every 30 seconds.
     */
    fun onForeground() {
        val a = _account.value
        if (!a.signedIn || a.busy || demo.value) return
        val now = System.currentTimeMillis()
        if (now - lastAuto < 30_000) return
        lastAuto = now
        sync(quiet = true)
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

    /** Admins: see the app as a normal user (to test it), or go back to the admin view. */
    fun setAsUser(on: Boolean) {
        if (!_account.value.realAdmin) return
        repo.asUser = on
        _account.value = repo.storedState()
        demo.value = repo.demo
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

    /** Your iRating of every discipline, as the PC last saw it. */
    fun ratings(): Map<String, Triple<Int, String, Long>> = runCatching { repo.ratings() }.getOrDefault(emptyMap())

    /** Your notes on other drivers (the race summary shows their tag and lets you change it). */
    val driverNotes = MutableStateFlow<Map<String, DriverNote>>(emptyMap())
    fun loadDriverNotes() { driverNotes.value = runCatching { repo.driverNotes() }.getOrDefault(emptyMap()) }
    fun setDriverNote(key: String, name: String, tag: String, note: String, done: (String?) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val err = runCatching { repo.setDriverNote(key, name, tag, note) }.exceptionOrNull()
        loadDriverNotes()
        withContext(Dispatchers.Main) { done(err?.message) }
    }
    fun loadSessions() = load(sessions) { repo.sessions() }
    fun loadBests() = load(bests) { repo.bests() }
    fun loadLaps(id: String) { laps.value = Loadable(); load(laps) { repo.laps(id) } }
    fun loadCombos() = load(combos) { repo.combos() }
    fun loadBoard(c: Combo) { board.value = Loadable(); load(board) { repo.leaderboard(c.trackId, c.carId) } }
    fun loadReports() = load(reports) { repo.reports() }
    fun loadSetups() = load(setups) { repo.setups() }
    fun loadDevices() = load(devices) { Got(repo.devices()) }
    /** A driver's profile, from one of their laps (null: yours). */
    fun loadProfile(lapId: String?) { profileLap = lapId; profile.value = Loadable(); load(profile) { Got(repo.profile(lapId)) } }
    val leagues = MutableStateFlow(Loadable<List<League>>())
    fun loadLeagues() = load(leagues) { Got(repo.leagues()) }
    fun saveLeague(id: String?, l: League, done: (String?) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val e = runCatching { repo.saveLeague(id, l) }.exceptionOrNull()
        kotlinx.coroutines.withContext(Dispatchers.Main) { done(e?.let { errKey(it) }) }
        if (e == null) loadLeagues()
    }
    fun deleteLeague(id: String) = viewModelScope.launch(Dispatchers.IO) { runCatching { repo.deleteLeague(id) }; loadLeagues() }
    fun setBadgeHidden(hidden: Boolean) = viewModelScope.launch(Dispatchers.IO) { runCatching { repo.setBadgeHidden(hidden) }; loadProfile(profileLap) }
    fun adminSupporter(id: String, on: Boolean) = viewModelScope.launch(Dispatchers.IO) { runCatching { repo.adminSupporter(id, on) }; loadAdmin("users") }

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
            // the car and track of this session among the ones the community knows, matched by name
            val combos = runCatching { repo.combos().data }.getOrNull() ?: emptyList()
            val c = combos.firstOrNull { it.track.equals(s.track, true) && it.car.equals(s.car, true) }
                ?: combos.firstOrNull { s.track.startsWith(it.track, true) && it.car.equals(s.car, true) }
            // the car card needs the car alone: what it does on other tracks, for when nobody known drove it here yet
            val card = (c ?: combos.firstOrNull { it.car.equals(s.car, true) })?.let { runCatching { repo.carCard(it.carId) }.getOrNull() }
            if (kind == RefKind.MY_BEST) {
                val best = all.filter { it.valid && it.time > 0 && it.id != lap.id }.minByOrNull { it.time }
                if (best != null) {
                    ref = runCatching { repo.lapTrace(best.id) }.getOrNull()
                    label = "L${best.n}"
                    refTime = best.time
                    refSec = best.sectors
                }
            } else {
                // the community leaderboard of this track and car
                val top = c?.let { runCatching { repo.leaderboard(it.trackId, it.carId).data }.getOrNull() }?.firstOrNull { it.hasTrace }
                if (top != null) {
                    ref = runCatching { repo.communityTrace(top.id) }.getOrNull()
                    label = top.alias
                    refTime = top.time
                    refSec = top.sectors
                }
            }
            // a rival's lap has no path (it comes from its place on track): the line is then measured against your
            // own best lap of the session, the fastest line known here
            val r0 = ref
            if (r0 != null && (r0.x == null || r0.y == null)) {
                val best = all.filter { it.valid && it.time > 0 && it.id != lap.id }.minByOrNull { it.time }
                val bt = best?.let { runCatching { repo.lapTrace(it.id) }.getOrNull() }
                val bx = bt?.x; val by = bt?.y
                if (bx != null && by != null && kotlin.math.abs(bx.size - r0.rows.size) <= 6) {
                    fun fit(v: List<Double>) = List(r0.rows.size) { v[minOf(it, v.size - 1)] }
                    ref = r0.copy(x = fit(bx), y = fit(by))
                }
            }
            // the official turn numbers of this track, when an admin has placed them on its map (else the corners are counted)
            val turns = c?.let { runCatching { repo.turns(it.trackId) }.getOrNull() } ?: emptyList()
            Got(LapAnalysis(lap, tr, ref, label, refTime, refSec, card, turns))
        }
    }

    // only while the live screen is open: the PC streams only while somebody watches
    fun startLive() { if (_account.value.signedIn) live.start() }
    fun stopLive() = live.stop()
    fun liveWatch(m: LiveMode, code: String = "") = live.watch(m, code)
    fun liveShare(on: Boolean, new: Boolean = false) = live.share(on, new)
    fun liveAskAgain() = live.askAgain()
    fun setDrinks(on: Boolean, guest: String, guestAuto: Boolean) = live.setDrinks(on, guest, guestAuto)
    // a new DRINKS name is checked first: names used by other people on Pitlane HQ are refused
    val drinksTaken = MutableStateFlow<String?>(null)
    fun setDrinksGuest(name: String, known: List<String>) {
        drinksTaken.value = null
        if (name.isBlank() || known.any { it.equals(name, true) }) { setDrinks(true, name, false); return }
        viewModelScope.launch(Dispatchers.IO) { if (repo.nameFree(name)) setDrinks(true, name, false) else drinksTaken.value = name }
    }
    // a DRINKS driver renamed: a new name is checked first like any other
    fun renameDrinksGuest(from: String, to: String, known: List<String>) {
        drinksTaken.value = null
        val n = to.trim().take(32)
        if (n.isBlank() || n == from) return
        if (!n.equals(from, true) && known.any { it.equals(n, true) }) { drinksTaken.value = n; return }
        viewModelScope.launch(Dispatchers.IO) { if (repo.nameFree(n)) live.renameDrinksGuest(from, n) else drinksTaken.value = n }
    }
    fun forgetDrinksGuest(name: String) = live.forgetDrinksGuest(name)
    // admin profile
    data class AdminState(val kind: String, val items: List<org.json.JSONObject>? = null, val error: String? = null)
    val admin = MutableStateFlow(AdminState("uploads"))
    fun loadAdmin(kind: String) {
        admin.value = AdminState(kind)
        viewModelScope.launch(Dispatchers.IO) {
            admin.value = runCatching { AdminState(kind, repo.adminList(kind)) }.getOrElse { AdminState(kind, error = it.message ?: "server_down") }
        }
    }
    fun adminSince(id: String, since: Long?) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { repo.adminSince(id, since) }; loadAdmin("users") }
    }
    fun adminAccount(id: String, act: String, name: String? = null) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { repo.adminAccount(id, act, name) }; loadAdmin("users") }
    }
    fun adminUnlock(key: String, value: String, reload: String) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { repo.adminUnlock(key, value) }; loadAdmin(reload) }
    }
    fun adminTool(tool: String) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { repo.adminTool(tool) }; loadAdmin("status") }
    }
    fun adminDeleteUser(id: String) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { repo.adminDeleteUser(id) }; loadAdmin("users") }
    }
    fun adminDelete(kind: String, id: String) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { repo.adminDelete(kind, id) }; loadAdmin("uploads") }
    }

    override fun onCleared() {
        live.stop()
        runCatching { cm?.unregisterNetworkCallback(netCallback) }
        super.onCleared()
    }
}
