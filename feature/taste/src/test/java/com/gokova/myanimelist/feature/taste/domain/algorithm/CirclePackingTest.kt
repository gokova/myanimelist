package com.gokova.myanimelist.feature.taste.domain.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class CirclePackingTest {
    @Test
    fun pack_emptyList_returnsEmpty() {
        val result = CirclePacking.pack<String>(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun pack_singleCircle_isAtOrigin() {
        val result = CirclePacking.pack(listOf(PackableCircle("Action", 40f)))
        assertEquals(1, result.size)
        assertEquals("Action", result[0].item)
        assertEquals(0f, result[0].x, 0.001f)
        assertEquals(0f, result[0].y, 0.001f)
    }

    @Test
    fun pack_multipleCircles_noOverlaps() {
        val radii = listOf(80f, 65f, 55f, 48f, 42f, 40f, 38f, 36f, 36f, 36f)
        val items = radii.mapIndexed { i, r -> PackableCircle("Item_$i", r) }
        val spacing = 4f

        val result = CirclePacking.pack(items, spacing = spacing)
        assertEquals(items.size, result.size)

        // Check non-overlapping invariant for every pair
        for (i in result.indices) {
            for (j in i + 1 until result.size) {
                val c1 = result[i]
                val c2 = result[j]
                val dx = c1.x - c2.x
                val dy = c1.y - c2.y
                val dist = sqrt(dx * dx + dy * dy)
                val minDist = c1.radius + c2.radius + spacing - 0.05f

                assertTrue(
                    "Circle $i and circle $j overlap! dist=$dist, minDist=$minDist",
                    dist >= minDist,
                )
            }
        }
    }

    @Test
    fun pack_isDeterministic() {
        val items =
            listOf(
                PackableCircle("Action", 75f),
                PackableCircle("Comedy", 60f),
                PackableCircle("Drama", 50f),
                PackableCircle("Fantasy", 45f),
                PackableCircle("Romance", 38f),
            )

        val run1 = CirclePacking.pack(items)
        val run2 = CirclePacking.pack(items)

        assertEquals(run1.size, run2.size)
        for (i in run1.indices) {
            assertEquals(run1[i].item, run2[i].item)
            assertEquals(run1[i].x, run2[i].x, 0.0001f)
            assertEquals(run1[i].y, run2[i].y, 0.0001f)
            assertEquals(run1[i].radius, run2[i].radius, 0.0001f)
        }
    }
}
