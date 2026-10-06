package com.pitlanehq.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.pitlanehq.android.model.ConnectionState
import com.pitlanehq.android.viewmodel.PitlaneViewModel

private val Ink=Color(0xFF11151B)
private val Surface=Color(0xFF19202A)
private val Surface2=Color(0xFF1E2631)
private val Line=Color(0xFF2B3542)
private val Fg=Color(0xFFE7EBF1)
private val Muted=Color(0xFF8A97A9)
private val Accent=Color(0xFFFFB02E)
private val Good=Color(0xFF38C97C)
private val Bad=Color(0xFFFF6363)
private val Purple=Color(0xFFB98CFF)
private val Blue=Color(0xFF5C9DFF)

private data class Destination(val route:String,val label:String,val icon:ImageVector)
private val mainDestinations=listOf(
    Destination("home","Home",Icons.Default.Home),
    Destination("analysis","Analysis",Icons.Default.QueryStats),
    Destination("community","Community",Icons.Default.Groups),
    Destination("profile","Profile",Icons.Default.Person)
)

private data class AnalysisCard(val track:String,val car:String,val lap:String,val delta:String,val date:String)
private val sampleAnalyses=listOf(
    AnalysisCard("Silverstone","Porsche 911 GT3 R","1:58.421","PB","Latest analysis"),
    AnalysisCard("Spa-Francorchamps","Ferrari 296 GT3","2:17.884","+0.643","Previous session")
)
private data class CommunityCard(val title:String,val creator:String,val track:String,val car:String,val price:String,val rating:String)
private val communitySamples=listOf(
    CommunityCard("GT3 Race Analysis","Apex Engineering","Spa","Ferrari 296 GT3","£4.99","4.9"),
    CommunityCard("Qualifying Lap Pack","TrackLab","Silverstone","Porsche 911 GT3 R","£3.49","4.8"),
    CommunityCard("Race Stint Review","Delta Works","Daytona","BMW M4 GT3","£5.99","4.7")
)

@Composable
fun PitlaneApp(vm:PitlaneViewModel=viewModel()){
    val colors=darkColorScheme(
        primary=Accent,onPrimary=Ink,background=Ink,onBackground=Fg,
        surface=Surface,onSurface=Fg,surfaceVariant=Surface2,onSurfaceVariant=Muted,
        outline=Line,error=Bad
    )
    MaterialTheme(colorScheme=colors){
        val nav=rememberNavController()
        Scaffold(
            containerColor=Ink,
            bottomBar={PitlaneBottomBar(nav)}
        ){pad->
            NavHost(navController=nav,startDestination="home",modifier=Modifier.padding(pad)){
                composable("home"){HomeScreen(nav)}
                composable("analysis"){AnalysisScreen(nav)}
                composable("analysis/new"){NewAnalysisScreen(nav)}
                composable("analysis/detail"){AnalysisDetailScreen(nav)}
                composable("community"){CommunityScreen(nav)}
                composable("community/detail"){CommunityDetailScreen(nav)}
                composable("profile"){ProfileScreen(nav)}
                composable("telemetry"){TelemetryScreen(vm,nav)}
                composable("connection"){ConnectionScreen(vm,nav)}
                composable("iracing"){IRacingScreen(nav)}
            }
        }
    }
}

@Composable private fun PitlaneBottomBar(nav:NavHostController){
    val current=nav.currentBackStackEntryAsState().value?.destination?.route
    NavigationBar(containerColor=Surface,tonalElevation=0.dp){
        mainDestinations.forEach{d->
            val selected=current==d.route || (d.route=="analysis" && current?.startsWith("analysis/")==true) || (d.route=="community" && current?.startsWith("community/")==true)
            NavigationBarItem(
                selected=selected,
                onClick={nav.navigate(d.route){popUpTo("home"){saveState=true};launchSingleTop=true;restoreState=true}},
                icon={Icon(d.icon,null)},
                label={Text(d.label,fontSize=11.sp,fontWeight=FontWeight.SemiBold)},
                colors=NavigationBarItemDefaults.colors(
                    selectedIconColor=Ink,selectedTextColor=Accent,indicatorColor=Accent,
                    unselectedIconColor=Muted,unselectedTextColor=Muted
                )
            )
        }
    }
}

