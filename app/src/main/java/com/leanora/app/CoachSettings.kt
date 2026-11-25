package com.leanora.app

import android.content.pm.ApplicationInfo
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val SUGGESTIONS = listOf("Is my day on track?", "High-protein Indian snack ideas", "How can I eat fewer calories without feeling hungry?")

@Composable
internal fun CoachScreen(
    chat: List<ChatMessage>,
    busy: Boolean,
    todaySummary: String,
    modifier: Modifier,
    onAsk: (String, String, () -> Unit) -> Unit,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    var language by rememberSaveable { mutableStateOf("English") }
    var question by rememberSaveable { mutableStateOf("") }
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember { TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS } }
    DisposableEffect(tts) { onDispose { tts.stop(); tts.shutdown() } }
    val listState = rememberLazyListState()
    LaunchedEffect(chat.size) { if (chat.isNotEmpty()) listState.animateScrollToItem(chat.lastIndex) }

    fun speak(text: String) {
        if (!ttsReady) {
            Toast.makeText(context, "Text-to-speech isn't ready yet.", Toast.LENGTH_SHORT).show()
            return
        }
        val result = tts.setLanguage(if (language == "Tamil") Locale("ta", "IN") else Locale("en", "IN"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(context, "A $language voice isn't installed on this device.", Toast.LENGTH_LONG).show()
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "leanora-coach")
        }
    }

    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Leanora Coach", Modifier.weight(1f), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (chat.isNotEmpty()) TextButton(onClick = onClear) { Text("New chat") }
        }
        Text(todaySummary, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Reply in", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilterChip(selected = language == "English", onClick = { language = "English" }, label = { Text("English") })
            FilterChip(selected = language == "Tamil", onClick = { language = "Tamil" }, label = { Text("தமிழ்") })
        }
        if (chat.isEmpty()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Info("Coach", "Hi! Ask me about your food, calories, or nutrition. I can see today's totals and what you've logged recently.")
                SUGGESTIONS.forEach { OutlinedButton(onClick = { question = it }, modifier = Modifier.fillMaxWidth()) { Text(it) } }
            }
        } else {
            LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chat) { message ->
                    val bubbleColor = when {
                        message.isError -> MaterialTheme.colorScheme.errorContainer
                        message.fromUser -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                        Card(Modifier.fillMaxWidth(.88f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = bubbleColor)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                Text(
                                    message.text, Modifier.weight(1f),
                                    color = if (message.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                                )
                                if (!message.fromUser && !message.isError) {
                                    IconButton(onClick = { speak(message.text) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.VolumeUp, "Read response aloud") }
                                }
                            }
                        }
                    }
                }
                if (busy) item { Text("Thinking…", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp)) }
            }
        }
        OutlinedTextField(question, { question = it.take(1000) }, modifier = Modifier.fillMaxWidth(), label = { Text("Ask a nutrition question") })
        Button(
            enabled = !busy && question.isNotBlank(),
            onClick = { onAsk(question.trim(), language) { question = "" } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (busy) "Thinking..." else "Ask AI Coach") }
        Text("AI estimates are educational, not medical advice.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Text(title, Modifier.padding(start = 10.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.background)
            content()
        }
    }
}

