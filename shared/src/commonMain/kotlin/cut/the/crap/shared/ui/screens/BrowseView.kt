package cut.the.crap.shared.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cut.the.crap.shared.ui.components.SearchableTopAppBar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cut.the.crap.shared.model.Broadcaster
import cut.the.crap.shared.ui.Channel
import cut.the.crap.shared.ui.SampleData

/**
 * Cross-platform BrowseView for KMP (shared module)
 * Does not use Android-specific resources - all strings are inline
 */
@Composable
fun BrowseView(
    channels: List<Channel> = SampleData.sampleChannels,
    titles: List<String> = SampleData.sampleTitles,
    selectedChannel: Channel? = null,
    selectedTitle: String? = null,
    currentTheme: String = "Themen",
    isShowingTitles: Boolean = false,
    showBackButton: Boolean = isShowingTitles,
    hasMoreItems: Boolean = true,
    searchQuery: String = "",
    isSearching: Boolean = false,
    isSearchVisible: Boolean = false,
    onSearchQueryChanged: (String) -> Unit = {},
    onSearchVisibilityChanged: (Boolean) -> Unit = {},
    onChannelSelected: (Channel) -> Unit = {},
    onTitleSelected: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onTimePeriodClick: () -> Unit = {},
    onCheckUpdateClick: () -> Unit = {},
    onReinstallClick: () -> Unit = {},
    onSettingsClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Full-width top bar
        SearchableTopAppBar(
            title = if (isShowingTitles) "Titel: $currentTheme" else currentTheme,
            searchQuery = searchQuery,
            isSearching = isSearching,
            isSearchVisible = isSearchVisible,
            onSearchQueryChanged = onSearchQueryChanged,
            onSearchVisibilityChanged = onSearchVisibilityChanged,
            showBackButton = showBackButton,
            onBackClick = onBackClick,
            onTimePeriodClick = onTimePeriodClick,
            onCheckUpdateClick = onCheckUpdateClick,
            onReinstallClick = onReinstallClick,
            onSettingsClick = onSettingsClick,
            modifier = Modifier.fillMaxWidth()
        )

        // Content area: Channel list and titles side by side
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Left side: Channel list
            SharedChannelList(
                channels = channels,
                selectedChannel = selectedChannel,
                onChannelSelected = onChannelSelected,
                modifier = Modifier
                    .width(180.dp)
                    .fillMaxHeight()
            )

            // Right side: Titles list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = titles,
                    key = { it.hashCode() }
                ) { title ->
                    SharedTitleItem(
                        title = title,
                        isSelected = title == selectedTitle,
                        onClick = { onTitleSelected(title) }
                    )
                }

                if (hasMoreItems && titles.isNotEmpty()) {
                    item {
                        SharedMoreItem(onClick = onLoadMore)
                    }
                }
            }
        }
    }
}

@Composable
fun SharedChannelList(
    channels: List<Channel>,
    selectedChannel: Channel?,
    onChannelSelected: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(channels) { channel ->
                SharedChannelItem(
                    channel = channel,
                    isSelected = channel == selectedChannel,
                    onClick = { onChannelSelected(channel) }
                )
            }
        }
    }
}

@Composable
fun SharedChannelItem(
    channel: Channel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val broadcaster = Broadcaster.getByName(channel.name)
    val brandColorValue = (broadcaster?.brandColor ?: 0xFF808080L).toInt()
    val brandColor = Color(brandColorValue)
    val abbreviation = broadcaster?.abbreviation?.ifEmpty { channel.displayName } ?: channel.displayName

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = brandColor
            ),
            border = if (isSelected) {
                BorderStroke(3.dp, Color.White)
            } else null,
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 8.dp else 2.dp
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = abbreviation,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun SharedTitleItem(
    title: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val selectedBorderColor = Color(0xFF81B4D2)
    val selectedIndicatorColor = Color(0xFF81B4D2)
    val unselectedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, selectedBorderColor)
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .background(
                        if (isSelected) selectedIndicatorColor else unselectedIndicatorColor,
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) selectedBorderColor else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun SharedMoreItem(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A3A4A)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "++ mehr ++",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF81B4D2),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
