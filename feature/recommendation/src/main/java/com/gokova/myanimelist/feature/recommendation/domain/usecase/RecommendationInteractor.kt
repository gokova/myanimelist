package com.gokova.myanimelist.feature.recommendation.domain.usecase

import javax.inject.Inject

class RecommendationInteractor
    @Inject
    constructor(
        val observeRecommendations: ObserveRecommendationsUseCase,
        val observeRecommendationState: ObserveRecommendationStateUseCase,
        val triggerCalculation: TriggerRecommendationCalculationUseCase,
        val scheduleWork: ScheduleRecommendationWorkUseCase,
        val calculateLabMatchCount: CalculateLabMatchCountUseCase,
    )
