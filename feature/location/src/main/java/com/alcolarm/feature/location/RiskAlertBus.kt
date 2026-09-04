package com.alcolarm.feature.location

import com.alcolarm.core.model.RiskPlaceId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class RiskAlertEvent(
    val riskPlaceId: RiskPlaceId,
    val placeName: String,
    val simulated: Boolean = false,
)

/**
 * In-process alert trigger (SharedFlow). Collected by NavHost.
 * Not persisted. Dismiss signals reset Home debounce without storing location.
 */
@Singleton
class RiskAlertBus @Inject constructor() {
    private val _events = MutableSharedFlow<RiskAlertEvent>(
        extraBufferCapacity = 1,
    )
    val events: SharedFlow<RiskAlertEvent> = _events.asSharedFlow()

    private val _dismissed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val dismissed: SharedFlow<Unit> = _dismissed.asSharedFlow()

    suspend fun emit(event: RiskAlertEvent) {
        _events.emit(event)
    }

    fun tryEmit(event: RiskAlertEvent): Boolean = _events.tryEmit(event)

    fun tryEmitDismissed(): Boolean = _dismissed.tryEmit(Unit)
}
