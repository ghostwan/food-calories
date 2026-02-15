package com.ghostwan.snapcal.data.remote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ghostwan.snapcal.SnapCalApp

class DailyBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as SnapCalApp
        val authManager = app.googleAuthManager

        if (!authManager.isSignedIn()) return Result.success()

        return try {
            val meals = app.mealRepository.getAllMeals()
            val profile = app.userProfileRepository.getProfile()
            val goal = app.userProfileRepository.getGoal()
            val weightRecords = app.userProfileRepository.getWeightHistory(9999)

            val success = app.driveBackupManager.backup(
                profile = profile,
                goal = goal,
                meals = meals,
                weightRecords = weightRecords
            )

            if (success) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
