package com.vyrena.mconbridge.launch

import android.content.Context
import com.vyrena.mconbridge.domain.LaunchPayload
import com.vyrena.mconbridge.domain.LaunchResult

interface LaunchAdapter {
    fun supports(payload: LaunchPayload): Boolean
    fun validate(payload: LaunchPayload): String?
    fun launch(context: Context, gameId: String, payload: LaunchPayload): LaunchResult
}
