package com.mymagicalpet.server

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val simVersion: Int,
)
