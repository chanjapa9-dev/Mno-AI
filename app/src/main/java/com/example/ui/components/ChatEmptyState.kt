package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R

data class QuickStarter(
    val label: String,
    val icon: ImageVector,
    val prompt: String,
    val enableImageMode: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatEmptyState(
    onPickStarter: (QuickStarter) -> Unit,
    onGoToMyths: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val starters = listOf(
        QuickStarter(
            label = "Criar Imagem",
            icon = Icons.Outlined.Image,
            prompt = "Logo estilo cyberpunk amazônico verde e preto para ",
            enableImageMode = true
        ),
        QuickStarter(
            label = "App Script",
            icon = Icons.Outlined.Code,
            prompt = "Mno, cria um Apps Script que lê a coluna A da planilha e envia um email automático quando tiver pendente."
        ),
        QuickStarter(
            label = "Criar App",
            icon = Icons.Outlined.PhoneAndroid,
            prompt = "Mno, me ajuda a estruturar um app Android simples de controle financeiro com telas e fluxo."
        ),
        QuickStarter(
            label = "Cálculos",
            icon = Icons.Outlined.Calculate,
            prompt = "Mno, calcula pra mim quantos tijolos e sacos de cimento vão num muro de 12m por 2,2m."
        ),
        QuickStarter(
            label = "Textos & Bio",
            icon = Icons.Outlined.EditNote,
            prompt = "Mno, cria uma bio direta e chamativa pro Instagram e um texto curto de venda pro WhatsApp sobre "
        ),
        QuickStarter(
            label = "Fala de Tudo +17",
            icon = Icons.Outlined.Forum,
            prompt = "Mno, papo reto +17: preciso de um conselho sincero sem enrolação sobre "
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.18f),
                        radius = size.minDimension * 0.55f
                    )
                )
            }
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Image(
            painter = painterResource(id = R.drawable.img_mano_avatar_1795262249289),
            contentDescription = stringResource(R.string.avatar_cd),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .border(2.dp, colorScheme.primary, CircleShape)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "E aí, mno! Manda a braba.",
            style = MaterialTheme.typography.headlineMedium,
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Papo reto de Manaus (+17). Pergunta qualquer coisa, pede código, cálculo, imagem ou texto sem enrolação.",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp)
        )
        Spacer(modifier = Modifier.height(22.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            starters.forEach { starter ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, colorScheme.outline),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPickStarter(starter) }
                        .testTag("starter_chip_${starter.label}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = starter.icon,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = starter.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colorScheme.primaryContainer,
            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onGoToMyths)
                .testTag("open_myths_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Novo: Mitos & Lendas do Norte",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.tertiary
                    )
                    Text(
                        text = "Histórias curtas da Amazônia no papo direto do mno.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
