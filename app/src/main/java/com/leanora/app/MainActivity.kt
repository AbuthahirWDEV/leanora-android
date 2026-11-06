package com.leanora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Cream = Color(0xFFF7F5EF)
private val Emerald = Color(0xFF176B52)
private val Ink = Color(0xFF1C2924)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LeanoraApp() }
    }
}

@Composable
fun LeanoraApp() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Emerald, background = Cream)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("LEANORA", color = Emerald, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("Your wellness,\nin balance.", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
                Text("Daily nutrition overview", color = Ink.copy(alpha = 0.65f), fontSize = 16.sp)
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Emerald)) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Calories remaining", color = Color.White.copy(alpha = 0.8f))
                        Text("1,250 kcal", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Text("of 1,850 kcal daily goal", color = Color.White.copy(alpha = 0.8f))
                    }
                }
                Text("Today's nutrition", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacroCard("Protein", "62 g", Modifier.weight(1f))
                    MacroCard("Carbs", "148 g", Modifier.weight(1f))
                    MacroCard("Fat", "42 g", Modifier.weight(1f))
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                    Text("+ Log food", fontSize = 17.sp)
                }
                Text("Starter UI • nutrition tracking and AI features coming next", color = Ink.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MacroCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = Ink.copy(alpha = 0.6f), fontSize = 12.sp)
            Text(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}
