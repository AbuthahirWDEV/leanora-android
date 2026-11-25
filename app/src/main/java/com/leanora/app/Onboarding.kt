package com.leanora.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun LeanoraSplash() {
    val transition = rememberInfiniteTransition(label = "splash-loader")
    val alpha by transition.animateFloat(
        initialValue = .35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "loader-alpha",
    )
    Box(Modifier.fillMaxSize().background(SplashBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(112.dp).background(BrandOrange, RoundedCornerShape(32.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Favorite, null, tint = Color(0xFFFFD7B8), modifier = Modifier.size(56.dp))
            }
            Text("LEANORA", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 5.sp, color = Color.White)
            Text("Nourish your goals. Own your journey.", fontSize = 14.sp, color = Color(0xFFFFDCC7))
            Spacer(Modifier.height(18.dp))
            CircularProgressIndicator(color = BrandOrange, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Text("Getting things ready…", fontSize = 12.sp, color = Color.White.copy(alpha = alpha))
        }
    }
}

@Composable
internal fun LeanoraWelcome(onStart: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "leanora-welcome")
    val scale by transition.animateFloat(
        initialValue = .94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "logo-scale",
    )
    val rotation by transition.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "logo-rotation",
    )
    Box(Modifier.fillMaxSize().background(SplashBackground).padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                Modifier.size(128.dp).graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation }
                    .background(BrandOrange, RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Favorite, null, tint = Mint, modifier = Modifier.size(62.dp)) }
            Text("LEANORA", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 5.sp, color = Color.White)
            Text("Nourish your goals. Own your journey.", fontSize = 15.sp, color = Color(0xFFFFDCC7))
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = SplashBackground),
            ) {
                Text("Get started", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null)
            }
            Text("Your wellness, thoughtfully tracked.", fontSize = 12.sp, color = Color(0xFFD2B7A7))
        }
    }
}

@Composable
internal fun ProfileSetup(
    initial: LeanoraProfile,
    isEditing: Boolean,
    canCancel: Boolean,
    goalIsManual: Boolean,
    onCancel: () -> Unit,
    onSave: (LeanoraProfile, Boolean) -> String?,
) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var age by rememberSaveable { mutableStateOf(initial.age.toString()) }
    var sex by rememberSaveable { mutableStateOf(initial.sex) }
    var height by rememberSaveable { mutableStateOf(initial.heightCm.toString()) }
    var weight by rememberSaveable { mutableStateOf(initial.weightKg.toString()) }
    var target by rememberSaveable { mutableStateOf(initial.targetKg.toString()) }
    var activity by rememberSaveable { mutableStateOf(initial.activity) }
    var goal by rememberSaveable { mutableStateOf(initial.goal) }
    var recalc by rememberSaveable { mutableStateOf(!goalIsManual) }
    var error by remember { mutableStateOf<String?>(null) }

    val ageValue = age.toIntOrNull()
    val isMinor = ageValue != null && ageValue < Nutrition.ADULT_AGE
    val effectiveGoal = if (isMinor) Nutrition.MAINTAIN else goal

    LazyColumn(
        Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 30.dp, bottom = 30.dp),
    ) {
        item {
            Text("LEANORA", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text(
                if (isEditing) "Edit your profile" else "Let’s personalize your plan",
                fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
            )
            Text("Tell us about yourself to estimate daily calorie needs.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { OutlinedTextField(name, { name = it }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item {
            OutlinedTextField(
                age, { age = it.filter(Char::isDigit).take(3) }, label = { Text("Age") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text("Sex (used for the calorie formula)")
            Row { listOf("Male", "Female").forEach { FilterChip(selected = sex == it, onClick = { sex = it }, label = { Text(it) }, modifier = Modifier.padding(end = 8.dp)) } }
        }
        item {
            OutlinedTextField(
                height, { height = it.filter(Char::isDigit).take(3) }, label = { Text("Height (cm)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                weight, { weight = it.take(6) }, label = { Text("Current weight (kg)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                target, { target = it.take(6) }, label = { Text("Target weight (kg)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text("Fitness goal", fontWeight = FontWeight.SemiBold)
            if (isMinor) {
                Text(
                    "Under 18: Leanora only offers a maintenance target. Please talk to a doctor or dietitian before changing your weight.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                )
            }
            FITNESS_GOALS.forEach {
                FilterChip(
                    selected = effectiveGoal == it, onClick = { goal = it }, enabled = !isMinor || it == Nutrition.MAINTAIN,
                    label = { Text(it) }, modifier = Modifier.padding(end = 4.dp),
                )
            }
        }
        item {
            Text("Activity level", fontWeight = FontWeight.SemiBold)
            ACTIVITY_LEVELS.forEach { FilterChip(selected = activity == it, onClick = { activity = it }, label = { Text(it) }, modifier = Modifier.padding(end = 4.dp)) }
        }
        if (isEditing) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Recalculate daily calorie goal", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (goalIsManual) "You set a custom goal. Turn on to replace it with the calculated one." else "Update the goal from these details.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = recalc, onCheckedChange = { recalc = it })
                }
            }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) } }
        item {
            Button(
                onClick = {
                    val profile = LeanoraProfile(
                        name = name.trim(), age = ageValue ?: 0, sex = sex, heightCm = height.toIntOrNull() ?: 0,
                        weightKg = Nutrition.parseDecimal(weight) ?: 0f, targetKg = Nutrition.parseDecimal(target) ?: 0f,
                        activity = activity, goal = effectiveGoal,
                    )
                    error = onSave(profile, recalc)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
            ) { Text(if (isEditing) "Save profile" else "Create my nutrition plan", fontWeight = FontWeight.Bold) }
        }
        if (canCancel) item { OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") } }
        item { Text("Calorie estimates are a guide, not medical advice.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
