package com.gokova.myanimelist.feature.details.domain.usecase

import com.gokova.myanimelist.core.domain.usecase.DeleteAnimeFromListUseCase
import com.gokova.myanimelist.core.domain.usecase.UpdateAnimeStatusUseCase
import javax.inject.Inject

class AnimeStatusInteractor
    @Inject
    constructor(
        val updateStatus: UpdateAnimeStatusUseCase,
        val deleteAnime: DeleteAnimeFromListUseCase,
    )
