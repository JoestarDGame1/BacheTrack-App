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

    val repaired_at: String? = null,

    val image_url: String? = null,

    // ==========================================
    // ANÁLISIS DE IA
    // ==========================================

    // PEQUENO / MEDIANO / GRANDE / MUY_GRANDE
    val apparent_size: String? = null,

    // 1 - 4
    val size_points: Int? = null,

    // LEVE / MODERADA / SEVERA / CRITICA
    val visual_severity: String? = null,

    // 1 - 4
    val severity_points: Int? = null,

    // Score utilizado por el análisis visual
    val visual_score: Int? = null,

    // Métricas visuales
    val visual_contrast: Double? = null,

    val edge_density: Double? = null,

    val dark_area: Double? = null
)