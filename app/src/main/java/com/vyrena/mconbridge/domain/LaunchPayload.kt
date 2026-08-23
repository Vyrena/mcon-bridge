package com.vyrena.mconbridge.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface LaunchPayload

@Serializable
@SerialName("azahar")
data class AzaharPayload(
    val titleId: String,
    val productCode: String? = null,
    val region: String? = null,
    val gameUri: String? = null,
    val filename: String? = null,
    val fileType: String? = null,
) : LaunchPayload

@Serializable
@SerialName("artemis")
data class ArtemisPayload(
    val hostUuid: String,
    val hostName: String? = null,
    val appUuid: String? = null,
    val appName: String? = null,
    val appId: String? = null,
) : LaunchPayload

@Serializable
@SerialName("kirin")
data class KirinPayload(
    val gamePath: String,
    val selectedRoot: String? = null,
) : LaunchPayload

object LaunchPayloadCodec {
    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(payload: LaunchPayload): String = json.encodeToString(LaunchPayload.serializer(), payload)

    fun decode(value: String): LaunchPayload = json.decodeFromString(LaunchPayload.serializer(), value)
}
