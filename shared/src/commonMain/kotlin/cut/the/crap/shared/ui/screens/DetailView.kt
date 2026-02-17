package cut.the.crap.shared.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import kuckmal.shared.generated.resources.*
import kuckmal.shared.generated.resources.Res
import cut.the.crap.shared.ui.MediaItem
import cut.the.crap.shared.ui.SampleData
import cut.the.crap.shared.ui.util.Orientation
import cut.the.crap.shared.ui.util.rememberOrientation

@Composable
fun DetailView(
    mediaItem: MediaItem = SampleData.sampleMediaItem,
    onPlayClick: (Boolean) -> Unit = {},
    onDownloadClick: (Boolean) -> Unit = {}
) {
    var selectedQuality by remember { mutableStateOf(QualityOption.LOW) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    val orientation = rememberOrientation()

    when (orientation) {
        Orientation.Portrait -> DetailViewPortrait(
            mediaItem = mediaItem,
            selectedQuality = selectedQuality,
            onQualityChange = { selectedQuality = it },
            isDescriptionExpanded = isDescriptionExpanded,
            onDescriptionToggle = { isDescriptionExpanded = !isDescriptionExpanded },
            onPlayClick = onPlayClick,
            onDownloadClick = onDownloadClick
        )
        Orientation.Landscape -> DetailViewLandscape(
            mediaItem = mediaItem,
            selectedQuality = selectedQuality,
            onQualityChange = { selectedQuality = it },
            isDescriptionExpanded = isDescriptionExpanded,
            onDescriptionToggle = { isDescriptionExpanded = !isDescriptionExpanded },
            onPlayClick = onPlayClick,
            onDownloadClick = onDownloadClick
        )
    }
}

@Composable
private fun DetailViewPortrait(
    mediaItem: MediaItem,
    selectedQuality: QualityOption,
    onQualityChange: (QualityOption) -> Unit,
    isDescriptionExpanded: Boolean,
    onDescriptionToggle: () -> Unit,
    onPlayClick: (Boolean) -> Unit,
    onDownloadClick: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            ChannelLogoCard(channelName = mediaItem.channel)
            Spacer(modifier = Modifier.height(20.dp))
            ThemeSection(theme = mediaItem.theme)
            Spacer(modifier = Modifier.height(20.dp))
            TitleSection(title = mediaItem.title)
            Spacer(modifier = Modifier.height(12.dp))
            GeoRestrictionBanner(mediaItem = mediaItem)
            Spacer(modifier = Modifier.height(20.dp))
            MetadataCard(mediaItem = mediaItem)
            Spacer(modifier = Modifier.height(20.dp))
            DescriptionCard(
                description = mediaItem.description,
                isExpanded = isDescriptionExpanded,
                onToggle = onDescriptionToggle
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Sticky Quality section at bottom
        PlaybackControlsCard(
            selectedQuality = selectedQuality,
            onQualityChange = onQualityChange,
            onPlayClick = { onPlayClick(selectedQuality == QualityOption.HIGH) },
            onDownloadClick = { onDownloadClick(selectedQuality == QualityOption.HIGH) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun DetailViewLandscape(
    mediaItem: MediaItem,
    selectedQuality: QualityOption,
    onQualityChange: (QualityOption) -> Unit,
    isDescriptionExpanded: Boolean,
    onDescriptionToggle: () -> Unit,
    onPlayClick: (Boolean) -> Unit,
    onDownloadClick: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // Left Panel (40%) - Info section
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .background(Color(0xFF1A1A1A))
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            ChannelLogoCard(
                channelName = mediaItem.channel,
                modifier = Modifier.height(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            ThemeSection(theme = mediaItem.theme)
            Spacer(modifier = Modifier.height(16.dp))
            TitleSection(title = mediaItem.title)
            Spacer(modifier = Modifier.height(12.dp))
            GeoRestrictionBanner(mediaItem = mediaItem)
            Spacer(modifier = Modifier.height(16.dp))
            MetadataCard(mediaItem = mediaItem)
        }

        // Right Panel (60%) - Description and actions
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
        ) {
            // Scrollable description
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                DescriptionCard(
                    description = mediaItem.description,
                    isExpanded = isDescriptionExpanded,
                    onToggle = onDescriptionToggle
                )
            }

            // Fixed bottom controls
            PlaybackControlsCard(
                selectedQuality = selectedQuality,
                onQualityChange = onQualityChange,
                onPlayClick = { onPlayClick(selectedQuality == QualityOption.HIGH) },
                onDownloadClick = { onDownloadClick(selectedQuality == QualityOption.HIGH) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

// Shared Components

@Composable
private fun ChannelLogoCard(
    channelName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (modifier == Modifier) Modifier.height(100.dp) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF6B7B8C)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = channelName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ThemeSection(theme: String) {
    Text(
        text = stringResource(Res.string.theme_label),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = theme,
        style = MaterialTheme.typography.headlineSmall,
        color = Color(0xFF81B4D2),
        fontWeight = FontWeight.Normal,
        lineHeight = 32.sp
    )
}

@Composable
private fun TitleSection(title: String) {
    Text(
        text = stringResource(Res.string.title_label),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun GeoRestrictionBanner(mediaItem: MediaItem) {
    if (mediaItem.hasGeoRestriction()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0x33FFA500)  // Orange with transparency
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFFA500)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mediaItem.getGeoRestrictionText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFA500),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MetadataCard(mediaItem: MediaItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            MetadataRow(stringResource(Res.string.date_label), mediaItem.date)
            Spacer(modifier = Modifier.height(8.dp))
            MetadataRow(stringResource(Res.string.time_label), mediaItem.time)
            Spacer(modifier = Modifier.height(8.dp))
            MetadataRow(stringResource(Res.string.duration_label), mediaItem.duration)
            Spacer(modifier = Modifier.height(8.dp))
            MetadataRow(stringResource(Res.string.size_label), mediaItem.size)
        }
    }
}

@Composable
private fun DescriptionCard(
    description: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(animationSpec = tween(300))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.description_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF81B4D2),
                    fontWeight = FontWeight.Normal
                )

                val rotationAngle by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    animationSpec = tween(300),
                    label = "rotation"
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(Res.string.collapse) else stringResource(Res.string.expand),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle),
                    tint = Color(0xFF81B4D2)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaybackControlsCard(
    selectedQuality: QualityOption,
    onQualityChange: (QualityOption) -> Unit,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.quality_label),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = selectedQuality == QualityOption.HIGH,
                        onClick = { onQualityChange(QualityOption.HIGH) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color.White,
                            unselectedColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.quality_high),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = selectedQuality == QualityOption.LOW,
                        onClick = { onQualityChange(QualityOption.LOW) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color.White,
                            unselectedColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.quality_low),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0088AA)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(Res.string.cd_play),
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }

                Button(
                    onClick = onDownloadClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3A4A5A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = stringResource(Res.string.cd_download),
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MetadataRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFAAAAAA),
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

enum class QualityOption {
    HIGH, LOW
}
