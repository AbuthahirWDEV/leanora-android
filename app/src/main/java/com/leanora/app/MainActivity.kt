package com.leanora.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LeanoraApp() }
    }
}

@Composable
internal fun LeanoraApp(vm: LeanoraViewModel = viewModel()) {
    val context = LocalContext.current
    LeanoraTheme(vm.dark) {
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshToday() }
        LaunchedEffect(Unit) {
            while (true) {
                delay(60_000)
                vm.refreshToday()
            }
        }
        vm.toast?.let { message ->
            LaunchedEffect(message) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                vm.toast = null
            }
        }
        var splash by rememberSaveable { mutableStateOf(!vm.welcomeSeen) }
        LaunchedEffect(Unit) {
            if (splash) {
                delay(1200)
                splash = false
            }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                splash -> LeanoraSplash()
                !vm.welcomeSeen -> LeanoraWelcome(vm::markWelcomeSeen)
                !vm.onboarded || vm.editingProfile -> ProfileSetup(
                    initial = vm.profile,
                    isEditing = vm.onboarded,
                    canCancel = vm.onboarded && Nutrition.validateSafety(vm.profile) == null,
                    goalIsManual = vm.goalIsManual,
                    onCancel = vm::cancelProfileEdit,
                    onSave = vm::saveProfile,
                )
                else -> MainScaffold(vm)
            }
        }
        vm.busy?.let { BusyDialog(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(vm: LeanoraViewModel) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var dialog by rememberSaveable { mutableStateOf("") }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(-1) }
    var deleteIndex by rememberSaveable { mutableIntStateOf(-1) }
    var editIndex by rememberSaveable { mutableIntStateOf(-1) }
    val addTarget = vm.addTarget
    var cameraMode by remember { mutableStateOf<CameraMode?>(null) }
    var portionFood by remember { mutableStateOf<Food?>(null) }
    var consentAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun openPicker(date: LocalDate, meal: String?) {
        vm.addTarget = AddTarget(date, meal)
        pickerOpen = true
    }

    fun requireAi(action: () -> Unit) {
        if (vm.aiConsent) action() else consentAction = action
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) vm.estimateFromVoice(spoken, vm.addTarget)
        }
    }

    fun launchVoice(language: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, if (language == "ta-IN") "உணவின் பெயர் மற்றும் அளவை சொல்லுங்கள்" else "Say food name and quantity")
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Speech recognition isn't available on this device. Use typing, scanning or a photo instead.", Toast.LENGTH_LONG).show()
        }
    }

    val diaryFoods = vm.foodsOn(vm.diaryDate)
    val todayFoods = vm.foodsOn(vm.today)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Today") })
                NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.Restaurant, null) }, label = { Text("Food") })
                NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.ShowChart, null) }, label = { Text("Progress") })
                NavigationBarItem(tab == 3, { tab = 3 }, icon = { Icon(Icons.Default.SmartToy, null) }, label = { Text("Coach") })
                NavigationBarItem(tab == 4, { tab = 4 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        },
        floatingActionButton = {
            if (tab < 2) {
                FloatingActionButton(onClick = { openPicker(if (tab == 1) vm.diaryDate else vm.today, null) }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, "Add food", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
    ) { pad ->
        val content = Modifier.padding(pad)
        when (tab) {
            0 -> DashboardScreen(
                name = vm.profile.name, foods = todayFoods, goal = vm.goal, macros = vm.macroTargets(),
                waterMl = vm.waterOn(vm.today), waterGoal = vm.waterGoal, today = vm.today, modifier = content,
                onAdd = { meal -> openPicker(vm.today, meal) }, onWaterChange = { vm.setWater(vm.today, it) },
            )
            1 -> DiaryScreen(
                foods = diaryFoods, date = vm.diaryDate, today = vm.today, firstDate = vm.firstUseDate, modifier = content,
                onDateChange = { vm.diaryDate = it }, onAdd = { openPicker(vm.diaryDate, null) },
                onOpenFood = { selectedIndex = it }, onCalendar = { dialog = "calendar" },
            )
            2 -> ProgressScreen(
                weight = vm.profile.weightKg, target = vm.profile.targetKg, weights = vm.weights(), week = vm.weekSummary(),
                goal = vm.goal, streak = vm.streak(), modifier = content, onAddWeight = { dialog = "weight" },
            )
            3 -> CoachScreen(
                chat = vm.chat, busy = vm.coachBusy,
                todaySummary = "Today: ${todayFoods.sumOf { it.kcal }} / ${vm.goal} kcal • Protein ${todayFoods.sumOf { it.protein }} g",
                modifier = content, onAsk = { question, language, sent -> requireAi { vm.askCoach(question, language); sent() } }, onClear = vm::clearChat,
            )
            else -> SettingsScreen(
                vm = vm, modifier = content, onEditProfile = vm::beginProfileEdit, onGoal = { dialog = "goal" },
                onWeight = { dialog = "weight" }, onTarget = { dialog = "target" }, onWaterGoal = { dialog = "water" },
            )
        }
    }

    if (pickerOpen) {
        AddFoodDialog(
            recent = vm.recentFoods(), favorites = vm.favorites, onToggleFavorite = vm::toggleFavorite,
            onCustom = { pickerOpen = false; dialog = "custom" },
            onScan = { pickerOpen = false; cameraMode = CameraMode.BARCODE },
            onManualBarcode = { pickerOpen = false; dialog = "barcode" },
            onPhoto = { requireAi { pickerOpen = false; cameraMode = CameraMode.FOOD_PHOTO } },
            onVoice = { language -> requireAi { pickerOpen = false; launchVoice(language) } },
            onPick = { food -> pickerOpen = false; portionFood = food },
            onDismiss = { pickerOpen = false },
        )
    }

    portionFood?.let { base ->
        PortionDialog(base, onDismiss = { portionFood = null }) { scaled ->
            vm.addFood(addTarget.date, scaled.copy(meal = addTarget.meal ?: scaled.meal))
            portionFood = null
            Toast.makeText(context, "Portion added", Toast.LENGTH_SHORT).show()
        }
    }

    if (dialog == "custom") {
        FoodFormDialog(
            title = "Custom food entry", confirmLabel = "Add food",
            initial = Food("", 0, 0, 0, 0, addTarget.meal ?: Nutrition.mealForHour(LocalTime.now().hour)), blankNumbers = true,
            onDismiss = { dialog = "" },
        ) { food -> vm.addFood(addTarget.date, food); dialog = "" }
    }

    if (dialog == "barcode") {
        BarcodeEntryDialog(busy = vm.busy != null, onDismiss = { dialog = "" }) { code ->
            vm.lookupBarcode(code, addTarget) { found -> if (found) dialog = "" }
        }
    }

    cameraMode?.let { mode ->
        CameraCaptureDialog(
            mode = mode, onClose = { cameraMode = null },
            onBarcode = { code -> cameraMode = null; vm.lookupBarcode(code, addTarget) },
            onPhoto = { bitmap -> cameraMode = null; vm.estimateFromPhoto(bitmap, addTarget) },
        )
    }

    vm.pending?.let { entry ->
        FoodFormDialog(
            title = "Review before saving", confirmLabel = "Add to diary", initial = entry.food, note = entry.note,
            per100 = entry.per100, defaultGrams = entry.defaultGrams, onDismiss = { vm.pending = null },
        ) { food ->
            vm.addFood(entry.date, food)
            vm.pending = null
            Toast.makeText(context, "Added ${food.name}", Toast.LENGTH_SHORT).show()
        }
    }

    if (selectedIndex in diaryFoods.indices) {
        val index = selectedIndex
        FoodDetailDialog(
            diaryFoods[index], onDismiss = { selectedIndex = -1},
            onCopy = { vm.copyFood(vm.diaryDate, index); selectedIndex = -1; Toast.makeText(context, "Food copied", Toast.LENGTH_SHORT).show() },
            onEdit = { editIndex = index; selectedIndex = -1 },
            onDelete = { deleteIndex = index; selectedIndex = -1 },
        )
    }

    if (editIndex in diaryFoods.indices) {
        val index = editIndex
        FoodFormDialog(
            title = "Edit food entry", confirmLabel = "Save", initial = diaryFoods[index], onDismiss = { editIndex = -1 },
        ) { food -> vm.replaceFood(vm.diaryDate, index, food); editIndex = -1 }
    }

    if (deleteIndex in diaryFoods.indices) {
        val index = deleteIndex
        ConfirmDialog(
            title = "Delete food entry?", message = "Delete ${diaryFoods[index].name} from this day’s diary?", confirm = "Delete", dismiss = "Keep entry",
            onConfirm = { vm.deleteFood(vm.diaryDate, index); deleteIndex = -1 }, onDismiss = { deleteIndex = -1 },
        )
    }

    if (dialog == "calendar") {
        val first = vm.firstUseDate
        val last = vm.today
        val state = rememberDatePickerState(
            initialSelectedDateMillis = vm.diaryDate.coerceIn(first, last).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = first.year..last.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !date.isBefore(first) && !date.isAfter(last)
                }

                override fun isSelectableYear(year: Int): Boolean = year in first.year..last.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { dialog = "" },
            confirmButton = {
                TextButton(enabled = state.selectedDateMillis != null, onClick = {
                    state.selectedDateMillis?.let { vm.diaryDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    dialog = ""
                }) { Text("View day") }
            },
            dismissButton = { TextButton(onClick = { dialog = "" }) { Text("Cancel") } },
        ) { DatePicker(state, showModeToggle = false, title = { Text("Choose a diary date") }) }
    }

    if (dialog == "goal") {
        ValueDialog(
            "Daily calorie goal", "kcal", vm.goal.toString(), decimal = false,
            hint = "Minimum ${Nutrition.minCalories(vm.profile.sex)} kcal. Very low goals should only be followed under medical supervision.",
            onDismiss = { dialog = "" },
        ) { vm.setCalorieGoal(it.toIntOrNull()) }
    }
    if (dialog == "weight") {
        ValueDialog("Current weight", "kg", vm.profile.weightKg.toString(), decimal = true, onDismiss = { dialog = "" }) { vm.logWeight(Nutrition.parseDecimal(it)) }
    }
    if (dialog == "target") {
        ValueDialog("Target weight", "kg", vm.profile.targetKg.toString(), decimal = true, onDismiss = { dialog = "" }) { vm.setTargetWeight(Nutrition.parseDecimal(it)) }
    }
    if (dialog == "water") {
        ValueDialog(
            "Daily water target", "Goal (ml)", vm.waterGoal.toString(), decimal = false, hint = "Set a daily hydration goal in millilitres.",
            onDismiss = { dialog = "" },
        ) { vm.setWaterGoal(it.toIntOrNull() ?: 0) }
    }

    consentAction?.let { action ->
        ConsentDialog(
            onAllow = { vm.updateAiConsent(true); consentAction = null; action() },
            onDecline = { consentAction = null },
        )
    }
}
