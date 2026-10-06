package com.pitlanehq.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.pitlanehq.android.model.ConnectionState
import com.pitlanehq.android.viewmodel.PitlaneViewModel

private val Ink=Color(0xFF11151B);private val Surface=Color(0xFF19202A);private val Surface2=Color(0xFF1E2631);private val Line=Color(0xFF2B3542);private val Fg=Color(0xFFE7EBF1);private val Muted=Color(0xFF8A97A9);private val Accent=Color(0xFFFFB02E);private val Good=Color(0xFF38C97C);private val Bad=Color(0xFFFF6363);private val Purple=Color(0xFFB98CFF)
private data class Dest(val route:String,val label:String,val icon:ImageVector)
private val dests=listOf(Dest("home","Home",Icons.Default.Home),Dest("analysis","Analysis",Icons.Default.QueryStats),Dest("community","Community",Icons.Default.Groups),Dest("profile","Profile",Icons.Default.Person))

@Composable fun PitlaneApp(vm:PitlaneViewModel=viewModel()){
 val account by vm.account.collectAsState()
 val scheme=darkColorScheme(primary=Accent,onPrimary=Ink,background=Ink,onBackground=Fg,surface=Surface,onSurface=Fg,surfaceVariant=Surface2,onSurfaceVariant=Muted,outline=Line,error=Bad)
 MaterialTheme(colorScheme=scheme){if(!account.signedIn)LoginScreen(vm,account.error,account.syncing)else MainApp(vm)}
}

@Composable private fun LoginScreen(vm:PitlaneViewModel,error:String?,busy:Boolean){
 var email by remember{mutableStateOf("")};var password by remember{mutableStateOf("")}
 Box(Modifier.fillMaxSize().background(Ink),contentAlignment=Alignment.Center){
  Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text("PITLANE HQ",color=Fg,fontSize=34.sp,fontWeight=FontWeight.Black);Text("ANDROID COMPANION",color=Accent,fontFamily=FontFamily.Monospace,fontSize=12.sp,fontWeight=FontWeight.Bold)
   Spacer(Modifier.height(18.dp));Panel{
    Text("SIGN IN",fontSize=22.sp,fontWeight=FontWeight.Black);Text("Use your normal Pitlane HQ account to sync your PC data.",color=Muted,fontSize=12.sp)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,modifier=Modifier.fillMaxWidth())
    OutlinedTextField(password,{password=it},label={Text("Password")},singleLine=true,modifier=Modifier.fillMaxWidth())
    error?.let{Text(it,color=Bad,fontSize=12.sp)}
    Button(onClick={vm.login(email,password)},enabled=!busy&&email.isNotBlank()&&password.isNotBlank(),modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Accent,contentColor=Ink)){Text(if(busy)"SYNCING…" else "SIGN IN",fontWeight=FontWeight.Black)}
   }
   Text("Your password is used locally to derive the account key. It is never sent to Pitlane HQ.",color=Muted,fontSize=11.sp)
  }
 }
}

@Composable private fun MainApp(vm:PitlaneViewModel){
 val nav=rememberNavController();Scaffold(containerColor=Ink,bottomBar={BottomBar(nav)}){pad->NavHost(nav,"home",Modifier.padding(pad)){
  composable("home"){Home(vm,nav)};composable("analysis"){Analysis(nav)};composable("community"){Community()};composable("profile"){Profile(vm,nav)}
  composable("telemetry"){Telemetry(vm,nav)};composable("connection"){Connection(vm,nav)}
 }}
}

