package com.pitlanehq.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import com.pitlanehq.android.data.AppUpdate
import com.pitlanehq.android.data.AccountState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.pitlanehq.android.BuildConfig
import com.pitlanehq.android.data.SERVER
import com.pitlanehq.android.model.*
import com.pitlanehq.android.viewmodel.LapAnalysis
import com.pitlanehq.android.viewmodel.PitlaneViewModel
import com.pitlanehq.android.viewmodel.RefKind
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt

private val Ink = Color(0xFF11151B)
private val Surface = Color(0xFF19202A)
private val Bg = Color(0xFF11151B)
private val Surface2 = Color(0xFF1E2631)
private val Line = Color(0xFF2B3542)
private val Fg = Color(0xFFE7EBF1)
private val Muted = Color(0xFF8A97A9)
private val Accent = Color(0xFFFFB02E)
private val Good = Color(0xFF38C97C)
private val Bad = Color(0xFFFF6363)
private val Purple = Color(0xFFB98CFF)
private val Blue = Color(0xFF5AA9FF)

private const val WEB_APP = "$SERVER/"

/** Support Pitlane HQ (Settings); empty: not shown. */
private const val PATREON_URL = "https://www.patreon.com/c/PitlaneHQ/membership"
/** The changelog of the phone apps (the "What's new" of the update notice): one entry per version. */
private const val CHANGELOG_URL = "https://pitlanehq.app/changelog/phones"
/** Where "Send feedback" goes. */
private const val SUPPORT_EMAIL = "support@pitlanehq.app"
private const val FEEDBACK_URL = "mailto:$SUPPORT_EMAIL?subject=Pitlane%20HQ%20feedback"

private data class Dest(val route: String, val label: String, val icon: ImageVector)

private fun day(ms: Long) = if (ms <= 0) "" else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))
private fun dayTime(ms: Long) = if (ms <= 0) "" else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

@Composable
fun PitlaneApp(vm: PitlaneViewModel = viewModel()) {
    val account by vm.account.collectAsState()
    // every time the app comes to the screen, the account syncs by itself
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.onForeground() }
    val scheme = darkColorScheme(
        primary = Accent, onPrimary = Ink, background = Ink, onBackground = Fg, surface = Surface, onSurface = Fg,
        surfaceVariant = Surface2, onSurfaceVariant = Muted, outline = Line, error = Bad
    )
    MaterialTheme(colorScheme = scheme) {
        key(I18n.lang) {
            if (!account.signedIn) LoginScreen(vm, account.error, account.busy) else MainApp(vm)
        }
    }
}

