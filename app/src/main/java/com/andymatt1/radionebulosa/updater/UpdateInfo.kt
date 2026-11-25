package com.andymatt1.radionebulosa.updater

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Estructura del archivo remoto `version.json`. */
@Serializable
data class UpdateInfo(
    @SerialName("latestVersion") val latestVersion: String = "",
    @SerialName("latestVersionCode") val latestVersionCode: Int = 0,
    @SerialName("sha256") val sha256: String = "",
    @SerialName("apkUrl") val apkUrl: String = "",
    @SerialName("changelog") val changelog: String = ""
)