@Composable
private fun SettingsAction(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SettingsScreen(
    vm: LeanoraViewModel,
    modifier: Modifier,
    onEditProfile: () -> Unit,
    onGoal: () -> Unit,
    onWeight: () -> Unit,
    onTarget: () -> Unit,
    onWaterGoal: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var urlText by rememberSaveable(vm.backendUrl) { mutableStateOf(vm.backendUrl) }
    var tokenText by rememberSaveable(vm.apiToken) { mutableStateOf(vm.apiToken) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Not tested yet") }
    var statusOk by remember { mutableStateOf<Boolean?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val versionName = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?" }
    val profile = vm.profile
    val onSurface = MaterialTheme.colorScheme.onSurface
    val variant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = onSurface)
        Text("Personalize your Leanora experience", color = variant)
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("YOUR PROFILE", fontSize = 12.sp, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .85f))
                    Text("Nutrition & goals", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    Text("Keep your details up to date", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .9f))
                }
                Icon(Icons.Default.PersonOutline, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp))
            }
        }
        SettingsSection("Profile & goals", Icons.Default.Person) {
            SettingsAction(Icons.Default.Edit, "Edit personal profile", "Name, age, activity and goal", onEditProfile)
            SettingsAction(Icons.Default.LocalFireDepartment, "Daily calorie goal", "${vm.goal} kcal • Tap to adjust", onGoal)
            SettingsAction(Icons.Default.MonitorWeight, "Current weight", "${profile.weightKg} kg • Update measurement", onWeight)
            SettingsAction(Icons.Default.Flag, "Target weight", "${profile.targetKg} kg • Set your goal", onTarget)
        }
        SettingsSection("Hydration", Icons.Default.WaterDrop) {
            SettingsAction(Icons.Default.WaterDrop, "Daily water target", "${vm.waterGoal} ml • Tap to change", onWaterGoal)
        }
        SettingsSection("Appearance", Icons.Default.Palette) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Dark mode", fontWeight = FontWeight.SemiBold, color = onSurface)
                    Text("Use a darker color scheme", fontSize = 12.sp, color = variant)
                }
                Switch(checked = vm.dark, onCheckedChange = vm::updateDark, modifier = Modifier.semantics { contentDescription = "Dark mode switch" })
            }
        }
        SettingsSection("AI backend", Icons.Default.Cloud) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Use AI features", fontWeight = FontWeight.SemiBold, color = onSurface)
                        Text("Voice logging, photo estimates and coach send data to the server below.", fontSize = 12.sp, color = variant)
                    }
                    Switch(checked = vm.aiConsent, onCheckedChange = vm::updateAiConsent, modifier = Modifier.semantics { contentDescription = "AI features switch" })
                }
                Text("Backend API URL", fontWeight = FontWeight.SemiBold, color = onSurface)
                OutlinedTextField(
                    value = urlText, onValueChange = { urlText = it; urlError = null }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = urlError != null,
                    placeholder = { Text("https://your-service.onrender.com") }, shape = RoundedCornerShape(14.dp), leadingIcon = { Icon(Icons.Default.Link, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    value = tokenText, onValueChange = { tokenText = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Access token (if your server requires one)") }, shape = RoundedCornerShape(14.dp),
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                ErrorText(urlError)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { urlError = vm.saveBackend(urlText, tokenText); if (urlError == null) status = "Saved" }, modifier = Modifier.weight(1f)) { Text("Save") }
                    Button(
                        enabled = !checking && urlText.isNotBlank(),
                        onClick = {
                            val normalized = Api.normalizeBaseUrl(urlText)
                            if (normalized == null) {
                                urlError = "Enter a full https:// URL, for example https://your-service.onrender.com"
                            } else {
                                checking = true; statusOk = null
                                status = "Checking /health… this may take up to 2 minutes while the server wakes"
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) { Api.checkHealth(ApiConfig(normalized, tokenText)) }
                                    checking = false; statusOk = result.first; status = result.second
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(if (checking) "Waiting…" else "Test connection") }
                }
                Text(status, color = when (statusOk) { true -> Color(0xFF1E7B4F); false -> MaterialTheme.colorScheme.error; null -> variant }, fontSize = 13.sp)
            }
        }
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            SettingsSection("Developer testing", Icons.Default.Science) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Add two days of example diary and weight entries to try date navigation.", fontSize = 13.sp, color = variant)
                    Button(onClick = vm::seedDemoData, modifier = Modifier.fillMaxWidth()) { Text("Add 2-day sample data") }
                }
            }
        }
        SettingsSection("Privacy & app", Icons.Default.Security) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Your data", fontWeight = FontWeight.SemiBold, color = onSurface)
                    Text(
                        "Food logs, weight and settings are stored on this device and excluded from cloud backup. Only what you send to an AI feature (voice text, photos, coach questions and context) leaves the device.",
                        fontSize = 13.sp, color = variant,
                    )
                }
            }
            SettingsAction(Icons.Default.Delete, "Delete all my data", "Erase everything stored by Leanora on this device") { confirmDelete = true }
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Text("Leanora • Android", Modifier.weight(1f).padding(start = 12.dp), color = onSurface)
                Text("v$versionName", color = variant)
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete all data?", message = "This permanently erases your profile, diary, weight history and settings from this device. It can't be undone.",
            confirm = "Delete everything", dismiss = "Keep my data",
            onConfirm = { confirmDelete = false; vm.resetAllData() }, onDismiss = { confirmDelete = false },
        )
    }
}