@Composable private fun BottomBar(nav:NavHostController){val current=nav.currentBackStackEntryAsState().value?.destination?.route;NavigationBar(containerColor=Surface,tonalElevation=0.dp){dests.forEach{d->NavigationBarItem(current==d.route,{nav.navigate(d.route){popUpTo("home"){saveState=true};launchSingleTop=true;restoreState=true}},icon={Icon(d.icon,null)},label={Text(d.label,fontSize=11.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Ink,selectedTextColor=Accent,indicatorColor=Accent,unselectedIconColor=Muted,unselectedTextColor=Muted))}}}

@Composable private fun Header(title:String,sub:String="",back:(()->Unit)?=null){Row(Modifier.fillMaxWidth().background(Ink).padding(16.dp),verticalAlignment=Alignment.CenterVertically){if(back!=null)IconButton(back){Icon(Icons.Default.ArrowBack,"Back",tint=Fg)};Column(Modifier.weight(1f)){Text(title.uppercase(),color=Fg,fontSize=26.sp,fontWeight=FontWeight.Black);if(sub.isNotBlank())Text(sub,color=Muted,fontSize=12.sp)};Spacer(Modifier.width(4.dp))}}

@Composable private fun Home(vm:PitlaneViewModel,nav:NavHostController){val a by vm.account.collectAsState();LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){item{Header("Pitlane HQ","Your racing analysis companion")};item{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Section("ACCOUNT");Panel{Text(if(a.display.isBlank())a.email else a.display,fontWeight=FontWeight.Black,fontSize=20.sp);Text(a.email,color=Muted,fontSize=12.sp);Spacer(Modifier.height(8.dp));Status(if(a.syncing)"SYNCING" else "SYNCED",if(a.syncing)Accent else Good)}
 Section("SYNCED PC DATA");Panel{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("${a.syncedFiles} data files",fontWeight=FontWeight.Bold);Text("Cloud copy from your PitWall PC · version ${a.syncVersion}",color=Muted,fontSize=12.sp)}Text("↻",color=Accent,fontSize=24.sp,modifier=Modifier.clickable{vm.sync()}.padding(8.dp))}}
 Section("QUICK ACTIONS");Action("LAP ANALYSIS","Review sessions and find time",Icons.Default.QueryStats){nav.navigate("analysis")};Action("COMMUNITY","Shared analysis and driver knowledge",Icons.Default.Groups){nav.navigate("community")};Action("LIVE TELEMETRY","Optional race-day view from PitWall PC",Icons.Default.Sensors){nav.navigate("telemetry")}
}}}}

@Composable private fun Analysis(nav:NavHostController){LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){item{Header("Analysis","Sessions, laps and engineering")};item{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Section("YOUR DATA");Panel{Text("SESSION HISTORY",fontWeight=FontWeight.Black,fontSize=19.sp);Text("Your synchronized PC sessions will appear here for lap analysis and comparison.",color=Muted,fontSize=12.sp)};Action("COMPARE LAPS","Compare pace, braking, throttle and speed",Icons.Default.CompareArrows){};Action("LAP ANALYSIS","Corner-by-corner time loss",Icons.Default.Timeline){};Section("STATUS");Panel{Text("READY FOR CLOUD DATA",color=Good,fontWeight=FontWeight.Bold);Text("The account/sync layer is connected. Analysis screens will consume the synchronized Pitlane HQ data.",color=Muted,fontSize=12.sp)}}}}}

@Composable private fun Community(){LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){item{Header("Community","Shared analysis and racing knowledge")};item{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Section("EXPLORE");Panel{Text("COMMUNITY ANALYSIS",fontWeight=FontWeight.Black,fontSize=19.sp);Text("Discover shared laps, analysis, setups and engineering notes from other drivers.",color=Muted,fontSize=12.sp)};Action("SHARED LAPS","Learn from community telemetry",Icons.Default.Speed){};Action("ANALYSIS","Browse shared engineering findings",Icons.Default.Insights){};Action("SETUPS & NOTES","Community knowledge for your next session",Icons.Default.SettingsSuggest){}}}}}