@Composable
private fun LoginScreen(vm: PitlaneViewModel, error: String?, busy: Boolean) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val needCode by vm.account.collectAsState()
    val uri = LocalUriHandler.current
    if (needCode.needCode) {
        // the password was right: the authenticator code (or a recovery code) finishes the sign-in
        AlertDialog(
            onDismissRequest = { vm.cancelCode() },
            title = { Text(t("two_factor"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("two_factor_code"), color = Muted, fontSize = 12.sp)
                    OutlinedTextField(code, { code = it }, label = { Text(t("code")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    error?.let { Text(t(it), color = Bad, fontSize = 12.sp) }
                }
            },
            confirmButton = { TextButton({ vm.loginCode(code) }, enabled = !busy && code.isNotBlank()) { Text(t("sign_in"), color = Accent) } },
            dismissButton = { TextButton({ vm.cancelCode() }) { Text(t("cancel"), color = Muted) } }
        )
    }
    LazyColumn(
        Modifier.fillMaxSize().background(Ink).systemBarsPadding().imePadding(),
        contentPadding = PaddingValues(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(32.dp)) }
        item { Text("PITLANE HQ", color = Fg, fontSize = 34.sp, fontWeight = FontWeight.Black) }
        item { Text(t("companion"), color = Accent, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        item {
            Panel {
                Text(t("sign_in").uppercase(), fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(t("same_account"), color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    email, { email = it }, label = { Text(t("email")) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    password, { password = it }, label = { Text(t("password")) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(t(it), color = Bad, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.login(email, password) }, enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                ) { Text((if (busy) t("signing_in") else t("sign_in")).uppercase(), fontWeight = FontWeight.Black) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton({ uri.openUri(WEB_APP) }) { Text(t("create_account"), color = Accent, fontSize = 12.sp) }
                    TextButton({ uri.openUri("$SERVER/account/forgot") }) { Text(t("forgot"), color = Muted, fontSize = 12.sp) }
                }
            }
        }
        item { Text(t("pw_note"), color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun MainApp(vm: PitlaneViewModel) {
    val nav = rememberNavController()
    val demo by vm.demo.collectAsState()
    val online by vm.online.collectAsState()
    val upd by vm.update.collectAsState()
    val uri = LocalUriHandler.current
    val info = rememberInfo(vm, nav)
    CompositionLocalProvider(LocalInfo provides info) {
    Scaffold(containerColor = Ink, bottomBar = { BottomBar(nav) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.statusBarsPadding()) {
                if (!online) Banner(t("showing_saved"), Bad)
                if (demo) Banner(t("demo_banner"), Accent)
                upd?.let { u -> Banner(t("update_available", u.version), Good) { uri.openUri(u.url) } }
            }
            UpdateNotice(upd)
            NavHost(nav, "home", Modifier.weight(1f)) {
                composable("home") { Home(vm, nav) }
                composable("races") { RacesAll(vm, nav) }
                composable("race") { RaceDetail(vm, nav) }
                composable("analysis") { Analysis(vm, nav) }
                composable("session") { SessionDetail(vm, nav) }
                composable("lap") { LapDetail(vm, nav) }
                composable("community") { Community(vm, nav) }
                composable("combo") { ComboDetail(vm, nav) }
                composable("profile") { ProfileScreen(vm, nav) }
                composable("live") { Live(vm) }
                composable("settings") { Settings(vm, nav) }
                composable("drinks") { DrinksScreen(vm, nav) }
                composable("admin") { AdminScreen(vm, nav) }
            }
        }
    }
    }
}

/** One entry of Info: an alert (dismissable) or one of the permanent items (support, feedback). */
private class InfoItem(val id: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val text: String, val keep: Boolean = false, val acts: List<Pair<String, () -> Unit>>)

/** Info, shared by every screen's header: the alerts, how many are new, and the sheet. */
private class InfoState(val alerts: List<InfoItem>, val always: List<InfoItem>, val fresh: Int, val open: () -> Unit)
private val LocalInfo = compositionLocalOf<InfoState?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberInfo(vm: PitlaneViewModel, nav: NavHostController): InfoState {
    val a by vm.account.collectAsState()
    val upd by vm.update.collectAsState()
    val news by vm.news.collectAsState()
    val seen by vm.inboxSeen.collectAsState()
    var open by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val alerts = buildList {
        upd?.let { u -> add(InfoItem("upd:" + u.version, Icons.Default.Download, t("update_title", u.version), t("update_body", BuildConfig.VERSION_NAME.replace("-", " ")), acts = listOf(t("download") to { uri.openUri(u.url) }, t("whats_new") to { uri.openUri(CHANGELOG_URL + "#" + u.version.replace(".", "") + "-beta") }))) }
        if (!a.twoFactor) add(InfoItem("2fa", Icons.Default.Lock, t("two_factor_rec_short"), t("two_factor_rec_sub"), acts = listOf(t("turn_on") to { nav.navigate("settings") })))
        news.forEach { n -> add(InfoItem("news:" + n.id, Icons.Default.Newspaper, n.title, n.text + if (n.date.isNotBlank()) " · " + n.date else "", acts = listOfNotNull(n.url?.let { u -> t("open") to { uri.openUri(u) } }, n.view?.let { v -> t("see_it") to { nav.navigate(if (v == "me") "settings" else v) } }))) }
    }.filter { it.keep || it.id !in seen }
    val always = buildList {
        if (PATREON_URL.isNotEmpty()) add(InfoItem("support", Icons.Default.Favorite, t("support"), t("support_sub"), keep = true, acts = listOf("Patreon" to { uri.openUri(PATREON_URL) })))
        add(InfoItem("feedback", Icons.Default.Forum, t("feedback"), t("feedback_sub"), keep = true, acts = listOf(t("send_feedback") to { uri.openUri(FEEDBACK_URL) })))
    }
    val fresh = alerts.count { "seen:" + it.id !in seen }
    if (open) ModalBottomSheet(onDismissRequest = { open = false; vm.inboxMark(alerts.map { "seen:" + it.id }) }, containerColor = Surface) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t("alerts").uppercase(), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                if (alerts.any { !it.keep }) TextButton({ vm.inboxMark(alerts.filter { !it.keep }.map { it.id }) }) { Text(t("clear"), color = Muted, fontSize = 12.sp) }
            }
            if (alerts.isEmpty()) Text(t("nothing_new"), color = Muted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
            alerts.forEach { InfoRow(it, { open = false }) { vm.inboxMark(listOf(it.id)) } }
            Text("PITLANE HQ", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp))
            always.forEach { InfoRow(it, { open = false }, null) }
        }
    }
    return InfoState(alerts, always, fresh) { open = true }
}

@Composable
private fun InfoRow(it: InfoItem, close: () -> Unit, dismiss: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().background(Surface2, androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).padding(10.dp), verticalAlignment = Alignment.Top) {
        Icon(it.icon, null, tint = Accent, modifier = Modifier.padding(end = 10.dp, top = 2.dp))
        Column(Modifier.weight(1f)) {
            Text(it.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(it.text, color = Muted, fontSize = 12.sp)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                it.acts.forEachIndexed { i, (label, run) ->
                    if (i == 0) Button({ close(); run() }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black) }
                    else OutlinedButton({ close(); run() }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text(label.uppercase(), fontSize = 11.sp, color = Fg) }
                }
            }
        }
        if (dismiss != null && !it.keep) IconButton(dismiss, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, t("dismiss"), tint = Muted) }
    }
}

/** The Info button in the header of every screen, with the number of new alerts. */
@Composable
private fun InfoButton() {
    val info = LocalInfo.current ?: return
    BadgedBox(badge = { if (info.fresh > 0) Badge(containerColor = Accent, contentColor = Ink) { Text("${info.fresh}") } }) {
        IconButton(info.open) { Icon(Icons.Outlined.Info, "Info", tint = Muted) }
    }
}

/** When the app starts with a newer version published: what's new, download, or later (the banner stays). */
@Composable
private fun UpdateNotice(u: AppUpdate?) {
    var shown by rememberSaveable { mutableStateOf("") }
    val uri = LocalUriHandler.current
    if (u == null || shown == u.version) return
    AlertDialog(
        onDismissRequest = { shown = u.version },
        title = { Text(t("update_title", u.version), fontWeight = FontWeight.Black) },
        text = { Text(t("update_body", BuildConfig.VERSION_NAME.replace("-", " ")), color = Muted, fontSize = 13.sp) },
        confirmButton = {
            Row {
                TextButton({ uri.openUri(CHANGELOG_URL + "#" + u.version.replace(".", "") + "-beta") }) { Text(t("whats_new"), color = Accent) }
                TextButton({ shown = u.version; uri.openUri(u.url) }) { Text(t("download"), color = Accent, fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = { TextButton({ shown = u.version }) { Text(t("later"), color = Muted) } }
    )
}

@Composable
private fun Banner(s: String, c: Color, onClick: (() -> Unit)? = null) {
    Text(
        s, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth().background(c).let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun BottomBar(nav: NavHostController) {
    val dests = listOf(
        Dest("home", t("home"), Icons.Default.Home),
        Dest("analysis", t("analysis"), Icons.Default.QueryStats),
        Dest("live", t("live"), Icons.Default.Sensors),
        Dest("community", t("community"), Icons.Default.Groups),
        Dest("settings", t("account_tab"), Icons.Default.Person)
    )
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    val tab = when (current) { "races", "race" -> "home"; "session", "lap" -> "analysis"; "combo" -> "community"; "drinks", "admin" -> "settings"; else -> current }
    NavigationBar(containerColor = Surface, tonalElevation = 0.dp, modifier = Modifier.height(64.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
        dests.forEach { d ->
            NavigationBarItem(
                tab == d.route,
                { nav.navigate(d.route) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
                icon = { Icon(d.icon, null) },
                label = { Text(d.label, fontSize = 10.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Ink, selectedTextColor = Accent, indicatorColor = Accent,
                    unselectedIconColor = Muted, unselectedTextColor = Muted
                )
            )
        }
    }
}

@Composable
private fun Screen(title: String, sub: String = "", back: (() -> Unit)? = null, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(Ink),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (back != null) IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Fg) }
                Column(Modifier.weight(1f)) {
                    Text(title.uppercase(), color = Fg, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (sub.isNotBlank()) Text(sub, color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                InfoButton()
            }
        }
        content()
    }
}

/** Loading bar, error with a retry button, and the "saved copy" note. */
private fun <T> LazyListScope.state(l: Loadable<T>, retry: () -> Unit) {
    if (l.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent, trackColor = Surface2) }
    l.error?.let { e ->
        item {
            Panel {
                Text(t(e), color = Bad, fontSize = 13.sp)
                TextButton(retry) { Text(t("retry").uppercase(), color = Accent, fontWeight = FontWeight.Bold) }
            }
        }
    }
    if (l.stale && l.error == null) item { Text(t("showing_saved"), color = Accent, fontSize = 11.sp) }
}

// ---------- Home ----------
@Composable
private fun Home(vm: PitlaneViewModel, nav: NavHostController) {
    val a by vm.account.collectAsState()
    val r by vm.races.collectAsState()
    val races = r.data ?: emptyList()
    Screen(a.display.ifBlank { t("driver") }, t("racing_companion")) {
        if (a.admin || (a.supporter && !a.supporterHidden)) item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { MyBadges(a) } }
        state(r) { vm.loadRaces() }
        item { Licences() }
        item { DaysDriven(vm, nav, races) }
        item { Section(t("race_summary")) }
        item {
            val recent = races.take(10)
            val last = races.firstOrNull()
            val ch = recent.sumOf { it.irChange }
            Grid(
                listOf(
                    MetricData(t("current_ir"), if (last != null && last.ir > 0) "${last.ir + last.irChange}" else "—"),
                    MetricData(t("ir_change"), if (recent.isEmpty()) "—" else signed(ch), if (ch > 0) Good else if (ch < 0) Bad else Fg, t("last_races", recent.size)),
                    MetricData(t("races"), "${races.size}"),
                    MetricData(t("wins"), "${races.count { it.finish == 1 }}"),
                    MetricData(t("top5"), "${races.count { it.finish in 1..5 }}"),
                    MetricData(t("avg_inc"), if (recent.isEmpty()) "—" else "%.1f".format(recent.map { it.inc }.average()))
                ), columns = 2
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Section(t("recent_races"))
                Spacer(Modifier.weight(1f))
                if (races.size > 5) TextButton({ nav.navigate("races") }) { Text(t("see_all"), color = Accent, fontSize = 12.sp) }
            }
        }
        if (r.data != null && races.isEmpty()) item { Empty(t("no_races")) }
        items(races.take(5)) { x -> RaceRow(x) { vm.race = x; nav.navigate("race") } }
        item { Text(t("ir_estimate"), color = Muted, fontSize = 11.sp) }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("agent_note"), fontSize = 12.sp)
                        Text(
                            if (a.syncUpdated > 0) t("synced", dayTime(a.syncUpdated)) else t("never_synced"),
                            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
                        )
                        a.error?.let { Text(t(it), color = Bad, fontSize = 12.sp) }
                    }
                    TextButton({ vm.sync() }, enabled = !a.busy) { Text(if (a.busy) "…" else t("sync").uppercase(), color = Accent, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

/**
 * The days you drove, like the web and the PC: the last 26 weeks in squares, brighter the more races and sessions that day.
 * A day opens what you drove: the race summaries and the sessions.
 */
@Composable
private fun DaysDriven(vm: PitlaneViewModel, nav: NavHostController, races: List<Race>) {
    val sl by vm.sessions.collectAsState()
    LaunchedEffect(Unit) { if (sl.data == null && !sl.loading) vm.loadSessions() }
    val sessions = sl.data ?: emptyList()
    val zone = java.time.ZoneId.systemDefault()
    fun day(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    val byRace = races.groupBy { day(it.whenMs) }
    val bySes = sessions.groupBy { day(it.started) }
    val today = java.time.LocalDate.now(zone)
    val weeks = 26
    val start = today.minusDays(((today.dayOfWeek.value - 1) + (weeks - 1) * 7).toLong())
    val count = { d: java.time.LocalDate -> (byRace[d]?.size ?: 0) + (bySes[d]?.size ?: 0) }
    val max = ((0 until weeks * 7).maxOfOrNull { count(start.plusDays(it.toLong())) } ?: 1).coerceAtLeast(1)
    val driven = (0 until weeks * 7).count { val d = start.plusDays(it.toLong()); !d.isAfter(today) && count(d) > 0 }
    var open by remember { mutableStateOf<java.time.LocalDate?>(null) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Section(t("days_driven")); Spacer(Modifier.weight(1f)); Text(t("days_in_6m", driven), color = Muted, fontSize = 11.sp)
    }
    Panel {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (w in 0 until weeks) Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (d in 0 until 7) {
                    val date = start.plusDays((w * 7 + d).toLong())
                    val n = count(date)
                    val a = if (n == 0) 0f else listOf(.35f, .6f, .8f, 1f)[(kotlin.math.ceil(n.toDouble() / max * 4).toInt() - 1).coerceIn(0, 3)]
                    Box(
                        Modifier.size(11.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (date.isAfter(today)) Color.Transparent else if (n == 0) Surface2 else Accent.copy(alpha = a))
                            .then(if (n > 0) Modifier.clickable { open = date } else Modifier)
                    )
                }
            }
        }
    }
    open?.let { d ->
        AlertDialog(
            onDismissRequest = { open = null },
            confirmButton = { TextButton({ open = null }) { Text(t("close"), color = Accent) } },
            title = { Text(d.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM")), fontWeight = FontWeight.Black) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    byRace[d]?.forEach { x ->
                        Text("${x.track} · P${x.finish}/${x.field} · ${x.inc}x", fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().clickable { open = null; vm.race = x; nav.navigate("race") }.padding(vertical = 6.dp))
                    }
                    bySes[d]?.forEach { x ->
                        Text("${kindText(x.kind)} · ${x.track} · ${x.car} · ${lapTime(x.best)}", color = Fg,
                            modifier = Modifier.fillMaxWidth().clickable { open = null; vm.session = x; vm.loadLaps(x.id); nav.navigate("session") }.padding(vertical = 6.dp))
                    }
                }
            }
        )
    }
}

@Composable
private fun RaceRow(x: Race, onClick: () -> Unit) {
    Panel(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(x.track, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(x.car, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(day(x.whenMs), if (x.official) t("official") else t("unofficial"), if (x.sof > 0) "SOF ${x.sof}" else "").filter { it.isNotBlank() }.joinToString(" · "),
                    color = Muted, fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (x.dnf) t("dnf") else "P${x.finish}/${x.field}", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = if (x.finish == 1) Purple else Fg)
                Text("iR " + signed(x.irChange), color = if (x.irChange > 0) Good else if (x.irChange < 0) Bad else Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${x.inc}x", color = if (x.inc >= 8) Bad else Muted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun RacesAll(vm: PitlaneViewModel, nav: NavHostController) {
    val r by vm.races.collectAsState()
    Screen(t("all_races"), back = { nav.popBackStack() }) {
        items(r.data ?: emptyList()) { x -> RaceRow(x) { vm.race = x; nav.navigate("race") } }
    }
}

@Composable
private fun RaceDetail(vm: PitlaneViewModel, nav: NavHostController) {
    val x = vm.race ?: return
    Screen(x.track, x.car + " · " + dayTime(x.whenMs), back = { nav.popBackStack() }) {
        item {
            Grid(
                listOf(
                    MetricData(t("start"), "P${x.start}"),
                    MetricData(t("finish"), if (x.dnf) t("dnf") else "P${x.finish}", if (x.finish == 1) Purple else Fg),
                    MetricData("iRating", signed(x.irChange), if (x.irChange > 0) Good else if (x.irChange < 0) Bad else Fg, if (x.ir > 0) "${x.ir} → ${x.ir + x.irChange}" else null),
                    MetricData(t("incidents"), "${x.inc}x", if (x.inc >= 8) Bad else Fg),
                    MetricData(t("pos_gain"), signed(x.start - x.finish), if (x.start > x.finish) Good else if (x.start < x.finish) Bad else Fg),
                    MetricData(t("sof"), if (x.sof > 0) "${x.sof}" else "—")
                ), columns = 3
            )
        }
        item {
            Panel {
                InfoRow(t("best_lap"), lapTime(x.best), Purple)
                InfoRow(t("field_best"), lapTime(x.fieldBest))
                InfoRow(t("average"), lapTime(x.avg))
                InfoRow(t("consistency"), x.consistency?.let { "±%.3f s".format(it) } ?: "—")
                InfoRow(t("pits"), "${x.pits}")
                InfoRow(t("fuel_used"), x.fuelUsed?.let { "%.1f L".format(it) } ?: "—")
                InfoRow(t("laps"), "${x.laps.size} · " + t("drivers", x.field))
            }
        }
        if (x.laps.isNotEmpty()) {
            item { Section(t("laps").uppercase()) }
            item {
                Panel {
                    val best = x.laps.filter { it.time > 0 && !it.cut }.minOfOrNull { it.time }
                    x.laps.forEach { l ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text("L${l.n}", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.width(44.dp))
                            val strike = if (l.cut) TextDecoration.LineThrough else TextDecoration.None
                            Text(lapTime(l.time), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = if (l.cut) Muted else if (l.time == best) Purple else Fg, textDecoration = strike, modifier = Modifier.weight(1f).alpha(if (l.cut) .6f else 1f))
                            Text("P${l.pos}", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp, textDecoration = strike, modifier = Modifier.width(44.dp).alpha(if (l.cut) .6f else 1f))
                            Text(listOfNotNull(if (l.cut) t("invalid").lowercase() else null, if (l.pit) t("pit") else null, if (l.inc > 0) "${l.inc}x" else null).joinToString(" · "), color = if (l.pit && !l.cut) Blue else Bad, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.widthIn(min = 40.dp))
                        }
                    }
                }
            }
        }
        if (x.results.isNotEmpty()) {
            item { Section(t("results")) }
            item {
                Panel {
                    x.results.forEach { p ->
                        val me = p.pos == x.finish
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${p.pos}", color = if (me) Accent else Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.width(30.dp))
                            Text(p.name, fontSize = 12.sp, fontWeight = if (me) FontWeight.Black else FontWeight.Normal, color = if (me) Accent else Fg, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(if (p.ir > 0) "${p.ir}" else "", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                            Text(lapTime(p.best), fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(66.dp))
                            Text("${p.inc}x", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(30.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---------- Analysis ----------
@Composable
private fun Analysis(vm: PitlaneViewModel, nav: NavHostController) {
    val s by vm.sessions.collectAsState()
    val b by vm.bests.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var fCat by rememberSaveable { mutableStateOf("") }
    var fCar by rememberSaveable { mutableStateOf("") }
    var fTrack by rememberSaveable { mutableStateOf("") }
    var fKind by rememberSaveable { mutableStateOf("") }
    var fDay by rememberSaveable { mutableStateOf("") }
    var showDays by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(tab) { if (tab == 0 && s.data == null && !s.loading) vm.loadSessions(); if (tab == 1 && b.data == null && !b.loading) vm.loadBests() }
    Screen(t("analysis"), t("uploaded_by_pc")) {
        item { WebNote(t("coach_web")) }
        item { Tabs(listOf(t("sessions"), t("bests")), tab) { tab = it } }
        if (tab == 0) {
            state(s) { vm.loadSessions() }
            val all = s.data ?: emptyList()
            // many cars, tracks and sessions make a long list: four small menus (discipline, kind of session, car, track)
            // and the days you drove behind one button, like the web's "Choose a session". A car or a track is known by its
            // name without accents or broken characters, so one track never shows twice
            val trackName = { x: CloudSession -> fixText(x.track) + (if (x.trackConfig.isNotBlank()) " · " + fixText(x.trackConfig) else "") }
            val trackOf = { x: CloudSession -> nameKey(trackName(x)) }
            val carOf = { x: CloudSession -> nameKey(x.car) }
            val zone = java.time.ZoneId.systemDefault()
            val dayOf = { x: CloudSession -> java.time.Instant.ofEpochMilli(x.started).atZone(zone).toLocalDate().let { "${it.year}-${it.monthValue}-${it.dayOfMonth}" } }
            val match = { x: CloudSession, skip: String ->
                (skip == "cat" || fCat.isEmpty() || x.cat == fCat) && (skip == "car" || fCar.isEmpty() || carOf(x) == fCar) && (skip == "track" || fTrack.isEmpty() || trackOf(x) == fTrack) &&
                    (skip == "kind" || fKind.isEmpty() || kindOf(x.kind) == fKind) && (skip == "day" || fDay.isEmpty() || dayOf(x) == fDay)
            }
            val forCat = all.filter { match(it, "cat") }
            val forKind = all.filter { match(it, "kind") }
            val forCar = all.filter { match(it, "car") }
            val forTrack = all.filter { match(it, "track") }
            val forDay = all.filter { match(it, "day") }
            val group = { l: List<CloudSession>, key: (CloudSession) -> String, name: (CloudSession) -> String ->
                l.groupBy(key).map { (k, v) -> Triple(k, name(v.first()), v.size) }.sortedBy { it.second.lowercase() }
            }
            if (all.size > 1) item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterMenu(t("lic_all_full"), forCat.size, DISCS.filter { k -> all.any { it.cat == k } }.map { k -> Triple(k, discName(k), forCat.count { it.cat == k }) }, fCat, Modifier.weight(1f)) { fCat = it }
                        FilterMenu(t("kind_all"), forKind.size, listOf("race", "qual", "prac", "test").map { k -> Triple(k, t("filt_$k"), forKind.count { kindOf(it.kind) == k }) }.filter { it.third > 0 || it.first == fKind }, fKind, Modifier.weight(1f)) { fKind = it }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterMenu(t("every_car"), forCar.size, group(forCar, carOf) { fixText(it.car) }, fCar, Modifier.weight(1f)) { fCar = it }
                        FilterMenu(t("every_track"), forTrack.size, group(forTrack, trackOf, trackName), fTrack, Modifier.weight(1f)) { fTrack = it }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton({ showDays = !showDays }) { Text((if (fDay.isEmpty()) t("days_drove") else t("days_drove") + " · " + fDay) + if (showDays) " ▴" else " ▾", color = Accent, fontSize = 13.sp) }
                        Spacer(Modifier.weight(1f))
                        if ((fCat + fCar + fTrack + fKind + fDay).isNotEmpty()) TextButton({ fCat = ""; fCar = ""; fTrack = ""; fKind = ""; fDay = "" }) {
                            Text(t("clear_filters") + " · " + all.count { match(it, "") } + "/" + all.size, color = Accent, fontSize = 13.sp)
                        }
                    }
                    if (showDays) Panel { DayGrid(forDay.groupingBy(dayOf).eachCount(), fDay) { fDay = if (fDay == it) "" else it } }
                }
            }
            val list = s.data?.let { all.filter { x -> match(x, "") } }
            if (list != null && list.isEmpty()) item { Empty(t("no_sessions")) }
            items(list ?: emptyList(), key = { it.id }) { x ->
                Panel(Modifier.clickable { vm.session = x; vm.loadLaps(x.id); nav.navigate("session") }) {
                    Text(fixText(x.track) + if (x.trackConfig.isNotBlank()) " · " + fixText(x.trackConfig) else "", fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(fixText(x.car), color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f, fill = false)) {
                            Text(listOf(kindText(x.kind), day(x.started), t("laps_n", x.laps)).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            LicBadge(x.lic)
                        }
                        Text(lapTime(x.best), color = Purple, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            state(b) { vm.loadBests() }
            val list = b.data
            if (list != null && list.isEmpty()) item { Empty(t("no_sessions")) }
            items(list ?: emptyList()) { x ->
                Panel(Modifier.clickable(enabled = x.bestSessionId != null) {
                    val sess = CloudSession(x.bestSessionId!!, x.last, x.track, x.trackConfig, x.car, "", x.laps, x.best)
                    vm.session = sess; vm.loadLaps(sess.id); nav.navigate("session")
                }) {
                    Text(x.track + if (x.trackConfig.isNotBlank()) " · " + x.trackConfig else "", fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(x.car, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(listOf(day(x.last), t("laps_n", x.laps)).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 12.sp)
                        Text(lapTime(x.best), color = Purple, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionDetail(vm: PitlaneViewModel, nav: NavHostController) {
    val x = vm.session ?: return
    val l by vm.laps.collectAsState()
    val laps = l.data ?: emptyList()
    val valid = laps.filter { it.valid && it.time > 0 }
    val best = valid.minOfOrNull { it.time }
    val nSec = laps.maxOfOrNull { it.sectors.size } ?: 0
    // fastest time of each sector over the valid laps, shown in purple like in the sim
    val bestSec = (0 until nSec).map { i -> valid.mapNotNull { it.sectors.getOrNull(i) }.filter { it > 0 }.minOrNull() }
    // the average of the valid laps: a real figure, not a lap made of the best sectors
    val avg = if (valid.isNotEmpty()) valid.map { it.time }.average() else null
    Screen(x.track, x.car, back = { nav.popBackStack() }) {
        state(l) { vm.loadLaps(x.id) }
        if (x.cat != null) item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(t("viewing", x.car, discName(x.cat)), color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f, fill = false))
                LicBadge(x.lic)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric(t("best"), lapTime(best), Modifier.weight(1f).fillMaxHeight(), Purple)
                Metric(t("average"), lapTime(avg), Modifier.weight(1f).fillMaxHeight())
            }
        }
        item { Row { Section(t("laps").uppercase()); Spacer(Modifier.weight(1f)); Text(t("tap_lap"), color = Muted, fontSize = 11.sp) } }
        items(laps) { lap ->
            Panel(Modifier.clickable { vm.analyse(x, lap, laps, RefKind.MY_BEST); nav.navigate("lap") }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("L${lap.n}", color = Muted, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                    Text(
                        lapTime(lap.time), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                        color = when { !lap.valid -> Muted; lap.time == best -> Purple; else -> Fg },
                        textDecoration = if (!lap.valid) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (best != null && lap.valid && lap.time > best) Text("+%.3f".format(lap.time - best), color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    if (lap.inc > 0) Text("  ${lap.inc}x", color = Bad, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    if (!lap.valid) Text(t("invalid"), color = Bad, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                }
                if (lap.sectors.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        lap.sectors.forEachIndexed { i, s ->
                            Text(
                                "S${i + 1} " + "%.3f".format(s), fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                color = if (lap.valid && s == bestSec.getOrNull(i)) Purple else Muted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LapDetail(vm: PitlaneViewModel, nav: NavHostController) {
    val x = vm.session ?: return
    val a by vm.analysis.collectAsState()
    val laps by vm.laps.collectAsState()
    var kind by rememberSaveable { mutableStateOf(RefKind.MY_BEST) }
    val an: LapAnalysis? = a.data
    // the point you touch on a chart, shared by the three charts
    var pick by remember(an) { mutableStateOf<Int?>(null) }
    Screen(t("lap_analysis") + (an?.let { " · L${it.lap.n}" } ?: ""), x.track + " · " + x.car, back = { nav.popBackStack() }) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t("compare_with"), color = Muted, fontSize = 12.sp)
                RefKind.entries.forEach { k ->
                    FilterChip(kind == k, {
                        kind = k
                        an?.let { vm.analyse(x, it.lap, laps.data ?: emptyList(), k) }
                    }, label = { Text(if (k == RefKind.MY_BEST) t("my_best") else t("community_fastest"), fontSize = 12.sp) })
                }
            }
        }
        state(a) { an?.let { vm.analyse(x, it.lap, laps.data ?: emptyList(), kind) } }
        item { WebNote(t("coach_web")) }
        if (an != null) {
            item {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric(t("you"), lapTime(an.lap.time), Modifier.weight(1f).fillMaxHeight(), Accent)
                    Metric(an.refLabel.ifBlank { t("ref") }, lapTime(an.refTime), Modifier.weight(1f).fillMaxHeight(), Blue)
                    val d = an.refTime?.let { an.lap.time - it }
                    Metric(t("delta"), d?.let { "%+.3f".format(it) } ?: "—", Modifier.weight(1f).fillMaxHeight(), if (d == null) Fg else if (d <= 0) Good else Bad)
                }
            }
            // the car card: what this car does on other tracks (its hardest braking, where the fast drivers shift up, its
            // top speed) against this lap, like the web's coach
            if (an.car != null && an.trace != null) item { CarCard(an.car, an.trace) }
            if (an.trace == null) item { Empty(t("no_trace")) }
            else {
                val c = compare(an.trace, an.ref)
                if (an.ref == null) item { Text(t("no_reference"), color = Muted, fontSize = 12.sp) }
                val refName = an.refLabel.ifBlank { t("ref") }
                val card: @Composable (Int) -> Unit = { i -> PointCard(c, i, refName, an.lap.sectors, an.refSectors) }
                if (an.trace.hasShape) item { TrackMap(c, an.trace, pick) { pick = it } }
                item {
                    Chart(t("speed") + " (km/h)", listOfNotNull(Series(t("you"), c.speedA, Accent), c.speedB?.let { Series(refName, it, Blue) }), c.step, { "%.0f".format(it) },
                        sel = pick, onSel = { pick = it }, card = card)
                }
                c.delta?.let { d -> item { Chart(t("delta") + " (s)", listOf(Series(t("delta"), d, Purple)), c.step, { "%+.3f".format(it) }, zero = true,
                    sel = pick, onSel = { pick = it }) } }
                item { Chart(t("inputs"), listOf(Series(t("throttle"), c.thrA, Good), Series(t("brake"), c.brkA, Bad)), c.step, { "%.0f%%".format(it * 100) }, fixedMax = 1.0,
                    sel = pick, onSel = { pick = it }) }
                if (an.ref != null) {
                    // the coach in four phases (braking, entry, apex, exit), like the web's
                    val cs = corners(c)
                    if (cs.isEmpty()) {
                        item { Section(t("where_time")) }
                        val ls = losses(c)
                        if (ls.isEmpty()) item { Empty(t("all_clean")) }
                        items(ls) { lo ->
                            Panel {
                                Row {
                                    Text(t("at_m", lo.fromM), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                                    Text(t("lost", "%.3f".format(lo.lost)), color = Bad, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                                Text(t("min_speed", "%.0f".format(lo.minA), "%.0f".format(lo.minB)), color = Muted, fontSize = 12.sp)
                            }
                        }
                    } else {
                        val tot = PHASE_KEYS.associateWith { k -> cs.sumOf { maxOf(0.0, it.phases[k] ?: 0.0) } }
                        val worst = tot.maxBy { it.value }
                        item { Section(t("by_phase")) }
                        item {
                            Grid(PHASE_KEYS.map { k -> val v = tot[k] ?: 0.0; MetricData(t("phase_$k"), (if (v > 0) "+" else "") + "%.2f".format(v), if (v > .03) Bad else Muted) }, columns = 4)
                        }
                        item {
                            Text(if (worst.value > .05) t("most_phase", t("phase_" + worst.key).lowercase()) else t("no_phase"), color = Muted, fontSize = 12.sp)
                        }
                        item {
                            val ca = cs.sumOf { it.coastA }
                            val cb = cs.sumOf { it.coastB }
                            Panel { InfoRow(t("coasting"), "${ca.toInt()} m · " + t("ref") + " ${cb.toInt()} m", if (ca - cb > 20) Bad else Fg) }
                        }
                        item { Section(t("where_time")) }
                        val top = cs.filter { it.tip != null }.sortedByDescending { it.lost }.take(5)
                        if (top.isEmpty()) item { Empty(t("all_clean")) }
                        items(top) { k ->
                            Panel {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(t("corner_n", k.n), fontWeight = FontWeight.Black, color = Accent)
                                    Text("  " + t("at_m", k.atM) + " · " + t("phase_" + k.phase), color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    Text("+%.2f".format(k.lost), color = Bad, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                                Text(t(k.tip!!, *k.args.toTypedArray()), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            if (an.lap.sectors.isNotEmpty()) {
                item { Section(t("sectors")) }
                item {
                    Panel {
                        an.lap.sectors.forEachIndexed { i, s ->
                            val r = an.refSectors.getOrNull(i)
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text("S${i + 1}", color = Muted, fontFamily = FontFamily.Monospace, modifier = Modifier.width(40.dp))
                                Text("%.3f".format(s), fontFamily = FontFamily.Monospace, color = Accent, modifier = Modifier.weight(1f))
                                Text(r?.let { "%.3f".format(it) } ?: "—", fontFamily = FontFamily.Monospace, color = Blue, modifier = Modifier.weight(1f))
                                Text(r?.let { "%+.3f".format(s - it) } ?: "", fontFamily = FontFamily.Monospace, color = if (r != null && s <= r) Good else Bad)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Practice, qualifying or race, from iRacing's session type. */
private fun kindText(k: String): String {
    val x = k.lowercase()
    return when {
        "race" in x -> t("kind_race")
        "qual" in x -> t("kind_qual")
        "warm" in x -> t("kind_warm")
        "practice" in x || "test" in x || "offline" in x -> t("kind_prac")
        else -> k
    }
}

data class Series(val label: String, val values: List<Double>, val color: Color)

/**
 * A line chart over the lap distance. Tap, or touch and hold and drag, to read the values at
 * that point, like hovering in the web and PC app. The charts of a lap share the point ([sel], a
 * point index); the first one shows [card], everything at that point, the others the line and their values.
 */
@Composable
private fun Chart(
    title: String, series: List<Series>, step: Double, fmt: (Double) -> String, zero: Boolean = false, fixedMax: Double? = null,
    sel: Int? = null, onSel: ((Int) -> Unit)? = null, card: (@Composable (Int) -> Unit)? = null
) {
    var own by remember { mutableStateOf<Int?>(null) }
    Panel {
        Section(title.uppercase())
        val all = series.flatMap { it.values }.filter { it.isFinite() }
        val n = series.maxOfOrNull { it.values.size } ?: 0
        if (all.isNotEmpty() && n >= 2) ChartBody(series, step, fmt, zero, fixedMax, all, n, if (onSel != null) sel else own, card, onSel ?: { own = it })
    }
}

@Composable
private fun ChartBody(
    series: List<Series>, step: Double, fmt: (Double) -> String, zero: Boolean, fixedMax: Double?, all: List<Double>, n: Int,
    sel: Int?, card: (@Composable (Int) -> Unit)?, onSel: (Int) -> Unit
) {
    var width by remember { mutableIntStateOf(1) }
    fun pickAt(x: Float) = onSel(((x / width.coerceAtLeast(1)) * (n - 1)).roundToInt().coerceIn(0, n - 1))
    Column {
        val idx = sel?.coerceIn(0, n - 1)
        if (card != null) {
            // its place is kept before you touch, so the chart does not move under your finger
            if (idx != null) card(idx)
            else Box(Modifier.fillMaxWidth().padding(bottom = 6.dp).heightIn(min = 108.dp).background(Surface2, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(t("hold_hint"), color = Muted, fontSize = 12.sp)
            }
        } else Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // the values under the finger
            if (idx == null) Text(" ", fontSize = 11.sp)
            else {
                Text("${(idx * step).roundToInt()} m", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                series.forEach { se -> se.values.getOrNull(idx)?.let { Text(se.label + " " + fmt(it), color = se.color, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) } }
            }
        }
        val lo = if (fixedMax != null) 0.0 else if (zero) minOf(all.min(), 0.0) else all.min()
        val hi = fixedMax ?: if (zero) maxOf(all.max(), 0.0) else all.max()
        val span = (hi - lo).takeIf { it > 1e-9 } ?: 1.0
        Canvas(
            Modifier.fillMaxWidth().height(130.dp)
                .onSizeChanged { width = it.width }
                // at once: the finger down picks the point, sliding sideways follows it (up and down still scrolls)
                .pointerInput(n) { detectTapGestures(onPress = { pickAt(it.x) }) }
                .pointerInput(n) {
                    detectHorizontalDragGestures(
                        onDragStart = { pickAt(it.x) },
                        onHorizontalDrag = { change, _ -> pickAt(change.position.x); change.consume() }
                    )
                }
        ) {
            fun y(v: Double) = (size.height * (1 - ((if (v.isFinite()) v else lo) - lo) / span)).toFloat()
            if (zero) drawLine(Line, Offset(0f, y(0.0)), Offset(size.width, y(0.0)), 1f)
            series.forEach { se ->
                val vals = se.values
                if (vals.size < 2) return@forEach
                val p = Path()
                vals.forEachIndexed { i, v ->
                    val px = size.width * i / (vals.size - 1)
                    if (i == 0) p.moveTo(px, y(v)) else p.lineTo(px, y(v))
                }
                drawPath(p, se.color, style = Stroke(width = 2f))
            }
            if (idx != null) {
                val x = size.width * idx / (n - 1).coerceAtLeast(1)
                drawLine(Fg.copy(alpha = 0.6f), Offset(x, 0f), Offset(x, size.height), 1.5f)
                series.forEach { se -> se.values.getOrNull(idx)?.let { drawCircle(se.color, 4f, Offset(x, y(it))) } }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (fixedMax == 1.0) "0%" else fmt(lo), color = Muted, fontSize = 10.sp)
            Text("${((n - 1) * step / 1000).let { "%.1f".format(it) }} km", color = Muted, fontSize = 10.sp)
            Text(if (fixedMax == 1.0) "100%" else fmt(hi), color = Muted, fontSize = 10.sp)
        }
    }
}

/** A small on/off chip, like the web's toggles over the map. */
@Composable
private fun Chip(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (on) Bg else Muted,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) Accent else Surface2).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

// the disciplines iRacing splits its licenses into, and the license classes, like on the web
private val DISCS = listOf("oval", "sports_car", "formula_car", "dirt_oval", "dirt_road")
/** The kind of a session for the filter: race, qual(ifying), prac(tice) or test (drive). */
private fun kindOf(k: String): String { val x = k.lowercase(); return if ("race" in x) "race" else if ("qual" in x) "qual" else if ("test" in x) "test" else if ("prac" in x || "warm" in x || "offline" in x) "prac" else "" }
private val LICS = listOf("R", "D", "C", "B", "A", "P")
private fun discName(k: String?) = if (k in DISCS) t("disc_$k") else ""
private fun licColor(k: String) = when (k) { "R" -> Color(0xFFFF6363); "D" -> Color(0xFFFF8F45); "C" -> Color(0xFFF2C94C); "B" -> Color(0xFF38C97C); "A" -> Color(0xFF5C9DFF); else -> Color(0xFFC9D1DC) }

/** The license class as a small coloured square (R, D, C, B, A, Pro). */
@Composable
private fun LicBadge(k: String?) {
    if (k == null || k !in LICS) return
    val c = licColor(k)
    Text(if (k == "P") "Pro" else k, color = Fg, fontSize = 10.sp, fontWeight = FontWeight.Black,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(c.copy(alpha = 0.55f)).border(1.5.dp, c, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp))
}

/** Your badges, wherever your name shows: Admin, and Supporter unless you hid it. */
@Composable
private fun MyBadges(a: AccountState) {
    if (a.admin) Status("ADMIN", Accent)
    if (a.supporter && !a.supporterHidden) SupBadge()
}

/** The supporter badge: people who donate, given by hand by the owner of Pitlane HQ. */
@Composable
private fun SupBadge() {
    Text("♥ SUPPORTER", color = Color(0xFFFFD6E7), fontSize = 9.sp, fontWeight = FontWeight.Black,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0x55E83E8C)).border(1.dp, Color(0xAAE83E8C), RoundedCornerShape(999.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
}

/** An incident as the game names it, from its points: 1x off track, 2x loss of control (or a slight contact), 4x car contact. */
private fun incName(kind: String) = t(when (kind) { "contact" -> "inc_contact"; "light" -> "inc_light"; "loss" -> "inc_loss"; else -> "inc_off" })

/**
 * The track, drawn from where the car was on this lap (Pitlane HQ records it), coloured where you gain
 * (green) or lose (red) time against the reference, like the map in the web and the PC app. Touch
 * or drag on it to read that point: the charts follow it.
 */
@Composable
private fun TrackMap(c: Compared, tr: Trace, sel: Int?, onSel: (Int) -> Unit) {
    val xs = tr.x ?: return
    val ys = tr.y ?: return
    val incs = tr.incidents
    var showInc by rememberSaveable { mutableStateOf(true) }
    var showBrk by rememberSaveable { mutableStateOf(true) }
    var showCoach by rememberSaveable { mutableStateOf(true) }
    val brkA = remember(c) { brakePoints(c.brkA, c.step) }
    val brkB = remember(c) { brakePoints(c.brkB, c.step) }
    val rings = remember(c) { if (c.delta != null) corners(c).filter { it.lost > .05 } else emptyList() }
    val labelPx = with(LocalDensity.current) { 9.sp.toPx() }
    val incPaint = remember(labelPx) { android.graphics.Paint().apply { color = android.graphics.Color.rgb(0xFF, 0x63, 0x63); textSize = labelPx; isFakeBoldText = true; isAntiAlias = true } }
    val secPaint = remember(labelPx) { android.graphics.Paint().apply { color = android.graphics.Color.rgb(0xFF, 0xB0, 0x2E); textSize = labelPx * 1.1f; isFakeBoldText = true; isAntiAlias = true } }
    val n = xs.size
    val m = c.speedA.size
    val len = (n - 1) * tr.bin
    val idxAt = { i: Int -> ((i * tr.bin) / c.step).roundToInt().coerceIn(0, m - 1) }
    val minX = xs.min(); val maxX = xs.max(); val minY = ys.min(); val maxY = ys.max()
    var size by remember { mutableStateOf(IntSize(1, 1)) }
    fun pt(i: Int): Offset {
        val pad = 18f
        val k = minOf((size.width - 2 * pad) / maxOf(1e-6, maxX - minX).toFloat(), (size.height - 2 * pad) / maxOf(1e-6, maxY - minY).toFloat())
        val ox = (size.width - 2 * pad - (maxX - minX).toFloat() * k) / 2
        val oy = (size.height - 2 * pad - (maxY - minY).toFloat() * k) / 2
        return Offset(pad + (xs[i] - minX).toFloat() * k + ox, size.height - pad - (ys[i] - minY).toFloat() * k - oy)
    }
    fun pickAt(p: Offset) {
        var bi = 0; var bd = Float.MAX_VALUE
        for (i in 0 until n) { val q = pt(i); val d = (q.x - p.x) * (q.x - p.x) + (q.y - p.y) * (q.y - p.y); if (d < bd) { bd = d; bi = i } }
        if (bd < 60f * 60f) onSel(idxAt(bi))
    }
    Panel {
        Section(t("track_map").uppercase())
        Canvas(
            Modifier.fillMaxWidth().height(230.dp).onSizeChanged { size = it }
                .pointerInput(n) { detectTapGestures(onPress = { pickAt(it) }) }
                .pointerInput(n) { detectDragGestures(onDragStart = { pickAt(it) }, onDrag = { change, _ -> pickAt(change.position); change.consume() }) }
        ) {
            // the track
            val track = Path()
            for (i in 0 until n) { val q = pt(i); if (i == 0) track.moveTo(q.x, q.y) else track.lineTo(q.x, q.y) }
            track.close()
            drawPath(track, Line, style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            // where lap A gains or loses against the reference: 48 stretches, stronger where more time changes hands
            val delta = c.delta
            if (delta != null && m > 2) {
                val segs = 48
                val vals = (0 until segs).map { j -> val a = j * (m - 1) / segs; val b = (j + 1) * (m - 1) / segs; delta[b] - delta[a] }
                val mx = maxOf(0.01, vals.maxOf { kotlin.math.abs(it) })
                for (j in 0 until segs) {
                    val v = vals[j]
                    if (kotlin.math.abs(v) < 0.003) continue
                    val i0 = (j * (n - 1) / segs); val i1 = ((j + 1) * (n - 1) / segs)
                    val p = Path()
                    for (i in i0..i1) { val q = pt(i); if (i == i0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y) }
                    val col = if (v > 0) Bad else Good
                    drawPath(p, col.copy(alpha = (0.3 + 0.7 * minOf(1.0, kotlin.math.abs(v) / mx)).toFloat()), style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            // the sectors like the web: the start/finish line, a cut at each sector change, S1, S2, S3 in the middle of each
            val cnt = 3
            fun bar(i: Int, len: Float, col: Color, width: Float) {
                val q = pt(i); val q2 = pt((i + 3).coerceAtMost(n - 1)); val tx = q2.x - q.x; val ty = q2.y - q.y; val tm = maxOf(1e-3f, kotlin.math.hypot(tx, ty)); val nx = -ty / tm; val ny = tx / tm
                drawLine(col, Offset(q.x - nx * len, q.y - ny * len), Offset(q.x + nx * len, q.y + ny * len), width, StrokeCap.Round)
            }
            bar(0, 10f, Fg, 4f)
            for (k in 1 until cnt) bar(k * (n - 1) / cnt, 8f, Fg.copy(alpha = .8f), 2.5f)
            val cx = size.width / 2f; val cy = size.height / 2f
            for (k in 0 until cnt) {
                val i = ((k + .5f) * (n - 1) / cnt).toInt().coerceIn(0, n - 1); val q = pt(i); val q2 = pt((i + 3).coerceAtMost(n - 1))
                val tx = q2.x - q.x; val ty = q2.y - q.y; val tm = maxOf(1e-3f, kotlin.math.hypot(tx, ty)); var nx = -ty / tm; var ny = tx / tm
                if (nx * (q.x - cx) + ny * (q.y - cy) > 0) { nx = -nx; ny = -ny } // the label on the inside of the track
                drawContext.canvas.nativeCanvas.drawText("S${k + 1}", q.x + nx * 22f - labelPx * .6f, q.y + ny * 22f + labelPx * .35f, secPaint)
            }
            // the coach: the corners where you lose time against the reference
            if (showCoach) rings.forEach { k ->
                val q = pt((k.atM / tr.bin).roundToInt().coerceIn(0, n - 1))
                drawCircle(Purple.copy(alpha = .18f), 14f, q); drawCircle(Purple, 14f, q, style = Stroke(width = 2.5f))
            }
            // where each lap brakes: you (accent), the reference (blue)
            if (showBrk) {
                brkB.forEach { d -> val q = pt((d / tr.bin).roundToInt().coerceIn(0, n - 1)); drawCircle(Bg, 7f, q); drawCircle(Blue, 5f, q) }
                brkA.forEach { d -> val q = pt((d / tr.bin).roundToInt().coerceIn(0, n - 1)); drawCircle(Bg, 8f, q); drawCircle(Accent, 6f, q) }
            }
            // the incidents of the lap (✕ and its points, like the game), where they happened
            if (showInc) incs.forEach { (d, pts) ->
                val q = pt((d / tr.bin).roundToInt().coerceIn(0, n - 1))
                drawLine(Bad, Offset(q.x - 6f, q.y - 6f), Offset(q.x + 6f, q.y + 6f), 3.5f, StrokeCap.Round)
                drawLine(Bad, Offset(q.x + 6f, q.y - 6f), Offset(q.x - 6f, q.y + 6f), 3.5f, StrokeCap.Round)
                drawContext.canvas.nativeCanvas.drawText("${pts}x", q.x + 8f, q.y - 7f, incPaint)
            }
            // the point under the finger
            if (sel != null) { val i = ((sel * c.step) / tr.bin).roundToInt().coerceIn(0, n - 1); val q = pt(i); drawCircle(Fg, 9f, q); drawCircle(Accent, 6f, q) }
        }
        // the same switches as the web and the race summary: braking, incidents, coach
        Row(Modifier.fillMaxWidth().padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(t("map_braking"), showBrk) { showBrk = !showBrk }
            if (incs.isNotEmpty()) Chip("✕ " + t("map_incidents"), showInc) { showInc = !showInc }
            if (rings.isNotEmpty()) Chip("Coach", showCoach) { showCoach = !showCoach }
        }
        if (incs.isNotEmpty()) {
            val pts = incs.sumOf { it.pts }
            val by = incs.groupBy { incName(it.kind) }
            Text(t("incidents_sum", incs.size, pts) + ": " + by.entries.joinToString(" · ") { "${it.value.size} ${it.key.lowercase()}" }, color = Bad, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }

        if (sel != null && showInc) {
            val d = sel * c.step
            incs.filter { kotlin.math.abs(it.d - d) < 60 }.forEach { e -> Text(incName(e.kind) + " (${e.pts}x)", color = Bad, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
        }
        val d = c.delta
        Text(
            if (sel != null) "${(sel * c.step).roundToInt()} m · ${"%.0f".format(c.speedA.getOrElse(sel) { 0.0 })} km/h" + (c.speedB?.getOrNull(sel)?.let { " · " + t("ref") + " ${"%.0f".format(it)}" } ?: "") + (d?.getOrNull(sel)?.let { " · %+.3f s".format(it) } ?: "")
            else if (d != null) t("map_hint") else t("map_hint_a"),
            color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Everything at one point of the lap, like the box that follows the mouse on the web and the PC:
 * distance and sector, speed, throttle, brake and gear of both laps, the gap there and the sector
 * times.
 */
@Composable
private fun PointCard(c: Compared, i: Int, refName: String, sec: List<Double>, refSec: List<Double>) {
    val n = c.speedA.size
    val ns = sec.size.coerceAtMost(3).coerceAtLeast(1)
    val s = minOf(ns, i / maxOf(1, n / ns) + 1)
    val d = c.delta?.getOrNull(i)
    val mono = FontFamily.Monospace
    Column(
        Modifier.fillMaxWidth().padding(bottom = 6.dp).heightIn(min = 108.dp).background(Surface2, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${(i * c.step).roundToInt()} m · S$s", fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = mono, modifier = Modifier.weight(1f))
            if (d != null) Text("%+.3f s".format(d), color = if (d <= 0) Good else Bad, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = mono)
        }
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1.4f))
            listOf("km/h", t("throttle"), t("brake"), t("gear")).forEach { h ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { Text(h, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        PointRow(t("you"), Accent, c.speedA.getOrNull(i), c.thrA.getOrNull(i), c.brkA.getOrNull(i), c.gearA?.getOrNull(i))
        if (c.speedB != null) PointRow(refName, Blue, c.speedB.getOrNull(i), c.thrB?.getOrNull(i), c.brkB?.getOrNull(i), c.gearB?.getOrNull(i))
        val a = sec.getOrNull(s - 1)
        val b = refSec.getOrNull(s - 1)
        if (a != null) Row(Modifier.fillMaxWidth()) {
            Text("S$s", color = Muted, fontSize = 12.sp, fontFamily = mono, modifier = Modifier.weight(1.4f))
            Text("%.3f".format(a) + (b?.let { " · " + "%.3f".format(it) } ?: ""), fontSize = 12.sp, fontFamily = mono, modifier = Modifier.weight(3f))
            if (b != null) Text("%+.3f".format(a - b), color = if (a <= b) Good else Bad, fontSize = 12.sp, fontFamily = mono, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PointRow(name: String, color: Color, spd: Double?, thr: Double?, brk: Double?, gear: Double?) {
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.weight(1.4f)) { Text(name, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        listOf(spd?.let { "%.0f".format(it) }, thr?.let { "%.0f%%".format(it * 100) }, brk?.let { "%.0f%%".format(it * 100) }, gear?.let { "%.0f".format(it) })
            .forEach { v -> Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { Text(v ?: "–", fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1) } }
    }
}

// ---------- Community ----------
@Composable
private fun Community(vm: PitlaneViewModel, nav: NavHostController) {
    val c by vm.combos.collectAsState()
    val rp by vm.reports.collectAsState()
    val su by vm.setups.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var q by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(tab) {
        when (tab) {
            0 -> if (c.data == null && !c.loading) vm.loadCombos()
            1 -> if (rp.data == null && !rp.loading) vm.loadReports()
            else -> if (su.data == null && !su.loading) vm.loadSetups()
        }
    }
    fun match(vararg s: String) = q.isBlank() || s.any { it.contains(q, true) }
    LaunchedEffect(Unit) { tab = 0 }
    var sec by rememberSaveable { mutableIntStateOf(0) }
    var cat by rememberSaveable { mutableStateOf("") }
    val acc by vm.account.collectAsState()
    val lg by vm.leagues.collectAsState()
    val uri = LocalUriHandler.current
    var lsec by rememberSaveable { mutableIntStateOf(0) }
    var lcat by rememberSaveable { mutableStateOf("") }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var fName by rememberSaveable { mutableStateOf("") }
    var fAbout by rememberSaveable { mutableStateOf("") }
    var fCat by rememberSaveable { mutableStateOf("") }
    var fDiscord by rememberSaveable { mutableStateOf("") }
    var fWeb by rememberSaveable { mutableStateOf("") }
    var fSched by rememberSaveable { mutableStateOf("") }
    var fCars by rememberSaveable { mutableStateOf("") }
    var fLang by rememberSaveable { mutableStateOf("") }
    var fErr by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(sec, acc.admin) { if (sec == 1 && acc.admin && lg.data == null && !lg.loading) vm.loadLeagues() }
    Screen(t("community"), t("shared_by")) {
        item { Tabs(listOf(t("leaderboards"), t("leagues")), sec) { sec = it } }
        if (sec == 1) {
            // leagues: in development, only the admins get in; everyone else reads that we are working on it
            item {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(t("leagues"), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f)); Status(t("in_development"), Accent) }
                    Text(if (acc.admin) t("leagues_admin") else t("leagues_wip"), color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            if (!acc.admin) return@Screen
            item { Tabs(listOf(t("leagues_explore"), if (editId != null) t("leagues_edit") else t("leagues_post"), t("leagues_mine")), lsec) { lsec = it; if (it != 1) editId = null } }
            val all = lg.data ?: emptyList()
            when (lsec) {
                1 -> item {
                    Panel {
                        OutlinedTextField(fName, { fName = it }, label = { Text(t("league_name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DISCS.forEach { k -> Chip(discName(k), fCat == k) { fCat = if (fCat == k) "" else k } }
                        }
                        OutlinedTextField(fDiscord, { fDiscord = it }, label = { Text(t("league_discord")) }, placeholder = { Text("https://discord.gg/…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(fWeb, { fWeb = it }, label = { Text(t("league_web")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(fSched, { fSched = it }, label = { Text(t("league_when")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(fCars, { fCars = it }, label = { Text(t("league_cars")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(fLang, { fLang = it }, label = { Text(t("league_lang")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(fAbout, { fAbout = it }, label = { Text(t("league_about")) }, minLines = 3, modifier = Modifier.fillMaxWidth())
                        fErr?.let { Text(t(it), color = Bad, fontSize = 12.sp) }
                        Button({
                            fErr = when {
                                fName.trim().length < 3 -> "league_need_name"
                                !Regex("^https://(discord\\.gg|(www\\.)?discord\\.com/invite)/[A-Za-z0-9-]{2,40}/?$").matches(fDiscord.trim()) -> "league_need_discord"
                                else -> null
                            }
                            if (fErr == null) vm.saveLeague(editId, League("", fName.trim(), fAbout.trim(), fCat.ifBlank { null }, fDiscord.trim(), fWeb.trim(), fSched.trim(), fCars.trim(), fLang.trim())) { e ->
                                if (e == null) { editId = null; fName = ""; fAbout = ""; fCat = ""; fDiscord = ""; fWeb = ""; fSched = ""; fCars = ""; fLang = ""; lsec = 2 } else fErr = e
                            }
                        }, modifier = Modifier.padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)) { Text(if (editId != null) t("save") else t("league_publish")) }
                        Text(t("league_note"), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                else -> {
                    state(lg) { vm.loadLeagues() }
                    if (lsec == 0) item {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip(t("disc_all") + " (" + all.size + ")", lcat.isEmpty()) { lcat = "" }
                            DISCS.forEach { k -> Chip(discName(k) + " (" + all.count { it.cat == k } + ")", lcat == k) { lcat = if (lcat == k) "" else k } }
                        }
                    }
                    val shown = if (lsec == 2) all.filter { it.mine } else all.filter { lcat.isEmpty() || it.cat == lcat }
                    if (lg.data != null && shown.isEmpty()) item { Empty(t(if (lsec == 2) "leagues_none_mine" else "leagues_none")) }
                    items(shown, key = { it.id }) { x ->
                        Panel {
                            Text(x.name, fontWeight = FontWeight.Black)
                            Text(listOf(discName(x.cat), x.lang, t("by_name", x.by)).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 12.sp)
                            if (x.about.isNotBlank()) Text(x.about, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                            if (x.schedule.isNotBlank() || x.cars.isNotBlank()) Text(listOf(x.schedule, x.cars).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button({ uri.openUri(x.discord) }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)) { Text(t("join_discord")) }
                                if (x.web.isNotBlank()) TextButton({ uri.openUri(x.web) }) { Text(t("league_site"), color = Accent) }
                            }
                            if (x.mine || acc.admin) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton({ editId = x.id; fName = x.name; fAbout = x.about; fCat = x.cat ?: ""; fDiscord = x.discord; fWeb = x.web; fSched = x.schedule; fCars = x.cars; fLang = x.lang; fErr = null; lsec = 1 }) { Text(t("edit"), color = Accent) }
                                TextButton({ vm.deleteLeague(x.id) }) { Text(t("remove"), color = Bad) }
                            }
                        }
                    }
                }
            }
            return@Screen
        }
        item { WebNote(t("community_web")) }
        // setups and shared race analyses are switched off for now: only the leaderboards, by lap time, per discipline
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(t("disc_all"), cat.isEmpty()) { cat = "" }
                DISCS.forEach { k -> Chip(discName(k) + " (" + (c.data ?: emptyList()).count { it.cat == k } + ")", cat == k) { cat = if (cat == k) "" else k } }
            }
        }
        item { OutlinedTextField(q, { q = it }, label = { Text(t("search")) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        when (tab) {
            0 -> {
                state(c) { vm.loadCombos() }
                val list = (c.data ?: emptyList()).filter { match(it.track, it.car) && (cat.isEmpty() || it.cat == cat) }
                if (c.data != null && list.isEmpty()) item { Empty(t("nothing_found")) }
                items(list, key = { "${it.trackId}-${it.carId}" }) { x ->
                    Panel(Modifier.clickable { vm.combo = x; vm.loadBoard(x); nav.navigate("combo") }) {
                        Text(x.track, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(x.car, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(listOf(discName(x.cat), t("laps_n", x.laps)).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 12.sp)
                            Text(lapTime(x.best), color = Purple, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            1 -> {
                state(rp) { vm.loadReports() }
                val list = (rp.data ?: emptyList()).filter { match(it.track, it.car, it.alias) }
                if (rp.data != null && list.isEmpty()) item { Empty(t("nothing_found")) }
                items(list, key = { it.id }) { x ->
                    Panel {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(x.track, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(x.car, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text((if (x.alias == "Anonymous") t("anonymous") else x.alias) + (if (x.mine) " · " + t("you_badge") else "") + " · " + day(x.created), color = if (x.mine) Accent else Muted, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (x.finish > 0) "P${x.finish}/${x.field}" else "—", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                Text(lapTime(x.best), color = Purple, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            else -> {
                state(su) { vm.loadSetups() }
                val list = (su.data ?: emptyList()).filter { match(it.track, it.car, it.name, it.alias) }
                if (su.data != null && list.isEmpty()) item { Empty(t("nothing_found")) }
                items(list, key = { it.id }) { x ->
                    Panel {
                        Text(x.name, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOf(x.car, x.track).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 12.sp, maxLines = 2)
                        if (x.notes.isNotBlank()) Text(x.notes, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(x.alias + " · " + t("downloads", x.downloads) + " · " + day(x.created), color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComboDetail(vm: PitlaneViewModel, nav: NavHostController) {
    val x = vm.combo ?: return
    val b by vm.board.collectAsState()
    val a by vm.account.collectAsState()
    val all = b.data ?: emptyList()
    val list = all
    val top = all.firstOrNull()?.time
    // the server marks the signed-in driver's own lap (anonymous ones too); older servers: by the public name
    val mineAll = all.indexOfFirst { it.mine }.let { i -> if (i >= 0) i else all.indexOfFirst { a.display.isNotBlank() && it.alias.equals(a.display, true) } }
    Screen(x.track, x.car, back = { nav.popBackStack() }) {
        state(b) { vm.loadBoard(x) }
        if (b.data != null) item {
            Panel {
                if (mineAll == 0 && all.size > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("🏆 ", fontSize = 16.sp); Text(t("you_fastest"), color = Purple, fontWeight = FontWeight.Black) }
                    Text(t("you_fastest_sub", all.size, lapTime(all[0].time)), color = Muted, fontSize = 12.sp)
                } else if (mineAll >= 0) Text(t("your_position", mineAll + 1, all.size, lapTime(all[mineAll].time)), color = Accent, fontWeight = FontWeight.Bold)
                else Text(t("not_on_board"), color = Muted, fontSize = 12.sp)
                Text(listOf(discName(x.cat), t("drivers", all.size)).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 11.sp)
            }
        }
        item { Section(t("fastest_drivers")) }
        items(list.size) { j ->
            val lap = list[j]
            val i = all.indexOf(lap)
            val me = i == mineAll
            Panel(if (lap.prof) Modifier.clickable { vm.loadProfile(lap.id); nav.navigate("profile") } else Modifier) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", color = if (i == 0) Purple else if (me) Accent else Muted, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (lap.alias == "Anonymous") t("anonymous") else lap.alias, fontWeight = FontWeight.Bold, color = if (me) Accent else Fg, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                                textDecoration = if (lap.prof) TextDecoration.Underline else TextDecoration.None)
                            if (lap.sup) SupBadge()
                            if (lap.mine) Text(t("you_badge"), color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        if (lap.field && !lap.mine) Text(t("rival_race"), color = Muted, fontSize = 10.sp)
                        if (lap.sectors.isNotEmpty()) Text(lap.sectors.joinToString("  ") { "%.3f".format(it) }, color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        if (!lap.hasTrace) Text(t("trace_not_shared"), color = Muted, fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(lapTime(lap.time), color = if (i == 0) Purple else Fg, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        if (top != null && i > 0) Text("+%.3f".format(lap.time - top), color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

/** A driver's profile: nickname (never the iRacing name), license classes, recent races, laps on the leaderboards. */
@Composable
private fun ProfileScreen(vm: PitlaneViewModel, nav: NavHostController) {
    val st by vm.profile.collectAsState()
    val p = st.data
    Screen(p?.name ?: t("profile"), if (p != null) t("since", day(p.since)) else "", back = { nav.popBackStack() }) {
        if (p == null) {
            if (st.error != null) item { Empty(if (st.error!!.contains("anonymous", true)) t("anonymous_private") else t(st.error!!)) } else item { Empty(t("loading")) }
            return@Screen
        }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(p.name, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f, fill = false), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (p.supporter && !(p.mine && p.supporterHidden)) SupBadge()
                }
                if (p.anonymous) Text("🔒 " + t(if (p.mine) "anon_mine" else "anon_admins_only"), color = Muted, fontSize = 12.sp)
                if (p.mine && p.supporter && p.supporterHidden) Text(t("badge_hidden"), color = Muted, fontSize = 12.sp)
                val ls = DISCS.filter { p.lics[it] != null }
                if (ls.isNotEmpty()) Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ls.forEach { k -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { LicBadge(p.lics[k]); Text(discName(k), fontSize = 12.sp) } }
                }
                if (p.mine && p.supporter) TextButton({ vm.setBadgeHidden(!p.supporterHidden) }) { Text(t(if (p.supporterHidden) "show_badge" else "hide_badge"), color = Accent) }
            }
        }
        item { Section(t("days_driven").uppercase() + " · " + t("days_in_6m", p.days.size)) }
        item { Panel { DayGrid(p.days) } }
        item { Section(t("recent_races").uppercase()) }
        if (p.races.isEmpty()) item { Empty(t("no_races_yet")) }
        items(p.races.size) { i ->
            val r = p.races[i]
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(listOf(day(r.whenMs), if (r.official) t("official") else "", discName(r.cat)).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f, fill = false))
                    LicBadge(r.lic)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.track, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(r.car, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(if (r.dnf) "DNF" else if (r.finish > 0) "P${r.finish}/${r.field}" else "—", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
                val dif = if (r.start > 0 && r.finish > 0) r.start - r.finish else 0
                Text(listOf(if (dif != 0) (if (dif > 0) "+" else "") + dif + " " + t("places") else "", "${r.inc}x", if (r.irChange != 0) "iR " + (if (r.irChange > 0) "+" else "") + r.irChange else "", if (r.best != null) t("best_short") + " " + lapTime(r.best) else "").filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 11.sp)
            }
        }
        item { Section(t("on_leaderboards").uppercase()) }
        if (p.laps.isEmpty()) item { Empty(t("no_laps_yet")) }
        items(p.laps.size) { i ->
            val l = p.laps[i]
            Panel(if (l.trackId > 0 && l.carId > 0) Modifier.clickable { val c = Combo(l.trackId, l.track, l.carId, l.car, 0, l.time, l.cat); vm.combo = c; vm.loadBoard(c); nav.navigate("combo") } else Modifier) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LicBadge(l.lic)
                    Column(Modifier.weight(1f)) {
                        Text(l.track, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOf(l.car, discName(l.cat)).filter { it.isNotBlank() }.joinToString(" · ") + if (l.anon) " · 🔒 " + t(if (p.mine) "anon_lap_mine" else "anon_lap_admins") else "", color = Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(lapTime(l.time), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        if (l.pos > 0) Text("P${l.pos}" + if (l.of > 0) "/${l.of}" else "", color = if (l.pos == 1) Purple else Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

/** The last 26 weeks in squares, brighter the more sessions that day (keys "y-m-d" like the web). */
/** Text that came through a wrong encoding ("AutÃ³dromo") read back as it was written ("Autódromo"). */
fun fixText(s: String): String = if (s.contains('Ã') || s.contains('Â')) runCatching { String(s.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8) }.getOrNull()?.takeIf { !it.contains('\uFFFD') } ?: s else s
/** A name as a key: no accents, no case, letters and numbers only, so the same car or track is one entry. */
fun nameKey(s: String): String = java.text.Normalizer.normalize(fixText(s).lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9]+"), "")

/** One small filter menu: "Every car (5)" and, when opened, each choice with how many sessions it has. */
@Composable
private fun FilterMenu(all: String, total: Int, items: List<Triple<String, String, Int>>, value: String, modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val cur = items.firstOrNull { it.first == value }
    Box(modifier) {
        OutlinedButton({ open = true }, Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            border = BorderStroke(1.dp, if (cur != null) Accent else Muted.copy(alpha = 0.4f))) {
            Text(cur?.let { "${it.second} (${it.third})" } ?: "$all ($total)", color = if (cur != null) Accent else Fg, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(" ▾", color = Muted, fontSize = 12.sp)
        }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem({ Text("$all ($total)", fontWeight = if (cur == null) FontWeight.Bold else FontWeight.Normal) }, { open = false; onPick("") })
            items.forEach { (k, n, c) -> DropdownMenuItem({ Text("$n ($c)", fontWeight = if (k == value) FontWeight.Bold else FontWeight.Normal) }, { open = false; onPick(k) }) }
        }
    }
}

@Composable
private fun DayGrid(days: Map<String, Int>, selected: String = "", onDay: ((String) -> Unit)? = null) {
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone)
    val start = today.minusDays(((today.dayOfWeek.value - 1) + 25 * 7).toLong())
    val key = { d: java.time.LocalDate -> "${d.year}-${d.monthValue}-${d.dayOfMonth}" }
    val max = (days.values.maxOrNull() ?: 1).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (w in 0 until 26) Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            for (d in 0 until 7) {
                val day = start.plusDays((w * 7 + d).toLong())
                val n = if (day.isAfter(today)) -1 else days[key(day)] ?: 0
                val lv = if (n > 0) ((n.toFloat() / max) * 4).toInt().coerceIn(1, 4) else 0
                val k = key(day)
                // a day with sessions can be pressed (Analysis: only that day's sessions)
                Box(Modifier.size(if (onDay != null) 14.dp else 10.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (n < 0) Color.Transparent else if (lv == 0) Surface2 else Accent.copy(alpha = 0.25f + lv * 0.18f))
                    .then(if (k == selected) Modifier.border(2.dp, Fg, RoundedCornerShape(2.dp)) else Modifier)
                    .then(if (onDay != null && n > 0) Modifier.clickable { onDay(k) } else Modifier))
            }
        }
    }
}

// ---------- Live ----------
@Composable
private fun Live(vm: PitlaneViewModel) {
    val s by vm.liveState.collectAsState()
    DisposableEffect(Unit) {
        vm.startLive()
        onDispose { vm.stopLive() }
    }
    val (label, color) = when {
        s.link != LinkState.OPEN -> (if (s.link == LinkState.CONNECTING) t("connecting") else t("offline_s")) to Muted
        !s.pcOnline -> t("pc_offline") to Muted
        !s.simConnected -> t("pc_no_sim") to Accent
        else -> t("live_s") to Good
    }
    val uri = LocalUriHandler.current
    var codeIn by rememberSaveable { mutableStateOf("") }
    var codeErr by remember { mutableStateOf(false) }
    Screen(t("live_title"), t("live_sub")) {
        // your PC, from anywhere: Connect when it is online, Disconnect to stop the live data on this phone
        item {
            Panel {
                Text(t("your_pc_anywhere"), fontWeight = FontWeight.Black)
                when (s.mode) {
                    LiveMode.OWN -> if (s.pcOnline && s.ask == "wait") {
                        // your PC asks you there: Accept / Decline
                        var now by remember { mutableStateOf(System.currentTimeMillis()) }
                        LaunchedEffect(s.askAt) { kotlinx.coroutines.delay(61_000); now = System.currentTimeMillis() }
                        val late = now - s.askAt >= 60_000
                        Text(if (late) t("pc_no_answer") else t("accept_on_pc"), color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (late) Button({ vm.liveAskAgain() }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)) { Text(t("try_again")) }
                            OutlinedButton({ vm.liveWatch(LiveMode.IDLE) }) { Text(t("cancel"), color = Bad) }
                        }
                    } else {
                        Text(if (s.pcOnline) t("watching_pc") else t("waiting_pc"), color = if (s.pcOnline) Good else Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
                        OutlinedButton({ vm.liveWatch(LiveMode.IDLE) }) { Text(t("disconnect"), color = Bad) }
                        if (s.pcOnline && s.ask == "ok") {
                            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Line)
                            Text(t("your_code"), fontWeight = FontWeight.Bold)
                            if (s.myCode.isNotBlank()) {
                                Text(s.myCode, color = Accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 2.sp, modifier = Modifier.padding(vertical = 4.dp))
                                Text(t("your_code_sub"), color = Muted, fontSize = 12.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton({ vm.liveShare(true, true) }) { Text(t("new_code"), color = Accent) }
                                    TextButton({ vm.liveShare(false) }) { Text(t("stop_sharing"), color = Bad) }
                                }
                            } else {
                                Text(t("get_code_sub"), color = Muted, fontSize = 12.sp)
                                TextButton({ vm.liveShare(true) }) { Text(t("get_code"), color = Accent) }
                            }
                        }
                    }
                    LiveMode.CODE -> {
                        Text(t("watching_other_back"), color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
                        Button({ vm.liveWatch(LiveMode.OWN) }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)) { Text(t("connect")) }
                    }
                    else -> {
                        Text(if (s.pcOnline) t("pc_is_online") else t("open_pc"), color = if (s.pcOnline) Good else Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
                        Button({ vm.liveWatch(LiveMode.OWN) }, enabled = s.pcOnline, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)) { Text(t("connect")) }
                        if (System.currentTimeMillis() - s.declinedAt < 60_000) Text(t("declined_on_pc"), color = Bad, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        // the PC app, right under it: it is what sends the telemetry
        item { Action(t("download_pc").uppercase(), t("download_pc_sub"), Icons.Default.Computer) { uri.openUri("https://pitlanehq.app/dl/PitlaneHQ-Setup.exe") } }
        // someone else's telemetry with the code they give you
        item {
            Panel {
                Text(t("watch_other"), fontWeight = FontWeight.Black)
                if (s.mode == LiveMode.CODE) {
                    Text((if (s.pcOnline) t("watching") else t("waiting_their_pc")) + " · " + s.code.chunked(4).joinToString("-"), color = if (s.pcOnline) Good else Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
                    OutlinedButton({ vm.liveWatch(LiveMode.IDLE) }) { Text(t("disconnect"), color = Bad) }
                } else {
                    Text(t("watch_other_sub"), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                    OutlinedTextField(codeIn, { codeIn = it; codeErr = false }, label = { Text("ABCD-EFGH-JK") }, singleLine = true, modifier = Modifier.fillMaxWidth(), isError = codeErr)
                    if (codeErr) Text(t("code_bad"), color = Bad, fontSize = 12.sp)
                    Button({ val c = com.pitlanehq.android.data.Crypto.codeNorm(codeIn); if (com.pitlanehq.android.data.Crypto.codeOk(c)) vm.liveWatch(LiveMode.CODE, c) else codeErr = true },
                        modifier = Modifier.padding(top = 6.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)) { Text(t("watch")) }
                }
            }
        }
        if (s.mode == LiveMode.IDLE || (s.mode == LiveMode.OWN && s.ask != "ok")) return@Screen
        item { Status(label, color) }
        s.message?.let { item { Text(t(it), color = Bad, fontSize = 12.sp) } }
        item {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric(t("speed"), s.num("Speed")?.let { "%.0f".format(it * 3.6) } ?: "—", Modifier.weight(1f).fillMaxHeight())
                Metric(t("gear"), s.num("Gear")?.toInt()?.let { if (it < 0) "R" else if (it == 0) "N" else "$it" } ?: "—", Modifier.weight(1f).fillMaxHeight())
                Metric("RPM", s.num("RPM")?.let { "%.0f".format(it) } ?: "—", Modifier.weight(1f).fillMaxHeight())
            }
        }
        item {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric(t("lap"), s.num("Lap")?.toInt()?.toString() ?: "—", Modifier.weight(1f).fillMaxHeight())
                Metric(t("pos"), s.num("PlayerCarPosition")?.toInt()?.takeIf { it > 0 }?.let { "P$it" } ?: "—", Modifier.weight(1f).fillMaxHeight())
                Metric(t("fuel"), s.num("FuelLevel")?.let { "%.1f L".format(it) } ?: "—", Modifier.weight(1f).fillMaxHeight())
            }
        }
        item {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric(t("current"), lapTime(s.num("LapCurrentLapTime")), Modifier.weight(1f).fillMaxHeight())
                Metric(t("last"), lapTime(s.num("LapLastLapTime")), Modifier.weight(1f).fillMaxHeight())
                Metric(t("best"), lapTime(s.num("LapBestLapTime")), Modifier.weight(1f).fillMaxHeight(), Purple)
            }
        }
        item {
            val d = s.num("LapDeltaToBestLap")
            Metric(t("delta_best"), d?.let { "%+.3f".format(it) } ?: "—", Modifier.fillMaxWidth(), if (d == null) Fg else if (d <= 0) Good else Bad)
        }
        item {
            Panel {
                Section(t("inputs").uppercase())
                Bar(t("throttle"), s.num("Throttle"), Good)
                Bar(t("brake"), s.num("Brake"), Bad)
            }
        }
    }
}

// ---------- DRINKS mode (admins) ----------
// the admin profile: every shared lap and race analysis with the name it shows and who really
// uploaded it (anonymous items and DRINKS drivers too), and the accounts
@Composable
private fun AdminScreen(vm: PitlaneViewModel, nav: NavHostController) {
    val st by vm.admin.collectAsState()
    LaunchedEffect(Unit) { vm.loadAdmin("status") }
    Screen(t("admin_profile"), t("admin_profile_sub"), back = { nav.popBackStack() }) {
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("status" to t("admin_server"), "users" to t("admin_accounts"), "sessions" to t("admin_activity_t"), "uploads" to t("admin_shared"), "blocked" to t("admin_blocked")).forEach { (k, l) ->
                    FilterChip(st.kind == k, { vm.loadAdmin(k) }, label = { Text(l) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent, selectedLabelColor = Ink))
                }
            }
        }
        val list = st.items
        when {
            st.error != null -> item { Empty(t(st.error!!)) }
            list == null -> item { Empty(t("loading")) }
            list.isEmpty() -> item { Empty(t("admin_nothing")) }
            st.kind == "status" -> item {
                val x = list.first()
                val m = x.optJSONObject("mail") ?: org.json.JSONObject()
                val c = x.optJSONObject("counts") ?: org.json.JSONObject()
                val e = m.optJSONObject("lastError")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Panel {
                    Text(t("admin_mail") + ": " + if (m.optBoolean("ready")) t("admin_mail_ok") + " (" + m.optString("via") + ")" else t("admin_mail_off"), fontWeight = FontWeight.Bold, color = if (m.optBoolean("ready")) Good else Bad)
                    Text(m.optString("from"), color = Muted, fontSize = 12.sp)
                    Text(t("admin_mail_last", dayTime(m.optLong("lastOk"))), color = Muted, fontSize = 12.sp)
                    if (e != null) Text(t("admin_mail_err") + ": " + listOf("message", "reply", "status", "body").map { e.optString(it) }.filter { it.isNotBlank() }.joinToString(" · "), color = Bad, fontSize = 12.sp)
                }
                val g = x.optJSONObject("config") ?: org.json.JSONObject()
                Panel {
                    Text(t("admin_counts", c.optInt("accounts"), c.optInt("verified"), c.optInt("sessions"), c.optInt("shared")), fontSize = 12.sp)
                    Text(t("admin_activity", c.optInt("new7"), c.optInt("drivers7"), c.optInt("sessions1"), c.optInt("sessions7")), color = Muted, fontSize = 12.sp)
                    Text(t("admin_community", c.optInt("boards"), c.optInt("leagues"), if (g.optBoolean("leaguesOpen")) t("admin_open") else t("admin_only")), color = Muted, fontSize = 12.sp)
                    Text(t("admin_sup", c.optInt("supporters"), c.optInt("supportersPatreon"), c.optInt("patrons")), color = Muted, fontSize = 12.sp)
                    Text(t("admin_model", c.optInt("learnt"), c.optInt("models")) + " · " + t("admin_dirty", c.optInt("modelsDirty")), color = Muted, fontSize = 12.sp)
                    Text(if (g.optBoolean("patreon")) t("admin_patreon_ok") else t("admin_patreon_off"), color = if (g.optBoolean("patreon")) Good else Bad, fontSize = 12.sp)
                }
                // the tools: the coach models learn again, the blocked sign-ins open again
                Panel {
                    Text(t("admin_tools_t"), fontWeight = FontWeight.Bold)
                    TextButton({ vm.adminTool("models") }) { Text(t("admin_rebuild"), color = Accent) }
                    TextButton({ vm.adminTool("unlock") }) { Text(t("admin_unlock", c.optInt("authFails")), color = Accent) }
                }
                }
            }
            st.kind == "sessions" -> items(list.size) { i ->
                val x = list[i]
                Panel {
                    Text(x.optString("track") + (x.optString("trackConfig").takeIf { it.isNotBlank() && it != "null" }?.let { " · $it" } ?: ""), fontWeight = FontWeight.Bold)
                    Text(x.optString("car") + " · " + kindText(x.optString("kind")) + " · " + t("laps_n", x.optInt("laps")), color = Muted, fontSize = 12.sp)
                    Text(dayTime(x.optLong("started")) + " · " + x.optString("who").takeIf { it.isNotBlank() && it != "null" }.orEmpty(), color = Muted, fontSize = 12.sp)
                }
            }
            st.kind == "blocked" -> items(list.size) { i ->
                val x = list[i]
                val a = x.optJSONObject("account")
                Panel {
                    Text(x.optString("kind") + " · " + (a?.optString("display") ?: x.optString("net").takeIf { it.isNotBlank() && it != "null" } ?: "—") + if (x.optBoolean("blocked")) " · " + t("admin_is_blocked") else "", fontWeight = FontWeight.Bold, color = if (x.optBoolean("blocked")) Bad else Fg)
                    Text(t("admin_tries", x.optInt("n")) + " · " + dayTime(x.optLong("last")), color = Muted, fontSize = 12.sp)
                    TextButton({ vm.adminUnlock("k", x.optString("k"), "blocked") }) { Text(t("admin_unblock"), color = Accent) }
                }
            }
            st.kind == "users" -> items(list.size) { i ->
                val u = list[i]
                var ask by remember { mutableStateOf(false) }
                var since by remember { mutableStateOf<String?>(null) }
                val id = u.optString("id")
                val ms = u.optLong("memberSince").takeIf { it > 0 } ?: u.optLong("created")
                Panel {
                    Text(u.optString("display", "–") + if (u.optBoolean("admin")) " · Admin" else "", fontWeight = FontWeight.Bold)
                    Text(id, color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(t("admin_since", day(ms)) + if (u.optLong("memberSince") > 0) " ✎" else "", color = Muted, fontSize = 12.sp)
                    Text(t("admin_user_line", u.optInt("sessions"), u.optInt("laps"), u.optInt("guests")), color = Muted, fontSize = 12.sp)
                    // help with an account: confirm its email, turn off its two-step sign-in, sign it out, rename it, unblock it
                    var rename by remember { mutableStateOf<String?>(null) }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (u.optInt("verified") != 1) TextButton({ vm.adminAccount(id, "verify") }) { Text(t("admin_verify"), color = Accent) }
                        if (u.optInt("twoFactor") == 1) TextButton({ vm.adminAccount(id, "2fa-off") }) { Text(t("admin_2fa_off"), color = Accent) }
                        TextButton({ vm.adminAccount(id, "signout") }) { Text(t("admin_signout"), color = Accent) }
                        TextButton({ rename = u.optString("display") }) { Text(t("admin_rename"), color = Accent) }
                        TextButton({ vm.adminUnlock("account", id, "users") }) { Text(t("admin_unblock"), color = Accent) }
                    }
                    rename?.let { v ->
                        AlertDialog(
                            onDismissRequest = { rename = null },
                            title = { Text(t("admin_rename")) },
                            text = { OutlinedTextField(v, { rename = it.take(32) }, singleLine = true) },
                            confirmButton = { TextButton({ val n = v.trim(); rename = null; if (n.isNotEmpty()) vm.adminAccount(id, "rename", n) }) { Text(t("save"), color = Accent) } },
                            dismissButton = { TextButton({ rename = null }) { Text(t("cancel"), color = Muted) } }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton({ vm.loadProfile("acct:$id"); nav.navigate("profile") }) { Text(t("profile"), color = Accent) }
                        TextButton({ since = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString() }) { Text(t("admin_set_since"), color = Accent) }
                    }
                    val sup = u.optInt("supporter") == 1
                    TextButton({ vm.adminSupporter(u.optString("id"), !sup) }) {
                        Text((if (sup) "♥ " + t("supporter_remove") else t("supporter_give")) + if (sup && u.optInt("supporterHidden") == 1) " · " + t("supporter_hidden_by") else "", color = if (sup) Color(0xFFFF8FBF) else Accent)
                    }
                    if (!u.optBoolean("admin")) TextButton({ ask = true }) { Text(t("admin_delete_account"), color = Bad) }
                }
                since?.let { v ->
                    val ok = runCatching { java.time.LocalDate.parse(v.trim()) }.getOrNull()
                    AlertDialog(
                        onDismissRequest = { since = null },
                        title = { Text(t("admin_set_since")) },
                        text = { OutlinedTextField(v, { since = it }, label = { Text(t("admin_since_hint")) }, singleLine = true, isError = ok == null) },
                        confirmButton = { TextButton({ if (ok != null) { since = null; vm.adminSince(id, ok.atTime(12, 0).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()) } }, enabled = ok != null) { Text(t("save"), color = Accent) } },
                        dismissButton = { Row { if (u.optLong("memberSince") > 0) TextButton({ since = null; vm.adminSince(id, null) }) { Text(t("admin_account_date"), color = Muted) }; TextButton({ since = null }) { Text(t("cancel"), color = Muted) } } }
                    )
                }
                if (ask) AlertDialog(
                    onDismissRequest = { ask = false },
                    text = { Text(t("admin_delete_ask", u.optString("display"))) },
                    confirmButton = { TextButton({ ask = false; vm.adminDeleteUser(u.optString("id")) }) { Text(t("delete"), color = Bad) } },
                    dismissButton = { TextButton({ ask = false }) { Text(t("cancel"), color = Muted) } }
                )
            }
            else -> items(list.size) { i ->
                val x = list[i]
                Panel {
                    Text(x.optString("track") + " · " + x.optString("car"), fontWeight = FontWeight.Bold)
                    Text((if (x.optString("kind") == "laps") t("lap") else t("race")) + " · " + lapTime(x.optDouble("time").takeIf { !it.isNaN() }), color = Muted, fontSize = 12.sp)
                    Text(t("admin_shown_as", x.optString("shownAs")) + if (x.optInt("anon") == 1) " 🔒" else "", fontSize = 12.sp)
                    Text(t("admin_real", x.optString("realUploader")), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    TextButton({ vm.adminDelete(x.optString("kind"), x.optString("id")) }) { Text(t("delete"), color = Bad) }
                }
            }
        }
    }
}

@Composable
private fun DrinksScreen(vm: PitlaneViewModel, nav: NavHostController) {
    val s by vm.liveState.collectAsState()
    val demo by vm.demo.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    val taken by vm.drinksTaken.collectAsState()
    DisposableEffect(Unit) {
        vm.startLive()
        onDispose { vm.stopLive() }
    }
    val d = s.drinks
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    var forgetting by rememberSaveable { mutableStateOf<String?>(null) }
    editing?.let { from ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(t("drinks_rename"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("drinks_rename_note", from), color = Muted, fontSize = 12.sp)
                    OutlinedTextField(newName, { newName = it.take(32) }, label = { Text(t("drinks_name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton({ vm.renameDrinksGuest(from, newName, d?.guests ?: emptyList()); editing = null }, enabled = newName.isNotBlank() && newName.trim() != from) { Text(t("save"), color = Accent) } },
            dismissButton = { TextButton({ editing = null }) { Text(t("cancel"), color = Muted) } }
        )
    }
    forgetting?.let { g ->
        AlertDialog(
            onDismissRequest = { forgetting = null },
            title = { Text(t("drinks_forget"), fontWeight = FontWeight.Black) },
            text = { Text(t("drinks_forget_q", g), color = Muted, fontSize = 13.sp) },
            confirmButton = { TextButton({ vm.forgetDrinksGuest(g); forgetting = null }) { Text(t("drinks_forget"), color = Bad) } },
            dismissButton = { TextButton({ forgetting = null }) { Text(t("cancel"), color = Muted) } }
        )
    }
    Screen(t("drinks"), t("drinks_sub"), back = { nav.popBackStack() }) {
        item { Status(if (s.pcOnline) t("pc_online") else if (s.link == LinkState.CONNECTING) t("connecting") else t("pc_offline"), if (s.pcOnline) Good else Muted) }
        s.message?.let { item { Text(t(it), color = Bad, fontSize = 12.sp) } }
        item { Panel { Text(t("drinks_note"), color = Muted, fontSize = 12.sp) } }
        when {
            demo -> item { Empty(t("drinks_demo")) }
            d == null -> item { Empty(t("drinks_wait")) }
            !d.admin -> item { Empty(t("drinks_pc_admin")) }
            else -> {
                item {
                    Panel {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t("drinks_on"), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Switch(d.on, { vm.setDrinks(it, d.guest, d.guestAuto) })
                        }
                        Text(t("drinks_now", d.driver.ifBlank { t("you") }), color = if (d.on) Accent else Muted, fontWeight = FontWeight.Bold)
                    }
                }
                if (d.on) {
                    item {
                        Panel {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(t("drinks_auto"), fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Switch(d.guestAuto, { vm.setDrinks(true, d.guest, it) })
                            }
                            if (!d.guestAuto) {
                                taken?.let { Text(t("name_taken_drinks", it), color = Bad, fontSize = 12.sp) }
                                OutlinedTextField(name, { name = it.take(32) }, label = { Text(t("drinks_name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                    Button({ vm.setDrinksGuest(name.trim(), d.guests); name = "" }, enabled = name.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)) { Text(t("drinks_set")) }
                                    OutlinedButton({ vm.setDrinks(true, "", false) }) { Text(t("drinks_me")) }
                                }
                            }
                        }
                    }
                    if (!d.guestAuto && d.guests.isNotEmpty()) {
                        item { Section(t("drinks_recent")) }
                        item {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                d.guests.forEach { g ->
                                    FilterChip(g.equals(d.guest, true), { vm.setDrinks(true, g, false) }, label = { Text(g) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent, selectedLabelColor = Ink))
                                }
                            }
                        }
                        // change a name (the laps already shared change too) or take it off the list (they stay)
                        item {
                            Panel {
                                d.guests.forEach { g ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(g, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                        IconButton({ editing = g; newName = g }) { Icon(Icons.Default.Edit, t("drinks_rename"), tint = Muted) }
                                        IconButton({ forgetting = g }) { Icon(Icons.Default.Close, t("drinks_forget"), tint = Muted) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Settings ----------
@Composable
private fun Settings(vm: PitlaneViewModel, nav: NavHostController) {
    val a by vm.account.collectAsState()
    val d by vm.devices.collectAsState()
    val demo by vm.demo.collectAsState()
    val uri = LocalUriHandler.current
    LaunchedEffect(Unit) { if (d.data == null && !d.loading) vm.loadDevices() }
    Screen(t("account_tab")) {
        item { Section(t("account")) }
        item {
            Panel {
                var showMail by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(a.display.ifBlank { t("driver") }, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MyBadges(a)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (showMail) a.email else "•".repeat(a.email.length.coerceIn(8, 14)), color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f, fill = false))
                    TextButton({ showMail = !showMail }) { Text(t(if (showMail) "hide" else "show"), color = Accent, fontSize = 12.sp) }
                }
                Text(if (a.verified) t("verified") else t("not_verified"), color = if (a.verified) Good else Accent, fontSize = 11.sp)
            }
        }
        item { TwoFactorPanel(vm, a) }
        item { Action(t("web").uppercase(), t("web_sub"), Icons.Default.OpenInBrowser) { uri.openUri(WEB_APP) } }
        if (PATREON_URL.isNotEmpty()) item { Action(t("support").uppercase(), t("support_sub"), Icons.Default.Favorite) { uri.openUri(PATREON_URL) } }
        item { Section(t("devices")) }
        state(d) { vm.loadDevices() }
        items(d.data ?: emptyList(), key = { it.id }) { dev ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (dev.device.contains("Android", true) || dev.device.contains("iPhone", true)) Icons.Default.PhoneAndroid else Icons.Default.Computer, null, tint = Muted)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (dev.current) t("this_device") else dev.device.ifBlank { "—" }, fontWeight = FontWeight.Bold)
                        Text(dayTime(dev.lastSeen), color = Muted, fontSize = 11.sp)
                    }
                    if (!dev.current) TextButton({ vm.revoke(dev.id) }) { Text(t("sign_out_device"), color = Bad, fontSize = 12.sp) }
                }
            }
        }
        if ((d.data?.size ?: 0) > 1) item { TextButton({ vm.revoke("others") }) { Text(t("sign_out_others"), color = Bad) } }
        item { Section(t("language")) }
        item {
            Panel {
                listOf("system" to t("lang_system"), "en" to "English", "es" to "Español").forEach { (k, label) ->
                    Row(Modifier.fillMaxWidth().clickable { vm.setLanguage(k) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(I18n.choice == k, { vm.setLanguage(k) })
                        Text(label)
                    }
                }
            }
        }
        // admins: the app as everyone sees it, to test it (everything for admins and in development hides)
        if (a.realAdmin) item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("as_user"), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Switch(!a.admin, { vm.setAsUser(it) })
                }
                Text(t("as_user_sub"), color = Muted, fontSize = 12.sp)
            }
        }
        if (a.admin) item { Section(t("admin_tools")) }
        if (a.signedIn) item { Action(t("my_profile").uppercase(), t("my_profile_sub"), Icons.Default.Person) { vm.loadProfile(null); nav.navigate("profile") } }
        if (a.admin) item { Action(t("admin_profile").uppercase(), t("admin_profile_sub"), Icons.Default.AdminPanelSettings) { nav.navigate("admin") } }
        if (a.admin) item { Action(t("drinks").uppercase(), t("drinks_sub"), Icons.Default.LocalBar) { nav.navigate("drinks") } }
        if (a.admin) item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("demo_mode"), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Switch(demo, { vm.setDemo(it) })
                }
                Text(t("demo_note"), color = Muted, fontSize = 12.sp)
            }
        }
        item { Section(t("your_data")) }
        item { Panel { Text(t("privacy"), color = Muted, fontSize = 12.sp) } }
        item {
            Button(
                onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = Bad)
            ) { Text(t("sign_out").uppercase(), fontWeight = FontWeight.Bold) }
        }
        item { Text(t("version", BuildConfig.VERSION_NAME.replace("-", " ")), color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth()) }
    }
}

/** Two-step sign-in with an authenticator app: optional, recommended. Setup: password → link/key → first code → recovery codes once. */
@Composable
private fun TwoFactorPanel(vm: PitlaneViewModel, a: AccountState) {
    val setup by vm.twoFactorSetup.collectAsState()
    val codes by vm.recoveryCodes.collectAsState()
    val err by vm.twoFactorError.collectAsState()
    var step by remember { mutableStateOf("") } // "", "pw", "off"
    var pw by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val uri = LocalUriHandler.current
    val clip = LocalClipboardManager.current
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t("two_factor"), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(if (a.twoFactor) t("two_factor_on").uppercase() else t("recommended").uppercase(), color = if (a.twoFactor) Good else Accent, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
        Text(if (a.twoFactor) t("two_factor_is_on", a.recoveryLeft) else t("two_factor_rec"), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))
        if (a.twoFactor) OutlinedButton({ pw = ""; code = ""; vm.twoFactorError.value = null; step = "off" }) { Text(t("turn_off").uppercase(), color = Bad) }
        else Button({ pw = ""; code = ""; vm.twoFactorError.value = null; step = "pw" }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)) { Text(t("turn_on").uppercase(), fontWeight = FontWeight.Black) }
    }
    val field: @Composable (String, Boolean) -> Unit = { label, secret ->
        OutlinedTextField(
            if (secret) pw else code, { if (secret) pw = it else code = it }, label = { Text(label) }, singleLine = true,
            visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else KeyboardType.Number), modifier = Modifier.fillMaxWidth()
        )
    }
    if (step == "pw" && setup == null) AlertDialog(
        onDismissRequest = { step = "" },
        title = { Text(t("turn_on") + " · " + t("two_factor"), fontWeight = FontWeight.Black) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(t("two_factor_pw"), color = Muted, fontSize = 12.sp); field(t("password"), true); err?.let { Text(t(it), color = Bad, fontSize = 12.sp) } } },
        confirmButton = { TextButton({ vm.setup2fa(pw) }, enabled = pw.isNotEmpty()) { Text(t("continue"), color = Accent) } },
        dismissButton = { TextButton({ step = "" }) { Text(t("cancel"), color = Muted) } }
    )
    setup?.let { st ->
        AlertDialog(
            onDismissRequest = { vm.twoFactorSetup.value = null; step = "" },
            title = { Text(t("two_factor"), fontWeight = FontWeight.Black) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("two_factor_scan"), color = Muted, fontSize = 12.sp)
                    Button({ uri.openUri(st.url) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)) { Text(t("open_in_app").uppercase(), fontWeight = FontWeight.Black) }
                    Text(t("key") + ": " + st.secret.chunked(4).joinToString(" "), fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.clickable { clip.setText(AnnotatedString(st.secret)) })
                    field(t("code"), false)
                    err?.let { Text(t(it), color = Bad, fontSize = 12.sp) }
                }
            },
            confirmButton = { TextButton({ vm.enable2fa(code) }, enabled = code.length >= 6) { Text(t("turn_on"), color = Accent) } },
            dismissButton = { TextButton({ vm.twoFactorSetup.value = null; step = "" }) { Text(t("cancel"), color = Muted) } }
        )
    }
    codes?.let { cs ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(t("two_factor") + " · " + t("two_factor_on"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("two_factor_codes"), color = Muted, fontSize = 12.sp)
                    Text(cs.joinToString("\n"), fontFamily = FontFamily.Monospace, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().background(Surface2).padding(10.dp))
                }
            },
            confirmButton = { TextButton({ vm.recoveryCodes.value = null; step = "" }) { Text(t("saved_them"), color = Accent, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton({ clip.setText(AnnotatedString(cs.joinToString("\n"))) }) { Text(t("copy"), color = Muted) } }
        )
    }
    if (step == "off") AlertDialog(
        onDismissRequest = { step = "" },
        title = { Text(t("turn_off") + " · " + t("two_factor"), fontWeight = FontWeight.Black) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(t("two_factor_off"), color = Muted, fontSize = 12.sp); field(t("password"), true); field(t("code"), false); err?.let { Text(t(it), color = Bad, fontSize = 12.sp) } } },
        confirmButton = { TextButton({ vm.disable2fa(pw, code) { step = "" } }, enabled = pw.isNotEmpty() && code.isNotBlank()) { Text(t("turn_off"), color = Bad) } },
        dismissButton = { TextButton({ step = "" }) { Text(t("cancel"), color = Muted) } }
    )
}

// ---------- pieces ----------
private data class MetricData(val label: String, val value: String, val color: Color = Fg, val sub: String? = null)

/** Metric boxes in rows of the same height, so every box lines up. */
@Composable
private fun Grid(items: List<MetricData>, columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { m -> Metric(m.label, m.value, Modifier.weight(1f).fillMaxHeight(), m.color, m.sub) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Licences per category: in development until iRacing switches its data API back on. */
@Composable
private fun Licences() {
    val cats = listOf("cat_sports", "cat_formula", "cat_oval", "cat_dirt_road", "cat_dirt_oval")
    var cat by rememberSaveable { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Section(t("licences"))
            Spacer(Modifier.weight(1f))
            Status("WIP", Accent)
        }
        Panel {
            Tabs(cats.map { t(it) }, cat) { cat = it }
            Spacer(Modifier.height(8.dp))
            Grid(listOf(MetricData(t("lic_class"), "—", Muted), MetricData(t("safety"), "—", Muted), MetricData("iRating", "—", Muted)), columns = 3)
            Spacer(Modifier.height(8.dp))
            Text(t("lic_wip"), color = Accent, fontSize = 12.sp)
        }
    }
}

/** What is only in the web and PC app for now, with a button to open the web. */
/** The facts of one lap, as the server reads them: its hardest braking (m/s², the top 5 % of its braking), the speed
 *  it shifted up at in every gear (the highest point it took the gear to), its top speed. Rows [speed, throttle, brake, gear, steering, time]. */
private class LapFacts(val brake: Double?, val shifts: Map<Int, Double>, val vmax: Double)
private fun lapFacts(rows: List<DoubleArray>): LapFacts {
    val dec = ArrayList<Double>()
    val shifts = HashMap<Int, Double>()
    var vmax = 0.0
    for (i in 0 until rows.size - 1) {
        val a = rows[i]
        val b = rows[i + 1]
        if (a.size < 6 || b.size < 6) continue
        if (a[0] > vmax) vmax = a[0]
        val dt = b[5] - a[5]
        if (dt > 0.01 && dt < 2 && a[2] >= 0.5 && a[0] > 12 && a[0] > b[0]) dec.add((a[0] - b[0]) / dt)
        val g = a[3].toInt()
        if (g >= 1 && b[3].toInt() == g + 1 && a[0] > 5) shifts[g] = maxOf(shifts[g] ?: 0.0, a[0])
    }
    dec.sort()
    val brake = if (dec.size >= 5) dec[minOf(dec.size - 1, Math.floor(0.95 * (dec.size - 1) + 0.5).toInt())] else null
    return LapFacts(brake, shifts, vmax)
}

/** The car card: what this car does on other tracks (its hardest braking, where the fast drivers shift up, its top
 *  speed), learnt by the server from its laps everywhere, against what this lap did. Like the web's coach. */
@Composable
private fun CarCard(k: org.json.JSONObject, tr: Trace) {
    val f = remember(tr) { lapFacts(tr.rows) }
    val kmh = { v: Double -> "${Math.round(v * 3.6)} km/h" }
    val gee = { v: Double -> "%.2f g".format(v / 9.81) }
    val cardBrake = k.optDouble("brake", 0.0).takeIf { it > 0 }
    // the gear this lap shifted earliest against the fast drivers (the highest gear when the lap has no gear read)
    var pg = 0
    var pv = 0.0
    var pm: Double? = null
    var pd: Double? = null
    val sh = k.optJSONArray("shifts")
    if (sh != null) for (i in 0 until sh.length()) {
        val p = sh.optJSONArray(i) ?: continue
        val g = p.optInt(0)
        val v = p.optDouble(1)
        val mv = f.shifts[g]
        val d = mv?.let { it - v }
        val better = pd
        if (pg == 0 || (d != null && (better == null || d < better))) { pg = g; pv = v; pm = mv; pd = d }
    }
    val brakeLine = t("car_brake") + ": " + (cardBrake?.let(gee) ?: "—") + " · " +
        (f.brake?.let { b -> t("this_lap") + " " + gee(b) + (cardBrake?.let { c -> " (${Math.round(b / c * 100)} %)" } ?: "") } ?: t("car_no_brake"))
    val mine = pm
    val shiftLine = t("car_shifts") + ": " + if (pg > 0) "$pg→${pg + 1} ${kmh(pv)}" +
        (mine?.let { " · " + t("this_lap") + " " + kmh(it) + " (" + (if (it >= pv) "+" else "−") + Math.round(Math.abs(it - pv) * 3.6) + ")" } ?: "") else t("car_no_shifts")
    val at = k.optString("vmaxTrack").takeIf { it.isNotBlank() && it != "null" }
    val vmaxLine = t("car_vmax") + ": " + kmh(k.optDouble("vmax", 0.0)) + (at?.let { " " + t("car_at", it) } ?: "") + (if (f.vmax > 0) " · " + t("this_lap") + " " + kmh(f.vmax) else "")
    Panel {
        Text(t("car_card"), fontWeight = FontWeight.Bold)
        Text(t("car_card_from", k.optInt("n"), k.optInt("tracks")), color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(brakeLine, fontSize = 13.sp)
        Text(shiftLine, fontSize = 13.sp)
        Text(vmaxLine, fontSize = 13.sp)
    }
}

@Composable
private fun WebNote(text: String) {
    val uri = LocalUriHandler.current
    Panel {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, null, tint = Blue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(text, fontSize = 12.sp, color = Fg)
                TextButton({ uri.openUri(WEB_APP) }, contentPadding = PaddingValues(0.dp)) { Text(t("open_web"), color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
@Composable
private fun Tabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, l ->
            FilterChip(
                selected == i, { onSelect(i) }, label = { Text(l, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent, selectedLabelColor = Ink)
            )
        }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Line), shape = MaterialTheme.shapes.small
    ) { Column(Modifier.padding(12.dp), content = content) }
}

@Composable
private fun Section(s: String) {
    Text(s, color = Muted, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
}

@Composable
private fun Status(s: String, c: Color) {
    Surface(color = Color.Transparent, border = BorderStroke(1.dp, c), shape = MaterialTheme.shapes.extraLarge) {
        Text(s, color = c, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Composable
private fun Metric(l: String, v: String, m: Modifier, color: Color = Fg, sub: String? = null) {
    Panel(m) {
        Text(l.uppercase(), color = Muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(v, color = color, fontWeight = FontWeight.Black, fontSize = 18.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
        sub?.let { Text(it, color = Muted, fontSize = 10.sp, maxLines = 1) }
    }
}

@Composable
private fun InfoRow(l: String, v: String, color: Color = Fg) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(l, color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(v, color = color, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Bar(label: String, v: Double?, c: Color) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(90.dp))
        LinearProgressIndicator(
            progress = { (v ?: 0.0).toFloat().coerceIn(0f, 1f) }, color = c, trackColor = Surface2,
            modifier = Modifier.weight(1f).height(10.dp)
        )
    }
}

@Composable
private fun Empty(s: String) = Panel { Text(s, color = Muted, fontSize = 13.sp) }

@Composable
private fun Action(title: String, sub: String, icon: ImageVector, onClick: () -> Unit) {
    Panel(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black)
                Text(sub, color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}
