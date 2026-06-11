package com.alien.models

import kotlinx.serialization.Serializable

@Serializable
data class BidRequest(
    val amount: Double,
    val bidderName: String
)
