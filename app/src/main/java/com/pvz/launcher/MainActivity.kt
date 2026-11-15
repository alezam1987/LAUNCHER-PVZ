package com.pvz.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PvzLauncherScreen()
        }
    }
}

@Composable
fun PvzLauncherScreen() {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    val apps = remember { getInstalledApps(context.packageManager) }
    
    // Day/Night logic
    val hour = currentTime.get(Calendar.HOUR_OF_DAY)
    val isNight = hour >= 18 || hour < 6
    
    val skyBrush = if (isNight) {
        Brush.verticalGradient(listOf(Color(0xFF1E3C72), Color(0xFF2A5298)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)))
    }

    Box(modifier = Modifier.fillMaxSize().background(skyBrush)) {
        // 1. Dynamic Background (Lawn)
        LawnGrid(modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.6f), isNight = isNight)
        
        // 2. HUD
        Hud(currentTime, isNight)
        
        // 3. App Carousel (Seed Packets)
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(apps) { app ->
                    SeedPacket(app) {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) context.startActivity(launchIntent)
                    }
                }
            }
        }
    }
}

@Composable
fun LawnGrid(modifier: Modifier, isNight: Boolean) {
    val lightGrass = if (isNight) Color(0xFF1B5E20) else Color(0xFF4CAF50)
    val darkGrass = if (isNight) Color(0xFF003300) else Color(0xFF388E3C)
    
    Column(modifier = modifier.fillMaxWidth()) {
        for (row in 0 until 5) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0 until 9) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if ((row + col) % 2 == 0) lightGrass else darkGrass)
                    )
                }
            }
        }
    }
}

@Composable
fun Hud(time: Calendar, isNight: Boolean) {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateSdf = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES"))
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(Color.Yellow, shape = androidx.compose.foundation.shape.CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text("500", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text(sdf.format(time.time), color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
            Text(dateSdf.format(time.time), color = Color.White, fontSize = 18.sp)
            Text("BRAINS...", color = Color(0xFFCCFF00), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        // Icons Placeholder
        Text("🧤 📖 ⚙️", fontSize = 32.sp)
    }
}

@Composable
fun SeedPacket(app: AppInfo, onClick: () -> Unit) {
    // Basic Seed Packet UI
    androidx.compose.material3.Card(
        modifier = Modifier.size(140.dp, 200.dp),
        onClick = onClick,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF5D4037))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon Placeholder - In a real app, use Image with app icon
            Box(modifier = Modifier.size(60.dp).background(if(app.label == "XuperTV") Color(0xFFFFD700) else Color.White, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)))
            Text(app.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${app.cost}", modifier = Modifier.align(Alignment.End).padding(end = 4.dp), color = Color.Black, fontWeight = FontWeight.ExtraBold)
        }
    }
}

data class AppInfo(val label: String, val packageName: String, val cost: Int)

fun getInstalledApps(pm: PackageManager): List<AppInfo> {
    val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
    val apps = pm.queryIntentActivities(intent, 0).mapNotNull {
        val label = it.loadLabel(pm).toString()
        val isXuper = label.contains("Xuper", ignoreCase = true)
        val isYouTube = label.contains("YouTube", ignoreCase = true)
        val isSettings = label.contains("Settings", ignoreCase = true) || label.contains("Ajustes", ignoreCase = true)
        
        if (isXuper || isYouTube || isSettings) {
            val cost = when {
                isXuper -> 50
                isYouTube -> 100
                isSettings -> 75
                else -> 100
            }
            AppInfo(label, it.activityInfo.packageName, cost)
        } else {
            null
        }
    }
    // Sort to make XuperTV first, then YouTube, then Settings
    return apps.sortedWith(compareByDescending<AppInfo> { it.label.contains("Xuper", ignoreCase = true) }
        .thenByDescending { it.label.contains("YouTube", ignoreCase = true) }
        .thenBy { it.label })
}
