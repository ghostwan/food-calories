package com.ghostwan.snapcal

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ghostwan.snapcal.data.local.AppDatabase
import com.ghostwan.snapcal.data.local.HealthConnectManager
import com.ghostwan.snapcal.data.local.MealReminderManager
import com.ghostwan.snapcal.data.mapper.FoodAnalysisMapper
import com.ghostwan.snapcal.data.remote.DriveBackupManager
import com.ghostwan.snapcal.data.remote.GeminiApiService
import com.ghostwan.snapcal.data.remote.GoogleAuthManager
import com.ghostwan.snapcal.data.remote.OpenFoodFactsService
import com.ghostwan.snapcal.data.repository.FoodAnalysisRepositoryImpl
import com.ghostwan.snapcal.data.repository.MealRepositoryImpl
import com.ghostwan.snapcal.data.repository.SettingsRepositoryImpl
import com.ghostwan.snapcal.data.repository.DailyNoteRepositoryImpl
import com.ghostwan.snapcal.data.repository.ShoppingRepositoryImpl
import com.ghostwan.snapcal.data.repository.UsageRepositoryImpl
import com.ghostwan.snapcal.data.repository.UserProfileRepositoryImpl
import com.ghostwan.snapcal.domain.repository.MealRepository
import com.ghostwan.snapcal.domain.repository.SettingsRepository
import com.ghostwan.snapcal.domain.repository.DailyNoteRepository
import com.ghostwan.snapcal.domain.repository.ShoppingRepository
import com.ghostwan.snapcal.domain.repository.UsageRepository
import com.ghostwan.snapcal.domain.repository.UserProfileRepository
import com.ghostwan.snapcal.domain.usecase.AnalyzeFoodUseCase
import com.ghostwan.snapcal.domain.usecase.ComputeNutritionGoalUseCase
import com.ghostwan.snapcal.domain.usecase.CorrectAnalysisUseCase
import com.ghostwan.snapcal.domain.usecase.GetDailyNutritionUseCase
import com.ghostwan.snapcal.domain.usecase.GetNutritionHistoryUseCase
import com.ghostwan.snapcal.data.remote.DailyBackupWorker
import com.ghostwan.snapcal.domain.usecase.SaveMealUseCase
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SnapCalApp : Application() {

    lateinit var analyzeFoodUseCase: AnalyzeFoodUseCase
        private set
    lateinit var correctAnalysisUseCase: CorrectAnalysisUseCase
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var usageRepository: UsageRepository
        private set
    lateinit var saveMealUseCase: SaveMealUseCase
        private set
    lateinit var getDailyNutritionUseCase: GetDailyNutritionUseCase
        private set
    lateinit var getNutritionHistoryUseCase: GetNutritionHistoryUseCase
        private set
    lateinit var computeNutritionGoalUseCase: ComputeNutritionGoalUseCase
        private set
    lateinit var userProfileRepository: UserProfileRepository
        private set
    lateinit var mealRepository: MealRepository
        private set
    lateinit var healthConnectManager: HealthConnectManager
        private set
    lateinit var googleAuthManager: GoogleAuthManager
        private set
    lateinit var driveBackupManager: DriveBackupManager
        private set
    lateinit var mealReminderManager: MealReminderManager
        private set
    lateinit var shoppingRepository: ShoppingRepository
        private set
    lateinit var dailyNoteRepository: DailyNoteRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val apiService = GeminiApiService()
        val mapper = FoodAnalysisMapper()
        val settingsRepo = SettingsRepositoryImpl(this)
        settingsRepository = settingsRepo
        usageRepository = UsageRepositoryImpl(this)

        val database = AppDatabase.getInstance(this)
        val mealDao = database.mealDao()
        val weightDao = database.weightDao()
        val mealRepo = MealRepositoryImpl(mealDao)
        mealRepository = mealRepo
        shoppingRepository = ShoppingRepositoryImpl(database.shoppingItemDao())
        dailyNoteRepository = DailyNoteRepositoryImpl(database.dailyNoteDao())

        val userProfileRepo = UserProfileRepositoryImpl(this, weightDao)
        userProfileRepository = userProfileRepo

        healthConnectManager = HealthConnectManager(this)
        googleAuthManager = GoogleAuthManager(this)
        driveBackupManager = DriveBackupManager(this)

        val openFoodFactsService = OpenFoodFactsService()
        val foodAnalysisRepository = FoodAnalysisRepositoryImpl(apiService, settingsRepo, mapper, openFoodFactsService)
        analyzeFoodUseCase = AnalyzeFoodUseCase(foodAnalysisRepository)
        correctAnalysisUseCase = CorrectAnalysisUseCase(foodAnalysisRepository)
        saveMealUseCase = SaveMealUseCase(mealRepo, this)
        getDailyNutritionUseCase = GetDailyNutritionUseCase(mealRepo)
        getNutritionHistoryUseCase = GetNutritionHistoryUseCase(mealRepo)
        computeNutritionGoalUseCase = ComputeNutritionGoalUseCase(apiService, settingsRepo)

        mealReminderManager = MealReminderManager(this)
        mealReminderManager.createNotificationChannel()

        scheduleBackup()
    }

    fun scheduleBackup() {
        val frequencyDays = settingsRepository.getBackupFrequencyDays()

        if (frequencyDays <= 0) {
            WorkManager.getInstance(this).cancelUniqueWork("daily_drive_backup")
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelay = target.timeInMillis - now.timeInMillis

        val backupRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(
            frequencyDays.toLong(), TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_drive_backup",
            ExistingPeriodicWorkPolicy.UPDATE,
            backupRequest
        )
    }
}
