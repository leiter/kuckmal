package cut.the.crap.android.data

import android.app.Application
import app.cash.turbine.test
import cut.the.crap.android.base.ViewModelTestBase
import cut.the.crap.android.repository.FakeDownloadRepository
import cut.the.crap.android.repository.FakeMediaRepository
import cut.the.crap.android.util.FakeUpdateChecker
import cut.the.crap.shared.database.MediaEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests for MediaViewModel navigation functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class MediaViewModelNavigationTest : ViewModelTestBase() {

    private lateinit var viewModel: MediaViewModel
    private lateinit var fakeRepository: FakeMediaRepository
    private lateinit var fakeDownloadRepository: FakeDownloadRepository
    private lateinit var fakeUpdateChecker: FakeUpdateChecker
    private lateinit var application: Application

    @Before
    override fun setup() {
        super.setup()
        application = RuntimeEnvironment.getApplication()
        fakeRepository = FakeMediaRepository()
        fakeDownloadRepository = FakeDownloadRepository()
        fakeUpdateChecker = FakeUpdateChecker()

        viewModel = MediaViewModel(
            application = application,
            repository = fakeRepository,
            downloadRepository = fakeDownloadRepository,
            updateChecker = fakeUpdateChecker
        )
    }

    // =================================================================================
    // Initial Navigation State Tests
    // =================================================================================

    @Test
    fun `initial ViewState is Themes`() = runTest {
        assertTrue(viewModel.viewState.value is MediaViewModel.ViewState.Themes)
    }

    @Test
    fun `initial selectedChannel is null`() = runTest {
        assertNull(viewModel.selectedChannel)
    }

    @Test
    fun `initial selectedTheme is null`() = runTest {
        assertNull(viewModel.selectedTheme)
    }

    @Test
    fun `initial selectedTitle is null`() = runTest {
        assertNull(viewModel.selectedTitle)
    }

    @Test
    fun `initial currentMediaEntry is null`() = runTest {
        assertNull(viewModel.currentMediaEntry)
    }

    // =================================================================================
    // navigateToThemes Tests
    // =================================================================================

    @Test
    fun `navigateToThemes with channel updates state`() = runTest {
        viewModel.navigateToThemes(channel = "ARD")

        val state = viewModel.viewState.value as MediaViewModel.ViewState.Themes
        assertEquals("ARD", state.channel)
        assertNull(state.theme)
    }

    @Test
    fun `navigateToThemes with channel and theme updates state`() = runTest {
        viewModel.navigateToThemes(channel = "ARD", theme = "News")

        val state = viewModel.viewState.value as MediaViewModel.ViewState.Themes
        assertEquals("ARD", state.channel)
        assertEquals("News", state.theme)
    }

    @Test
    fun `navigateToThemes with null channel clears channel`() = runTest {
        viewModel.navigateToThemes(channel = "ARD")
        viewModel.navigateToThemes(channel = null)

        val state = viewModel.viewState.value as MediaViewModel.ViewState.Themes
        assertNull(state.channel)
    }

    @Test
    fun `navigateToThemes resets currentPart to zero`() = runTest {
        viewModel.setCurrentPart(5)
        assertEquals(5, viewModel.currentPart.value)

        viewModel.navigateToThemes(channel = "ARD")

        assertEquals(0, viewModel.currentPart.value)
    }

    @Test
    fun `navigateToThemes preserves search filter from previous Themes state`() = runTest {
        // Set up initial state with search
        viewModel.updateSearchQuery("test query")

        val initialState = viewModel.viewState.value as MediaViewModel.ViewState.Themes
        assertEquals("test query", initialState.searchFilter.themesQuery)

        // Navigate to a channel
        viewModel.navigateToThemes(channel = "ARD")

        val newState = viewModel.viewState.value as MediaViewModel.ViewState.Themes
        assertEquals("test query", newState.searchFilter.themesQuery)
    }

    // =================================================================================
    // navigateToDetail Tests
    // =================================================================================

    @Test
    fun `navigateToDetail transitions to Detail state when entry exists`() = runTest {
        // Setup: Add entry and navigate to theme first
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test Title")
        fakeRepository.addEntries(listOf(entry))
        viewModel.navigateToThemes(channel = "ARD", theme = "News")

        // Navigate to detail
        viewModel.navigateToDetail("Test Title")
        advanceUntilIdle()

        val state = viewModel.viewState.value
        assertTrue(state is MediaViewModel.ViewState.Detail)
        assertEquals("Test Title", (state as MediaViewModel.ViewState.Detail).title)
    }

    @Test
    fun `navigateToDetail preserves search filter`() = runTest {
        // Setup: Add entry and set search
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test Title")
        fakeRepository.addEntries(listOf(entry))
        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        viewModel.updateSearchQuery("search text")

        // Navigate to detail
        viewModel.navigateToDetail("Test Title")
        advanceUntilIdle()

        val state = viewModel.viewState.value as MediaViewModel.ViewState.Detail
        assertEquals("search text", state.searchFilter.titlesQuery)
    }

    @Test
    fun `navigateToDetail preserves navigation context`() = runTest {
        // Setup
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test Title")
        fakeRepository.addEntries(listOf(entry))
        viewModel.navigateToThemes(channel = "ARD", theme = "News")

        // Navigate to detail
        viewModel.navigateToDetail("Test Title")
        advanceUntilIdle()

        val state = viewModel.viewState.value as MediaViewModel.ViewState.Detail
        assertEquals("ARD", state.navigationChannel)
        assertEquals("News", state.navigationTheme)
    }

    @Test
    fun `selectedTitle returns title in Detail state`() = runTest {
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test Title")
        fakeRepository.addEntries(listOf(entry))
        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        viewModel.navigateToDetail("Test Title")
        advanceUntilIdle()

        assertEquals("Test Title", viewModel.selectedTitle)
    }

    @Test
    fun `currentMediaEntry returns entry in Detail state`() = runTest {
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test Title")
        fakeRepository.addEntries(listOf(entry))
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
        val entry = createTestEntry()

        viewModel.setSelectedItem(entry)

        val state = viewModel.viewState.value as MediaViewModel.ViewState.Themes
        assertEquals(entry, state.selectedItem)
    }

    @Test
    fun `clearSelectedItem clears selected item`() = runTest {
        val entry = createTestEntry()
        viewModel.setSelectedItem(entry)

        val stateBefore = viewModel.viewState.value as MediaViewModel.ViewState.Themes
        assertNotNull(stateBefore.selectedItem)

        viewModel.clearSelectedItem()

        val stateAfter = viewModel.viewState.value as MediaViewModel.ViewState.Themes
        assertNull(stateAfter.selectedItem)
    }

    @Test
    fun `selected item is preserved across navigation`() = runTest {
        val entry = createTestEntry(channel = "ARD", theme = "News", title = "Test")
        fakeRepository.addEntries(listOf(entry))

        viewModel.setSelectedItem(entry)
        viewModel.navigateToThemes(channel = "ARD", theme = "News")

        val state = viewModel.viewState.value as MediaViewModel.ViewState.Themes
        assertEquals(entry, state.selectedItem)
    }

    // =================================================================================
    // Search Filter Tests
    // =================================================================================

    @Test
    fun `updateSearchQuery updates themesQuery in theme mode`() = runTest {
        // In theme mode (theme == null)
        viewModel.navigateToThemes(channel = "ARD", theme = null)
        viewModel.updateSearchQuery("search text")

        val state = viewModel.viewState.value as MediaViewModel.ViewState.Themes
        assertEquals("search text", state.searchFilter.themesQuery)
        assertNull(state.searchFilter.titlesQuery)
    }

    @Test
    fun `updateSearchQuery updates titlesQuery in title mode`() = runTest {
        // In title mode (theme != null)
        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        viewModel.updateSearchQuery("search text")

        val state = viewModel.viewState.value as MediaViewModel.ViewState.Themes
        assertEquals("search text", state.searchFilter.titlesQuery)
        assertNull(state.searchFilter.themesQuery)
    }

    @Test
    fun `updateSearchQuery with empty string clears query`() = runTest {
        viewModel.navigateToThemes(channel = "ARD", theme = null)
        viewModel.updateSearchQuery("search text")
        viewModel.updateSearchQuery("")

        val state = viewModel.viewState.value as MediaViewModel.ViewState.Themes
        assertNull(state.searchFilter.themesQuery)
    }

    @Test
    fun `search filter queries are preserved independently`() = runTest {
        // Set themes query
        viewModel.navigateToThemes(channel = "ARD", theme = null)
        viewModel.updateSearchQuery("theme search")

        // Navigate to title mode and set title query
        viewModel.navigateToThemes(channel = "ARD", theme = "News")
        viewModel.updateSearchQuery("title search")

        val state = viewModel.viewState.value as MediaViewModel.ViewState.Themes
        assertEquals("theme search", state.searchFilter.themesQuery)
        assertEquals("title search", state.searchFilter.titlesQuery)
    }

    // =================================================================================
    // ViewState Flow Tests
    // =================================================================================

    @Test
    fun `viewState emits updates when navigating`() = runTest {
        viewModel.viewState.test {
            // Initial state
            val initial = awaitItem()
            assertTrue(initial is MediaViewModel.ViewState.Themes)

            // Navigate to channel
            viewModel.navigateToThemes(channel = "ARD")
            val afterChannel = awaitItem()
            assertEquals("ARD", (afterChannel as MediaViewModel.ViewState.Themes).channel)

            // Navigate to theme
            viewModel.navigateToThemes(channel = "ARD", theme = "News")
            val afterTheme = awaitItem()
            assertEquals("News", (afterTheme as MediaViewModel.ViewState.Themes).theme)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // =================================================================================
    // Helper Functions
    // =================================================================================

    private fun createTestEntry(
        id: Long = 1,
        channel: String = "ARD",
        theme: String = "News",
        title: String = "Test Title",
        timestamp: Long = System.currentTimeMillis() / 1000
    ): MediaEntry {
        return MediaEntry(
            id = id,
            channel = channel,
            theme = theme,
            title = title,
            date = "01.01.2024",
            time = "20:00:00",
            duration = "00:30:00",
            sizeMB = "500",
            description = "Test Description",
            url = "https://example.com/video.mp4",
            website = "https://example.com",
            subtitleUrl = "",
            smallUrl = "https://example.com/video_small.mp4",
            hdUrl = "https://example.com/video_hd.mp4",
            timestamp = timestamp,
            geo = "DE",
            isNew = false
        )
    }
}
