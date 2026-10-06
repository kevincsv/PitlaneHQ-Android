package com.pitlanehq.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.pitlanehq.android.model.PitWallConnection
import com.pitlanehq.android.viewmodel.PitlaneViewModel

private val destinations=listOf("dashboard" to "Dashboard","sessions" to "Sessions","laps" to "My Laps","telemetry" to "Telemetry","connection" to "Connection","settings" to "Settings")

@Composable
fun PitlaneApp(vm:PitlaneViewModel=viewModel()){
    MaterialTheme(colorScheme=darkColorScheme()){
        val nav=rememberNavController()
        Scaffold(bottomBar={
            NavigationBar{destinations.take(4).forEach{(route,label)->
                NavigationBarItem(selected=false,onClick={nav.navigate(route){launchSingleTop=true}},icon={Icon(Icons.Default.Circle,null)},label={Text(label)})
            }}
        }){pad->NavHost(nav,"dashboard",Modifier.padding(pad)){
            composable("dashboard"){DashboardScreen(vm)}
            composable("sessions"){SimpleScreen("Sessions","Session history")}
            composable("laps"){SimpleScreen("My Laps","Saved laps and records")}
            composable("telemetry"){TelemetryScreen(vm)}
            composable("connection"){ConnectionScreen(vm)}
            composable("settings"){SettingsScreen()}
        }}
    }
}

@Composable private fun DashboardScreen(vm:PitlaneViewModel){
    val t by vm.telemetry.collectAsState()
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text("PITLANE HQ",style=MaterialTheme.typography.headlineMedium)
        Text("Live race engineering")
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.fillMaxWidth()){
            Kpi("Speed",t.speedKph?.let{"%.0f km/h".format(it)}?:"—",Modifier.weight(1f))
            Kpi("RPM",t.rpm?.let{"%.0f".format(it)}?:"—",Modifier.weight(1f))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.fillMaxWidth()){
            Kpi("Gear",t.gear?.toString()?:"—",Modifier.weight(1f))
            Kpi("Lap",t.lap?.toString()?:"—",Modifier.weight(1f))
        }
        Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("Connection");Text("Open Connection Center in the next build to pair your PitWall PC.")}}
    }
}
@Composable private fun Kpi(title:String,value:String,modifier:Modifier){Card(modifier){Column(Modifier.padding(16.dp)){Text(title,style=MaterialTheme.typography.labelMedium);Text(value,style=MaterialTheme.typography.headlineSmall)}}}
@Composable private fun TelemetryScreen(vm:PitlaneViewModel){
    val t by vm.telemetry.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{Text("Full Telemetry",style=MaterialTheme.typography.headlineMedium)}
        item{Text("Live values from PitWall PC.")}
        item{ListItem(headlineContent={Text("Speed")},supportingContent={Text(t.speedKph?.toString()?:"—")})}
        item{ListItem(headlineContent={Text("RPM")},supportingContent={Text(t.rpm?.toString()?:"—")})}
        item{ListItem(headlineContent={Text("Gear")},supportingContent={Text(t.gear?.toString()?:"—")})}
        item{ListItem(headlineContent={Text("Fuel")},supportingContent={Text(t.fuelLitres?.toString()?:"—")})}
        item{ListItem(headlineContent={Text("Lap")},supportingContent={Text(t.lap?.toString()?:"—")})}
    }
}
@Composable private fun ConnectionScreen(vm:PitlaneViewModel){
    val c by vm.connection.collectAsState()
    var host by remember{mutableStateOf(c.host)}
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text("Connection Center",style=MaterialTheme.typography.headlineMedium)
        Text("PitWall PC • "+c.state)
        OutlinedTextField(host,{host=it},label={Text("PC address")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            Button(onClick={vm.connect(host)}){Text("Connect")}
            OutlinedButton(onClick={vm.disconnect()}){Text("Disconnect")}
        }
        c.message?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Text("The app receives telemetry from PitWall PC over the local network. iRacing stays on the PC.",style=MaterialTheme.typography.bodySmall)
    }
}
@Composable private fun SettingsScreen(){
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text("Settings",style=MaterialTheme.typography.headlineMedium)
        ListItem(headlineContent={Text("Pitlane HQ")},supportingContent={Text("Android companion • v0.1.0")})
        ListItem(headlineContent={Text("Telemetry")},supportingContent={Text("Live stream: 30 Hz target")})
        ListItem(headlineContent={Text("Security")},supportingContent={Text("iRacing authentication will be added before cloud features")})
    }
}
@Composable private fun SimpleScreen(title:String,body:String){Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(title,style=MaterialTheme.typography.headlineMedium);Text(body)}}
