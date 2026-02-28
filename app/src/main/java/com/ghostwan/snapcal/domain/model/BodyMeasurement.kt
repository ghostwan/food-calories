package com.ghostwan.snapcal.domain.model

data class BodyMeasurement(
    val id: Long = 0,
    val waist: Float? = null,
    val hips: Float? = null,
    val chest: Float? = null,
    val arms: Float? = null,
    val thighs: Float? = null,
    val date: String = ""
)
