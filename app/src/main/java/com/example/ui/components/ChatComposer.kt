package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import java.io.File

@Composable
fun ChatComposerBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isImageMode: Boolean,
    isListening: Boolean,
    isSendingText: Boolean,
    isGeneratingImage: Boolean,
    attachedImagePath: String?,
    activeError: String?,
    onDismissError: () -> Unit,
    onStopVoice: () -> Unit,
    onClearAttachedImage: () -> Unit,
    onPickImage: () -> Unit,
    onToggleImageMode: () -> Unit,
    onToggleVoiceInput: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val canSend = (inputText.isNotBlank() || attachedImagePath != null) &&
        !isSendingText && !isGeneratingImage

    Column(modifier = modifier.fillMaxWidth()) {
        // Error Banner (Chat or Voice)
        AnimatedVisibility(visible = activeError != null) {
            Surface(
                color = colorScheme.errorContainer,
                border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = activeError.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismissError) {
                        Text(
                            text = stringResource(R.string.dismiss_label),
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Active Voice Listening Indicator
        AnimatedVisibility(visible = isListening) {
            Surface(
                color = colorScheme.primaryContainer,
                border = BorderStroke(1.dp, colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.voice_listening_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.tertiary
                        )
                    }
                    TextButton(onClick = onStopVoice) {
                        Text(
                            text = stringResource(R.string.stop_label),
                            color = colorScheme.primary
                        )
                    }
                }
            }
        }

        // Attached Image Preview
        AnimatedVisibility(visible = attachedImagePath != null) {
            if (attachedImagePath != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.surfaceVariant)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    AsyncImage(
                        model = File(attachedImagePath),
                        contentDescription = stringResource(R.string.attached_photo_label),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.attached_photo_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.primary
                        )
                        Text(
                            text = stringResource(
                                if (isImageMode) {
                                    R.string.attached_photo_edit_hint
                                } else {
                                    R.string.attached_photo_analyze_hint
                                }
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onClearAttachedImage,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear_attachment_cd),
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Material 3 Bottom Input Dock
        Surface(
            color = colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, colorScheme.outline),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Primary Input Row: spacious OutlinedTextField + Send button
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        placeholder = {
                            Text(
                                text = stringResource(
                                    if (isImageMode) R.string.chat_placeholder_image else R.string.chat_placeholder_text
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outline,
                            focusedContainerColor = colorScheme.surfaceVariant,
                            unfocusedContainerColor = colorScheme.surfaceVariant,
                            cursorColor = colorScheme.primary,
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (canSend) onSend()
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = if (canSend) colorScheme.primary else colorScheme.secondaryContainer,
                        modifier = Modifier
                            .padding(bottom = 2.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(enabled = canSend, onClick = onSend)
                            .testTag("send_message_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSendingText || isGeneratingImage) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.send_message_cd),
                                    tint = if (canSend) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Tool Row: Photo Picker, Image Generation Mode Chip, Voice Dictation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = attachedImagePath != null,
                        onClick = onPickImage,
                        label = {
                            Text(
                                text = stringResource(R.string.attach_image_cd),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colorScheme.surfaceVariant,
                            labelColor = colorScheme.onSurfaceVariant,
                            iconColor = colorScheme.onSurfaceVariant,
                            selectedContainerColor = colorScheme.primaryContainer,
                            selectedLabelColor = colorScheme.primary,
                            selectedLeadingIconColor = colorScheme.primary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = attachedImagePath != null,
                            borderColor = colorScheme.outline,
                            selectedBorderColor = colorScheme.primary.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("attach_image_button")
                    )

                    FilterChip(
                        selected = isImageMode,
                        onClick = onToggleImageMode,
                        label = {
                            Text(
                                text = stringResource(R.string.mode_image_label),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colorScheme.surfaceVariant,
                            labelColor = colorScheme.onSurfaceVariant,
                            iconColor = colorScheme.onSurfaceVariant,
                            selectedContainerColor = colorScheme.primary,
                            selectedLabelColor = colorScheme.onPrimary,
                            selectedLeadingIconColor = colorScheme.onPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isImageMode,
                            borderColor = colorScheme.outline,
                            selectedBorderColor = colorScheme.primary
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_image_mode_button")
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onToggleVoiceInput,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("voice_input_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Outlined.Mic,
                            contentDescription = stringResource(
                                if (isListening) R.string.voice_stop_cd else R.string.voice_input_cd
                            ),
                            tint = if (isListening) colorScheme.primary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratingImageCard(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            CircularProgressIndicator(
                color = colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.generating_image_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.generating_image_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
