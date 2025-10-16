package com.example.cleanlake.Model

data class RiwayatModel(
    val timestamp: String? = null,
    val pH: Double? = null,
    val pH_min: Double? = null,
    val pH_max: Double? = null,
    val Suhu: Double? = null,
    val Suhu_min: Double? = null,
    val Suhu_max: Double? = null,
    val TDS: Double? = null,
    val TDS_min: Double? = null,
    val TDS_max: Double? = null,
    val Kekeruhan: Double? = null,
    val Kekeruhan_min: Double? = null,
    val Kekeruhan_max: Double? = null,
    val status: String? = null,
    val deskripsi: String? = null
)
