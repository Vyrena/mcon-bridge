package com.vyrena.mconbridge.domain

sealed interface LaunchResult {
    data object Started : LaunchResult
    data class Error(val message: String) : LaunchResult
}
