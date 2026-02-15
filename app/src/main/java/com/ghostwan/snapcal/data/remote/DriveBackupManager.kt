package com.ghostwan.snapcal.data.remote

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.domain.model.NutritionGoal
import com.ghostwan.snapcal.domain.model.UserProfile
import com.ghostwan.snapcal.domain.model.WeightRecord

private const val BACKUP_FILE_NAME = "food_calories_backup.json"
private const val BACKUP_VERSION = 1

class DriveBackupManager(private val context: Context) {

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("SnapCal")
            .build()
    }

    suspend fun backup(
        profile: UserProfile,
        goal: NutritionGoal,
        meals: List<MealEntry>,
        weightRecords: List<WeightRecord>
    ): Boolean = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext false

        val json = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("profile", profileToJson(profile))
            put("goal", goalToJson(goal))
            put("meals", mealsToJson(meals))
            put("weightRecords", weightRecordsToJson(weightRecords))
        }

        val content = ByteArrayContent.fromString("application/json", json.toString())

        // Check if backup file already exists
        val existingFileId = findBackupFile(drive)

        if (existingFileId != null) {
            drive.files().update(existingFileId, null, content).execute()
        } else {
            val fileMetadata = File().apply {
                name = BACKUP_FILE_NAME
                parents = listOf("appDataFolder")
            }
            drive.files().create(fileMetadata, content)
                .setFields("id")
                .execute()
        }
        true
    }

    suspend fun restore(): BackupData? = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext null
        val fileId = findBackupFile(drive) ?: return@withContext null

        val outputStream = java.io.ByteArrayOutputStream()
        drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        val jsonString = outputStream.toString("UTF-8")

        try {
            val json = JSONObject(jsonString)
            BackupData(
                profile = jsonToProfile(json.getJSONObject("profile")),
                goal = jsonToGoal(json.getJSONObject("goal")),
                meals = jsonToMeals(json.getJSONArray("meals")),
                weightRecords = jsonToWeightRecords(json.optJSONArray("weightRecords") ?: JSONArray())
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun findBackupFile(drive: Drive): String? {
        val result = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$BACKUP_FILE_NAME'")
            .setFields("files(id)")
            .execute()
        return result.files?.firstOrNull()?.id
    }

    private fun profileToJson(profile: UserProfile) = JSONObject().apply {
        put("height", profile.height)
        put("weight", profile.weight.toDouble())
        put("targetWeight", profile.targetWeight.toDouble())
        put("age", profile.age)
        put("gender", profile.gender.name)
        put("activityLevel", profile.activityLevel.name)
    }

    private fun goalToJson(goal: NutritionGoal) = JSONObject().apply {
        put("calories", goal.calories)
        put("proteins", goal.proteins.toDouble())
        put("carbs", goal.carbs.toDouble())
        put("fats", goal.fats.toDouble())
        put("fiber", goal.fiber.toDouble())
        put("explanation", goal.explanation ?: JSONObject.NULL)
    }

    private fun mealsToJson(meals: List<MealEntry>) = JSONArray().apply {
        meals.forEach { meal ->
            put(JSONObject().apply {
                put("dishName", meal.dishName)
                put("calories", meal.calories)
                put("proteins", meal.proteins.toDouble())
                put("carbs", meal.carbs.toDouble())
                put("fats", meal.fats.toDouble())
                put("fiber", meal.fiber.toDouble())
                put("date", meal.date)
                put("ingredientsJson", meal.ingredientsJson)
            })
        }
    }

    private fun weightRecordsToJson(records: List<WeightRecord>) = JSONArray().apply {
        records.forEach { record ->
            put(JSONObject().apply {
                put("weight", record.weight.toDouble())
                put("date", record.date)
            })
        }
    }

    private fun jsonToProfile(json: JSONObject) = UserProfile(
        height = json.getInt("height"),
        weight = json.getDouble("weight").toFloat(),
        targetWeight = json.getDouble("targetWeight").toFloat(),
        age = json.getInt("age"),
        gender = com.ghostwan.snapcal.domain.model.Gender.valueOf(json.getString("gender")),
        activityLevel = com.ghostwan.snapcal.domain.model.ActivityLevel.valueOf(json.getString("activityLevel"))
    )

    private fun jsonToGoal(json: JSONObject) = NutritionGoal(
        calories = json.getInt("calories"),
        proteins = json.getDouble("proteins").toFloat(),
        carbs = json.getDouble("carbs").toFloat(),
        fats = json.getDouble("fats").toFloat(),
        fiber = json.getDouble("fiber").toFloat(),
        explanation = if (json.isNull("explanation")) null else json.getString("explanation")
    )

    private fun jsonToMeals(json: JSONArray): List<MealEntry> {
        val list = mutableListOf<MealEntry>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            list.add(
                MealEntry(
                    dishName = obj.getString("dishName"),
                    calories = obj.getInt("calories"),
                    proteins = obj.getDouble("proteins").toFloat(),
                    carbs = obj.getDouble("carbs").toFloat(),
                    fats = obj.getDouble("fats").toFloat(),
                    fiber = obj.getDouble("fiber").toFloat(),
                    date = obj.getString("date"),
                    ingredientsJson = obj.optString("ingredientsJson", "")
                )
            )
        }
        return list
    }

    private fun jsonToWeightRecords(json: JSONArray): List<WeightRecord> {
        val list = mutableListOf<WeightRecord>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            list.add(
                WeightRecord(
                    weight = obj.getDouble("weight").toFloat(),
                    date = obj.getString("date")
                )
            )
        }
        return list
    }
}

data class BackupData(
    val profile: UserProfile,
    val goal: NutritionGoal,
    val meals: List<MealEntry>,
    val weightRecords: List<WeightRecord>
)
