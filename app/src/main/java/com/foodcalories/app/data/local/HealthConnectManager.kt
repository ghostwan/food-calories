package com.foodcalories.app.data.local

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(WeightRecord::class)
    )

    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return requiredPermissions.all { it in granted }
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
