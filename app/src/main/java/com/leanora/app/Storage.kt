package com.leanora.app

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal data class WeightEntry(val date: LocalDate, val kg: Float)

internal object FoodCodec {
    fun encode(foods: List<Food>): String {
        val array = JSONArray()
        foods.forEach {
            array.put(
                JSONObject().put("name", it.name).put("kcal", it.kcal).put("protein", it.protein)
                    .put("carbs", it.carbs).put("fat", it.fat).put("meal", it.meal),
            )
        }
        return array.toString()
    }

    /** Skips individual corrupt entries instead of discarding the whole day. */
    fun decode(raw: String?): List<Food> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            runCatching {
                val o = array.getJSONObject(index)
                Food(
                    o.getString("name"), o.getInt("kcal"), o.optInt("protein", 0), o.optInt("carbs", 0),
                    o.optInt("fat", 0), Nutrition.sanitizeMeal(o.optString("meal")),
                )
            }.getOrNull()
        }
    }
}

internal object WeightCodec {
    fun encode(entries: List<WeightEntry>): String {
        val array = JSONArray()
        entries.forEach { array.put(JSONObject().put("date", it.date.toString()).put("kg", it.kg.toDouble())) }
        return array.toString()
    }

    fun decode(raw: String?): List<WeightEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            runCatching {
                val o = array.getJSONObject(index)
                WeightEntry(LocalDate.parse(o.getString("date")), o.getDouble("kg").toFloat())
            }.getOrNull()
        }.sortedBy { it.date }
    }

    fun upsert(entries: List<WeightEntry>, entry: WeightEntry): List<WeightEntry> =
        (entries.filterNot { it.date == entry.date } + entry).sortedBy { it.date }
}

internal class LeanoraStore(private val prefs: SharedPreferences) {
    fun foods(date: LocalDate): List<Food> = FoodCodec.decode(prefs.getString("foods_$date", null))
    fun saveFoods(date: LocalDate, foods: List<Food>) {
        prefs.edit().putString("foods_$date", FoodCodec.encode(foods)).apply()
    }

    fun water(date: LocalDate): Int = prefs.getInt("water_$date", 0)
    fun setWater(date: LocalDate, ml: Int) = prefs.edit().putInt("water_$date", ml).apply()

    fun weights(): List<WeightEntry> = WeightCodec.decode(prefs.getString("weight_history", "[]"))
    fun addWeight(date: LocalDate, kg: Float) {
        prefs.edit().putString("weight_history", WeightCodec.encode(WeightCodec.upsert(weights(), WeightEntry(date, kg)))).apply()
    }

    fun favorites(): Set<String> = prefs.getStringSet("favorite_foods", emptySet())?.toSet() ?: emptySet()
    fun setFavorites(values: Set<String>) = prefs.edit().putStringSet("favorite_foods", values).apply()

    fun profile(): LeanoraProfile = LeanoraProfile(
        name = prefs.getString("profile_name", "") ?: "",
        age = prefs.getInt("profile_age", 26),
        sex = prefs.getString("profile_sex", "Male") ?: "Male",
        heightCm = prefs.getInt("profile_height", 173),
        weightKg = prefs.getFloat("weight", 78f),
        targetKg = prefs.getFloat("target", 72f),
        activity = prefs.getString("activity", "Lightly active") ?: "Lightly active",
        goal = prefs.getString("fitness_goal", "Lose weight") ?: "Lose weight",
    )

    fun saveProfile(p: LeanoraProfile) {
        prefs.edit().putString("profile_name", p.name).putInt("profile_age", p.age).putString("profile_sex", p.sex)
            .putInt("profile_height", p.heightCm).putFloat("weight", p.weightKg).putFloat("target", p.targetKg)
            .putString("activity", p.activity).putString("fitness_goal", p.goal).apply()
    }

    var calorieGoal: Int
        get() = prefs.getInt("goal", 1850)
        set(value) = prefs.edit().putInt("goal", value).apply()
    var goalIsManual: Boolean
        get() = prefs.getBoolean("goal_manual", false)
        set(value) = prefs.edit().putBoolean("goal_manual", value).apply()
    var waterGoal: Int
        get() = prefs.getInt("water_goal", 2000)
        set(value) = prefs.edit().putInt("water_goal", value).apply()
    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(value) = prefs.edit().putBoolean("onboarded", value).apply()
    var welcomeSeen: Boolean
        get() = prefs.getBoolean("welcome_seen", false)
        set(value) = prefs.edit().putBoolean("welcome_seen", value).apply()
    var darkMode: Boolean
        get() = prefs.getBoolean("dark", false)
        set(value) = prefs.edit().putBoolean("dark", value).apply()
    var backendUrl: String
        get() = prefs.getString("backend", DEFAULT_BACKEND) ?: DEFAULT_BACKEND
        set(value) = prefs.edit().putString("backend", value).apply()
    var apiToken: String
        get() = prefs.getString("api_token", "") ?: ""
        set(value) = prefs.edit().putString("api_token", value).apply()
    var aiConsent: Boolean
        get() = prefs.getBoolean("ai_consent", false)
        set(value) = prefs.edit().putBoolean("ai_consent", value).apply()
    var firstUseDate: LocalDate?
        get() = prefs.getString("first_use_date", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        set(value) = prefs.edit().putString("first_use_date", value?.toString()).apply()
    var demoSeeded: Boolean
        get() = prefs.getBoolean("sample_data_seeded", false)
        set(value) = prefs.edit().putBoolean("sample_data_seeded", value).apply()

    fun clearAll() = prefs.edit().clear().apply()

    companion object {
        const val DEFAULT_BACKEND = "https://leanora-android.onrender.com"
    }
}
