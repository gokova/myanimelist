package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.csv.IgnoredAnimeCsvFormat
import com.gokova.myanimelist.core.domain.repository.IgnoredAnimeRepository
import java.io.OutputStream
import javax.inject.Inject

class ExportIgnoredAnimeUseCase
    @Inject
    constructor(
        private val repository: IgnoredAnimeRepository,
    ) {
        suspend operator fun invoke(outputStream: OutputStream): Result<Int> =
            runCatching {
                val items = repository.getAllIgnoredAnime()
                val csvContent = IgnoredAnimeCsvFormat.serialize(items)
                outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(csvContent)
                    writer.flush()
                }
                items.size
            }
    }
