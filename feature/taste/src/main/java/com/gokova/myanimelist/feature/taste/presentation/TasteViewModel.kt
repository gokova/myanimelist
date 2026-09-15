package com.gokova.myanimelist.feature.taste.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.feature.taste.domain.model.TasteBubble
import com.gokova.myanimelist.feature.taste.domain.model.TasteType
import com.gokova.myanimelist.feature.taste.domain.usecase.ObserveTasteAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TasteViewModel
    @Inject
    constructor(
        observeTasteAnalyticsUseCase: ObserveTasteAnalyticsUseCase,
    ) : ViewModel() {
        private val selectedType = MutableStateFlow(TasteType.GENRE)
        private val selectedBubble = MutableStateFlow<TasteBubble?>(null)
        private val recenterTrigger = MutableStateFlow(0)

        val uiState: StateFlow<TasteUiState> =
            combine(
                observeTasteAnalyticsUseCase(),
                selectedType,
                selectedBubble,
                recenterTrigger,
            ) { analytics, type, bubble, recenter ->
                TasteUiState(
                    isLoading = false,
                    totalAnimeCount = analytics.totalAnimeCount,
                    selectedType = type,
                    genres = analytics.genres,
                    themes = analytics.themes,
                    selectedBubble = bubble,
                    recenterTrigger = recenter,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = TasteUiState(),
            )

        fun onTypeSelected(type: TasteType) {
            AppLog.ui.i { "User selected taste type: ${type.name}" }
            if (selectedType.value != type) {
                AppLog.viewModel.d { "Switching selectedType to ${type.name} and resetting bubble" }
                selectedType.value = type
                selectedBubble.value = null
                recenterTrigger.value++
            }
        }

        fun onBubbleSelected(bubble: TasteBubble) {
            AppLog.ui.i {
                "User selected bubble: '${bubble.name}' (count=${bubble.count})"
            }
            selectedBubble.value = bubble
        }

        fun onDismissBottomSheet() {
            AppLog.ui.i { "User dismissed taste bottom sheet" }
            selectedBubble.value = null
        }

        fun onRecenterClicked() {
            AppLog.ui.i { "User clicked recenter chart" }
            recenterTrigger.value++
        }

        companion object {
            private const val STOP_TIMEOUT_MILLIS = 5000L
        }
    }
