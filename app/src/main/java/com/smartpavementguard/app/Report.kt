package com.smartpavementguard.app

import kotlinx.serialization.Serializable

@Serializable
data class Report(

    val id: Int? = null,

    val type: String,

    val description: String,

    val latitude: Double,

    val longitude: Double,

    val impact: Float? = null,

    val speed: Int? = null,

    val priority: Int,

    val status: String,

    val confirmations: Int? = 1,

    val verified: Boolean? = false,

    val category: String? = "bache",

    val municipality_status: String? = "reportado",

    val repaired_at: String? = null
)