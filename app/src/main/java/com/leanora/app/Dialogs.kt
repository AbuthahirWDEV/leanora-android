package com.leanora.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val FullWidthDialog = DialogProperties(usePlatformDefaultWidth = false)

@Composable
internal fun FormDialogColumn(content: @Composable ColumnScope.() -> Unit) {
    val maxHeight = (LocalContext.current.resources.configuration.screenHeightDp - 176).coerceAtLeast(144).dp
    Column(
        Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content,
    )
}

@Composable
internal fun ErrorText(message: String?) {
    if (message != null) Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
}

/** Single-value dialog. [onSave] returns an error message to show, or null when the value was accepted. */
@Composable
internal fun ValueDialog(
    title: String,
    label: String,
    initial: String,
    decimal: Boolean,
    hint: String? = null,
    onDismiss: () -> Unit,
    onSave: (String) -> String?,
) {
    var text by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                hint?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                OutlinedTextField(
                    value = text, onValueChange = { text = it.take(8); error = null }, label = { Text(label) }, singleLine = true, isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
                )
                ErrorText(error)
            }
        },
        confirmButton = { TextButton(onClick = { error = onSave(text); if (error == null) onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun MealSelector(meal: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Meal", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(meal, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, "Choose meal")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                MEALS.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
            }
        }
    }
}

/**
 * Shared form for custom foods, edits and reviewing AI/barcode estimates.
 * When [per100] is set, an amount-in-grams field rescales the nutrition values.
 */