@Composable private fun ScreenHeader(title:String,subtitle:String?=null,back:(()->Unit)?=null,action:(@Composable ()->Unit)?=null){
    Column(Modifier.fillMaxWidth().background(Ink).padding(horizontal=16.dp,vertical=12.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            if(back!=null) IconButton(onClick=back){Icon(Icons.Default.ArrowBack,"Back",tint=Fg)}
            Column(Modifier.weight(1f)){
                Text(title.uppercase(),color=Fg,fontSize=27.sp,fontWeight=FontWeight.Black,letterSpacing=0.5.sp)
                if(subtitle!=null) Text(subtitle,color=Muted,fontSize=13.sp)
            }
            action?.invoke()
        }
        HorizontalDivider(color=Line,modifier=Modifier.padding(top=10.dp))
    }
}

@Composable private fun HomeScreen(nav:NavHostController){
    LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=20.dp)){
        item{ScreenHeader("Pitlane HQ","Your racing analysis companion")}
        item{
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                SectionLabel("OVERVIEW")
                Panel{
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text("WELCOME BACK",fontWeight=FontWeight.Black,fontSize=22.sp)
                            Text("Review your driving. Find time. Learn from the community.",color=Muted,fontSize=13.sp)
                        }
                        StatusPill("COMPANION",Good)
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    HomeAction("NEW ANALYSIS","Compare laps and find time",Icons.Default.Addchart,Modifier.weight(1f)){nav.navigate("analysis/new")}
                    HomeAction("MY ANALYSES","Your saved reports",Icons.Default.QueryStats,Modifier.weight(1f)){nav.navigate("analysis")}
                }
                SectionLabel("RECENT ANALYSIS")
                AnalysisRow(sampleAnalyses.first()){nav.navigate("analysis/detail")}
                SectionLabel("COMMUNITY")
                CommunityRow(communitySamples.first()){nav.navigate("community/detail")}
                TextButton(onClick={nav.navigate("community")},modifier=Modifier.align(Alignment.End)){Text("EXPLORE COMMUNITY →",color=Accent,fontWeight=FontWeight.Bold)}
                SectionLabel("TOOLS")
                Panel(modifier=Modifier.clickable{nav.navigate("telemetry")}){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.Sensors,"",tint=Accent)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)){Text("LIVE TELEMETRY",fontWeight=FontWeight.Bold);Text("Optional live view from your PitWall PC",color=Muted,fontSize=12.sp)}
                        Icon(Icons.Default.ChevronRight,"",tint=Muted)
                    }
                }
            }
        }
    }
}

@Composable private fun AnalysisScreen(nav:NavHostController){
    LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){
        item{ScreenHeader("My Analysis","Sessions, laps and engineering reports",action={IconButton(onClick={nav.navigate("analysis/new")}){Icon(Icons.Default.Add,"New analysis",tint=Accent)}})}
        item{
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Button(onClick={nav.navigate("analysis/new")},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Accent,contentColor=Ink),shape=RoundedCornerShape(5.dp)){
                    Icon(Icons.Default.Addchart,null);Spacer(Modifier.width(8.dp));Text("CREATE ANALYSIS",fontWeight=FontWeight.Black)
                }
                SectionLabel("RECENT")
            }
        }
        items(sampleAnalyses){a->Box(Modifier.padding(horizontal=16.dp,vertical=5.dp)){AnalysisRow(a){nav.navigate("analysis/detail")}}}
        item{
            Box(Modifier.padding(16.dp)){
                EmptyState("Cloud-ready","Your real sessions and analyses will appear here when the account/data layer is connected.")
            }
        }
    }
}

@Composable private fun NewAnalysisScreen(nav:NavHostController){
    var mode by remember{mutableStateOf("Compare laps")}
    LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){
        item{ScreenHeader("New Analysis","Build an analysis from your racing data",back={nav.popBackStack()})}
        item{
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                SectionLabel("ANALYSIS TYPE")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    listOf("Compare laps","Single lap").forEach{x->
                        FilterChip(selected=mode==x,onClick={mode=x},label={Text(x.uppercase())},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Accent,selectedLabelColor=Ink,containerColor=Surface,labelColor=Muted))
                    }
                }
                SelectPanel("1","SELECT SESSION","Choose one of your uploaded iRacing sessions")
                SelectPanel("2","SELECT LAP","Pick your lap or comparison lap")
                SelectPanel("3","ANALYSE","Braking, throttle, speed, line and time loss")
                Button(onClick={},enabled=false,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(5.dp)){Text("RUN ANALYSIS")}
                Text("The workflow is ready for the account/cloud data source. No fake analysis is generated.",color=Muted,fontSize=12.sp)
            }
        }
    }
}

