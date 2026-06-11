package com.alien.models

import kotlinx.serialization.Serializable

@Serializable
data class UploadProductRequest(
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val supplierId: String,
    val isAuction: Boolean,
    val durationHours: Int
)
