package com.example.data.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class ReelPage(
    val id: String,
    val pageNumber: Int,
    val content: String,
    val docTitle: String = "",
    val isTitlePage: Boolean = false,
    val tags: List<String> = emptyList()
)

data class ReelDocument(
    val title: String,
    val sourceUrl: String = "",
    val pages: List<ReelPage> = emptyList()
)

data class ReelAudioTrack(
    val id: String,
    val title: String,
    val streamUrl: String,
    val originalDriveUrl: String = "",
    val isPreset: Boolean = false
)

enum class ReelBackgroundTheme(val displayName: String) {
    DYNAMIC_GRADIENT("Dynamic Gradients"),
    DEEP_OBSIDIAN("Deep Obsidian"),
    ROYAL_MIDNIGHT("Royal Midnight"),
    FOREST_EMERALD("Forest Emerald"),
    SUNSET_EMBER("Sunset Ember"),
    AURORA_CYAN("Aurora Cyan")
}

enum class ReelFilterMode(val label: String) {
    ALL("All"),
    UNSEEN("Unseen"),
    SEEN("Seen")
}

data class ReelDocOption(
    val id: String,
    val title: String,
    val sourceUrl: String = "",
    val isPreset: Boolean = false
)

data class ReelsSettingsState(
    val docUrl: String = "",
    val musicEnabled: Boolean = true,
    val randomTrackPerPage: Boolean = true,
    val volume: Float = 0.65f,
    val fontSizeSp: Float = 22f,
    val highlightNumbers: Boolean = true,
    val numberHighlightColorHex: String = "#FFB703", // vibrant amber gold
    val theme: ReelBackgroundTheme = ReelBackgroundTheme.DYNAMIC_GRADIENT,
    val autoScrollSeconds: Int = 0, // 0 = off, 5, 8, 12, 16, 20
    val isFullscreen: Boolean = false,
    val filterMode: ReelFilterMode = ReelFilterMode.ALL
)

object ReelGradients {
    val dynamicPalettes: List<Brush> = listOf(
        // 1. Midnight Royal Indigo
        Brush.verticalGradient(
            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF0F172A))
        ),
        // 2. Sunset Crimson Velvet
        Brush.verticalGradient(
            listOf(Color(0xFF1F1020), Color(0xFF4C0519), Color(0xFF1B0711))
        ),
        // 3. Deep Forest Emerald
        Brush.verticalGradient(
            listOf(Color(0xFF041F1A), Color(0xFF064E3B), Color(0xFF021B14))
        ),
        // 4. Oceanic Abyssal Blue
        Brush.verticalGradient(
            listOf(Color(0xFF081B2E), Color(0xFF1E3A8A), Color(0xFF051322))
        ),
        // 5. Twilight Amethyst
        Brush.verticalGradient(
            listOf(Color(0xFF190B2E), Color(0xFF3B0764), Color(0xFF140824))
        ),
        // 6. Cyber Teal Aurora
        Brush.verticalGradient(
            listOf(Color(0xFF031E23), Color(0xFF115E59), Color(0xFF02161A))
        ),
        // 7. Warm Amber Charcoal
        Brush.verticalGradient(
            listOf(Color(0xFF201308), Color(0xFF451A03), Color(0xFF160D05))
        ),
        // 8. Dark Titanium Slate
        Brush.verticalGradient(
            listOf(Color(0xFF18181B), Color(0xFF27272A), Color(0xFF09090B))
        )
    )

    fun getGradientForTheme(theme: ReelBackgroundTheme, pageIndex: Int): Brush {
        return when (theme) {
            ReelBackgroundTheme.DYNAMIC_GRADIENT -> {
                val index = (pageIndex.coerceAtLeast(0)) % dynamicPalettes.size
                dynamicPalettes[index]
            }
            ReelBackgroundTheme.DEEP_OBSIDIAN -> {
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1117), Color(0xFF161B22), Color(0xFF090D12))
                )
            }
            ReelBackgroundTheme.ROYAL_MIDNIGHT -> {
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF0A0F1D))
                )
            }
            ReelBackgroundTheme.FOREST_EMERALD -> {
                Brush.verticalGradient(
                    listOf(Color(0xFF031C16), Color(0xFF064E3B), Color(0xFF02140F))
                )
            }
            ReelBackgroundTheme.SUNSET_EMBER -> {
                Brush.verticalGradient(
                    listOf(Color(0xFF210F1D), Color(0xFF4C0519), Color(0xFF1A0A16))
                )
            }
            ReelBackgroundTheme.AURORA_CYAN -> {
                Brush.verticalGradient(
                    listOf(Color(0xFF041C22), Color(0xFF115E59), Color(0xFF021317))
                )
            }
        }
    }
}