@Composable private fun AnalysisDetailScreen(nav:NavHostController){
    LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){
        item{ScreenHeader("Lap Analysis","Silverstone · Porsche 911 GT3 R",back={nav.popBackStack()})}
        item{
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Metric("LAP","1:58.421",Modifier.weight(1f));Metric("DELTA","PB",Modifier.weight(1f),Purple)}
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Metric("MAX SPEED","271 km/h",Modifier.weight(1f));Metric("VALID","YES",Modifier.weight(1f),Good)}
                SectionLabel("ENGINEER SUMMARY")
                Panel{Text("Analysis summary",fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp));Text("This screen is prepared for generated analysis results. Braking, throttle, speed, racing line and sector findings will be shown here from the real analysis service.",color=Muted)}
                SectionLabel("LAP DATA")
                Insight("BRAKING","Braking zones and brake release")
                Insight("THROTTLE","Application and traction")
                Insight("SPEED","Minimum and maximum speed comparison")
                Insight("TIME LOSS","Corner and sector deltas")
            }
        }
    }
}

@Composable private fun CommunityScreen(nav:NavHostController){
    var search by remember{mutableStateOf("")}
    LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){
        item{ScreenHeader("Community","Discover analysis shared by other drivers")}
        item{
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                OutlinedTextField(value=search,onValueChange={search=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Search track, car or creator")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Accent,unfocusedBorderColor=Line))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Tag("TRENDING",true);Tag("GT3",false);Tag("ROAD",false)}
                SectionLabel("MARKETPLACE")
            }
        }
        items(communitySamples.filter{search.isBlank() || (it.title+it.creator+it.track+it.car).contains(search,true)}){c->
            Box(Modifier.padding(horizontal=16.dp,vertical=5.dp)){CommunityRow(c){nav.navigate("community/detail")}}
        }
    }
}

@Composable private fun CommunityDetailScreen(nav:NavHostController){
    LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){
        item{ScreenHeader("Community Analysis","Apex Engineering",back={nav.popBackStack()})}
        item{
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Panel{
                    Text("GT3 RACE ANALYSIS",fontWeight=FontWeight.Black,fontSize=22.sp)
                    Text("Spa-Francorchamps · Ferrari 296 GT3",color=Muted)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment=Alignment.CenterVertically){Text("£4.99",color=Accent,fontWeight=FontWeight.Black,fontSize=25.sp);Spacer(Modifier.weight(1f));StatusPill("★ 4.9",Good)}
                }
                SectionLabel("INCLUDES")
                Insight("LAP COMPARISON","Reference lap and telemetry comparison")
                Insight("ENGINEER NOTES","Braking, throttle and corner findings")
                Insight("DATA","Shared analysis package for this car/track combination")
                Button(onClick={},enabled=false,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Accent,contentColor=Ink)){Text("BUY ANALYSIS",fontWeight=FontWeight.Black)}
                Text("Purchase flow is intentionally disabled until the marketplace backend is connected.",color=Muted,fontSize=12.sp)
            }
        }
    }
}

@Composable private fun ProfileScreen(nav:NavHostController){
    LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){
        item{ScreenHeader("Profile","Account, racing data and companion tools")}
        item{
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Panel{
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Box(Modifier.size(48.dp).background(Surface2,RoundedCornerShape(24.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Person,null,tint=Accent)}
                        Spacer(Modifier.width(12.dp));Column{Text("PITLANE DRIVER",fontWeight=FontWeight.Black);Text("Account not connected",color=Muted,fontSize=12.sp)}
                    }
                }
                SectionLabel("RACING DATA")
                MenuRow(Icons.Default.SportsScore,"iRACING","Connect account and sync racing data","READY FOR OAUTH"){nav.navigate("iracing")}
                SectionLabel("COMPANION TOOLS")
                MenuRow(Icons.Default.Sensors,"LIVE TELEMETRY","Optional live data from PitWall PC",null){nav.navigate("telemetry")}
                MenuRow(Icons.Default.Computer,"PITWALL PC","Pair or change your local PC",null){nav.navigate("connection")}
                SectionLabel("APP")
                Panel{Text("PITLANE HQ",fontWeight=FontWeight.Bold);Text("Native Android companion · v0.1.0",color=Muted,fontSize=12.sp)}
            }
        }
    }
}

