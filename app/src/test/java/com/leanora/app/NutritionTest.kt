package com.leanora.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NutritionTest {
    private val adult = LeanoraProfile("Asha", 30, "Female", 165, 70f, 62f, "Lightly active", "Lose weight")

    @Test
    fun calorieTargetAppliesDeficitAndFloor() {
        val maintenance = Nutrition.maintenanceCalories(adult).toInt()
        assertEquals(maintenance - 400, Nutrition.calorieTarget(adult))
        val tiny = adult.copy(weightKg = 40f, heightCm = 150, age = 60, activity = "Sedentary")
        assertEquals(1200, Nutrition.calorieTarget(tiny))
        assertEquals(1500, Nutrition.calorieTarget(tiny.copy(sex = "Male")))
    }

    @Test
    fun minorsNeverGetADeficit() {
        val teen = adult.copy(age = 15, goal = "Lose weight")
        assertEquals(Nutrition.maintenanceCalories(teen).toInt().coerceAtLeast(1200), Nutrition.calorieTarget(teen))
        assertNotNull(Nutrition.validateProfile(teen))
        assertNull(Nutrition.validateProfile(teen.copy(goal = Nutrition.MAINTAIN, targetKg = 60f)))
    }

    @Test
    fun profileValidationCatchesUnsafeOrInconsistentGoals() {
        assertNull(Nutrition.validateProfile(adult))
        assertNotNull(Nutrition.validateProfile(adult.copy(name = " ")))
        assertNotNull(Nutrition.validateProfile(adult.copy(age = 12)))
        assertNotNull(Nutrition.validateProfile(adult.copy(targetKg = 45f)))
        assertNotNull(Nutrition.validateProfile(adult.copy(targetKg = 75f)))
        assertNotNull(Nutrition.validateProfile(adult.copy(goal = "Gain weight", targetKg = 65f)))
        assertNull(Nutrition.validateProfile(adult.copy(goal = "Gain weight", targetKg = 75f)))
        assertNull(Nutrition.validateProfile(adult.copy(goal = Nutrition.MAINTAIN, targetKg = 70f)))
    }

    @Test
    fun reachingTheTargetDoesNotMakeTheProfileUnusable() {
        val reached = adult.copy(weightKg = 61f)
        assertNull(Nutrition.validateSafety(reached))
        assertNotNull(Nutrition.validateGoalConsistency(reached))
        assertNotNull(Nutrition.validateSafety(adult.copy(age = 15)))
    }

    @Test
    fun manualGoalRespectsSexSpecificFloor() {
        assertNotNull(Nutrition.validateCalorieGoal(1000, "Female"))
        assertNull(Nutrition.validateCalorieGoal(1200, "Female"))
        assertNotNull(Nutrition.validateCalorieGoal(1200, "Male"))
        assertNotNull(Nutrition.validateCalorieGoal(null, "Male"))
        assertNotNull(Nutrition.validateCalorieGoal(7000, "Male"))
    }

    @Test
    fun macroTargetsAddUpAroundTheGoal() {
        val t = Nutrition.macroTargets(2000, 70f, "Lose weight")
        assertEquals(112, t.proteinG)
        val kcal = t.proteinG * 4 + t.carbsG * 4 + t.fatG * 9
        assertTrue(kotlin.math.abs(kcal - 2000) < 30)
    }

    @Test
    fun scalingRoundsAndNamesPortions() {
        val idli = Food("Idli (2)", 130, 4, 26, 1, "Breakfast")
        assertEquals(idli, Nutrition.scale(idli, 1f))
        val one5 = Nutrition.scale(idli, 1.5f)
        assertEquals("Idli (2) × 1.5", one5.name)
        assertEquals(195, one5.kcal)
        assertEquals(6, one5.protein)
        assertEquals("Idli (2) × 2", Nutrition.scale(idli, 2f).name)
        val per100 = Food("Oats", 380, 13, 67, 7, "Snack")
        assertEquals(190, Nutrition.scalePer100g(per100, 50).kcal)
    }

    @Test
    fun parsesDecimalCommasAndRejectsGarbage() {
        assertEquals(72.5f, Nutrition.parseDecimal("72,5")!!, 0.001f)
        assertEquals(72f, Nutrition.parseDecimal(" 72 ")!!, 0.001f)
        assertNull(Nutrition.parseDecimal("abc"))
        assertNull(Nutrition.parseDecimal("NaN"))
    }

    @Test
    fun mealForHourCoversTheDay() {
        assertEquals("Breakfast", Nutrition.mealForHour(8))
        assertEquals("Lunch", Nutrition.mealForHour(13))
        assertEquals("Snack", Nutrition.mealForHour(16))
        assertEquals("Dinner", Nutrition.mealForHour(20))
        assertEquals("Snack", Nutrition.mealForHour(2))
        assertEquals("Snack", Nutrition.sanitizeMeal("Meal"))
    }

    @Test
    fun streakCountsBackFromTodayOrYesterday() {
        val today = LocalDate.of(2026, 3, 10)
        val logged = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3), today.minusDays(5))
        assertEquals(3, Nutrition.streak(today) { it in logged })
        assertEquals(4, Nutrition.streak(today) { it in logged + today })
        assertEquals(0, Nutrition.streak(today) { false })
    }

    @Test
    fun weightGoalSummaryShowsDirection() {
        assertEquals("6.0 kg to lose", Nutrition.weightGoalSummary(78f, 72f))
        assertEquals("2.0 kg to gain", Nutrition.weightGoalSummary(58f, 60f))
        assertFalse(Nutrition.weightGoalSummary(60f, 60f).contains("kg to"))
    }

    @Test
    fun catalogIsUsableAndHasValidMeals() {
        assertTrue(CATALOG.size >= 40)
        assertTrue(CATALOG.all { it.meal in MEALS && it.kcal > 0 })
        assertEquals(CATALOG.size, CATALOG.map { it.name }.toSet().size)
    }
}
