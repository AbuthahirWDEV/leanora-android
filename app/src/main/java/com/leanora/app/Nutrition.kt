package com.leanora.app

import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

internal data class MacroTargets(val proteinG: Int, val carbsG: Int, val fatG: Int)

internal object Nutrition {
    const val MIN_AGE = 13
    const val ADULT_AGE = 18
    const val MIN_TARGET_BMI = 18.5f
    const val MAINTAIN = "Maintain weight"

    fun minCalories(sex: String): Int = if (sex == "Female") 1200 else 1500

    fun bmi(weightKg: Float, heightCm: Int): Float {
        val meters = heightCm / 100f
        return weightKg / (meters * meters)
    }

    fun maintenanceCalories(p: LeanoraProfile): Double {
        val base = 10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age
        val bmr = if (p.sex == "Male") base + 5 else base - 161
        val factor = when (p.activity) {
            "Sedentary" -> 1.2
            "Moderately active" -> 1.55
            "Very active" -> 1.725
            else -> 1.375
        }
        return bmr * factor
    }

    /** Under-18 users always get a maintenance target; deficits and surpluses are for adults only. */
    fun calorieTarget(p: LeanoraProfile): Int {
        val maintenance = maintenanceCalories(p)
        val adjusted = if (p.age < ADULT_AGE) {
            maintenance
        } else {
            when (p.goal) {
                "Lose weight", "Lose body fat" -> maintenance - 400
                "Gain weight", "Build muscle" -> maintenance + 250
                else -> maintenance
            }
        }
        return adjusted.toInt().coerceIn(minCalories(p.sex), 4500)
    }

    fun validateProfile(p: LeanoraProfile): String? = validateSafety(p) ?: validateGoalConsistency(p)

    /** Hard limits that must always hold; a profile failing these must be corrected before the app is usable. */
    fun validateSafety(p: LeanoraProfile): String? {
        if (p.name.isBlank()) return "Enter your name."
        if (p.age !in MIN_AGE..100) return "Age must be between $MIN_AGE and 100."
        if (p.heightCm !in 100..250) return "Height must be between 100 and 250 cm."
        if (p.weightKg !in 30f..300f) return "Current weight must be between 30 and 300 kg."
        if (p.targetKg !in 30f..300f) return "Target weight must be between 30 and 300 kg."
        if (p.age < ADULT_AGE && p.goal != MAINTAIN) {
            return "Under 18: weight-change goals aren't offered. Choose \"Maintain weight\" and talk to a doctor or dietitian."
        }
        if (bmi(p.targetKg, p.heightCm) < MIN_TARGET_BMI) {
            return "That target weight is below a healthy BMI (18.5) for your height. Please choose a higher target."
        }
        return null
    }

    /** Goal and target must point the same way; reaching or passing a target later doesn't lock the app. */
    fun validateGoalConsistency(p: LeanoraProfile): String? = when (p.goal) {
        "Lose weight" -> if (p.targetKg < p.weightKg) null else "For \"Lose weight\", the target must be lower than your current weight."
        "Lose body fat" -> if (p.targetKg <= p.weightKg) null else "For \"Lose body fat\", the target can't be above your current weight."
        "Gain weight" -> if (p.targetKg > p.weightKg) null else "For \"Gain weight\", the target must be higher than your current weight."
        "Build muscle" -> if (p.targetKg >= p.weightKg) null else "For \"Build muscle\", the target can't be below your current weight."
        else -> null
    }

    fun validateCalorieGoal(kcal: Int?, sex: String): String? {
        val min = minCalories(sex)
        return if (kcal == null || kcal !in min..6000) "Enter a goal between $min and 6000 kcal. Lower goals should only be set with a clinician." else null
    }

    fun macroTargets(goalKcal: Int, weightKg: Float, fitnessGoal: String): MacroTargets {
        val proteinPerKg = when (fitnessGoal) {
            "Build muscle", "Gain weight" -> 1.8f
            "Lose weight", "Lose body fat" -> 1.6f
            else -> 1.2f
        }
        val protein = minOf((weightKg * proteinPerKg).roundToInt(), (goalKcal * 0.35f / 4).roundToInt())
        val fat = (goalKcal * 0.27f / 9).roundToInt()
        val carbs = ((goalKcal - protein * 4 - fat * 9) / 4).coerceAtLeast(0)
        return MacroTargets(protein, carbs, fat)
    }

    fun scale(food: Food, multiplier: Float): Food = Food(
        name = if (multiplier == 1f) food.name else "${food.name} × ${formatQuantity(multiplier)}",
        kcal = (food.kcal * multiplier).roundToInt(),
        protein = (food.protein * multiplier).roundToInt(),
        carbs = (food.carbs * multiplier).roundToInt(),
        fat = (food.fat * multiplier).roundToInt(),
        meal = food.meal,
    )

    fun scalePer100g(per100: Food, grams: Int): Food {
        val factor = grams / 100f
        return per100.copy(
            kcal = (per100.kcal * factor).roundToInt(),
            protein = (per100.protein * factor).roundToInt(),
            carbs = (per100.carbs * factor).roundToInt(),
            fat = (per100.fat * factor).roundToInt(),
        )
    }

    fun mealForHour(hour: Int): String = when (hour) {
        in 5..10 -> "Breakfast"
        in 11..14 -> "Lunch"
        in 19..22 -> "Dinner"
        else -> "Snack"
    }

    fun parseDecimal(text: String): Float? =
        text.trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }

    fun formatQuantity(value: Float): String =
        if (value == value.toInt().toFloat()) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

    fun sanitizeMeal(meal: String?): String = if (meal in MEALS) meal!! else "Snack"

    /** Consecutive logged days ending today (or yesterday when today has no entries yet). */
    fun streak(today: LocalDate, hasEntries: (LocalDate) -> Boolean): Int {
        var day = if (hasEntries(today)) today else today.minusDays(1)
        var count = 0
        while (count < 3650 && hasEntries(day)) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun weightGoalSummary(weightKg: Float, targetKg: Float): String {
        val diff = weightKg - targetKg
        return when {
            kotlin.math.abs(diff) < 0.05f -> "You're at your target weight"
            diff > 0 -> String.format(Locale.US, "%.1f kg to lose", diff)
            else -> String.format(Locale.US, "%.1f kg to gain", -diff)
        }
    }
}
