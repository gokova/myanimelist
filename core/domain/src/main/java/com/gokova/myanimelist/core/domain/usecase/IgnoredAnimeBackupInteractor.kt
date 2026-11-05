package com.gokova.myanimelist.core.domain.usecase

import javax.inject.Inject

class IgnoredAnimeBackupInteractor
    @Inject
    constructor(
        val observeCount: ObserveIgnoredAnimeCountUseCase,
        val export: ExportIgnoredAnimeUseCase,
        val import: ImportIgnoredAnimeUseCase,
    )
