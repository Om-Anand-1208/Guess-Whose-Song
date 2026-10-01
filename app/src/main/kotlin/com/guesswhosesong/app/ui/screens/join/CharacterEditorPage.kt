package com.guesswhosesong.app.ui.screens.join

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guesswhosesong.app.ui.components.AvatarCharacter
import com.guesswhosesong.app.ui.components.AvatarOptions
import com.guesswhosesong.app.ui.components.avatarOption
import com.guesswhosesong.shared.models.AvatarCustomization
import com.guesswhosesong.shared.models.AvatarCustomizationCatalog

private enum class CharacterEditorTab(val title: String, val symbol: String) {
    SHAPE("Shape", "◇"),
    COLOR("Color", "●"),
    EYES("Eyes", "••"),
    MOUTH("Mouth", "⌣"),
    ACCESSORIES("Accessories", "♫")
}

@Composable
internal fun CharacterEditorPage(
    customization: AvatarCustomization,
    statusMessage: String?,
    statusIsError: Boolean,
    onCustomizationChange: (AvatarCustomization) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(CharacterEditorTab.SHAPE) }
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JoinWordmark(Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = onCancel) {
                androidx.compose.material3.Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = colorScheme.primary
                )
                Text("Back", color = colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        statusMessage?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                color = if (statusIsError) colorScheme.errorContainer else colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.18f))
            ) {
                Text(
                    message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = if (statusIsError) colorScheme.onErrorContainer else colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text("Character editor", style = MaterialTheme.typography.headlineLarge, color = colorScheme.onBackground)
        Text(
            "Choose a look for your character. You can always change it later.",
            modifier = Modifier.padding(top = 3.dp, bottom = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurfaceVariant
        )

        Surface(
            modifier = Modifier.fillMaxWidth().height(178.dp),
            color = colorScheme.surfaceVariant,
            shape = RoundedCornerShape(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                AvatarCharacter(
                    avatarId = customization.shapeId,
                    modifier = Modifier.size(154.dp),
                    customization = customization
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CharacterEditorTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                Column(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (selected) colorScheme.primaryContainer else colorScheme.surface)
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(17.dp)
                        )
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { selectedTab = tab }
                        )
                        .padding(vertical = 8.dp)
                        .semantics { contentDescription = "${tab.title} options" },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        tab.symbol,
                        color = if (selected) colorScheme.primary else colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        tab.title,
                        color = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Text(
            selectedTab.title,
            modifier = Modifier.padding(top = 13.dp),
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            when (selectedTab) {
                CharacterEditorTab.SHAPE -> "Pick a character shape."
                CharacterEditorTab.COLOR -> "Choose your character's color."
                CharacterEditorTab.EYES -> "Choose an expression for the eyes."
                CharacterEditorTab.MOUTH -> "Give your character a smile."
                CharacterEditorTab.ACCESSORIES -> "Add a finishing touch."
            },
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            choicesFor(selectedTab).chunked(4).forEach { rowOptions ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowOptions.forEach { option ->
                        val selected = selectedValue(selectedTab, customization) == option
                        val updated = updatedCustomization(selectedTab, customization, option)
                        Surface(
                            modifier = Modifier.weight(1f).aspectRatio(0.88f)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onCustomizationChange(updated) }
                                )
                                .semantics { contentDescription = "$option ${selectedTab.title.lowercase()}" },
                            color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                AvatarCharacter(
                                    avatarId = updated.shapeId,
                                    modifier = Modifier.size(62.dp),
                                    customization = updated
                                )
                                Text(
                                    choiceLabel(selectedTab, option),
                                    modifier = Modifier.padding(start = 3.dp, top = 2.dp, end = 3.dp),
                                    color = colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    repeat(4 - rowOptions.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.5.dp, colorScheme.outlineVariant)
            ) { Text("Cancel", color = colorScheme.onSurface, fontWeight = FontWeight.Bold) }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1.2f).height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
            ) { Text("Save character", fontWeight = FontWeight.Bold) }
        }
    }
}

private fun choicesFor(tab: CharacterEditorTab): List<String> = when (tab) {
    CharacterEditorTab.SHAPE -> AvatarCustomizationCatalog.shapeIds
    CharacterEditorTab.COLOR -> AvatarCustomizationCatalog.colorIds
    CharacterEditorTab.EYES -> AvatarCustomizationCatalog.eyesIds
    CharacterEditorTab.MOUTH -> AvatarCustomizationCatalog.mouthIds
    CharacterEditorTab.ACCESSORIES -> AvatarCustomizationCatalog.accessoryIds
}

private fun selectedValue(tab: CharacterEditorTab, customization: AvatarCustomization): String = when (tab) {
    CharacterEditorTab.SHAPE -> customization.shapeId
    CharacterEditorTab.COLOR -> customization.colorId
    CharacterEditorTab.EYES -> customization.eyesId
    CharacterEditorTab.MOUTH -> customization.mouthId
    CharacterEditorTab.ACCESSORIES -> customization.accessoryId
}

private fun updatedCustomization(
    tab: CharacterEditorTab,
    customization: AvatarCustomization,
    value: String
): AvatarCustomization = when (tab) {
    CharacterEditorTab.SHAPE -> customization.copy(shapeId = value)
    CharacterEditorTab.COLOR -> customization.copy(colorId = value)
    CharacterEditorTab.EYES -> customization.copy(eyesId = value)
    CharacterEditorTab.MOUTH -> customization.copy(mouthId = value)
    CharacterEditorTab.ACCESSORIES -> customization.copy(accessoryId = value)
}

private fun choiceLabel(tab: CharacterEditorTab, value: String): String = when (tab) {
    CharacterEditorTab.SHAPE -> avatarOption(value).name
    CharacterEditorTab.COLOR -> avatarOption(value).colorName
    CharacterEditorTab.EYES, CharacterEditorTab.MOUTH, CharacterEditorTab.ACCESSORIES ->
        value.replace('-', ' ').replaceFirstChar { it.uppercase() }
}
