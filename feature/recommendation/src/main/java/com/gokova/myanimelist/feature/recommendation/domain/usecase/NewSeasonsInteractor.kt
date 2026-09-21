package com.gokova.myanimelist.feature.recommendation.domain.usecase

import javax.inject.Inject

class NewSeasonsInteractor
    @Inject
    constructor(
        val observeNewSeasons: ObserveNewSeasonsUseCase,
        val observeNewSeasonState: ObserveNewSeasonStateUseCase,
        val triggerCalculation: TriggerNewSeasonCalculationUseCase,
        val scheduleWork: ScheduleNewSeasonWorkUseCase,
    )
