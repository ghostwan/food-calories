package com.ghostwan.snapcal.data.local

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return requiredPermissions.all { it in granted }
    }

    suspend fun readTodayCaloriesBurned(): Double {
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now().atStartOfDay(zone).toInstant()
        val now = Instant.now()

        val response = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
            )
        )
        val energy = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
        return energy?.inKilocalories ?: 0.0
    }

    suspend fun readCaloriesBurnedForDateRange(startDate: LocalDate, endDate: LocalDate): Map<String, Double> {
        val zone = ZoneId.systemDefault()
        val result = mutableMapOf<String, Double>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            val dayStart = current.atStartOfDay(zone).toInstant()
            val dayEnd = current.plusDays(1).atStartOfDay(zone).toInstant()
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd)
                )
            )
            val energy = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
            if (energy != null) {
                result[current.toString()] = energy.inKilocalories
            }
            current = current.plusDays(1)
        }
        return result
    }

    suspend fun readWeightRecords(days: Int = 90): List<WeightData> {
        val now = Instant.now()
        val start = now.minus(days.toLong(), ChronoUnit.DAYS)

        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, now)
        )

        val response = healthConnectClient.readRecords(request)
        return response.records.map { record ->
            WeightData(
                weightKg = record.weight.inKilograms.toFloat(),
                time = record.time
            )
        }
    }
}

data class WeightData(
    val weightKg: Float,
    val time: Instant
)