@Composable private fun Profile(vm:PitlaneViewModel,nav:NavHostController){val a by vm.account.collectAsState();LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){item{Header("Profile","Account and companion settings")};item{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Panel{Text(a.display.ifBlank{"PITLANE DRIVER"},fontWeight=FontWeight.Black,fontSize=20.sp);Text(a.email,color=Muted,fontSize=12.sp)};Action("SYNC DATA","Pull the latest synchronized PC data",Icons.Default.Sync){vm.sync()};Action("PITWALL PC","Optional live telemetry connection",Icons.Default.Computer){nav.navigate("connection")};Action("LIVE TELEMETRY","Race-day companion view",Icons.Default.Sensors){nav.navigate("telemetry")};Panel{Text("iRACING",fontWeight=FontWeight.Black);Text("OAuth-ready architecture. This release does not ask for iRacing credentials.",color=Muted,fontSize=12.sp)};Button(onClick={vm.logout()},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Surface2,contentColor=Bad)){Text("SIGN OUT",fontWeight=FontWeight.Bold)}}}}}

@Composable private fun Telemetry(vm:PitlaneViewModel,nav:NavHostController){val t by vm.telemetry.collectAsState();val c by vm.connection.collectAsState();LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){item{Header("Live Telemetry","Optional PitWall PC view",{nav.popBackStack()})};item{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Metric("SPEED",t.speedKph?.let{"%.0f km/h".format(it)}?:"—",Modifier.weight(1f));Metric("GEAR",t.gear?.toString()?:"—",Modifier.weight(1f))};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Metric("RPM",t.rpm?.let{"%.0f".format(it)}?:"—",Modifier.weight(1f));Metric("LAP",t.lap?.toString()?:"—",Modifier.weight(1f))};Status(if(c.state==ConnectionState.LIVE)"LIVE" else "OFFLINE",if(c.state==ConnectionState.LIVE)Good:Muted);Action("CONNECTION CENTER","Connect to your PitWall PC",Icons.Default.Wifi){nav.navigate("connection")};Text("Mobile does not read iRacing shared memory directly. PitWall PC remains the telemetry gateway.",color=Muted,fontSize=12.sp)}}}}

@Composable private fun Connection(vm:PitlaneViewModel,nav:NavHostController){val c by vm.connection.collectAsState();var host by remember{mutableStateOf(c.host)};LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){item{Header("PitWall PC","Local telemetry connection",{nav.popBackStack()})};item{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Status(c.state.name,if(c.state==ConnectionState.LIVE)Good else Muted);OutlinedTextField(host,{host=it},label={Text("PC address")},singleLine=true,modifier=Modifier.fillMaxWidth());Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({vm.connect(host)},colors=ButtonDefaults.buttonColors(containerColor=Accent,contentColor=Ink)){Text("CONNECT")};OutlinedButton({vm.disconnect()}){Text("DISCONNECT")}};c.message?.let{Text(it,color=Bad,fontSize=12.sp)}}}}}

@Composable private fun Panel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Card(modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Surface),border=BorderStroke(1.dp,Line),shape=MaterialTheme.shapes.small){Column(Modifier.padding(14.dp),content=content)}}
@Composable private fun Section(s:String){Text(s,color=Muted,fontFamily=FontFamily.Monospace,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp)}
@Composable private fun Status(s:String,c:Color){Surface(color=Color.Transparent,border=BorderStroke(1.dp,c),shape=MaterialTheme.shapes.extraLarge){Text(s,color=c,fontFamily=FontFamily.Monospace,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp))}}
@Composable private fun Metric(l:String,v:String,m:Modifier){Panel(m){Section(l);Text(v,color=Fg,fontWeight=FontWeight.Black,fontSize=25.sp)}}
@Composable private fun Action(title:String,sub:String,icon:ImageVector,onClick:()->Unit){Panel(Modifier.clickable(onClick=onClick)){Row(verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Accent);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Black);Text(sub,color=Muted,fontSize=12.sp)};Icon(Icons.Default.ChevronRight,null,tint=Muted)}}}