@Composable private fun IRacingScreen(nav:NavHostController){
    LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){
        item{ScreenHeader("iRacing","Account data connection",back={nav.popBackStack()})}
        item{
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                StatusPill("NOT CONNECTED",Muted)
                Panel{
                    Text("iRACING OAUTH",fontWeight=FontWeight.Black,fontSize=20.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("This app is structured so iRacing authentication can be added without changing the analysis, community or telemetry UI.",color=Muted)
                }
                Insight("ACCOUNT","Driver identity and profile")
                Insight("SESSIONS","Race and session data available through the account integration")
                Insight("ANALYSIS","Use synced data as a source for Pitlane HQ analysis")
                Button(onClick={},enabled=false,modifier=Modifier.fillMaxWidth()){Text("CONNECT iRACING")}
                Text("OAuth is not implemented yet. No credentials are requested or stored by this build.",color=Muted,fontSize=12.sp)
            }
        }
    }
}

@Composable private fun TelemetryScreen(vm:PitlaneViewModel,nav:NavHostController){
    val t by vm.telemetry.collectAsState()
    val c by vm.connection.collectAsState()
    LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){
        item{ScreenHeader("Live Telemetry","Optional race-day companion",back={nav.popBackStack()},action={StatusPill(if(c.state==ConnectionState.LIVE)"LIVE" else "OFFLINE",if(c.state==ConnectionState.LIVE)Good else Muted)})}
        item{
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Metric("SPEED",t.speedKph?.let{"%.0f km/h".format(it)}?:"—",Modifier.weight(1f));Metric("GEAR",t.gear?.toString()?:"—",Modifier.weight(1f))}
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Metric("RPM",t.rpm?.let{"%.0f".format(it)}?:"—",Modifier.weight(1f));Metric("LAP",t.lap?.toString()?:"—",Modifier.weight(1f))}
                Metric("FUEL",t.fuelLitres?.let{"%.1f L".format(it)}?:"—",Modifier.fillMaxWidth())
                OutlinedButton(onClick={nav.navigate("connection")},modifier=Modifier.fillMaxWidth(),border=BorderStroke(1.dp,Line)){Text("CONNECTION CENTER",color=Fg)}
                Text("Live telemetry is a secondary tool. Analysis and community remain available without a live PC connection.",color=Muted,fontSize=12.sp)
            }
        }
    }
}

@Composable private fun ConnectionScreen(vm:PitlaneViewModel,nav:NavHostController){
    val c by vm.connection.collectAsState()
    var host by remember{mutableStateOf(c.host)}
    LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(bottom=24.dp)){
        item{ScreenHeader("PitWall PC","Local telemetry connection",back={nav.popBackStack()})}
        item{
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                StatusPill(c.state.name,if(c.state==ConnectionState.LIVE)Good else Muted)
                OutlinedTextField(host,{host=it},label={Text("PC address")},singleLine=true,modifier=Modifier.fillMaxWidth(),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Accent,unfocusedBorderColor=Line))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(onClick={vm.connect(host)},colors=ButtonDefaults.buttonColors(containerColor=Accent,contentColor=Ink)){Text("CONNECT",fontWeight=FontWeight.Bold)}
                    OutlinedButton(onClick={vm.disconnect},border=BorderStroke(1.dp,Line)){Text("DISCONNECT",color=Fg)}
                }
                c.message?.let{Text(it,color=Bad)}
                Text("Telemetry comes from PitWall PC over your local network. The mobile app does not read iRacing shared memory directly.",color=Muted,fontSize=12.sp)
            }
        }
    }
}

