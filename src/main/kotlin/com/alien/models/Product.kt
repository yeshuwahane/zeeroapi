package com.alien.models

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val supplierId: String,
    val isApproved: Boolean,
    val currentHighestBid: Double,
    val highestBidderName: String,
    val auctionEndTimeMillis: Long
)
