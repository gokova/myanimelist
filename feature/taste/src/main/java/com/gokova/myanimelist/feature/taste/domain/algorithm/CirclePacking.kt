package com.gokova.myanimelist.feature.taste.domain.algorithm

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class PackableCircle<T>(
    val item: T,
    val radius: Float,
)

data class PositionedCircle<T>(
    val item: T,
    val radius: Float,
    val x: Float,
    val y: Float,
)

object CirclePacking {
    private const val EPSILON = 0.01f

    fun <T> pack(
        items: List<PackableCircle<T>>,
        spacing: Float = 3f,
    ): List<PositionedCircle<T>> {
        if (items.size <= 1) {
            return packTrivial(items)
        }
        return packMultiple(items, spacing)
    }

    private fun <T> packTrivial(items: List<PackableCircle<T>>): List<PositionedCircle<T>> =
        if (items.isEmpty()) {
            emptyList()
        } else {
            val first = items.first()
            listOf(PositionedCircle(item = first.item, radius = first.radius, x = 0f, y = 0f))
        }

    private fun <T> packMultiple(
        items: List<PackableCircle<T>>,
        spacing: Float,
    ): List<PositionedCircle<T>> {
        val indexed = items.mapIndexed { index, circle -> index to circle }
        val sorted = indexed.sortedByDescending { it.second.radius }
        val placed = mutableListOf<PlacedNode>()

        val first = sorted[0].second
        placed.add(PlacedNode(x = 0f, y = 0f, radius = first.radius))

        val second = sorted[1].second
        val dist12 = first.radius + second.radius + spacing
        placed.add(PlacedNode(x = dist12, y = 0f, radius = second.radius))

        for (i in 2 until sorted.size) {
            val current = sorted[i].second
            val bestCandidate = findBestPosition(placed, current.radius, spacing)
            placed.add(bestCandidate)
        }

        return alignToCenter(items, sorted, placed)
    }

    private fun <T> alignToCenter(
        items: List<PackableCircle<T>>,
        sorted: List<Pair<Int, PackableCircle<T>>>,
        placed: List<PlacedNode>,
    ): List<PositionedCircle<T>> {
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (node in placed) {
            minX = min(minX, node.x - node.radius)
            maxX = max(maxX, node.x + node.radius)
            minY = min(minY, node.y - node.radius)
            maxY = max(maxY, node.y + node.radius)
        }

        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f

        val resultMap = mutableMapOf<Int, PositionedCircle<T>>()
        for (i in sorted.indices) {
            val originalIndex = sorted[i].first
            val originalItem = sorted[i].second
            val node = placed[i]
            resultMap[originalIndex] =
                PositionedCircle(
                    item = originalItem.item,
                    radius = originalItem.radius,
                    x = node.x - centerX,
                    y = node.y - centerY,
                )
        }

        return items.indices.mapNotNull { resultMap[it] }
    }

    private fun findBestPosition(
        placed: List<PlacedNode>,
        radius: Float,
        spacing: Float,
    ): PlacedNode {
        var bestNode: PlacedNode? = null
        var bestDistSq = Float.MAX_VALUE

        for (j in placed.indices) {
            for (k in j + 1 until placed.size) {
                val candidate =
                    findBestCandidateForPair(placed[j], placed[k], radius, spacing, placed)
                if (candidate != null && candidate.second < bestDistSq) {
                    bestDistSq = candidate.second
                    bestNode = candidate.first
                }
            }
        }

        return bestNode ?: createFallbackPosition(placed, radius, spacing)
    }

    private fun findBestCandidateForPair(
        c1: PlacedNode,
        c2: PlacedNode,
        radius: Float,
        spacing: Float,
        placed: List<PlacedNode>,
    ): Pair<PlacedNode, Float>? {
        val candidates = getTangentCandidates(c1, c2, radius, spacing)
        var bestPair: Pair<PlacedNode, Float>? = null

        for (cand in candidates) {
            if (isValidPosition(cand, radius, placed, spacing)) {
                val distSq = cand.x * cand.x + cand.y * cand.y
                if (bestPair == null || distSq < bestPair.second) {
                    bestPair = Pair(PlacedNode(cand.x, cand.y, radius), distSq)
                }
            }
        }
        return bestPair
    }

    private fun createFallbackPosition(
        placed: List<PlacedNode>,
        radius: Float,
        spacing: Float,
    ): PlacedNode {
        val anchor = placed.minByOrNull { it.x * it.x + it.y * it.y } ?: placed.first()
        val dist = anchor.radius + radius + spacing
        return PlacedNode(x = anchor.x + dist, y = anchor.y, radius = radius)
    }

    private fun getTangentCandidates(
        c1: PlacedNode,
        c2: PlacedNode,
        r: Float,
        spacing: Float,
    ): List<Point2D> {
        val r1 = c1.radius + r + spacing
        val r2 = c2.radius + r + spacing
        val dx = c2.x - c1.x
        val dy = c2.y - c1.y
        val d = sqrt(dx * dx + dy * dy)
        val canIntersect = d <= r1 + r2 && d >= kotlin.math.abs(r1 - r2) && d >= EPSILON

        if (!canIntersect) {
            return emptyList()
        }

        val a = (r1 * r1 - r2 * r2 + d * d) / (2f * d)
        val hSq = max(0f, r1 * r1 - a * a)
        val h = sqrt(hSq)
        val px = c1.x + a * dx / d
        val py = c1.y + a * dy / d
        val rx = -dy * (h / d)
        val ry = dx * (h / d)

        return listOf(
            Point2D(px + rx, py + ry),
            Point2D(px - rx, py - ry),
        )
    }

    private fun isValidPosition(
        point: Point2D,
        radius: Float,
        placed: List<PlacedNode>,
        spacing: Float,
    ): Boolean {
        for (node in placed) {
            val dx = point.x - node.x
            val dy = point.y - node.y
            val dist = sqrt(dx * dx + dy * dy)
            val minDist = radius + node.radius + spacing - EPSILON
            if (dist < minDist) {
                return false
            }
        }
        return true
    }

    private data class PlacedNode(
        val x: Float,
        val y: Float,
        val radius: Float,
    )

    private data class Point2D(
        val x: Float,
        val y: Float,
    )
}
