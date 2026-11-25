package com.leanora.app

data class Food(val name: String, val kcal: Int, val protein: Int, val carbs: Int, val fat: Int, val meal: String)

val MEALS = listOf("Breakfast", "Lunch", "Dinner", "Snack")
val FITNESS_GOALS = listOf("Lose weight", "Lose body fat", "Gain weight", "Build muscle", "Maintain weight")
val ACTIVITY_LEVELS = listOf("Sedentary", "Lightly active", "Moderately active", "Very active")

internal val CATALOG = listOf(
    Food("Idli (2)", 130, 4, 26, 1, "Breakfast"),
    Food("Plain dosa", 170, 4, 30, 4, "Breakfast"),
    Food("Masala dosa", 250, 6, 38, 9, "Breakfast"),
    Food("Upma (1 cup)", 230, 5, 35, 8, "Breakfast"),
    Food("Poha (1 cup)", 250, 5, 40, 8, "Breakfast"),
    Food("Pongal (1 cup)", 240, 6, 38, 7, "Breakfast"),
    Food("Oats porridge with milk (1 bowl)", 190, 8, 30, 5, "Breakfast"),
    Food("Plain paratha", 200, 4, 28, 8, "Breakfast"),
    Food("Bread slice", 80, 3, 14, 1, "Breakfast"),
    Food("Chapati", 120, 3, 22, 3, "Lunch"),
    Food("Cooked rice (150 g)", 195, 4, 42, 0, "Lunch"),
    Food("Brown rice (150 g)", 165, 4, 35, 1, "Lunch"),
    Food("Sambar (1 cup)", 120, 6, 18, 3, "Lunch"),
    Food("Dal tadka (1 cup)", 180, 9, 24, 5, "Lunch"),
    Food("Rajma curry (1 cup)", 210, 10, 32, 4, "Lunch"),
    Food("Chana masala (1 cup)", 250, 11, 36, 7, "Lunch"),
    Food("Chicken curry (1 cup)", 240, 22, 6, 14, "Lunch"),
    Food("Fish curry (1 cup)", 200, 20, 6, 11, "Lunch"),
    Food("Vegetable biryani (1 cup)", 290, 6, 48, 8, "Lunch"),
    Food("Chicken biryani (1 cup)", 350, 18, 40, 13, "Lunch"),
    Food("Curd rice (1 cup)", 230, 6, 34, 7, "Lunch"),
    Food("Lemon rice (1 cup)", 260, 5, 44, 7, "Lunch"),
    Food("Chicken breast (100 g)", 165, 31, 0, 4, "Lunch"),
    Food("Paneer (100 g)", 265, 18, 4, 20, "Dinner"),
    Food("Palak paneer (1 cup)", 280, 14, 10, 20, "Dinner"),
    Food("Grilled fish (100 g)", 150, 25, 0, 5, "Dinner"),
    Food("Vegetable salad (1 bowl)", 80, 3, 12, 2, "Dinner"),
    Food("Curd (100 g)", 61, 3, 5, 3, "Dinner"),
    Food("Boiled egg", 78, 6, 1, 5, "Snack"),
    Food("Banana", 105, 1, 27, 0, "Snack"),
    Food("Apple", 95, 0, 25, 0, "Snack"),
    Food("Orange", 62, 1, 15, 0, "Snack"),
    Food("Mango (1 cup)", 100, 1, 25, 1, "Snack"),
    Food("Almonds (10)", 70, 3, 3, 6, "Snack"),
    Food("Roasted peanuts (30 g)", 175, 8, 5, 15, "Snack"),
    Food("Roasted chana (30 g)", 110, 6, 18, 2, "Snack"),
    Food("Sundal (1 cup)", 190, 10, 28, 4, "Snack"),
    Food("Medu vada (1)", 150, 5, 14, 9, "Snack"),
    Food("Samosa (1)", 260, 4, 24, 17, "Snack"),
    Food("Buttermilk (1 glass)", 40, 2, 4, 1, "Snack"),
    Food("Milk (1 glass, 250 ml)", 150, 8, 12, 8, "Snack"),
    Food("Filter coffee with milk and sugar", 80, 3, 11, 3, "Snack"),
    Food("Masala chai (1 cup)", 90, 2, 14, 3, "Snack"),
    Food("Whey protein (1 scoop)", 120, 24, 3, 1, "Snack"),
)
