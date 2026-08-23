// Copyright Citra Emulator Project / Azahar Emulator Project
// Licensed under GPLv2 or any later version.
//
// Compatibility mirror of ordinary Azahar's public intent Parcelable. It is deliberately kept
// in Azahar's namespace so Android can deserialize it with Azahar's own Game class after the
// cross-application Intent is delivered.
package org.citra.citra_emu.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Game(
    val valid: Boolean = false,
    val title: String = "",
    val description: String = "",
    val path: String = "",
    val titleId: Long = 0L,
    val mediaType: MediaType = MediaType.GAME_CARD,
    val company: String = "",
    val regions: String = "",
    val isInstalled: Boolean = false,
    val isSystemTitle: Boolean = false,
    val isVisibleSystemTitle: Boolean = false,
    val isInsertable: Boolean = false,
    val icon: IntArray? = null,
    val fileType: String = "",
    val isCompressed: Boolean = false,
    val filename: String,
) : Parcelable {
    enum class MediaType(val value: Int) {
        NAND(0),
        SDMC(1),
        GAME_CARD(2),
    }
}
