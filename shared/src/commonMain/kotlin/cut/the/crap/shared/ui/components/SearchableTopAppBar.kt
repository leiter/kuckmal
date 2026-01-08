package cut.the.crap.shared.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TitleColor = Color(0xFF81B4D2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableTopAppBar(
    title: String,
    searchQuery: String,
    isSearching: Boolean,
    isSearchVisible: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onSearchVisibilityChanged: (Boolean) -> Unit,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onTimePeriodClick: () -> Unit = {},
    onCheckUpdateClick: () -> Unit = {},
    onReinstallClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchVisible) {
        if (isSearchVisible) {
            searchFocusRequester.requestFocus()
        }
    }

    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                // Placeholder to keep title alignment consistent
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        title = {
            AnimatedContent(
                targetState = isSearchVisible,
                transitionSpec = {
                    val enterAnim = slideInVertically(
                        initialOffsetY = { if (targetState) -it / 2 else it / 2 }
                    ) + fadeIn()
                    val exitAnim = slideOutVertically(
                        targetOffsetY = { if (targetState) it / 2 else -it / 2 }
                    ) + fadeOut()
                    enterAnim.togetherWith(exitAnim).using(SizeTransform(clip = false))
                },
                label = "SearchMorphAnimation"
            ) { searchActive ->
                if (searchActive) {
                    SearchActiveTitleContent(
                        title = title,
                        searchQuery = searchQuery,
                        isSearching = isSearching,
                        onSearchQueryChanged = onSearchQueryChanged,
                        focusRequester = searchFocusRequester
                    )
                } else {
                    NormalTitleContent(title = title)
                }
            }
        },
        actions = {
            // Search/Close button - always present, icon morphs based on state
            IconButton(
                onClick = {
                    if (isSearchVisible) {
                        if (searchQuery.isNotEmpty()) {
                            onSearchQueryChanged("")
                        } else {
                            onSearchVisibilityChanged(false)
                        }
                    } else {
                        onSearchVisibilityChanged(true)
                    }
                }
            ) {
                Icon(
                    imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearchVisible) "Close search" else "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Zeitraum") },
                        onClick = {
                            showOverflowMenu = false
                            onTimePeriodClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Update prüfen") },
                        onClick = {
                            showOverflowMenu = false
                            onCheckUpdateClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Filmliste neu laden") },
                        onClick = {
                            showOverflowMenu = false
                            onReinstallClick()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    )
}

@Composable
private fun NormalTitleContent(title: String) {
    // Title as hint in container (no outline)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchActiveTitleContent(
    title: String,
    searchQuery: String,
    isSearching: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    focusRequester: FocusRequester
) {
    // TextInputLayout-style: floating label above input (no outline)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                // Floating label (small title)
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Input row
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Suchen...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
    }
}
