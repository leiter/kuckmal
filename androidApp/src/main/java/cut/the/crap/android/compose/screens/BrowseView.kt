package cut.the.crap.android.compose.screens

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
import cut.the.crap.shared.ui.components.SearchableTopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cut.the.crap.android.R
import cut.the.crap.shared.ui.Channel
import cut.the.crap.shared.ui.SampleData
import cut.the.crap.android.compose.ui.theme.KuckmalTheme
import cut.the.crap.android.model.Broadcaster

@Composable
fun BrowseView(
    channels: List<Channel> = SampleData.sampleChannels,
    titles: List<String> = SampleData.sampleTitles,
    selectedChannel: Channel? = SampleData.sampleChannels.find { it.name == "PHOENIX" },
    selectedTitle: String? = null,
    currentTheme: String = "1000 Inseln im Sankt-Lorenz-Strom",
    isShowingTitles: Boolean = false,
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
    onTimePeriodClick: () -> Unit = {},
    onCheckUpdateClick: () -> Unit = {},
    onReinstallClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Full-width top bar
        SearchableTopAppBar(
            title = if (isShowingTitles) stringResource(R.string.titles_for_theme, currentTheme) else currentTheme,
            searchQuery = searchQuery,
            isSearching = isSearching,
            isSearchVisible = isSearchVisible,
            onSearchQueryChanged = onSearchQueryChanged,
            onSearchVisibilityChanged = onSearchVisibilityChanged,
            onTimePeriodClick = onTimePeriodClick,
            onCheckUpdateClick = onCheckUpdateClick,
            onReinstallClick = onReinstallClick,
            modifier = Modifier.fillMaxWidth()
        )

        // Content area: Channel list and titles side by side
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Left side: Channel list
            ChannelList(
                channels = channels,
                selectedChannel = selectedChannel,
                onChannelSelected = onChannelSelected,
                modifier = Modifier
                    .width(160.dp)
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
                    TitleItem(
                        title = title,
                        isSelected = title == selectedTitle,
                        onClick = { onTitleSelected(title) },
                        modifier = Modifier.animateItem()
                    )
                }

                // Show "++ more ++" item if there are more items to load
                if (hasMoreItems && titles.isNotEmpty()) {
                    item {
                        MoreItem(onClick = onLoadMore)
                    }
                }
            }
        }
    }
}

@Composable
fun MoreItem(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A3A4A) // Slightly different background
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
                text = stringResource(R.string.load_more),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF81B4D2), // Cyan color to stand out
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ChannelList(
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
                ChannelItem(
                    channel = channel,
                    isSelected = channel == selectedChannel,
                    onClick = { onChannelSelected(channel) }
                )
            }
        }
    }
}

/**
 * Channel item with two display modes controlled by [Broadcaster.useFallbackDisplay]:
 * - Logo mode (useFallbackDisplay = false): Shows the channel logo icon
 * - Fallback mode (useFallbackDisplay = true): Shows brand color background with abbreviation
 *   (KuckmalWeb style)
 *
 * Uses the official brand colors and icons from [Broadcaster] model.
 */
@Composable
fun ChannelItem(
    channel: Channel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Get broadcaster data
    val broadcaster = Broadcaster.getByName(channel.name)
    val useFallback = Broadcaster.useFallbackDisplay

    // Convert Long ARGB to Compose Color using the Int constructor
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
        if (useFallback) {
            // Fallback mode: Brand color background with abbreviation text
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = brandColor
                ),
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(3.dp, Color.White)
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
        } else {
            // Logo mode: Show channel logo icon
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    }
                ),
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(2.dp, brandColor.copy(alpha = 0.5f))
                } else null,
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isSelected) 4.dp else 1.dp
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    broadcaster?.iconRes?.let { iconRes ->
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = iconRes),
                            contentDescription = channel.displayName,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } ?: Text(
                        text = channel.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = brandColor
                    )
                }
            }
        }
    }
}

/**
 * Title/Theme item with selection state support.
 * Shows visual feedback when selected (highlighted border and accent color indicator).
 */
@Composable
fun TitleItem(
    title: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    // Selection colors
    val selectedBorderColor = Color(0xFF81B4D2) // Cyan accent
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
            androidx.compose.foundation.BorderStroke(2.dp, selectedBorderColor)
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
            // Vertical bar indicator - highlighted when selected
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
                color = if (isSelected) {
                    selectedBorderColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 16.sp
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 600)
@Composable
fun BrowseViewPreview() {
    KuckmalTheme {
        BrowseView(
            channels = SampleData.sampleChannels,
            selectedChannel = SampleData.sampleChannels.find { it.name == "ZDF" },
            selectedTitle = SampleData.sampleTitles.getOrNull(1) // Select second item
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun TitleItemPreview() {
    KuckmalTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TitleItem(
                title = "Unselected Item",
                isSelected = false,
                onClick = {}
            )
            TitleItem(
                title = "Selected Item",
                isSelected = true,
                onClick = {}
            )
        }
    }
}
