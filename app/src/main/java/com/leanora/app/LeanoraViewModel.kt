package com.leanora.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

internal data class AddTarget(val date: LocalDate, val meal: String?)

/** An AI or barcode result waiting for the user to review it before it is saved. */
internal data class PendingEntry(
    val food: Food,
    val note: String,
    val date: LocalDate,
    val per100: Food? = null,
    val defaultGrams: Int = 100,
)

internal data class ChatMessage(val fromUser: Boolean, val text: String, val isError: Boolean = false)
internal data class DaySummary(val date: LocalDate, val kcal: Int)

internal class LeanoraViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LeanoraStore(app.getSharedPreferences("leanora", Context.MODE_PRIVATE))
    private val foodCache = HashMap<LocalDate, List<Food>>()

    var revision by mutableIntStateOf(0)
        private set
    var today by mutableStateOf(LocalDate.now())
        private set
    var diaryDate by mutableStateOf(LocalDate.now())
    var addTarget by mutableStateOf(AddTarget(LocalDate.now(), null))

    var profile by mutableStateOf(store.profile())
        private set
    var goal by mutableIntStateOf(store.calorieGoal)
        private set
    var waterGoal by mutableIntStateOf(store.waterGoal)
        private set
    var onboarded by mutableStateOf(store.onboarded)
        private set
    var welcomeSeen by mutableStateOf(store.welcomeSeen)
        private set
    var dark by mutableStateOf(store.darkMode)
        private set
    var backendUrl by mutableStateOf(store.backendUrl)
        private set
    var apiToken by mutableStateOf(store.apiToken)
        private set
    var aiConsent by mutableStateOf(store.aiConsent)
        private set
    var favorites by mutableStateOf(store.favorites())
        private set
    var editingProfile by mutableStateOf(store.onboarded && Nutrition.validateSafety(store.profile()) != null)
        private set

    var busy by mutableStateOf<String?>(null)
        private set
    var pending by mutableStateOf<PendingEntry?>(null)
    var toast by mutableStateOf<String?>(null)

    val chat = mutableStateListOf<ChatMessage>()
    var coachBusy by mutableStateOf(false)
        private set

    val firstUseDate: LocalDate get() = store.firstUseDate ?: LocalDate.of(2000, 1, 1)
    private val apiConfig get() = ApiConfig(backendUrl, apiToken)

    init {
        if (Api.normalizeBaseUrl(backendUrl) != null) {
            viewModelScope.launch(Dispatchers.IO) { Api.warmUp(apiConfig) }
        }
    }

    private fun changed() {
        revision++
    }

    fun refreshToday() {
        val now = LocalDate.now()
        if (now != today) {
            if (diaryDate == today) diaryDate = now
            today = now
        }
    }

    fun foodsOn(date: LocalDate): List<Food> {
        revision.hashCode()
        return foodCache.getOrPut(date) { store.foods(date) }
    }

    fun waterOn(date: LocalDate): Int {
        revision.hashCode()
        return store.water(date)
    }

    fun weights(): List<WeightEntry> {
        revision.hashCode()
        return store.weights()
    }

    fun recentFoods(limit: Int = 30): List<Food> =
        (0..6).flatMap { foodsOn(today.minusDays(it.toLong())).asReversed() }.distinctBy { it.name }.take(limit)

    fun recentFoodNames(): List<String> = recentFoods().map { it.name }

    fun weekSummary(): List<DaySummary> = (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        DaySummary(date, foodsOn(date).sumOf { it.kcal })
    }

    fun streak(): Int = Nutrition.streak(today) { foodsOn(it).isNotEmpty() }

    private fun updateFoods(date: LocalDate, transform: (List<Food>) -> List<Food>) {
        val updated = transform(foodsOn(date))
        foodCache[date] = updated
        store.saveFoods(date, updated)
        changed()
    }

    fun addFood(date: LocalDate, food: Food) = updateFoods(date) { it + food }

    fun replaceFood(date: LocalDate, index: Int, food: Food) = updateFoods(date) { list ->
        list.toMutableList().also { if (index in it.indices) it[index] = food }
    }

    fun deleteFood(date: LocalDate, index: Int) = updateFoods(date) { list ->
        list.toMutableList().also { if (index in it.indices) it.removeAt(index) }
    }

    fun copyFood(date: LocalDate, index: Int) = updateFoods(date) { list ->
        list.toMutableList().also { if (index in it.indices) it.add(index + 1, it[index].copy()) }
    }

    fun setWater(date: LocalDate, ml: Int) {
        store.setWater(date, ml.coerceAtLeast(0))
        changed()
    }

    fun setWaterGoal(ml: Int): String? {
        if (ml !in 500..10000) return "Enter a goal between 500 and 10000 ml."
        waterGoal = ml
        store.waterGoal = ml
        return null
    }

    fun toggleFavorite(name: String) {
        favorites = if (name in favorites) favorites - name else favorites + name
        store.setFavorites(favorites)
    }

    fun macroTargets(): MacroTargets = Nutrition.macroTargets(goal, profile.weightKg, profile.goal)

    fun markWelcomeSeen() {
        welcomeSeen = true
        store.welcomeSeen = true
    }

    fun beginProfileEdit() {
        editingProfile = true
    }

    fun cancelProfileEdit() {
        if (Nutrition.validateSafety(profile) == null) editingProfile = false
    }

    val goalIsManual: Boolean get() = store.goalIsManual

    /** Returns an error message, or null when the profile was saved. */
    fun saveProfile(updated: LeanoraProfile, recalculateGoal: Boolean): String? {
        Nutrition.validateProfile(updated)?.let { return it }
        val firstTime = !onboarded
        val weightChanged = firstTime || updated.weightKg != profile.weightKg
        profile = updated
        store.saveProfile(updated)
        if (firstTime || recalculateGoal || !store.goalIsManual || updated.age < Nutrition.ADULT_AGE) {
            goal = Nutrition.calorieTarget(updated)
            store.calorieGoal = goal
            store.goalIsManual = false
        }
        if (weightChanged) store.addWeight(today, updated.weightKg)
        if (store.firstUseDate == null) store.firstUseDate = today
        onboarded = true
        store.onboarded = true
        editingProfile = false
        changed()
        return null
    }

    fun setCalorieGoal(kcal: Int?): String? {
        Nutrition.validateCalorieGoal(kcal, profile.sex)?.let { return it }
        goal = kcal!!
        store.calorieGoal = goal
        store.goalIsManual = true
        return null
    }

    fun logWeight(kg: Float?): String? {
        if (kg == null || kg !in 30f..300f) return "Enter a weight between 30 and 300 kg."
        profile = profile.copy(weightKg = kg)
        store.saveProfile(profile)
        store.addWeight(today, kg)
        changed()
        return null
    }

    fun setTargetWeight(kg: Float?): String? {
        if (kg == null) return "Enter a weight in kg."
        val candidate = profile.copy(targetKg = kg)
        Nutrition.validateProfile(candidate)?.let { return it }
        profile = candidate
        store.saveProfile(candidate)
        return null
    }

    fun updateDark(value: Boolean) {
        dark = value
        store.darkMode = value
    }

    fun saveBackend(url: String, token: String): String? {
        val normalized = Api.normalizeBaseUrl(url) ?: return "Enter a full https:// URL, for example https://your-service.onrender.com"
        backendUrl = normalized
        apiToken = token.trim()
        store.backendUrl = normalized
        store.apiToken = apiToken
        return null
    }

    fun updateAiConsent(value: Boolean) {
        aiConsent = value
        store.aiConsent = value
    }

    fun resetAllData() {
        store.clearAll()
        foodCache.clear()
        chat.clear()
        profile = store.profile()
        goal = store.calorieGoal
        waterGoal = store.waterGoal
        onboarded = false
        welcomeSeen = false
        dark = false
        backendUrl = store.backendUrl
        apiToken = ""
        aiConsent = false
        favorites = emptySet()
        editingProfile = false
        diaryDate = today
        changed()
        toast = "All Leanora data on this device was deleted."
    }

    fun seedDemoData() {
        if (store.demoSeeded) {
            toast = "Sample diary data is already loaded"
            return
        }
        store.saveFoods(today.minusDays(1), listOf(Food("Demo • Oat bowl", 320, 12, 45, 8, "Breakfast"), Food("Demo • Yogurt", 140, 15, 12, 2, "Snack")))
        store.saveFoods(today.minusDays(2), listOf(Food("Demo • Idli", 130, 4, 26, 1, "Breakfast"), Food("Demo • Paneer", 265, 18, 4, 20, "Dinner")))
        store.addWeight(today.minusDays(2), profile.weightKg + 0.7f)
        store.addWeight(today.minusDays(1), profile.weightKg + 0.3f)
        store.firstUseDate = today.minusDays(2)
        store.demoSeeded = true
        foodCache.clear()
        changed()
        toast = "Two days of sample diary data added"
    }

    private fun defaultMeal(target: AddTarget): String = target.meal ?: Nutrition.mealForHour(LocalTime.now().hour)

    private fun showEstimate(result: ApiResult<Estimate>, target: AddTarget, keepSuggestedMeal: Boolean) {
        busy = null
        when (result) {
            is ApiResult.Ok -> {
                val food = result.value.food
                val meal = target.meal ?: if (keepSuggestedMeal) food.meal else defaultMeal(target)
                pending = PendingEntry(food.copy(meal = meal), result.value.note, target.date)
            }
            is ApiResult.Err -> toast = result.message
        }
    }

    fun estimateFromVoice(description: String, target: AddTarget) {
        busy = "Estimating nutrition…"
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { Api.estimateText(apiConfig, description) }
            showEstimate(result, target, keepSuggestedMeal = true)
        }
    }

    fun estimateFromPhoto(bitmap: Bitmap, target: AddTarget) {
        busy = "Analyzing your photo…"
        viewModelScope.launch {
            val meal = defaultMeal(target)
            val result = withContext(Dispatchers.IO) { Api.estimatePhoto(apiConfig, bitmap, meal) }
            showEstimate(result, target, keepSuggestedMeal = false)
        }
    }

    fun lookupBarcode(code: String, target: AddTarget, onDone: (Boolean) -> Unit = {}) {
        busy = "Looking up barcode…"
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { Api.lookupBarcode(code) }
            busy = null
            when (result) {
                is ApiResult.Ok -> {
                    val product = result.value
                    val grams = product.servingGrams ?: 100
                    pending = PendingEntry(
                        food = Nutrition.scalePer100g(product.per100, grams).copy(name = product.name, meal = defaultMeal(target)),
                        note = "Nutrition per 100 g from Open Food Facts; adjust the amount below.",
                        date = target.date,
                        per100 = product.per100,
                        defaultGrams = grams,
                    )
                    onDone(true)
                }
                is ApiResult.Err -> {
                    toast = result.message
                    onDone(false)
                }
            }
        }
    }

    fun askCoach(question: String, language: String) {
        val history = chat.filter { !it.isError }.takeLast(8).map { ChatTurn(if (it.fromUser) "user" else "coach", it.text) }
        chat.add(ChatMessage(true, question))
        coachBusy = true
        val foods = foodsOn(today)
        val payload = CoachPayload(
            message = question, language = language, calorieGoal = goal, calories = foods.sumOf { it.kcal },
            proteinG = foods.sumOf { it.protein }, carbsG = foods.sumOf { it.carbs }, fatG = foods.sumOf { it.fat },
            fitnessGoal = profile.goal, weightKg = profile.weightKg, targetKg = profile.targetKg,
            recentFoods = recentFoodNames(), history = history,
        )
        viewModelScope.launch {
            when (val result = withContext(Dispatchers.IO) { Api.coach(apiConfig, payload) }) {
                is ApiResult.Ok -> chat.add(ChatMessage(false, result.value))
                is ApiResult.Err -> chat.add(ChatMessage(false, result.message, isError = true))
            }
            coachBusy = false
        }
    }

    fun clearChat() = chat.clear()
}
