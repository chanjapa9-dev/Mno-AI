package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private data class CapabilityItem(
    val icon: ImageVector,
    val title: String,
    val detail: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseSheet(
    onDismiss: () -> Unit
) {
    val items = listOf(
        CapabilityItem(
            icon = Icons.Outlined.Forum,
            title = "Fala de Tudo +17",
            detail = "Papo reto de Manaus: desabafo, ideia, opinião, explicação, zoeira e conversa adulta sem travar nem enrolar."
        ),
        CapabilityItem(
            icon = Icons.Outlined.Image,
            title = "Geração & Edição de Imagem",
            detail = "Ative o botão IMG ou peça logo, capa, anime ou arte na hora. Também pode mandar uma foto sua pro Mano editar."
        ),
        CapabilityItem(
            icon = Icons.Outlined.Code,
            title = "Google Apps Script",
            detail = "JavaScript pronto pra Planilhas, Gmail, Drive e Forms (getRange, MailApp) com passo a passo pra rodar."
        ),
        CapabilityItem(
            icon = Icons.Outlined.PhoneAndroid,
            title = "Criar App",
            detail = "Estrutura telas, fluxos, lógica Kotlin/Compose ou Web e ajuda a tirar teu projeto do papel."
        ),
        CapabilityItem(
            icon = Icons.Outlined.Calculate,
            title = "Cálculos Rápidos",
            detail = "Fazenda (área, gado, lucro), Obra (tijolo, cimento, m²) e Dinheiro (meta, falta por dia, juros)."
        ),
        CapabilityItem(
            icon = Icons.Outlined.EditNote,
            title = "Textos que Convertem",
            detail = "Nomes, bios, roteiros curtos, legendas, mensagens de WhatsApp e textos de venda."
        ),
        CapabilityItem(
            icon = Icons.Outlined.AutoAwesome,
            title = "Mitos & Lendas do Norte",
            detail = "Aba exclusiva com histórias curtas da Amazônia narradas na voz do mno e com arte gerada na hora."
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CarbonSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = stringResource(R.string.knowledge_base_title),
                style = MaterialTheme.typography.titleLarge,
                color = EmeraldPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Base de conhecimento do Mano AI • Direto, sem enrolação e sem ser robótico.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            items.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CarbonElevated,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MintAccent,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = item.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
