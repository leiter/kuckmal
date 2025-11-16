package com.mediathekview.android.compose.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediathekview.android.compose.models.Channel
import com.mediathekview.android.compose.models.SampleData
import com.mediathekview.android.compose.ui.theme.MediathekViewTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseView(
    channels: List<Channel> = SampleData.sampleChannels,
    titles: List<String> = SampleData.sampleTitles,
    selectedChannel: Channel? = SampleData.sampleChannels[9], // phoenix
    currentTheme: String = "1000 Inseln im Sankt-Lorenz-Strom",
    isShowingTitles: Boolean = false, // true when showing titles within a theme, false when showing themes
    hasMoreItems: Boolean = true, // whether there are more items to load
    searchQuery: String = "", // current search query
    onSearchQueryChanged: (String) -> Unit = {}, // callback for search query changes
    onChannelSelected: (Channel) -> Unit = {},
    onTitleSelected: (String) -> Unit = {},
    onLoadMore: () -> Unit = {}, // callback to load more items
    onMenuClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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

        // Right side: Titles list with search
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // Title header with menu
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        // Show "Titel: theme name" when showing titles, otherwise just the label
                        text = if (isShowingTitles) "Titel: $currentTheme" else currentTheme,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF81B4D2), // Cyan/blue matching theme
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search bar
            var searchText by remember { mutableStateOf("") }
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Titel suchen...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Titles list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(titles) { title ->
                    TitleItem(
                        title = title,
                        onClick = { onTitleSelected(title) }
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
                text = "++ more ++",
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

@Composable
fun ChannelItem(
    channel: Channel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Channel-specific colors to simulate logos
    val channelColor = when (channel.name) {
        "3sat" -> Color(0xFFE6004C) // Red
        "ARD" -> Color(0xFF0066CC) // Blue
        "arte" -> Color(0xFFFF6600) // Orange
        "BR" -> Color(0xFF0099CC) // Cyan
        "hr" -> Color(0xFF0066CC) // Blue
        "KIKA" -> Color(0xFFFFCC00) // Yellow
        "mdr" -> Color(0xFF0099CC) // Cyan
        "NDR" -> Color(0xFF0066CC) // Blue
        "ORF" -> Color(0xFFCC0000) // Red
        "phoenix" -> Color(0xFF006699) // Teal
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Channel logo box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                }
            ),
            border = if (isSelected) {
                androidx.compose.foundation.BorderStroke(2.dp, channelColor.copy(alpha = 0.5f))
            } else null
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = channelColor
                )
            }
        }
    }
}

@Composable
fun TitleItem(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical bar indicator
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 600)
@Composable
fun BrowseViewPreview() {
    MediathekViewTheme {
        BrowseView(
            channels = SampleData.sampleChannels
        )
    }
}
