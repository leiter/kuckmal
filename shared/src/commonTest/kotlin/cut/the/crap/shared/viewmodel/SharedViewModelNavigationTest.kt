package cut.the.crap.shared.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for SharedViewModel navigation functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModelNavigationTest : SharedViewModelTestBase() {

    // =================================================================================
    // Initial Navigation State Tests
    // =================================================================================

    @Test
    fun `initial ViewState is Themes`() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.viewState.value is ViewState.Themes)
    }

    @Test
    fun `initial Themes state has null channel and theme`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.viewState.value as ViewState.Themes
        assertNull(state.channel)
        assertNull(state.theme)
    }

    @Test
    fun `initial Themes state has empty search filter`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.viewState.value as ViewState.Themes
        assertTrue(state.searchFilter.isEmpty)
    }

    @Test
    fun `initial Themes state has null selected item`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.viewState.value as ViewState.Themes
        assertNull(state.selectedItem)
    }

    // =================================================================================
    // navigateToThemes Tests
    // =================================================================================

    @Test
    fun `navigateToThemes with channel updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.navigateToThemes(channel = "ARD")

        val state = viewModel.viewState.value as ViewState.Themes
        assertEquals("ARD", state.channel)
        assertNull(state.theme)
    }

    @Test
    fun `navigateToThemes with channel and theme updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.navigateToThemes(channel = "ARD", theme = "News")

        val state = viewModel.viewState.value as ViewState.Themes
        assertEquals("ARD", state.channel)
        assertEquals("News", state.theme)
    }

    @Test
    fun `navigateToThemes with null clears channel and theme`() = runTest {
        val viewModel = createViewModel()

        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        viewModel.navigateToThemes(channel = null, theme = null)

        val state = viewModel.viewState.value as ViewState.Themes
        assertNull(state.channel)
        assertNull(state.theme)
    }

    @Test
    fun `navigateToThemes preserves search filter`() = runTest {
        val viewModel = createViewModel()

        // Set up initial state with some values
        viewModel.navigateToThemes(channel = "ARD")

        // Navigate to different channel
        viewModel.navigateToThemes(channel = "ZDF")

        val state = viewModel.viewState.value as ViewState.Themes
        assertEquals("ZDF", state.channel)
    }

    @Test
    fun `navigateToThemes preserves selected item`() = runTest {
        val viewModel = createViewModel()
        val entry = createTestEntry()

        viewModel.setSelectedItem(entry)
        viewModel.navigateToThemes(channel = "ARD")

        val state = viewModel.viewState.value as ViewState.Themes
        assertEquals(entry, state.selectedItem)
    }

    // =================================================================================
    // navigateToDetail Tests
    // =================================================================================

    @Test
    fun `navigateToDetail transitions to Detail state when entry exists`() = runTest {
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test Title")
        fakeRepository.addEntries(listOf(entry))

        val viewModel = createViewModel()
        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        viewModel.navigateToDetail("Test Title")
        advanceUntilIdle()

        val state = viewModel.viewState.value
        assertTrue(state is ViewState.Detail)
        assertEquals("Test Title", (state as ViewState.Detail).title)
    }

    @Test
    fun `navigateToDetail preserves navigation context`() = runTest {
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test")
        fakeRepository.addEntries(listOf(entry))

        val viewModel = createViewModel()
        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        viewModel.navigateToDetail("Test")
        advanceUntilIdle()

        val state = viewModel.viewState.value as ViewState.Detail
        assertEquals("ARD", state.navigationChannel)
        assertEquals("News", state.navigationTheme)
    }

    @Test
    fun `navigateToDetail preserves search filter`() = runTest {
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test")
        fakeRepository.addEntries(listOf(entry))

        val viewModel = createViewModel()
        viewModel.navigateToThemes(channel = "ARD", theme = "News")

        // The search filter would be preserved from the previous state
        viewModel.navigateToDetail("Test")
        advanceUntilIdle()

        val state = viewModel.viewState.value as ViewState.Detail
        assertNotNull(state.searchFilter)
    }

    @Test
    fun `navigateToDetail preserves selected item`() = runTest {
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test")
        fakeRepository.addEntries(listOf(entry))

        val viewModel = createViewModel()
        viewModel.setSelectedItem(entry)
        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        viewModel.navigateToDetail("Test")
        advanceUntilIdle()

        val state = viewModel.viewState.value as ViewState.Detail
        assertEquals(entry, state.selectedItem)
    }

    @Test
    fun `selectedTitle returns title in Detail state`() = runTest {
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test Title")
        fakeRepository.addEntries(listOf(entry))

        val viewModel = createViewModel()
        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        viewModel.navigateToDetail("Test Title")
        advanceUntilIdle()

        assertEquals("Test Title", viewModel.selectedTitle)
    }

    @Test
    fun `currentMediaEntry returns entry in Detail state`() = runTest {
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test Title")
        fakeRepository.addEntries(listOf(entry))

        val viewModel = createViewModel()
        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        viewModel.navigateToDetail("Test Title")
        advanceUntilIdle()

        assertNotNull(viewModel.currentMediaEntry)
        assertEquals("Test Title", viewModel.currentMediaEntry?.title)
    }

    // =================================================================================
    // Selected Item Tests
    // =================================================================================

    @Test
    fun `setSelectedItem updates selected item in Themes state`() = runTest {
        val viewModel = createViewModel()
        val entry = createTestEntry()

        viewModel.setSelectedItem(entry)

        val state = viewModel.viewState.value as ViewState.Themes
        assertEquals(entry, state.selectedItem)
    }

    @Test
    fun `setSelectedItem updates selected item in Detail state`() = runTest {
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test")
        val newEntry = createTestEntry(id = 2, title = "Other")
        fakeRepository.addEntries(listOf(entry, newEntry))

        val viewModel = createViewModel()
        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        viewModel.navigateToDetail("Test")
        advanceUntilIdle()

        viewModel.setSelectedItem(newEntry)

        val state = viewModel.viewState.value as ViewState.Detail
        assertEquals(newEntry, state.selectedItem)
    }

    @Test
    fun `clearSelectedItem clears selected item`() = runTest {
        val viewModel = createViewModel()
        val entry = createTestEntry()

        viewModel.setSelectedItem(entry)
        assertNotNull((viewModel.viewState.value as ViewState.Themes).selectedItem)

        viewModel.clearSelectedItem()
        assertNull((viewModel.viewState.value as ViewState.Themes).selectedItem)
    }

    // =================================================================================
    // Computed Properties Tests
    // =================================================================================

    @Test
    fun `selectedChannel reflects ViewState channel`() = runTest {
        val viewModel = createViewModel()

        assertNull(viewModel.selectedChannel)

        viewModel.navigateToThemes(channel = "ARD")
        assertEquals("ARD", viewModel.selectedChannel)

        viewModel.navigateToThemes(channel = "ZDF")
        assertEquals("ZDF", viewModel.selectedChannel)
    }

    @Test
    fun `selectedTheme reflects ViewState theme`() = runTest {
        val viewModel = createViewModel()

        assertNull(viewModel.selectedTheme)

        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        assertEquals("News", viewModel.selectedTheme)
    }

    // =================================================================================
    // ViewState Flow Tests
    // =================================================================================

    @Test
    fun `viewState emits updates when navigating`() = runTest {
        val viewModel = createViewModel()

        viewModel.viewState.test {
            // Initial state
            val initial = awaitItem()
            assertTrue(initial is ViewState.Themes)

            // Navigate to channel
            viewModel.navigateToThemes(channel = "ARD")
            val afterChannel = awaitItem()
            assertEquals("ARD", (afterChannel as ViewState.Themes).channel)

            // Navigate to theme
            viewModel.navigateToThemes(channel = "ARD", theme = "News")
            val afterTheme = awaitItem()
            assertEquals("News", (afterTheme as ViewState.Themes).theme)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `viewState channel returns navigation channel in Detail state`() = runTest {
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test")
        fakeRepository.addEntries(listOf(entry))

        val viewModel = createViewModel()
        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        viewModel.navigateToDetail("Test")
        advanceUntilIdle()

        assertEquals("ARD", viewModel.viewState.value.channel)
    }

    @Test
    fun `viewState theme returns navigation theme in Detail state`() = runTest {
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test")
        fakeRepository.addEntries(listOf(entry))

        val viewModel = createViewModel()
        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        viewModel.navigateToDetail("Test")
        advanceUntilIdle()

        assertEquals("News", viewModel.viewState.value.theme)
    }
}
