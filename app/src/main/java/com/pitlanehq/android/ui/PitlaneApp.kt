package com.pitlanehq.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.pitlanehq.android.data.SERVER
import com.pitlanehq.android.model.*
import com.pitlanehq.android.viewmodel.PitlaneViewModel
import java.text.DateFormat
import java.util.Date

private val Ink = Color(0xFF11151B)
private val Surface = Color(0xFF19202A)
private val Surface2 = Color(0xFF1E2631)
private val Line = Color(0xFF2B3542)
private val Fg = Color(0xFFE7EBF1)
private val Muted = Color(0xFF8A97A9)
private val Accent = Color(0xFFFFB02E)
private val Good = Color(0xFF38C97C)
private val Bad = Color(0xFFFF6363)
private val Purple = Color(0xFFB98CFF)

private const val WEB_APP = "$SERVER/app/?companion=1"

private data class Dest(val route: String, val label: String, val icon: ImageVector)

private val dests = listOf(
    Dest("home", "Home", Icons.Default.Home),
    Dest("analysis", "Analysis", Icons.Default.QueryStats),
    Dest("community", "Community", Icons.Default.Groups),
    Dest("live", "Live", Icons.Default.Sensors),
    Dest("profile", "Profile", Icons.Default.Person)
)

@Composable
fun PitlaneApp(vm: PitlaneViewModel = viewModel()) {
    val account by vm.account.collectAsState()
    val scheme = darkColorScheme(
        primary = Accent, onPrimary = Ink, background = Ink, onBackground = Fg, surface = Surface, onSurface = Fg,
        surfaceVariant = Surface2, onSurfaceVariant = Muted, outline = Line, error = Bad
    )
    MaterialTheme(colorScheme = scheme) {
        if (!account.signedIn) LoginScreen(vm, account.error, account.busy) else MainApp(vm)
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
        item { Text("COMPANION", color = Accent, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        item {
            Panel {
                Text("SIGN IN", fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Use the same Pitlane HQ account as on your PC.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    email, { email = it }, label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    password, { password = it }, label = { Text("Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = Bad, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.login(email, password) }, enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                ) { Text(if (busy) "SIGNING IN…" else "SIGN IN", fontWeight = FontWeight.Black) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton({ uri.openUri(WEB_APP) }) { Text("Create an account", color = Accent, fontSize = 12.sp) }
                    TextButton({ uri.openUri("$SERVER/account/forgot") }) { Text("Forgot password?", color = Muted, fontSize = 12.sp) }
                }
            }
        }
        item {
            Text(
                "Your password only unlocks your keys on this phone. It never leaves it.",
                color = Muted, fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun MainApp(vm: PitlaneViewModel) {
    val nav = rememberNavController()
    Scaffold(containerColor = Ink, bottomBar = { BottomBar(nav) }) { pad ->
        NavHost(nav, "home", Modifier.padding(pad)) {
            composable("home") { Home(vm, nav) }
            composable("analysis") { Analysis(vm, nav) }
            composable("session") { SessionDetail(vm, nav) }
            composable("community") { Community(vm, nav) }
            composable("combo") { ComboDetail(vm, nav) }
            composable("live") { Live(vm) }
            composable("profile") { Profile(vm) }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController) {
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    val tab = when (current) { "session" -> "analysis"; "combo" -> "community"; else -> current }
    NavigationBar(containerColor = Surface, tonalElevation = 0.dp) {
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
        Modifier.fillMaxSize().background(Ink).statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (back != null) IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Fg) }
                Column(Modifier.weight(1f)) {
                    Text(title.uppercase(), color = Fg, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (sub.isNotBlank()) Text(sub, color = Muted, fontSize = 12.sp)
                }
            }
        }
        content()
    }
}

// ---------- Home ----------
@Composable
private fun Home(vm: PitlaneViewModel, nav: NavHostController) {
    val a by vm.account.collectAsState()
    val uri = LocalUriHandler.current
    Screen("Pitlane HQ", "Your racing companion") {
        item { Section("ACCOUNT") }
        item {
            Panel {
                Text(a.display.ifBlank { "Pitlane driver" }, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(a.email, color = Muted, fontSize = 12.sp)
            }
        }
        item { Section("PC SETTINGS IN YOUR ACCOUNT") }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (a.syncVersion > 0) "${a.syncedFiles} files · version ${a.syncVersion}" else "Nothing synced yet", fontWeight = FontWeight.Bold)
                        Text(
                            if (a.syncUpdated > 0) "Updated " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(a.syncUpdated))
                            else "Turn on sync in PitlaneHQ.exe → Account", color = Muted, fontSize = 12.sp
                        )
                        a.error?.let { Text(it, color = Bad, fontSize = 12.sp) }
                    }
                    Button({ vm.sync() }, enabled = !a.busy, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)) {
                        Text(if (a.busy) "…" else "SYNC", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { Section("GO TO") }
        item { Action("MY LAPS", "Sessions your PC uploaded", Icons.Default.QueryStats) { nav.navigate("analysis") } }
        item { Action("COMMUNITY", "Fastest drivers per track and car", Icons.Default.Groups) { nav.navigate("community") } }
        item { Action("LIVE TELEMETRY", "Your PC, live, from anywhere", Icons.Default.Sensors) { nav.navigate("live") } }
        item { Action("FULL PITLANE HQ", "Coach, comparisons and everything else on the web", Icons.Default.OpenInBrowser) { uri.openUri(WEB_APP) } }
    }
}

// ---------- Analysis ----------
@Composable
private fun Analysis(vm: PitlaneViewModel, nav: NavHostController) {
    val s by vm.sessions.collectAsState()
    LaunchedEffect(Unit) { if (s.data == null) vm.loadSessions() }
    Screen("My laps", "Sessions uploaded by PitlaneHQ.exe") {
        item { Reload(s.loading) { vm.loadSessions() } }
        s.error?.let { item { Text(it, color = Bad, fontSize = 12.sp) } }
        val list = s.data
        if (list != null && list.isEmpty()) item { Empty("No sessions yet. Drive with PitlaneHQ.exe running and signed in: your laps upload by themselves.") }
        items(list ?: emptyList(), key = { it.id }) { x ->
            Panel(Modifier.clickable { vm.session = x; vm.loadLaps(x.id); nav.navigate("session") }) {
                Text(x.track + if (x.trackConfig.isNotBlank()) " · " + x.trackConfig else "", fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(x.car, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(listOf(x.kind, DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(x.started)), "${x.laps} laps").filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 12.sp)
                    Text(lapTime(x.best), color = Purple, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
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
        if (l.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent) }
        l.error?.let { item { Text(it, color = Bad, fontSize = 12.sp) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("BEST", lapTime(best), Modifier.weight(1f), Purple)
                Metric("BEST SECTORS", lapTime(ideal), Modifier.weight(1f))
            }
        }
        item { Section("LAPS") }
        items(laps) { lap ->
            Panel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("L${lap.n}", color = Muted, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                    Text(
                        lapTime(lap.time), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                        color = when { !lap.valid -> Bad; lap.time == best -> Purple; else -> Fg }
                    )
                    if (!lap.valid) Text("INVALID", color = Bad, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                if (lap.sectors.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        lap.sectors.forEachIndexed { i, t ->
                            Text(
                                "S${i + 1} " + "%.3f".format(t), fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                color = if (lap.valid && t == bestSec.getOrNull(i)) Purple else Muted
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Community ----------
@Composable
private fun Community(vm: PitlaneViewModel, nav: NavHostController) {
    val c by vm.combos.collectAsState()
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { if (c.data == null) vm.loadCombos() }
    Screen("Community", "Laps shared by Pitlane HQ drivers · iRacing") {
        item {
            OutlinedTextField(q, { q = it }, label = { Text("Search track or car") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item { Reload(c.loading) { vm.loadCombos() } }
        c.error?.let { item { Text(it, color = Bad, fontSize = 12.sp) } }
        val list = (c.data ?: emptyList()).filter { q.isBlank() || it.track.contains(q, true) || it.car.contains(q, true) }
        if (c.data != null && list.isEmpty()) item { Empty("No shared laps found.") }
        items(list, key = { "${it.trackId}-${it.carId}" }) { x ->
            Panel(Modifier.clickable { vm.combo = x; vm.loadBoard(x); nav.navigate("combo") }) {
                Text(x.track, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(x.car, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${x.laps} laps", color = Muted, fontSize = 12.sp)
                    Text(lapTime(x.best), color = Purple, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ComboDetail(vm: PitlaneViewModel, nav: NavHostController) {
    val x = vm.combo ?: return
    val b by vm.board.collectAsState()
    val list = b.data ?: emptyList()
    val top = list.firstOrNull()?.time
    Screen(x.track, x.car, back = { nav.popBackStack() }) {
        if (b.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent) }
        b.error?.let { item { Text(it, color = Bad, fontSize = 12.sp) } }
        item { Section("FASTEST DRIVERS") }
        itemsIndexed(list) { i, lap ->
            Panel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", color = if (i == 0) Purple else Muted, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                    Text(lap.alias, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        s.link != LinkState.OPEN -> (if (s.link == LinkState.CONNECTING) "CONNECTING" else "OFFLINE") to Muted
        !s.pcOnline -> "PC OFFLINE" to Muted
        !s.simConnected -> "PC ONLINE · SIM NOT RUNNING" to Accent
        else -> "LIVE" to Good
    }
    Screen("Live telemetry", "From PitlaneHQ.exe through your account") {
        item { Status(label, color) }
        s.message?.let { item { Text(it, color = Bad, fontSize = 12.sp) } }
        if (s.link == LinkState.OPEN && !s.pcOnline) item {
            Empty("Open PitlaneHQ.exe on your PC and sign in with this account. Live telemetry starts by itself.")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("SPEED", s.num("Speed")?.let { "%.0f".format(it * 3.6) } ?: "—", Modifier.weight(1f))
                Metric("GEAR", s.num("Gear")?.toInt()?.let { if (it < 0) "R" else if (it == 0) "N" else "$it" } ?: "—", Modifier.weight(1f))
                Metric("RPM", s.num("RPM")?.let { "%.0f".format(it) } ?: "—", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("LAP", s.num("Lap")?.toInt()?.toString() ?: "—", Modifier.weight(1f))
                Metric("POS", s.num("PlayerCarPosition")?.toInt()?.takeIf { it > 0 }?.let { "P$it" } ?: "—", Modifier.weight(1f))
                Metric("FUEL", s.num("FuelLevel")?.let { "%.1f L".format(it) } ?: "—", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("CURRENT", lapTime(s.num("LapCurrentLapTime")), Modifier.weight(1f))
                Metric("LAST", lapTime(s.num("LapLastLapTime")), Modifier.weight(1f))
                Metric("BEST", lapTime(s.num("LapBestLapTime")), Modifier.weight(1f), Purple)
            }
        }
        item {
            val d = s.num("LapDeltaToBestLap")
            Metric("DELTA TO BEST", d?.let { "%+.3f".format(it) } ?: "—", Modifier.fillMaxWidth(), if (d == null) Fg else if (d <= 0) Good else Bad)
        }
        item {
            Panel {
                Section("INPUTS")
                Bar("THROTTLE", s.num("Throttle"), Good)
                Bar("BRAKE", s.num("Brake"), Bad)
            }
        }
        item { Text("Encrypted with your account key: the server only passes it along.", color = Muted, fontSize = 11.sp) }
    }
}

// ---------- Profile ----------
@Composable
private fun Profile(vm: PitlaneViewModel) {
    val a by vm.account.collectAsState()
    val uri = LocalUriHandler.current
    Screen("Profile", "Account") {
        item {
            Panel {
                Text(a.display.ifBlank { "Pitlane driver" }, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(a.email, color = Muted, fontSize = 12.sp)
            }
        }
        item { Action("ACCOUNT SETTINGS", "Name, password and devices on the web", Icons.Default.ManageAccounts) { uri.openUri(WEB_APP) } }
        item {
            Panel {
                Text("YOUR DATA", fontWeight = FontWeight.Black)
                Text(
                    "Your password never leaves this phone. Your synced settings and live telemetry are encrypted with your account key before they reach the server.",
                    color = Muted, fontSize = 12.sp
                )
            }
        }
        item {
            Button(
                onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = Bad)
            ) { Text("SIGN OUT", fontWeight = FontWeight.Bold) }
        }
    }
}

// ---------- pieces ----------
@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Line), shape = MaterialTheme.shapes.small
    ) { Column(Modifier.padding(14.dp), content = content) }
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
private fun Metric(l: String, v: String, m: Modifier, color: Color = Fg) {
    Panel(m) {
        Section(l)
        Text(v, color = color, fontWeight = FontWeight.Black, fontSize = 20.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}

@Composable
private fun Bar(label: String, v: Double?, c: Color) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(80.dp))
        LinearProgressIndicator(
            progress = { (v ?: 0.0).toFloat().coerceIn(0f, 1f) }, color = c, trackColor = Surface2,
            modifier = Modifier.weight(1f).height(10.dp)
        )
    }
}

@Composable
private fun Reload(loading: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (loading) LinearProgressIndicator(Modifier.weight(1f), color = Accent) else Spacer(Modifier.weight(1f))
        TextButton(onClick, enabled = !loading) { Text("REFRESH", color = Accent, fontSize = 12.sp) }
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
