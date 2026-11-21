package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ChatMessageEntity
import com.example.data.ImageStorageHelper
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CodeBackground
import com.example.ui.theme.DarkEmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintAccent
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File

private sealed interface MessageSegment {
    data class TextBlock(val text: String) : MessageSegment
    data class CodeBlock(val language: String, val code: String) : MessageSegment
}

private fun parseMessageSegments(raw: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    val regex = Regex("```([a-zA-Z0-9_+-]*)\\n([\\s\\S]*?)```")
    var lastIndex = 0

    for (match in regex.findAll(raw)) {
        if (match.range.first > lastIndex) {
            val preceding = raw.substring(lastIndex, match.range.first).trim()
            if (preceding.isNotEmpty()) {
                segments.add(MessageSegment.TextBlock(preceding))
            }
        }
        val lang = match.groupValues[1].ifBlank { "code" }.uppercase()
        val code = match.groupValues[2].trimEnd()
        segments.add(MessageSegment.CodeBlock(lang, code))
        lastIndex = match.range.last + 1
    }

    if (lastIndex < raw.length) {
        val tail = raw.substring(lastIndex).trim()
        if (tail.isNotEmpty()) {
            segments.add(MessageSegment.TextBlock(tail))
        }
    }

    if (segments.isEmpty() && raw.isNotBlank()) {
        segments.add(MessageSegment.TextBlock(raw))
    }
    return segments
}

private fun formatInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = JetBrainsMonoFamily,
                                color = MintAccent,
                                background = CodeBackground
                            )
                        ) {
                            append(" ${text.substring(i + 1, end)} ")
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("_", i) && (i == 0 || text[i - 1].isWhitespace()) -> {
                    val end = text.indexOf("_", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = TextSecondary)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, context.getString(R.string.copied_feedback), Toast.LENGTH_SHORT).show()
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var zoomImagePath by remember { mutableStateOf<String?>(null) }

    val bubbleAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = bubbleAlignment
    ) {
        if (message.isUser) {
            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                color = DarkEmeraldContainer,
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                modifier = Modifier.widthIn(max = 310.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (!message.imagePath.isNullOrBlank()) {
                        AsyncImage(
                            model = File(message.imagePath),
                            contentDescription = "Imagem enviada",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { zoomImagePath = message.imagePath }
                        )
                        if (message.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        } else {
            val containerColor = if (message.isError) ErrorContainerDark else CarbonSurface
            val borderColor = if (message.isError) ErrorRed.copy(alpha = 0.6f) else BorderSubtle

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.widthIn(max = 560.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_mano_avatar_1795262249289),
                    contentDescription = stringResource(R.string.avatar_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.6f), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                    color = containerColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isStreaming) "Mano AI • digitando…" else "Mano AI",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (message.isError) ErrorRed else EmeraldPrimary
                            )
                            if (!isStreaming && message.text.isNotBlank() && !message.isError) {
                                TextButton(
                                    onClick = { copyToClipboard(context, "Mano AI", message.text) },
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("copy_reply_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = stringResource(R.string.copy_text),
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.copy_text),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        val segments = remember(message.text) { parseMessageSegments(message.text) }
                        segments.forEachIndexed { index, segment ->
                            if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                            when (segment) {
                                is MessageSegment.TextBlock -> {
                                    Text(
                                        text = formatInlineMarkdown(segment.text),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (message.isError) ErrorRed else TextPrimary
                                    )
                                }
                                is MessageSegment.CodeBlock -> {
                                    CodeSnippetCard(
                                        language = segment.language,
                                        code = segment.code,
                                        onCopy = { copyToClipboard(context, segment.language, segment.code) }
                                    )
                                }
                            }
                        }

                        if (!message.imagePath.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            AsyncImage(
                                model = File(message.imagePath),
                                contentDescription = "Imagem gerada pelo Mano AI",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                    .clickable { zoomImagePath = message.imagePath }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CarbonElevated,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            scope.launch {
                                                val ok = ImageStorageHelper.saveImageToGallery(
                                                    context,
                                                    message.imagePath
                                                )
                                                val msg = if (ok) {
                                                    context.getString(R.string.saved_gallery_feedback)
                                                } else {
                                                    context.getString(R.string.error_save_image)
                                                }
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .testTag("save_generated_image_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Download,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.save_image),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CarbonElevated,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val shareIntent = ImageStorageHelper.buildShareImageIntent(
                                                context,
                                                message.imagePath
                                            )
                                            if (shareIntent != null) {
                                                context.startActivity(
                                                    android.content.Intent.createChooser(
                                                        shareIntent,
                                                        "Compartilhar arte do Mano AI"
                                                    )
                                                )
                                            }
                                        }
                                        .testTag("share_generated_image_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
                                            contentDescription = null,
                                            tint = MintAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.share_image),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (zoomImagePath != null) {
        Dialog(
            onDismissRequest = { zoomImagePath = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack.copy(alpha = 0.96f))
                    .clickable { zoomImagePath = null }
            ) {
                AsyncImage(
                    model = File(zoomImagePath!!),
                    contentDescription = "Visualização ampliada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
                IconButton(
                    onClick = { zoomImagePath = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .background(CarbonElevated, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar visualização",
                        tint = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeSnippetCard(
    language: String,
    code: String,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CodeBackground,
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonElevated)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = language,
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldPrimary
                )
                TextButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("copy_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        tint = MintAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.copy_code),
                        style = MaterialTheme.typography.labelMedium,
                        color = MintAccent
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MintAccent
                )
            }
        }
    }
}
