package com.unum.keyboard.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LayoutEngineTest {

    private val engine = LayoutEngine()

    // Typical phone screen: 1080px wide, 260dp * 3 density = ~780px keyboard height
    private val screenWidth = 1080f
    private val keyboardHeight = 780f

    @Test
    fun computeLayout_producesGeometryForEveryKey() {
        val layout = QwertyLayouts.enUsLowercase
        val computed = engine.computeLayout(layout, screenWidth, keyboardHeight)

        val expectedKeyCount = layout.rows.sumOf { it.keys.size }
        assertEquals(expectedKeyCount, computed.keys.size)
    }

    @Test
    fun computeLayout_allBoundsWithinScreen() {
        val computed = engine.computeLayout(QwertyLayouts.enUsLowercase, screenWidth, keyboardHeight)

        for (geo in computed.keys) {
            assertTrue(geo.bounds.left >= 0f, "Key ${geo.key.id} left (${geo.bounds.left}) should be >= 0")
            assertTrue(geo.bounds.top >= 0f, "Key ${geo.key.id} top (${geo.bounds.top}) should be >= 0")
            assertTrue(geo.bounds.right <= screenWidth + 1f, "Key ${geo.key.id} right (${geo.bounds.right}) should be <= $screenWidth")
            assertTrue(geo.bounds.bottom <= keyboardHeight + 1f, "Key ${geo.key.id} bottom (${geo.bounds.bottom}) should be <= $keyboardHeight")
        }
    }

    @Test
    fun computeLayout_keysBoundsDoNotOverlapWithinRow() {
        val computed = engine.computeLayout(QwertyLayouts.enUsLowercase, screenWidth, keyboardHeight)
        val layout = QwertyLayouts.enUsLowercase

        var keyIndex = 0
        for (row in layout.rows) {
            val rowKeys = computed.keys.subList(keyIndex, keyIndex + row.keys.size)
            for (i in 0 until rowKeys.size - 1) {
                assertTrue(
                    rowKeys[i].bounds.right <= rowKeys[i + 1].bounds.left + 0.1f,
                    "Key ${rowKeys[i].key.id} overlaps with ${rowKeys[i + 1].key.id}"
                )
            }
            keyIndex += row.keys.size
        }
    }

    @Test
    fun computeLayout_widerKeysHaveLargerBounds() {
        val computed = engine.computeLayout(QwertyLayouts.enUsLowercase, screenWidth, keyboardHeight)

        val spaceKey = computed.keys.find { it.key.type == KeyType.SPACE }
        val letterKey = computed.keys.find { it.key.id == "e" }
        assertNotNull(spaceKey)
        assertNotNull(letterKey)
        assertTrue(
            spaceKey.bounds.width > letterKey.bounds.width * 2,
            "Space (${spaceKey.bounds.width}) should be much wider than letter key (${letterKey.bounds.width})"
        )
    }

    @Test
    fun computeLayout_centerPointIsInsideBounds() {
        val computed = engine.computeLayout(QwertyLayouts.enUsLowercase, screenWidth, keyboardHeight)

        for (geo in computed.keys) {
            assertTrue(
                geo.bounds.contains(geo.center.x, geo.center.y),
                "Center of ${geo.key.id} should be inside its bounds"
            )
        }
    }

    @Test
    fun findKeyAt_returnCorrectKey() {
        val computed = engine.computeLayout(QwertyLayouts.enUsLowercase, screenWidth, keyboardHeight)

        val eKey = computed.keys.find { it.key.id == "e" }!!
        val found = computed.findKeyAt(eKey.center.x, eKey.center.y)
        assertNotNull(found)
        assertEquals("e", found.key.id)
    }

    @Test
    fun findKeyAt_returnsNullOutsideBounds() {
        val computed = engine.computeLayout(QwertyLayouts.enUsLowercase, screenWidth, keyboardHeight)
        assertNull(computed.findKeyAt(-10f, -10f))
    }

    @Test
    fun adjacencyMap_adjacentKeysAreNeighbors() {
        val computed = engine.computeLayout(QwertyLayouts.enUsLowercase, screenWidth, keyboardHeight)

        // Q and W are adjacent on first row
        val qNeighbors = computed.adjacencyMap["q"]
        assertNotNull(qNeighbors)
        assertTrue("w" in qNeighbors, "Q should have W as neighbor, got: $qNeighbors")
    }

    @Test
    fun adjacencyMap_distantKeysAreNotNeighbors() {
        val computed = engine.computeLayout(QwertyLayouts.enUsLowercase, screenWidth, keyboardHeight)

        // Q and P are on opposite ends of the first row
        val qNeighbors = computed.adjacencyMap["q"] ?: emptyList()
        assertTrue("p" !in qNeighbors, "Q should not have P as neighbor, got: $qNeighbors")
    }

    @Test
    fun adjacencyMap_isSymmetric() {
        val computed = engine.computeLayout(QwertyLayouts.enUsLowercase, screenWidth, keyboardHeight)

        for ((keyId, neighbors) in computed.adjacencyMap) {
            for (neighborId in neighbors) {
                val reverseNeighbors = computed.adjacencyMap[neighborId]
                assertNotNull(reverseNeighbors, "$neighborId should have adjacency entry")
                assertTrue(
                    keyId in reverseNeighbors,
                    "If $keyId has $neighborId as neighbor, $neighborId should also have $keyId"
                )
            }
        }
    }

    @Test
    fun adjacencyMap_crossRowNeighborsExist() {
        val computed = engine.computeLayout(QwertyLayouts.enUsLowercase, screenWidth, keyboardHeight)

        // E (row 0) and D (row 1) should be neighbors (D is below E on QWERTY)
        val eNeighbors = computed.adjacencyMap["e"] ?: emptyList()
        assertTrue("d" in eNeighbors || "s" in eNeighbors, "E should have cross-row neighbors, got: $eNeighbors")
    }

    @Test
    fun symbolLayout_hasAllDigits() {
        val computed = engine.computeLayout(QwertyLayouts.enUsSymbols, screenWidth, keyboardHeight)
        for (digit in '0'..'9') {
            val key = computed.keys.find { it.key.id == digit.toString() }
            assertNotNull(key, "Symbol layout should contain digit $digit")
        }
    }

    @Test
    fun allLayouts_computeWithoutError() {
        val layouts = listOf(
            QwertyLayouts.enUsLowercase,
            QwertyLayouts.enUsUppercase,
            QwertyLayouts.enUsSymbols,
            QwertyLayouts.enUsSymbols2
        )
        for (layout in layouts) {
            val computed = engine.computeLayout(layout, screenWidth, keyboardHeight)
            assertTrue(computed.keys.isNotEmpty(), "Layout ${layout.id} should produce keys")
        }
    }
}
