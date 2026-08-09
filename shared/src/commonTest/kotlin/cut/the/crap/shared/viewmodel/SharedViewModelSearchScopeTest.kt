package cut.the.crap.shared.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Search must stay inside the active channel/theme filter.
 *
 * Reproduces the reported bug: with the 3sat channel selected, searching "3sat"
 * offered "MDR THÜRINGEN JOURNAL" - a theme that matches only through its
 * description and belongs to MDR. Selecting it navigated to
 * (channel = 3Sat, theme = MDR THÜRINGEN JOURNAL), which has no rows, so the
 * content pane went blank.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModelSearchScopeTest : SharedViewModelTestBase() {

    private fun seedCrossChannelData() {
        fakeRepository.addEntries(
            listOf(
                createTestEntry(id = 1, channel = "3Sat", theme = "3sat", title = "Laos Wunderland"),
                createTestEntry(id = 2, channel = "3Sat", theme = "Kulturzeit", title = "Kulturzeit vom 1."),
                // Belongs to MDR but mentions 3sat in its description, so an
                // unscoped search matches it.
                createTestEntry(
                    id = 3,
                    channel = "MDR",
                    theme = "MDR THÜRINGEN JOURNAL",
                    title = "Journal vom 1."
                ).copy(description = "Eine Koproduktion mit 3sat")
            )
        )
    }

    @Test
    fun `search with a channel selected excludes other channels themes`() = runTest {
        seedCrossChannelData()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.navigateToThemes(channel = "3Sat", theme = null)
        advanceUntilIdle()

        val results = viewModel.searchContentFlow("3sat", searchInTitles = false).first()

        assertContains(results, "3sat")
        assertFalse(
            results.contains("MDR THÜRINGEN JOURNAL"),
            "search leaked a theme from another channel: $results"
        )
    }

    @Test
    fun `every search result is reachable under the selected channel`() = runTest {
        seedCrossChannelData()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.navigateToThemes(channel = "3Sat", theme = null)
        advanceUntilIdle()

        val results = viewModel.searchContentFlow("3sat", searchInTitles = false).first()

        // The actual defect: selecting a result must not land on an empty list.
        for (theme in results) {
            val titles = viewModel.getTitlesFlow("3Sat", theme).first()
            assertTrue(
                titles.isNotEmpty(),
                "selecting '$theme' under 3Sat yields an empty list"
            )
        }
    }

    @Test
    fun `search without a channel selected still spans all channels`() = runTest {
        seedCrossChannelData()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.navigateToThemes(channel = null, theme = null)
        advanceUntilIdle()

        val results = viewModel.searchContentFlow("3sat", searchInTitles = false).first()

        assertContains(results, "3sat")
        assertContains(results, "MDR THÜRINGEN JOURNAL")
    }

    @Test
    fun `search inside a theme is scoped to that channel and theme`() = runTest {
        fakeRepository.addEntries(
            listOf(
                createTestEntry(id = 1, channel = "3Sat", theme = "Kulturzeit", title = "Wunderland"),
                createTestEntry(id = 2, channel = "3Sat", theme = "3sat", title = "Wunderland anders"),
                createTestEntry(id = 3, channel = "MDR", theme = "Kulturzeit", title = "Wunderland MDR")
            )
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.navigateToThemes(channel = "3Sat", theme = "Kulturzeit")
        advanceUntilIdle()

        val results = viewModel.searchContentFlow("Wunderland", searchInTitles = true).first()

        assertEquals(listOf("Wunderland"), results)
    }
}
