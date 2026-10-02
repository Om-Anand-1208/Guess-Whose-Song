package com.guesswhosesong.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.guesswhosesong.app.ui.theme.AppearanceMode
import com.guesswhosesong.app.ui.theme.LocalAppearanceSettings

@Composable
fun AppearanceToggleButton(modifier: Modifier = Modifier) {
    val settings = LocalAppearanceSettings.current
    val isDark = settings.mode.resolvesDark(isSystemInDarkTheme())
    HeaderIconButton(
        icon = if (isDark) Icons.Filled.WbSunny else Icons.Filled.DarkMode,
        description = if (isDark) "Switch to light mode" else "Switch to dark mode",
        onClick = { settings.setMode(if (isDark) AppearanceMode.LIGHT else AppearanceMode.DARK) },
        modifier = modifier
    )
}

@Composable
fun SettingsHeaderButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    HeaderIconButton(
        icon = Icons.Filled.Settings,
        description = "Appearance and player settings",
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = description }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(23.dp)
        )
    }
}