@Composable private fun Panel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
    Card(modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Surface),border=BorderStroke(1.dp,Line),shape=RoundedCornerShape(6.dp)){
        Column(Modifier.padding(14.dp),content=content)
    }
}
@Composable private fun SectionLabel(text:String){Text(text,color=Muted,fontFamily=FontFamily.Monospace,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp)}
@Composable private fun StatusPill(text:String,color:Color){Surface(color=Color.Transparent,border=BorderStroke(1.dp,color),shape=RoundedCornerShape(50)){Text(text,color=color,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold,fontSize=10.sp,modifier=Modifier.padding(horizontal=8.dp,vertical=5.dp))}}
@Composable private fun Metric(label:String,value:String,modifier:Modifier,color:Color=Fg){Card(modifier,colors=CardDefaults.cardColors(containerColor=Surface),border=BorderStroke(1.dp,Line),shape=RoundedCornerShape(6.dp)){Column(Modifier.padding(13.dp)){SectionLabel(label);Text(value,color=color,fontWeight=FontWeight.Black,fontSize=25.sp)}}}
@Composable private fun HomeAction(title:String,sub:String,icon:ImageVector,modifier:Modifier,onClick:()->Unit){Card(modifier.clickable(onClick=onClick),colors=CardDefaults.cardColors(containerColor=Surface),border=BorderStroke(1.dp,Line),shape=RoundedCornerShape(6.dp)){Column(Modifier.padding(14.dp)){Icon(icon,null,tint=Accent);Spacer(Modifier.height(12.dp));Text(title,fontWeight=FontWeight.Black,fontSize=15.sp);Text(sub,color=Muted,fontSize=11.sp,lineHeight=15.sp)}}}
@Composable private fun AnalysisRow(a:AnalysisCard,onClick:()->Unit){Panel(Modifier.clickable(onClick=onClick)){Row{Column(Modifier.weight(1f)){Text(a.track.uppercase(),fontWeight=FontWeight.Black,fontSize=18.sp);Text(a.car,color=Muted,fontSize=12.sp);Text(a.date,color=Muted,fontSize=11.sp)};Column(horizontalAlignment=Alignment.End){Text(a.lap,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold);Text(a.delta,color=if(a.delta=="PB")Purple else Bad,fontFamily=FontFamily.Monospace,fontSize=11.sp)}}}}
@Composable private fun CommunityRow(c:CommunityCard,onClick:()->Unit){Panel(Modifier.clickable(onClick=onClick)){Row{Column(Modifier.weight(1f)){Text(c.title.uppercase(),fontWeight=FontWeight.Black,fontSize=17.sp);Text(c.creator,color=Accent,fontSize=12.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(7.dp));Text("${c.track} · ${c.car}",color=Muted,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)};Column(horizontalAlignment=Alignment.End){Text(c.price,color=Fg,fontWeight=FontWeight.Black,fontSize=18.sp);Text("★ ${c.rating}",color=Good,fontSize=12.sp)}}}}
@Composable private fun EmptyState(title:String,body:String){Panel{Text(title.uppercase(),fontWeight=FontWeight.Bold);Text(body,color=Muted,fontSize=12.sp)}}
@Composable private fun SelectPanel(n:String,title:String,body:String){Panel{Row(verticalAlignment=Alignment.CenterVertically){Text(n,color=Accent,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Black,fontSize=22.sp);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold);Text(body,color=Muted,fontSize=12.sp)};Icon(Icons.Default.ChevronRight,null,tint=Muted)}}}
@Composable private fun Insight(title:String,body:String){Panel{SectionLabel(title);Spacer(Modifier.height(5.dp));Text(body,fontWeight=FontWeight.SemiBold)}}
@Composable private fun Tag(text:String,selected:Boolean){Surface(color=if(selected)Accent else Surface,contentColor=if(selected)Ink else Muted,border=BorderStroke(1.dp,if(selected)Accent else Line),shape=RoundedCornerShape(50)){Text(text,fontWeight=FontWeight.Bold,fontSize=11.sp,modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp))}}
@Composable private fun MenuRow(icon:ImageVector,title:String,body:String,badge:String?,onClick:()->Unit){Panel(Modifier.clickable(onClick=onClick)){Row(verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Accent);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold);Text(body,color=Muted,fontSize=12.sp)};if(badge!=null)Text(badge,color=Good,fontFamily=FontFamily.Monospace,fontSize=9.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.width(5.dp));Icon(Icons.Default.ChevronRight,null,tint=Muted)}}}
