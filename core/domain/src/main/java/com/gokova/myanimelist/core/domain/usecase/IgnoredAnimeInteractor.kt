package com.gokova.myanimelist.core.domain.usecase

import javax.inject.Inject

class IgnoredAnimeInteractor
    @Inject
    constructor(
        val observeIsIgnored: ObserveIsAnimeNotInterestedUseCase,
        val setNotInterested: SetAnimeNotInterestedUseCase,
        val removeNotInterested: RemoveAnimeFromNotInterestedUseCase,
    )
