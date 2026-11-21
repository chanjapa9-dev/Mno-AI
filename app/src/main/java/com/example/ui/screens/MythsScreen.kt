package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ImageStorageHelper
import com.example.data.NorthMythEntity
import com.example.ui.MythGenUiState
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.DarkEmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MintAccent
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextOnGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MythsScreen(
    mythState: MythGenUiState,
    savedMyths: List<NorthMythEntity>,
    illustratingMythId: Long?,
    mythImageError: String?,
    onGenerateMyth: (String) -> Unit,
    onIllustrateMyth: (NorthMythEntity) -> Unit,
    onDeleteMyth: (Long) -> Unit,
    onSendMythToChat: (NorthMythEntity) -> Unit,
    onDismissImageError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var topicInput by rememberSaveable { mutableStateOf("") }
    val presets = listOf(
        "Curupira no ramal",
        "Boto-Cor-de-Rosa na festa",
        "Matinta Pereira no quintal",
        "Cobra Grande no Rio Negro",
        "Mapinguari na mata fechada",
        "Iara no encontro das águas",
        "Visagem no igarapé à noite"
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.myths_header_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.myths_header_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CarbonSurface,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (topicInput == preset) DarkEmeraldContainer else CarbonElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (topicInput == preset) EmeraldPrimary else BorderSubtle
                                ),
                                modifier = Modifier
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { topicInput = preset }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (topicInput == preset) MintAccent else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.myths_prompt_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        },
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = CarbonElevated,
                            unfocusedContainerColor = CarbonElevated,
                            cursorColor = EmeraldPrimary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("myth_topic_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val isLoading = mythState is MythGenUiState.Loading
                    Button(
                        onClick = { onGenerateMyth(topicInput) },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = TextOnGreen
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_myth_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                color = TextOnGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Puxando a lenda, mno…", style = MaterialTheme.typography.labelLarge)
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.myths_generate_button),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        if (mythState is MythGenUiState.Error) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ErrorContainerDark,
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = mythState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        item {
            AnimatedVisibility(visible = mythImageError != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ErrorContainerDark,
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = mythImageError.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissImageError) {
                            Text("Fechar", color = TextPrimary)
                        }
                    }
                }
            }
        }

        if (savedMyths.isEmpty() && mythState is MythGenUiState.Idle) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CarbonSurface,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Escolhe um mito ou joga tua ideia ali em cima, mno.",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "O Mano conta a história curta do jeitão do Norte, sem enrolação, e ainda gera a arte se tu quiser.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(
                items = savedMyths,
                key = { it.id }
            ) { myth ->
                MythCard(
                    myth = myth,
                    isIllustrating = illustratingMythId == myth.id,
                    onIllustrate = { onIllustrateMyth(myth) },
                    onDelete = { onDeleteMyth(myth.id) },
                    onSendToChat = { onSendMythToChat(myth) }
                )
            }
        }
    }
}

@Composable
private fun MythCard(
    myth: NorthMythEntity,
    isIllustrating: Boolean,
    onIllustrate: () -> Unit,
    onDelete: () -> Unit,
    onSendToChat: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CarbonSurface,
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkEmeraldContainer,
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = myth.entityName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MintAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Apagar lenda",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = myth.title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = myth.setting,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }

            if (!myth.imagePath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = File(myth.imagePath),
                    contentDescription = "Ilustração de ${myth.entityName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = myth.story,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CarbonElevated,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "“${myth.catchphrase}”",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (myth.imagePath.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onIllustrate,
                        enabled = !isIllustrating,
                        border = BorderStroke(1.dp, EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("illustrate_myth_button")
                    ) {
                        if (isIllustrating) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Criando…", color = EmeraldPrimary)
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.myths_illustrate_button),
                                color = EmeraldPrimary,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val ok = ImageStorageHelper.saveImageToGallery(context, myth.imagePath)
                                val msg = if (ok) {
                                    context.getString(R.string.saved_gallery_feedback)
                                } else {
                                    context.getString(R.string.error_save_image)
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salvar Arte", color = TextPrimary)
                    }
                }

                IconButton(
                    onClick = {
                        copyToClipboard(
                            context,
                            myth.title,
                            "${myth.title}\n\n${myth.story}\n\n${myth.catchphrase}"
                        )
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(CarbonElevated, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copiar lenda",
                        tint = TextPrimary
                    )
                }

                IconButton(
                    onClick = onSendToChat,
                    modifier = Modifier
                        .size(48.dp)
                        .background(CarbonElevated, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Continuar no chat",
                        tint = MintAccent
                    )
                }
            }
        }
    }
}
