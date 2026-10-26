package com.gokova.myanimelist.core.domain.csv

import com.gokova.myanimelist.core.domain.model.IgnoredAnime

object IgnoredAnimeCsvFormat {
    private const val CSV_HEADER =
        "anime_id,title,title_english,picture_url,created_at\r\n"
    private const val DEFAULT_ID_INDEX = 0
    private const val DEFAULT_TITLE_INDEX = 1
    private const val DEFAULT_TITLE_EN_INDEX = 2
    private const val DEFAULT_PICTURE_URL_INDEX = 3
    private const val DEFAULT_CREATED_AT_INDEX = 4

    private val SPECIAL_CHARS = charArrayOf(',', '"', '\n', '\r')
    private val HEADER_NAMES = setOf("title", "name", "animeid", "id")

    fun serialize(items: List<IgnoredAnime>): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER)
        for (item in items) {
            sb.append(item.animeId).append(',')
            sb.append(escape(item.title)).append(',')
            sb.append(escape(item.titleEnglish.orEmpty())).append(',')
            sb.append(escape(item.pictureUrl.orEmpty())).append(',')
            sb.append(item.createdAt)
            sb.append("\r\n")
        }
        return sb.toString()
    }

    fun escape(field: String): String {
        val needsQuotes = field.any { it in SPECIAL_CHARS }
        return if (!needsQuotes) field else "\"" + field.replace("\"", "\"\"") + "\""
    }

    fun parse(csvContent: String): List<IgnoredAnime> {
        val cleanContent = csvContent.removePrefix("\uFEFF").trim()
        val lines = cleanContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val firstRowTokens = CsvLineParser(lines.first()).parse()
        requireNotNull(firstRowTokens) { "CSV header is malformed" }

        val hasHeader = isHeaderRow(firstRowTokens)
        val headerInfo = resolveHeader(firstRowTokens, hasHeader)
        val dataLines = if (hasHeader) lines.drop(1) else lines

        val currentTime = System.currentTimeMillis()
        val result =
            dataLines.mapNotNull { line ->
                val tokens = CsvLineParser(line).parse() ?: return@mapNotNull null
                parseRow(tokens, headerInfo, currentTime)
            }

        require(dataLines.isEmpty() || result.isNotEmpty()) {
            "No valid anime records found in CSV"
        }

        return result
    }

    private fun resolveHeader(
        firstRow: List<String>,
        hasHeader: Boolean,
    ): ParsedHeader {
        if (!hasHeader) {
            return ParsedHeader(
                DEFAULT_ID_INDEX,
                DEFAULT_TITLE_INDEX,
                DEFAULT_TITLE_EN_INDEX,
                DEFAULT_PICTURE_URL_INDEX,
                DEFAULT_CREATED_AT_INDEX,
            )
        }
        val headerMap =
            firstRow
                .mapIndexed { index, name ->
                    normalizeHeaderCell(name) to index
                }.toMap()

        val idIndex = findColumnIndex(headerMap, "animeid", "id")
        val titleIndex = findColumnIndex(headerMap, "title", "name", "animetitle")
        require(idIndex != -1 && titleIndex != -1) {
            "CSV missing required header columns: anime_id and title"
        }

        return ParsedHeader(
            idIndex = idIndex,
            titleIndex = titleIndex,
            titleEnIndex = findColumnIndex(headerMap, "titleenglish", "englishtitle", "titleen"),
            pictureUrlIndex = findColumnIndex(headerMap, "pictureurl", "picture", "imageurl"),
            createdAtIndex = findColumnIndex(headerMap, "createdat", "timestamp", "date"),
        )
    }

    private fun findColumnIndex(
        headerMap: Map<String, Int>,
        vararg aliases: String,
    ): Int {
        for (alias in aliases) {
            val found = headerMap[alias]
            if (found != null) return found
        }
        return -1
    }

    private fun parseRow(
        row: List<String>,
        header: ParsedHeader,
        currentTime: Long,
    ): IgnoredAnime? {
        val id = row.getOrNull(header.idIndex)?.toLongOrNull()
        val title = row.getOrNull(header.titleIndex)?.takeIf { it.isNotBlank() }
        if (id == null || title == null) return null

        val titleEn =
            header.titleEnIndex
                .takeIf { it >= 0 }
                ?.let { row.getOrNull(it)?.takeIf { titleEn -> titleEn.isNotBlank() } }
        val pictureUrl =
            header.pictureUrlIndex
                .takeIf { it >= 0 }
                ?.let { row.getOrNull(it)?.takeIf { pictureUrl -> pictureUrl.isNotBlank() } }
        val createdAt =
            header.createdAtIndex.takeIf { it >= 0 }?.let { row.getOrNull(it)?.toLongOrNull() }
                ?: currentTime

        return IgnoredAnime(
            animeId = id,
            title = title,
            titleEnglish = titleEn,
            pictureUrl = pictureUrl,
            createdAt = createdAt,
        )
    }

    private fun isHeaderRow(row: List<String>): Boolean {
        val firstCell =
            row
                .firstOrNull()
                ?.trim()
                ?.removePrefix("\uFEFF")
                .orEmpty()
        val isNumericId = firstCell.toLongOrNull() != null
        val hasHeaderName = row.any { normalizeHeaderCell(it) in HEADER_NAMES }
        return !isNumericId && hasHeaderName
    }

    private fun normalizeHeaderCell(cell: String): String =
        cell
            .lowercase()
            .trim()
            .removePrefix("\uFEFF")
            .replace("_", "")
            .replace("-", "")

    private data class ParsedHeader(
        val idIndex: Int,
        val titleIndex: Int,
        val titleEnIndex: Int,
        val pictureUrlIndex: Int,
        val createdAtIndex: Int,
    )
}

internal class CsvLineParser(
    private val line: String,
) {
    private val fields = mutableListOf<String>()
    private val currentField = StringBuilder()
    private var inQuotes = false
    private var wasQuoted = false
    private var quoteClosed = false

    fun parse(): List<String>? {
        var i = 0
        while (i < line.length) {
            val nextIndex = consumeChar(i)
            if (nextIndex == -1) return null
            i = nextIndex
        }
        return if (inQuotes) null else flushAndGetFields()
    }

    private fun flushAndGetFields(): List<String> {
        flushField()
        return fields
    }

    private fun consumeChar(index: Int): Int {
        val c = line[index]
        return if (inQuotes) {
            handleQuotedChar(c, index)
        } else {
            handleUnquotedChar(c, index)
        }
    }

    private fun handleQuotedChar(
        c: Char,
        index: Int,
    ): Int {
        if (c != '"') {
            currentField.append(c)
            return index + 1
        }
        val isEscapedQuote = index + 1 < line.length && line[index + 1] == '"'
        return if (isEscapedQuote) {
            currentField.append('"')
            index + 2
        } else {
            inQuotes = false
            quoteClosed = true
            index + 1
        }
    }

    private fun handleUnquotedChar(
        c: Char,
        index: Int,
    ): Int =
        when {
            c == ',' -> {
                flushField()
                index + 1
            }
            c == '"' -> {
                if (currentField.isNotBlank() || quoteClosed) {
                    -1
                } else {
                    inQuotes = true
                    wasQuoted = true
                    currentField.clear()
                    index + 1
                }
            }
            quoteClosed -> if (c.isWhitespace()) index + 1 else -1
            else -> {
                currentField.append(c)
                index + 1
            }
        }

    private fun flushField() {
        val token = if (wasQuoted) currentField.toString() else currentField.toString().trim()
        fields.add(token)
        currentField.clear()
        wasQuoted = false
        quoteClosed = false
    }
}
