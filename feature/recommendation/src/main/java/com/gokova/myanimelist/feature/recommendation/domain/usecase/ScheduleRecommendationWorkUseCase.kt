package com.gokova.myanimelist.feature.recommendation.domain.usecase

import com.gokova.myanimelist.feature.recommendation.domain.repository.RecommendationRepository
import javax.inject.Inject

class ScheduleRecommendationWorkUseCase
    @Inject
    constructor(
        private val repository: RecommendationRepository,
    ) {
        suspend operator fun invoke() {
            repository.scheduleInitialOrPeriodicWork()
        }
    }
