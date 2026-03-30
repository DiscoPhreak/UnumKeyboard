package com.unum.keyboard.text

import com.unum.keyboard.core.TextAction
import kotlin.test.*

class CursorControllerTest {

    private lateinit var controller: CursorController

    @BeforeTest
    fun setUp() {
        controller = CursorController()
    }

    // --- Activation ---

    @Test
    fun notActiveInitially() {
        assertFalse(controller.isActive)
        assertFalse(controller.isSelecting)
    }

    @Test
    fun notActiveBeforeDelay() {
        controller.onTouchStart(100f, 200f, 0L)
        val consumed = controller.onTouchMove(120f, 200f, 200L) // 200ms < 300ms default
        assertFalse(consumed)
        assertFalse(controller.isActive)
    }

    @Test
    fun activatesAfterDelay() {
        controller.onTouchStart(100f, 200f, 0L)
        val consumed = controller.onTouchMove(120f, 200f, 300L) // exactly at delay
        assertTrue(consumed)
        assertTrue(controller.isActive)
    }

    @Test
    fun activatesAfterCustomDelay() {
        controller = CursorController(activationDelay = 500L)
        controller.onTouchStart(100f, 200f, 0L)
        assertFalse(controller.onTouchMove(120f, 200f, 400L))
        assertTrue(controller.onTouchMove(130f, 200f, 500L))
        assertTrue(controller.isActive)
    }

    // --- Cursor Movement ---

    @Test
    fun movesCursorRight() {
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L) // activate
        controller.onTouchMove(130f, 200f, 310L) // move 30px right → 2 positions

        val actions = controller.drainActions()
        assertTrue(actions.isNotEmpty())
        assertTrue(actions.all { it is TextAction.MoveCursor })
        val totalOffset = actions.filterIsInstance<TextAction.MoveCursor>().sumOf { it.offset }
        assertEquals(2, totalOffset) // 30px / 15px sensitivity = 2
    }

    @Test
    fun movesCursorLeft() {
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L) // activate
        controller.onTouchMove(55f, 200f, 310L) // move 45px left → -3 positions

        val actions = controller.drainActions()
        assertTrue(actions.isNotEmpty())
        val totalOffset = actions.filterIsInstance<TextAction.MoveCursor>().sumOf { it.offset }
        assertEquals(-3, totalOffset)
    }

    @Test
    fun noMovementWithinSensitivity() {
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L) // activate
        controller.onTouchMove(107f, 200f, 310L) // move 7px → rounds to 0

        val actions = controller.drainActions()
        assertTrue(actions.isEmpty())
    }

    @Test
    fun customSensitivity() {
        controller = CursorController(sensitivity = 30f)
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L) // activate
        controller.onTouchMove(160f, 200f, 310L) // 60px / 30 = 2

        val actions = controller.drainActions()
        val totalOffset = actions.filterIsInstance<TextAction.MoveCursor>().sumOf { it.offset }
        assertEquals(2, totalOffset)
    }

    @Test
    fun totalCursorOffsetTracked() {
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L)
        controller.onTouchMove(145f, 200f, 310L) // 45px / 15 = 3

        assertEquals(3, controller.totalCursorOffset)
    }

    // --- Selection Mode ---

    @Test
    fun notSelectingByDefault() {
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L) // activate
        controller.onTouchMove(130f, 200f, 310L) // horizontal only

        assertFalse(controller.isSelecting)
    }

    @Test
    fun entersSelectionOnVerticalDrag() {
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L) // activate
        controller.onTouchMove(130f, 235f, 310L) // dy=35 > threshold 30

        assertTrue(controller.isSelecting)
    }

    @Test
    fun selectionGeneratesExtendSelectionActions() {
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L) // activate
        controller.onTouchMove(100f, 235f, 305L) // enter selection mode
        controller.onTouchMove(145f, 235f, 310L) // move 45px right in selection mode

        val actions = controller.drainActions()
        assertTrue(actions.any { it is TextAction.ExtendSelection })
    }

    @Test
    fun verticalSelectionCanBeDisabled() {
        controller = CursorController(verticalSelectionEnabled = false)
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L)
        controller.onTouchMove(130f, 260f, 310L) // large vertical drag

        assertFalse(controller.isSelecting)
        val actions = controller.drainActions()
        assertTrue(actions.all { it is TextAction.MoveCursor })
    }

    // --- Touch End ---

    @Test
    fun touchEndDeactivates() {
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L)
        assertTrue(controller.isActive)

        val wasActive = controller.onTouchEnd(130f, 200f, 400L)
        assertTrue(wasActive)
        assertFalse(controller.isActive)
        assertFalse(controller.isSelecting)
    }

    @Test
    fun touchEndReturnsFalseIfNotActive() {
        controller.onTouchStart(100f, 200f, 0L)
        val wasActive = controller.onTouchEnd(100f, 200f, 100L) // before delay
        assertFalse(wasActive)
    }

    // --- Cancel ---

    @Test
    fun cancelClearsState() {
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L)
        controller.onTouchMove(130f, 200f, 310L)
        assertTrue(controller.isActive)

        controller.cancel()
        assertFalse(controller.isActive)
        assertFalse(controller.isSelecting)
        assertTrue(controller.drainActions().isEmpty())
    }

    // --- Drain Actions ---

    @Test
    fun drainActionsClearsQueue() {
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L)
        controller.onTouchMove(130f, 200f, 310L)

        val first = controller.drainActions()
        assertTrue(first.isNotEmpty())
        val second = controller.drainActions()
        assertTrue(second.isEmpty())
    }

    // --- Reset on new touch ---

    @Test
    fun newTouchStartResetsState() {
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L)
        assertTrue(controller.isActive)

        controller.onTouchStart(200f, 200f, 500L) // new touch
        assertFalse(controller.isActive)
        assertFalse(controller.isSelecting)
        assertEquals(0, controller.totalCursorOffset)
    }

    // --- Incremental offsets ---

    @Test
    fun producesIncrementalOffsets() {
        controller.onTouchStart(100f, 200f, 0L)
        controller.onTouchMove(100f, 200f, 300L) // activate
        controller.onTouchMove(115f, 200f, 310L) // offset 1
        controller.onTouchMove(130f, 200f, 320L) // offset 2

        val actions = controller.drainActions()
        // Should be two separate MoveCursor(1) actions, not one MoveCursor(2)
        assertEquals(2, actions.size)
        assertTrue(actions.all { it is TextAction.MoveCursor && it.offset == 1 })
    }
}

