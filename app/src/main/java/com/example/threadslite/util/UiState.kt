package com.example.threadslite.util

/**
 * Sealed class representing UI loading states throughout the app.
 * Every screen/operation maps its async result to one of these states.
 */
sealed class UiState<out T> {
    /** Initial state — no action has been triggered yet */
    object Idle : UiState<Nothing>()

    /** Operation is in progress — show a loading indicator */
    object Loading : UiState<Nothing>()

    /**
     * Operation completed successfully.
     * [data] is optional — some operations (e.g. delete, logout) don't return a value.
     */
    data class Success<T>(val data: T? = null) : UiState<T>()

    /** Operation failed — [message] contains a user-facing error description */
    data class Error(val message: String) : UiState<Nothing>()
}
