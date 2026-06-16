package com.oneuihomeclone.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherLogicTest {

    private fun app(id: String, name: String = id) =
        CloneApp(id = id, name = name, color = Color.Gray)

    private fun appItem(id: String, name: String = id) =
        AppItemModel(app(id, name))

    private fun folder(id: String, vararg appIds: String) =
        FolderModel(
            id = id,
            title = "Folder $id",
            summary = "${appIds.size} apps",
            apps = appIds.map { app(it) },
        )

    private fun page(
        id: Int,
        items: List<HomeGridItemModel> = emptyList(),
        widgets: List<WidgetTemplateModel> = emptyList(),
    ) = HomePageModel(
        id = id,
        label = "Home $id",
        eyebrow = "",
        value = "",
        status = "",
        note = "",
        widgets = widgets,
        items = items,
    )

    @Test
    fun totalPageCount_withoutMediaPage() {
        assertEquals(3, totalPageCount(3, mediaPageEnabled = false))
    }

    @Test
    fun totalPageCount_withMediaPage() {
        assertEquals(4, totalPageCount(3, mediaPageEnabled = true))
    }

    @Test
    fun visualIndexForHomePage_withoutMediaPage() {
        assertEquals(0, visualIndexForHomePage(0, mediaPageEnabled = false))
        assertEquals(2, visualIndexForHomePage(2, mediaPageEnabled = false))
    }

    @Test
    fun visualIndexForHomePage_withMediaPage() {
        assertEquals(1, visualIndexForHomePage(0, mediaPageEnabled = true))
        assertEquals(3, visualIndexForHomePage(2, mediaPageEnabled = true))
    }

    @Test
    fun homePageIndexFromVisual_withoutMediaPage() {
        assertEquals(0, homePageIndexFromVisual(0, mediaPageEnabled = false))
        assertEquals(2, homePageIndexFromVisual(2, mediaPageEnabled = false))
    }

    @Test
    fun homePageIndexFromVisual_mediaPageIndex_returnsNull() {
        assertNull(homePageIndexFromVisual(0, mediaPageEnabled = true))
    }

    @Test
    fun homePageIndexFromVisual_withMediaPage_offset() {
        assertEquals(0, homePageIndexFromVisual(1, mediaPageEnabled = true))
        assertEquals(2, homePageIndexFromVisual(3, mediaPageEnabled = true))
    }

    @Test
    fun moveListItem_swapAdjacent() {
        val items = listOf("A", "B", "C")
        assertEquals(listOf("B", "A", "C"), moveListItem(items, 0, 1))
    }

    @Test
    fun moveListItem_sameIndex_noChange() {
        val items = listOf("A", "B", "C")
        assertEquals(items, moveListItem(items, 1, 1))
    }

    @Test
    fun moveListItem_moveToEnd() {
        val items = listOf("A", "B", "C")
        assertEquals(listOf("B", "C", "A"), moveListItem(items, 0, 2))
    }

    @Test
    fun movedIndexForSwap_trackedIsMoved() {
        assertEquals(2, movedIndexForSwap(trackedIndex = 0, fromIndex = 0, toIndex = 2))
    }

    @Test
    fun movedIndexForSwap_trackedUnaffected() {
        assertEquals(3, movedIndexForSwap(trackedIndex = 3, fromIndex = 0, toIndex = 1))
    }

    @Test
    fun movedIndexForSwap_trackedBetweenForwardSwap() {
        assertEquals(0, movedIndexForSwap(trackedIndex = 1, fromIndex = 0, toIndex = 2))
    }

    @Test
    fun reorderHomeGridItems_swapByIds() {
        val items: List<HomeGridItemModel> = listOf(
            appItem("a"),
            appItem("b"),
            appItem("c"),
        )
        val result = reorderHomeGridItems(items, "a", "c")
        assertEquals(listOf("b", "c", "a"), result.map { it.id })
    }

    @Test
    fun reorderHomeGridItems_unknownId_noChange() {
        val items: List<HomeGridItemModel> = listOf(appItem("a"), appItem("b"))
        val result = reorderHomeGridItems(items, "a", "missing")
        assertEquals(items, result)
    }

    @Test
    fun alphabeticalAppSections_groupsByFirstLetter() {
        val apps = listOf(app("a1", "Alpha"), app("b1", "Beta"), app("a2", "Apex"))
        val sections = alphabeticalAppSections(apps)
        assertEquals(2, sections.size)
        assertEquals("A", sections[0].first)
        assertEquals(2, sections[0].second.size)
        assertEquals("B", sections[1].first)
        assertEquals(1, sections[1].second.size)
    }

    @Test
    fun alphabeticalAppSections_emptyList() {
        assertTrue(alphabeticalAppSections(emptyList()).isEmpty())
    }

    @Test
    fun applyHiddenAppsToPages_removesHiddenApps() {
        val items: List<HomeGridItemModel> = listOf(appItem("a"), appItem("b"), appItem("c"))
        val pages = listOf(page(1, items))
        val result = applyHiddenAppsToPages(pages, setOf("b"))
        assertEquals(2, result[0].items.size)
        assertEquals(listOf("a", "c"), result[0].items.map { it.id })
    }

    @Test
    fun applyHiddenAppsToPages_removesEmptyFolders() {
        val items: List<HomeGridItemModel> = listOf(
            appItem("a"),
            folder("f1", "b"),
        )
        val pages = listOf(page(1, items))
        val result = applyHiddenAppsToPages(pages, setOf("b"))
        assertEquals(1, result[0].items.size)
        assertEquals("a", result[0].items[0].id)
    }

    @Test
    fun applyHiddenAppsToPages_filtersAppsInsideFolders() {
        val items: List<HomeGridItemModel> = listOf(
            folder("f1", "a", "b", "c"),
        )
        val pages = listOf(page(1, items))
        val result = applyHiddenAppsToPages(pages, setOf("b"))
        val folder = result[0].items[0] as FolderModel
        assertEquals(2, folder.apps.size)
        assertEquals(listOf("a", "c"), folder.apps.map { it.id })
    }

    @Test
    fun applyHiddenAppsToPages_noHiddenIds_unchanged() {
        val items: List<HomeGridItemModel> = listOf(appItem("a"))
        val pages = listOf(page(1, items))
        val result = applyHiddenAppsToPages(pages, emptySet())
        assertEquals(pages, result)
    }
}