@Composable
internal fun FoodFormDialog(
    title: String,
    confirmLabel: String,
    initial: Food,
    blankNumbers: Boolean = false,
    note: String? = null,
    per100: Food? = null,
    defaultGrams: Int = 100,
    onDismiss: () -> Unit,
    onSave: (Food) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var kcal by remember { mutableStateOf(if (blankNumbers) "" else initial.kcal.toString()) }
    var protein by remember { mutableStateOf(initial.protein.toString()) }
    var carbs by remember { mutableStateOf(initial.carbs.toString()) }
    var fat by remember { mutableStateOf(initial.fat.toString()) }
    var meal by remember { mutableStateOf(initial.meal) }
    var grams by remember { mutableStateOf(defaultGrams.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val digits = KeyboardOptions(keyboardType = KeyboardType.Number)

    fun rescale(text: String) {
        grams = text.filter(Char::isDigit).take(4)
        val g = grams.toIntOrNull()
        if (per100 != null && g != null && g > 0) {
            val scaled = Nutrition.scalePer100g(per100, g)
            kcal = scaled.kcal.toString(); protein = scaled.protein.toString(); carbs = scaled.carbs.toString(); fat = scaled.fat.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            FormDialogColumn {
                note?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                OutlinedTextField(name, { name = it.take(120); error = null }, label = { Text("Food name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (per100 != null) {
                    OutlinedTextField(grams, { rescale(it); error = null }, label = { Text("Amount (g)") }, singleLine = true, keyboardOptions = digits, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(kcal, { kcal = it.filter(Char::isDigit).take(5); error = null }, label = { Text("Calories (kcal)") }, singleLine = true, keyboardOptions = digits, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(protein, { protein = it.filter(Char::isDigit).take(4) }, label = { Text("Protein (g)") }, singleLine = true, keyboardOptions = digits, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(carbs, { carbs = it.filter(Char::isDigit).take(4) }, label = { Text("Carbs (g)") }, singleLine = true, keyboardOptions = digits, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(fat, { fat = it.filter(Char::isDigit).take(4) }, label = { Text("Fat (g)") }, singleLine = true, keyboardOptions = digits, modifier = Modifier.fillMaxWidth())
                MealSelector(meal) { meal = it }
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val kcalValue = kcal.toIntOrNull()
                val gramsValue = grams.toIntOrNull()
                when {
                    name.isBlank() -> error = "Enter a food name."
                    kcalValue == null -> error = "Enter the calories."
                    kcalValue > 10000 -> error = "Calories look too high for one entry."
                    per100 != null && (gramsValue == null || gramsValue <= 0) -> error = "Enter the amount in grams."
                    else -> {
                        val finalName = if (per100 != null) "${name.trim()} ($gramsValue g)" else name.trim()
                        onSave(Food(finalName, kcalValue, protein.toIntOrNull() ?: 0, carbs.toIntOrNull() ?: 0, fat.toIntOrNull() ?: 0, meal))
                    }
                }
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun PortionDialog(food: Food, onDismiss: () -> Unit, onAdd: (Food) -> Unit) {
    var text by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose portion") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(food.name, fontWeight = FontWeight.SemiBold)
                Text("Enter servings (1 = listed serving). Calories and macros scale proportionally.")
                OutlinedTextField(
                    text, { text = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(5); error = null },
                    label = { Text("Servings / quantity") }, singleLine = true, isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val multiplier = Nutrition.parseDecimal(text)
                if (multiplier == null || multiplier !in 0.1f..20f) error = "Enter a quantity between 0.1 and 20." else onAdd(Nutrition.scale(food, multiplier))
            }) { Text("Add portion") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun BarcodeEntryDialog(busy: Boolean, onDismiss: () -> Unit, onLookup: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Barcode nutrition lookup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the barcode number printed below the product barcode. Nutrition data is retrieved from Open Food Facts.")
                OutlinedTextField(
                    code, { code = it.filter(Char::isDigit).take(20) }, label = { Text("Barcode number") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = code.length >= 8 && !busy, onClick = { onLookup(code) }) { Text(if (busy) "Looking up…" else "Look up") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun ConsentDialog(onAllow: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text("Use AI features?") },
        text = {
            Text(
                "Voice food logging, food photos and the AI coach send what you say, type or photograph — plus your calorie, weight and " +
                    "recent-food context for the coach — to the Leanora server, which forwards it to Google Gemini to produce an estimate. " +
                    "Nothing is sent unless you use these features, and you can turn them off any time in Settings. Everything else stays on this device.",
            )
        },
        confirmButton = { TextButton(onClick = onAllow) { Text("Allow") } },
        dismissButton = { TextButton(onClick = onDecline) { Text("Not now") } },
    )
}

@Composable
internal fun ConfirmDialog(title: String, message: String, confirm: String, dismiss: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss) } },
    )
}

@Composable
internal fun BusyDialog(message: String) {
    Dialog(onDismissRequest = {}) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(16.dp))
                Text(message, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
internal fun FoodDetailDialog(food: Food, onDismiss: () -> Unit, onCopy: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = FullWidthDialog) {
        Surface(
            modifier = Modifier.fillMaxWidth(.94f).widthIn(max = 480.dp), shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp,
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(food.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(food.meal, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close food details") }
                }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ENERGY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            Text("${food.kcal} kcal", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Icon(Icons.Default.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                    }
                }
                Text("Nutrition per logged portion", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailMacro("Protein", "${food.protein} g", Modifier.weight(1f))
                    DetailMacro("Carbohydrates", "${food.carbs} g", Modifier.weight(1f))
                    DetailMacro("Fat", "${food.fat} g", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Icon(Icons.Default.DeleteOutline, null); Spacer(Modifier.width(6.dp)); Text("Delete") }
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(6.dp)); Text("Edit") }
                    Button(onClick = onCopy, modifier = Modifier.weight(1f)) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copy") }
                }
            }
        }
    }
}

@Composable
private fun DetailMacro(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
internal fun AddFoodDialog(
    recent: List<Food>,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onCustom: () -> Unit,
    onScan: () -> Unit,
    onManualBarcode: () -> Unit,
    onPhoto: () -> Unit,
    onVoice: (String) -> Unit,
    onPick: (Food) -> Unit,
    onDismiss: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    val options = (recent + CATALOG).distinctBy { it.name }.filter { it.name.contains(search, ignoreCase = true) && (!favoritesOnly || it.name in favorites) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add food") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onCustom, modifier = Modifier.fillMaxWidth()) { Text("Custom food / portion") }
                OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.QrCodeScanner, null); Spacer(Modifier.width(8.dp)); Text("Scan product barcode")
                }
                OutlinedButton(onClick = onManualBarcode, modifier = Modifier.fillMaxWidth()) { Text("Enter barcode manually") }
                Button(onClick = onPhoto, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Camera • estimate food")
                }
                Button(onClick = { onVoice("en-IN") }, modifier = Modifier.fillMaxWidth()) { Text("Voice food logging • English") }
                Button(onClick = { onVoice("ta-IN") }, modifier = Modifier.fillMaxWidth()) { Text("தமிழில் உணவு பதிவு") }
                OutlinedTextField(
                    value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Search recent and catalog foods") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !favoritesOnly, onClick = { favoritesOnly = false }, label = { Text("All / Recent") })
                    FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = true }, label = { Text("★ Favorites") })
                }
                if (options.isEmpty()) Text("No matching foods. Try a custom entry or AI estimate.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                options.forEach { food ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onPick(food) }, modifier = Modifier.weight(1f)) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(food.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                Text("${food.kcal} kcal • P${food.protein} C${food.carbs} F${food.fat}g", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        val isFavorite = food.name in favorites
                        IconButton(onClick = { onToggleFavorite(food.name) }) {
                            Icon(
                                if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