class EditingActionsTest {

    @Test
    fun allActionsHaveUniqueIds() {
        val ids = EditingAction.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun fromIdReturnsCorrectAction() {
        assertEquals(EditingAction.CURSOR_LEFT, EditingAction.fromId("cursor_left"))
        assertEquals(EditingAction.PASTE, EditingAction.fromId("paste"))
        assertEquals(EditingAction.REDO, EditingAction.fromId("redo"))
    }

    @Test
    fun fromIdReturnsNullForUnknown() {
        assertNull(EditingAction.fromId("nonexistent"))
    }

    @Test
    fun toolbarRow1HasNavigationActions() {
        val row1 = EditingAction.toolbarRow1
        assertEquals(6, row1.size)
        assertTrue(row1.contains(EditingAction.CURSOR_LEFT))
        assertTrue(row1.contains(EditingAction.CURSOR_RIGHT))
        assertTrue(row1.contains(EditingAction.CURSOR_WORD_LEFT))
        assertTrue(row1.contains(EditingAction.CURSOR_WORD_RIGHT))
        assertTrue(row1.contains(EditingAction.CURSOR_LINE_START))
        assertTrue(row1.contains(EditingAction.CURSOR_LINE_END))
    }

    @Test
    fun toolbarRow2HasEditingActions() {
        val row2 = EditingAction.toolbarRow2
        assertEquals(6, row2.size)
        assertTrue(row2.contains(EditingAction.SELECT_ALL))
        assertTrue(row2.contains(EditingAction.CUT))
        assertTrue(row2.contains(EditingAction.COPY))
        assertTrue(row2.contains(EditingAction.PASTE))
        assertTrue(row2.contains(EditingAction.UNDO))
        assertTrue(row2.contains(EditingAction.REDO))
    }

    @Test
    fun allActionsHaveNonEmptyLabels() {
        EditingAction.entries.forEach {
            assertTrue(it.label.isNotEmpty(), "Action ${it.id} has empty label")
            assertTrue(it.icon.isNotEmpty(), "Action ${it.id} has empty icon")
        }
    }
}
