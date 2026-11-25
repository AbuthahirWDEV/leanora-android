package com.leanora.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun Info(title: String, body: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MacroTile(title: String, eaten: Int, target: Int, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text("${eaten}g", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { (eaten / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp))
            Text("of ${target}g", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
internal fun DashboardScreen(
    name: String,
    foods: List<Food>,
    goal: Int,
    macros: MacroTargets,
    waterMl: Int,
    waterGoal: Int,
    today: LocalDate,
    modifier: Modifier,
    onAdd: (String?) -> Unit,
    onWaterChange: (Int) -> Unit,
) {
    val kcal = foods.sumOf { it.kcal }
    val remaining = goal - kcal
    val progress = (kcal.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TODAY • " + today.format(DateTimeFormatter.ofPattern("EEE, MMM d")), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = variant)
                    Text("Hi, " + name.ifBlank { "there" } + " 👋", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = onSurface)
                    Text("Small steps. Lasting change.", fontSize = 13.sp, color = variant)
                }
                Box(Modifier.size(42.dp).background(Lavender, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Favorite, null, tint = Color(0xFFB5471A)) }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Ink)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("CALORIES LEFT", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, color = Color.White.copy(alpha = .7f))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(kotlin.math.abs(remaining).toString(), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text(if (remaining >= 0) " kcal left" else " kcal over", fontSize = 14.sp, color = Color.White.copy(alpha = .75f), modifier = Modifier.padding(bottom = 6.dp))
                            }
                        }
                        Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), color = BrandOrange, trackColor = Color.White.copy(alpha = .12f), strokeWidth = 8.dp)
                            Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Text("$kcal eaten  •  $goal daily goal", fontSize = 13.sp, color = Color.White.copy(alpha = .75f))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = BrandOrange, trackColor = Color.White.copy(alpha = .12f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroTile("PROTEIN", foods.sumOf { it.protein }, macros.proteinG, Modifier.weight(1f))
                MacroTile("CARBS", foods.sumOf { it.carbs }, macros.carbsG, Modifier.weight(1f))
                MacroTile("FAT", foods.sumOf { it.fat }, macros.fatG, Modifier.weight(1f))
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WaterDrop, null, tint = WaterBlue, modifier = Modifier.size(28.dp))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text("Water intake", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface)
                            Text("$waterMl / $waterGoal ml logged", fontSize = 13.sp, color = variant)
                        }
                        Text("${(waterMl * 100 / waterGoal.coerceAtLeast(1)).coerceAtMost(100)}%", fontWeight = FontWeight.Bold, color = WaterBlue)
                    }
                    LinearProgressIndicator(
                        progress = { (waterMl / waterGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp), color = WaterBlue, trackColor = Color(0xFFE8F2FA),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onWaterChange((waterMl - 250).coerceAtLeast(0)) }, modifier = Modifier.weight(1f), enabled = waterMl > 0) { Text("−250 ml") }
                        Button(onClick = { onWaterChange(waterMl + 250) }, modifier = Modifier.weight(1f)) { Text("+250 ml") }
                        Button(onClick = { onWaterChange(waterMl + 500) }, modifier = Modifier.weight(1f)) { Text("+500 ml") }
                    }
                    Text("Hydration target is a customizable reminder, not medical advice.", fontSize = 11.sp, color = variant)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Meals & snacks", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onSurface)
                    Text("Your day, one meal at a time", fontSize = 12.sp, color = variant)
                }
                TextButton(onClick = { onAdd(null) }) { Icon(Icons.Default.Add, null); Text("Add food") }
            }
        }
        if (foods.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Restaurant, null, tint = primary, modifier = Modifier.size(30.dp))
                        Text("Start with your first meal", fontWeight = FontWeight.Bold, color = onSurface)
                        Text("Log food to see calories and macros here.", fontSize = 13.sp, color = variant)
                        Button(onClick = { onAdd(null) }, shape = RoundedCornerShape(14.dp)) { Text("Log food") }
                    }
                }
            }
        }
        MEALS.forEach { meal ->
            val mealFoods = foods.filter { it.meal == meal }
            if (mealFoods.isNotEmpty()) {
                item(key = meal) {
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(meal, fontWeight = FontWeight.Bold, color = onSurface)
                                    Text("${mealFoods.size} item${if (mealFoods.size == 1) "" else "s"}", fontSize = 11.sp, color = variant)
                                }
                                Text("${mealFoods.sumOf { it.kcal }} kcal", fontWeight = FontWeight.Bold, color = primary)
                                IconButton(onClick = { onAdd(meal) }) { Icon(Icons.Default.Add, "Add food to $meal", tint = primary) }
                            }
                            mealFoods.forEach {
                                Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 3.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(it.name, Modifier.weight(1f), fontSize = 14.sp, color = onSurface)
                                    Text("${it.kcal} kcal", fontSize = 13.sp, color = variant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DiaryScreen(
    foods: List<Food>,
    date: LocalDate,
    today: LocalDate,
    firstDate: LocalDate,
    modifier: Modifier,
    onDateChange: (LocalDate) -> Unit,
    onAdd: () -> Unit,
    onOpenFood: (Int) -> Unit,
    onCalendar: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var mealFilter by rememberSaveable { mutableStateOf("All") }
    val filtered = foods.withIndex().filter { (_, x) -> (mealFilter == "All" || x.meal.equals(mealFilter, true)) && x.name.contains(query, true) }
    val primary = MaterialTheme.colorScheme.primary
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Food diary", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Your daily nutrition, at a glance", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add food", tint = primary) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(enabled = date.isAfter(firstDate), onClick = { onDateChange(date.minusDays(1)) }) { Icon(Icons.Default.ChevronLeft, "Previous day") }
            Column(Modifier.weight(1f).clickable(onClick = onCalendar), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (date == today) "Today" else date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")),
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                )
                Text(date.format(DateTimeFormatter.ofPattern("MMMM yyyy")), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(enabled = date.isBefore(today), onClick = { onDateChange(date.plusDays(1)) }) { Icon(Icons.Default.ChevronRight, "Next day") }
            IconButton(onClick = onCalendar) { Icon(Icons.Default.CalendarMonth, "Choose date", tint = primary) }
        }
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${foods.size} item${if (foods.size == 1) "" else "s"} logged", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text("${foods.sumOf { it.kcal }} kcal", fontWeight = FontWeight.Bold, color = primary)
            }
        }
        OutlinedTextField(
            query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, "Search") },
            placeholder = { Text("Search logged foods") }, shape = RoundedCornerShape(16.dp),
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All") .plus(MEALS).forEach { FilterChip(selected = mealFilter == it, onClick = { mealFilter = it }, label = { Text(it) }) }
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.RestaurantMenu, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (foods.isEmpty()) "No foods logged yet" else "No matching foods", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Add a meal to start tracking.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onAdd) { Text("Add food") }
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                items(filtered) { (index, item) ->
                    Card(Modifier.fillMaxWidth().clickable { onOpenFood(index) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 15.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                Text(item.meal + " • Tap for details", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Text("${item.kcal} kcal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primary)
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProgressScreen(
    weight: Float,
    target: Float,
    weights: List<WeightEntry>,
    week: List<DaySummary>,
    goal: Int,
    streak: Int,
    modifier: Modifier,
    onAddWeight: () -> Unit,
) {
    val surface = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    val points = weights.takeLast(14)
    val loggedDays = week.filter { it.kcal > 0 }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Progress", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.weight(1f), shape = RoundedCornerShape(18.dp), colors = surface) {
                Column(Modifier.padding(14.dp)) {
                    Text("Current", fontSize = 12.sp, color = variant)
                    Text(String.format(Locale.US, "%.1f kg", weight), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = onSurface)
                }
            }
            Card(Modifier.weight(1f), shape = RoundedCornerShape(18.dp), colors = surface) {
                Column(Modifier.padding(14.dp)) {
                    Text("Target", fontSize = 12.sp, color = variant)
                    Text(String.format(Locale.US, "%.1f kg", target), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Info("Remaining to target", Nutrition.weightGoalSummary(weight, target))
        Card(shape = RoundedCornerShape(20.dp), colors = surface) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Weight trend", fontWeight = FontWeight.Bold, color = onSurface)
                Text(if (points.size < 2) "Log at least two measurements to see your trend." else "Last ${points.size} measurements", fontSize = 12.sp, color = variant)
                if (points.isNotEmpty()) {
                    WeightTrendChart(points.map { it.kg }, Modifier.fillMaxWidth().height(150.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(points.first().date.format(DateTimeFormatter.ofPattern("d MMM")), fontSize = 11.sp, color = variant)
                        Text(points.last().date.format(DateTimeFormatter.ofPattern("d MMM")), fontSize = 11.sp, color = variant)
                    }
                    Text(
                        "Min ${String.format(Locale.US, "%.1f", points.minOf { it.kg })} kg  •  Max ${String.format(Locale.US, "%.1f", points.maxOf { it.kg })} kg",
                        fontSize = 12.sp, color = variant,
                    )
                }
            }
        }
        Button(onClick = onAddWeight, modifier = Modifier.fillMaxWidth()) { Text("Add weight measurement") }
        Card(shape = RoundedCornerShape(20.dp), colors = surface) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Last 7 days • calories", fontWeight = FontWeight.Bold, color = onSurface)
                WeeklyCaloriesChart(week, goal, Modifier.fillMaxWidth().height(140.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { Text(it.date.format(DateTimeFormatter.ofPattern("EEEEE")), fontSize = 11.sp, color = variant) }
                }
                val average = if (loggedDays.isEmpty()) 0 else loggedDays.sumOf { it.kcal } / loggedDays.size
                Text(
                    "Average on logged days: $average kcal (goal $goal)  •  ${loggedDays.size}/7 days logged  •  $streak-day streak",
                    fontSize = 12.sp, color = variant,
                )
            }
        }
        Info(
            "Recent measurements",
            weights.takeLast(8).reversed().joinToString("\n") { it.date.toString() + " • " + String.format(Locale.US, "%.1f", it.kg) + " kg" }
                .ifBlank { "No measurements yet. Add your first weight." },
        )
    }
}

@Composable
private fun WeightTrendChart(values: List<Float>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .25f)
    val dot = MaterialTheme.colorScheme.surface
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val minV = values.min() - 1f
        val maxV = values.max() + 1f
        val range = (maxV - minV).coerceAtLeast(1f)
        val left = 12f
        val right = size.width - 12f
        val top = 12f
        val bottom = size.height - 12f
        for (i in 0..3) {
            val y = top + (bottom - top) * i / 3f
            drawLine(grid, Offset(left, y), Offset(right, y), strokeWidth = 1f)
        }
        val pts = values.mapIndexed { index, value ->
            Offset(if (values.size == 1) (left + right) / 2 else left + (right - left) * index / (values.size - 1), bottom - (value - minV) / range * (bottom - top))
        }
        pts.zipWithNext().forEach { (a, b) -> drawLine(line, a, b, strokeWidth = 4f, cap = StrokeCap.Round) }
        pts.forEach { drawCircle(line, 5f, it); drawCircle(dot, 2f, it) }
    }
}

@Composable
private fun WeeklyCaloriesChart(week: List<DaySummary>, goal: Int, modifier: Modifier = Modifier) {
    val bar = MaterialTheme.colorScheme.primary
    val goalLine = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val ceiling = maxOf(goal, week.maxOfOrNull { it.kcal } ?: 0, 1) * 1.1f
        val slot = size.width / week.size
        val barWidth = slot * 0.55f
        week.forEachIndexed { index, day ->
            val height = size.height * day.kcal / ceiling
            drawRect(bar, Offset(index * slot + (slot - barWidth) / 2, size.height - height), Size(barWidth, height))
        }
        val goalY = size.height - size.height * goal / ceiling
        drawLine(goalLine, Offset(0f, goalY), Offset(size.width, goalY), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
    }
}
