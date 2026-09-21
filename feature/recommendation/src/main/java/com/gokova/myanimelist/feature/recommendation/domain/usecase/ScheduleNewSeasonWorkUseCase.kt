package com.gokova.myanimelist.feature.recommendation.domain.usecase

import com.gokova.myanimelist.feature.recommendation.domain.repository.NewSeasonRepository
import javax.inject.Inject

class ScheduleNewSeasonWorkUseCase
    @Inject
    constructor(
        private val repository: NewSeasonRepository,
    ) {
        suspend operator fun invoke() {
            repository.scheduleInitialOrPeriodicWork()
        }
    }
