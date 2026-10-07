package com.pitlanehq.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.*
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

private const val WEB_APP = "$SERVER/app/?companion=1"

/** Support TrackIQ (Settings); empty: not shown. */
private const val PATREON_URL = "https://www.patreon.com/c/PitlaneHQ/membership"

private data class Dest(val route: String, val label: String, val icon: ImageVector)

private fun day(ms: Long) = if (ms <= 0) "" else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))
private fun dayTime(ms: Long) = if (ms <= 0) "" else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

@Composable
fun PitlaneApp(vm: PitlaneViewModel = viewModel()) {
    val account by vm.account.collectAsState()
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
    val uri = LocalUriHandler.current
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
    Scaffold(containerColor = Ink, bottomBar = { BottomBar(nav) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.statusBarsPadding()) {
                if (!online) Banner(t("showing_saved"), Bad)
                if (demo) Banner(t("demo_banner"), Accent)
                upd?.let { u -> Banner(t("update_available", u.version), Good) { uri.openUri(u.url) } }
            }
            NavHost(nav, "home", Modifier.weight(1f)) {
                composable("home") { Home(vm, nav) }
                composable("races") { RacesAll(vm, nav) }
                composable("race") { RaceDetail(vm, nav) }
                composable("analysis") { Analysis(vm, nav) }
                composable("session") { SessionDetail(vm, nav) }
                composable("lap") { LapDetail(vm, nav) }
                composable("community") { Community(vm, nav) }
                composable("combo") { ComboDetail(vm, nav) }
                composable("live") { Live(vm) }
                composable("settings") { Settings(vm, nav) }
                composable("drinks") { DrinksScreen(vm, nav) }
            }
        }
    }
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
        Dest("community", t("community"), Icons.Default.Groups),
        Dest("live", t("live"), Icons.Default.Sensors),
        Dest("settings", t("settings"), Icons.Default.Settings)
    )
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    val tab = when (current) { "races", "race" -> "home"; "session", "lap" -> "analysis"; "combo" -> "community"; "drinks" -> "settings"; else -> current }
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
        state(r) { vm.loadRaces() }
        item { Licences() }
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
                            Text(lapTime(l.time), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = if (l.time == best) Purple else Fg, modifier = Modifier.weight(1f))
                            Text("P${l.pos}", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.width(44.dp))
                            Text(if (l.cut) "✂" else if (l.pit) t("pit") else if (l.inc > 0) "${l.inc}x" else "", color = if (l.pit && !l.cut) Blue else Bad, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.width(40.dp))
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
    LaunchedEffect(tab) { if (tab == 0 && s.data == null && !s.loading) vm.loadSessions(); if (tab == 1 && b.data == null && !b.loading) vm.loadBests() }
    Screen(t("my_laps"), t("uploaded_by_pc")) {
        item { WebNote(t("coach_web")) }
        item { Tabs(listOf(t("sessions"), t("bests")), tab) { tab = it } }
        if (tab == 0) {
            state(s) { vm.loadSessions() }
            val list = s.data
            if (list != null && list.isEmpty()) item { Empty(t("no_sessions")) }
            items(list ?: emptyList(), key = { it.id }) { x ->
                Panel(Modifier.clickable { vm.session = x; vm.loadLaps(x.id); nav.navigate("session") }) {
                    Text(x.track + if (x.trackConfig.isNotBlank()) " · " + x.trackConfig else "", fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(x.car, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(listOf(kindText(x.kind), day(x.started), t("laps_n", x.laps)).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 12.sp)
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
    val ideal = if (nSec > 0 && bestSec.all { it != null }) bestSec.sumOf { it!! } else null
    Screen(x.track, x.car, back = { nav.popBackStack() }) {
        state(l) { vm.loadLaps(x.id) }
        item {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric(t("best"), lapTime(best), Modifier.weight(1f).fillMaxHeight(), Purple)
                Metric(t("best_sectors"), lapTime(ideal), Modifier.weight(1f).fillMaxHeight())
            }
        }
        item { Row { Section(t("laps").uppercase()); Spacer(Modifier.weight(1f)); Text(t("tap_lap"), color = Muted, fontSize = 11.sp) } }
        items(laps) { lap ->
            Panel(Modifier.clickable { vm.analyse(x, lap, laps, RefKind.MY_BEST); nav.navigate("lap") }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("L${lap.n}", color = Muted, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                    Text(
                        lapTime(lap.time), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                        color = when { !lap.valid -> Bad; lap.time == best -> Purple; else -> Fg }
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

/** An incident as the game names it, from its points: 1x off track, 2x loss of control (or a slight contact), 4x car contact. */
private fun incName(kind: String) = t(when (kind) { "contact" -> "inc_contact"; "light" -> "inc_light"; "loss" -> "inc_loss"; else -> "inc_off" })

/**
 * The track, drawn from where the car was on this lap (TrackIQ records it), coloured where you gain
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
            // sector marks and the start line
            val cnt = 3
            for (k in 1 until cnt) { val q = pt((k * (n - 1) / cnt)); drawCircle(Muted, 5f, q) }
            drawCircle(Fg, 5f, pt(0))
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
            incs.filter { kotlin.math.abs(it.d - d) < 60 }.forEach { e -> Text(incName(e.kind) + " ${e.pts}x", color = Bad, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
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
    Screen(t("community"), t("shared_by")) {
        item { WebNote(t("community_web")) }
        // setups and shared race analyses are switched off for now: only the leaderboards
        item { OutlinedTextField(q, { q = it }, label = { Text(t("search")) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        when (tab) {
            0 -> {
                state(c) { vm.loadCombos() }
                val list = (c.data ?: emptyList()).filter { match(it.track, it.car) }
                if (c.data != null && list.isEmpty()) item { Empty(t("nothing_found")) }
                items(list, key = { "${it.trackId}-${it.carId}" }) { x ->
                    Panel(Modifier.clickable { vm.combo = x; vm.loadBoard(x); nav.navigate("combo") }) {
                        Text(x.track, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(x.car, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t("laps_n", x.laps), color = Muted, fontSize = 12.sp)
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
                                Text(x.alias + " · " + day(x.created), color = Muted, fontSize = 11.sp)
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
    val list = b.data ?: emptyList()
    val top = list.firstOrNull()?.time
    val mine = list.indexOfFirst { a.display.isNotBlank() && it.alias.equals(a.display, true) }
    Screen(x.track, x.car, back = { nav.popBackStack() }) {
        state(b) { vm.loadBoard(x) }
        if (b.data != null) item {
            Panel {
                if (mine >= 0) Text(t("your_position", mine + 1, list.size, lapTime(list[mine].time)), color = Accent, fontWeight = FontWeight.Bold)
                else Text(t("not_on_board"), color = Muted, fontSize = 12.sp)
                Text(t("drivers", list.size), color = Muted, fontSize = 11.sp)
            }
        }
        item { Section(t("fastest_drivers")) }
        itemsIndexed(list) { i, lap ->
            val me = i == mine
            Panel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", color = if (i == 0) Purple else if (me) Accent else Muted, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text(lap.alias, fontWeight = FontWeight.Bold, color = if (me) Accent else Fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    Screen(t("live_title"), t("live_sub")) {
        item { Status(label, color) }
        item { WebNote(t("live_web")) }
        s.message?.let { item { Text(t(it), color = Bad, fontSize = 12.sp) } }
        if (s.link == LinkState.OPEN && !s.pcOnline) item { Empty(t("open_pc")) }
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
        item { Text(t("e2e"), color = Muted, fontSize = 11.sp) }
    }
}

// ---------- DRINKS mode (admins) ----------
@Composable
private fun DrinksScreen(vm: PitlaneViewModel, nav: NavHostController) {
    val s by vm.liveState.collectAsState()
    val demo by vm.demo.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    DisposableEffect(Unit) {
        vm.startLive()
        onDispose { vm.stopLive() }
    }
    val d = s.drinks
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
                                OutlinedTextField(name, { name = it.take(32) }, label = { Text(t("drinks_name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                    Button({ vm.setDrinks(true, name.trim(), false); name = "" }, enabled = name.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)) { Text(t("drinks_set")) }
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
    Screen(t("settings")) {
        item { Section(t("account")) }
        item {
            Panel {
                Text(a.display.ifBlank { t("driver") }, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(a.email, color = Muted, fontSize = 12.sp)
                Text(if (a.verified) t("verified") else t("not_verified"), color = if (a.verified) Good else Accent, fontSize = 11.sp)
            }
        }
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
        if (a.admin) item { Section(t("admin_tools")) }
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
