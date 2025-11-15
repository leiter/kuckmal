package com.mediathekview.android.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediathekview.android.compose.models.MediaItem
import com.mediathekview.android.compose.models.SampleData
import com.mediathekview.android.compose.ui.theme.MediathekViewTheme

@Composable
fun DetailView(
    mediaItem: MediaItem = SampleData.sampleMediaItem,
    onPlayClick: (Boolean) -> Unit = {},
    onDownloadClick: (Boolean) -> Unit = {}
) {
    var selectedQuality by remember { mutableStateOf(QualityOption.LOW) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // Pure black background like original
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp) // Reduced horizontal padding for better screen usage
    ) {
        // Channel Logo card - gray rounded rectangle like original
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp), // Significantly reduced to match original
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF6B7B8C) // Gray like original
            ),
            shape = RoundedCornerShape(12.dp) // Reduced from 16.dp for less rounded corners
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mediaItem.channel,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Theme section
        Text(
            text = "Thema",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp)) // Increased from 4.dp for better spacing
        Text(
            text = mediaItem.theme,
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF81B4D2), // Cyan like original
            fontWeight = FontWeight.Normal,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Title section
        Text(
            text = "Titel",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp)) // Increased from 4.dp for consistency
        Text(
            text = mediaItem.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Metadata - grid layout in gray rounded card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A2A) // Dark gray card for metadata
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                MetadataRow("Datum", mediaItem.date)
                Spacer(modifier = Modifier.height(8.dp))
                MetadataRow("Zeit", mediaItem.time)
                Spacer(modifier = Modifier.height(8.dp))
                MetadataRow("Dauer", mediaItem.duration)
                Spacer(modifier = Modifier.height(8.dp))
                MetadataRow("Größe", mediaItem.size)
            }
        }

        Spacer(modifier = Modifier.height(20.dp)) // Section spacing

        // Description section - in dark card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A) // Dark card
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Beschreibung",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF81B4D2), // Cyan
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(12.dp)) // Increased from 8.dp for better separation
                Text(
                    text = mediaItem.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp)) // Reduced from 24.dp for consistency

        // Quality section - in dark card with radio buttons
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    text = "Qualität",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(12.dp)) // Reduced from 16.dp for tighter grouping
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Hoch radio button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        RadioButton(
                            selected = selectedQuality == QualityOption.HIGH,
                            onClick = { selectedQuality = QualityOption.HIGH },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.White,
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hoch",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }

                    // Niedrig radio button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        RadioButton(
                            selected = selectedQuality == QualityOption.LOW,
                            onClick = { selectedQuality = QualityOption.LOW },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.White,
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Niedrig",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp)) // Reduced from 24.dp for tighter grouping

                // Play and Download buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // Reduced from 16.dp for compact layout
                ) {
                    // Play Button
                    Button(
                        onClick = { onPlayClick(selectedQuality == QualityOption.HIGH) },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0088AA) // Teal/cyan
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(48.dp), // Increased from 36.dp for better visibility
                            tint = Color.White
                        )
                    }

                    // Download Button
                    Button(
                        onClick = { onDownloadClick(selectedQuality == QualityOption.HIGH) },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3A4A5A) // Gray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(48.dp), // Increased from 36.dp for better visibility
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp)) // Final spacing - consistent with other sections
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
            color = Color(0xFFAAAAAA), // Lighter gray for better contrast on dark card
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
