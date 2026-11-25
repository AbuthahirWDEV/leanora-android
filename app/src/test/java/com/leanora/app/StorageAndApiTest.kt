package com.leanora.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StorageAndApiTest {
    @Test
    fun foodCodecRoundTripsAndSurvivesCorruption() {
        val foods = listOf(Food("Idli", 130, 4, 26, 1, "Breakfast"), Food("Tea", 40, 1, 6, 1, "Snack"))
        assertEquals(foods, FoodCodec.decode(FoodCodec.encode(foods)))
        assertTrue(FoodCodec.decode("not json").isEmpty())
        assertTrue(FoodCodec.decode(null).isEmpty())
        val mixed = """[{"name":"Good","kcal":10,"protein":1,"carbs":1,"fat":1,"meal":"Meal"},{"name":"Bad"}]"""
        val decoded = FoodCodec.decode(mixed)
        assertEquals(1, decoded.size)
        assertEquals("Snack", decoded[0].meal)
    }

    @Test
    fun weightCodecSortsAndUpsertsPerDay() {
        val d1 = LocalDate.of(2026, 1, 1)
        val d2 = LocalDate.of(2026, 1, 2)
        val entries = WeightCodec.upsert(WeightCodec.upsert(emptyList(), WeightEntry(d2, 70f)), WeightEntry(d1, 71f))
        assertEquals(listOf(d1, d2), entries.map { it.date })
        val replaced = WeightCodec.upsert(entries, WeightEntry(d2, 69.5f))
        assertEquals(2, replaced.size)
        assertEquals(69.5f, replaced.last().kg, 0.001f)
        assertEquals(replaced, WeightCodec.decode(WeightCodec.encode(replaced)))
    }

    @Test
    fun backendUrlMustBeHttps() {
        assertEquals("https://x.onrender.com", Api.normalizeBaseUrl("  https://x.onrender.com/ "))
        assertNull(Api.normalizeBaseUrl("http://x.onrender.com"))
        assertNull(Api.normalizeBaseUrl("https://"))
        assertNull(Api.normalizeBaseUrl("https://bad host"))
    }

    @Test
    fun textEstimateParsing() {
        val ok = Api.parseTextEstimate(
            JSONObject("""{"food_name":"Idli","portion":"2 pieces","calories_kcal":130,"protein_g":4,"carbs_g":26,"fat_g":1,"meal":"Breakfast"}"""), "2 idli",
        ) as ApiResult.Ok
        assertEquals("Idli (2 pieces)", ok.value.food.name)
        assertEquals("Breakfast", ok.value.food.meal)
        assertTrue(Api.parseTextEstimate(JSONObject("""{"calories_kcal":0}"""), "x") is ApiResult.Err)
    }

    @Test
    fun photoEstimateParsingKeepsConfidenceNotes() {
        val ok = Api.parsePhotoEstimate(
            JSONObject("""{"food_name":"Rice","portion_g":200,"calories_kcal":260,"confidence":"medium","assumptions":"plain rice"}"""), "Lunch",
        ) as ApiResult.Ok
        assertEquals("Rice (200 g)", ok.value.food.name)
        assertEquals("Lunch", ok.value.food.meal)
        assertTrue(ok.value.note.contains("medium") && ok.value.note.contains("plain rice"))
        assertTrue(Api.parsePhotoEstimate(JSONObject("""{"calories_kcal":-5}"""), "Lunch") is ApiResult.Err)
    }

    @Test
    fun productParsingHandlesMissingDataAndServingSize() {
        val product = Api.parseProduct(
            JSONObject("""{"product":{"product_name":"Oats","serving_quantity":"40","nutriments":{"energy-kcal_100g":380,"proteins_100g":13.4,"carbohydrates_100g":67,"fat_100g":7}}}"""), "123",
        ) as ApiResult.Ok
        assertEquals(380, product.value.per100.kcal)
        assertEquals(13, product.value.per100.protein)
        assertEquals(40, product.value.servingGrams)
        val kj = Api.parseProduct(JSONObject("""{"product":{"nutriments":{"energy_100g":1000}}}"""), "999") as ApiResult.Ok
        assertEquals(239, kj.value.per100.kcal)
        assertEquals("Scanned product 999", kj.value.name)
        assertTrue(Api.parseProduct(JSONObject("""{"product":{"nutriments":{}}}"""), "1") is ApiResult.Err)
        assertTrue(Api.parseProduct(JSONObject("""{"status":0}"""), "1") is ApiResult.Err)
    }

    @Test
    fun failureMessagesAreActionable() {
        assertTrue(Api.describeFailure(401, "").contains("token"))
        assertTrue(Api.describeFailure(429, "").contains("wait"))
        assertTrue(Api.describeFailure(503, "").contains("waking"))
        assertTrue(Api.describeFailure(500, """{"detail":"boom"}""").contains("boom"))
    }
}
