package com.septaalfauzan.saku.ui.state

sealed interface StateUi<out T> {
    object Idle : StateUi<Nothing>
    object Loading : StateUi<Nothing>
    data class Success<out T>(val data: T) : StateUi<T>
    data class Error(val message: String) : StateUi<Nothing>
}