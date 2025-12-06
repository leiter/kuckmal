package com.mediathekview.web

import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import com.mediathekview.shared.model.Broadcaster
import com.mediathekview.shared.model.MediaEntry

fun main() {
    renderComposable(rootElementId = "root") {
        MediathekViewApp()
    }
}

@Composable
fun MediathekViewApp() {
    var selectedChannel by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    Style(AppStyleSheet)

    Div({ classes(AppStyleSheet.container) }) {
        // Header
        Header({ classes(AppStyleSheet.header) }) {
            H1 { Text("MediathekView") }
            Div({ classes(AppStyleSheet.searchBox) }) {
                TextInput(searchQuery) {
                    onInput { event ->
                        searchQuery = event.value
                    }
                    attr("placeholder", "Suchen...")
                    classes(AppStyleSheet.searchInput)
                }
            }
        }

        // Main content
        Div({ classes(AppStyleSheet.mainContent) }) {
            // Channel sidebar
            Aside({ classes(AppStyleSheet.sidebar) }) {
                H3 { Text("Sender") }
                Broadcaster.channelList.forEach { broadcaster ->
                    ChannelButton(
                        broadcaster = broadcaster,
                        isSelected = selectedChannel == broadcaster.name,
                        onClick = { selectedChannel = broadcaster.name }
                    )
                }
            }

            // Content area
            Main({ classes(AppStyleSheet.content) }) {
                if (selectedChannel != null) {
                    H2 { Text("$selectedChannel - Sendungen") }
                    P { Text("Inhalte werden geladen...") }
                } else {
                    Div({ classes(AppStyleSheet.welcomeMessage) }) {
                        H2 { Text("Willkommen bei MediathekView") }
                        P { Text("Bitte waehlen Sie einen Sender aus der Liste.") }
                    }
                }
            }
        }

        // Footer
        Footer({ classes(AppStyleSheet.footer) }) {
            Text("MediathekView for webOS TV - Powered by Kotlin/JS")
        }
    }
}

@Composable
fun ChannelButton(
    broadcaster: Broadcaster,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = Color("#${(broadcaster.brandColor and 0xFFFFFF).toString(16).padStart(6, '0')}")

    Button({
        onClick { onClick() }
        style {
            backgroundColor(if (isSelected) bgColor else Color.transparent)
            color(if (isSelected) Color.white else Color.black)
            border(2.px, LineStyle.Solid, bgColor)
            padding(8.px, 16.px)
            marginBottom(4.px)
            width(100.percent)
            property("cursor", "pointer")
            borderRadius(4.px)
            textAlign("left")
        }
    }) {
        Text(broadcaster.abbreviation.ifEmpty { broadcaster.name })
    }
}

object AppStyleSheet : StyleSheet() {
    val container by style {
        fontFamily("Arial", "sans-serif")
        property("min-height", "100vh")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        backgroundColor(Color("#1a1a1a"))
        color(Color.white)
    }

    val header by style {
        backgroundColor(Color("#2d2d2d"))
        padding(16.px)
        display(DisplayStyle.Flex)
        justifyContent(JustifyContent.SpaceBetween)
        alignItems(AlignItems.Center)
    }

    val searchBox by style {
        display(DisplayStyle.Flex)
    }

    val searchInput by style {
        padding(8.px, 16.px)
        fontSize(16.px)
        borderRadius(4.px)
        border(1.px, LineStyle.Solid, Color("#444"))
        backgroundColor(Color("#333"))
        color(Color.white)
        width(300.px)
    }

    val mainContent by style {
        display(DisplayStyle.Flex)
        property("flex", "1")
    }

    val sidebar by style {
        width(200.px)
        backgroundColor(Color("#2d2d2d"))
        padding(16.px)
        property("overflow-y", "auto")
    }

    val content by style {
        property("flex", "1")
        padding(24.px)
    }

    val welcomeMessage by style {
        textAlign("center")
        marginTop(50.px)
    }

    val footer by style {
        backgroundColor(Color("#2d2d2d"))
        padding(16.px)
        textAlign("center")
        fontSize(12.px)
        color(Color("#888"))
    }
}
