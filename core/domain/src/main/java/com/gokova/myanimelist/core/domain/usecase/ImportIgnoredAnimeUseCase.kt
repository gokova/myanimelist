package com.gokova.myanimelist.core.domain.usecase

import com.gokova.myanimelist.core.domain.csv.IgnoredAnimeCsvFormat
import com.gokova.myanimelist.core.domain.model.ImportIgnoredAnimeResult
import com.gokova.myanimelist.core.domain.repository.IgnoredAnimeRepository
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

class ImportIgnoredAnimeUseCase
    @Inject
    constructor(
        private val repository: IgnoredAnimeRepository,
    ) {
        suspend operator fun invoke(inputStream: InputStream): ImportIgnoredAnimeResult =
            runCatching {
                val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (content.isBlank()) {
                    ImportIgnoredAnimeResult.Failure.EmptyFile
                } else {
                    val parsedItems = IgnoredAnimeCsvFormat.parse(content)
                    if (parsedItems.isEmpty()) {
                        ImportIgnoredAnimeResult.Failure.EmptyFile
                    } else {
                        repository.upsertIgnoredAnimeList(parsedItems)
                    }
                }
            }.getOrElse { error ->
                when (error) {
                    is IllegalArgumentException -> ImportIgnoredAnimeResult.Failure.InvalidFormat
                    is IOException -> ImportIgnoredAnimeResult.Failure.IoError(error.message)
                    else -> throw error
                }
            }
    }
