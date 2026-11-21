package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.ui.ManoTab
import com.example.ui.ManoViewModel
import com.example.ui.components.KnowledgeBaseSheet
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ManoVoiceMode
import com.example.ui.screens.MythsScreen
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.DarkEmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            ManoTheme {
                ManoAiApp()
            }
        }
    }
}

@Composable
fun ManoAiApp(
    viewModel: ManoViewModel = viewModel()
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val myths by viewModel.myths.collectAsStateWithLifecycle()
    val isImageMode by viewModel.isImageMode.collectAsStateWithLifecycle()
    val attachedImagePath by viewModel.attachedImagePath.collectAsStateWithLifecycle()
    val isSendingText by viewModel.isSendingText.collectAsStateWithLifecycle()
    val isGeneratingImage by viewModel.isGeneratingImage.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val mythState by viewModel.mythState.collectAsStateWithLifecycle()
    val illustratingMythId by viewModel.illustratingMythId.collectAsStateWithLifecycle()
    val mythImageError by viewModel.mythImageError.collectAsStateWithLifecycle()

    var showKnowledgeBase by remember { mutableStateOf(false) }
    var showVoiceMode by remember { mutableStateOf(false) }

    BackHandler(enabled = activeTab != ManoTab.CHAT) {
        viewModel.selectTab(ManoTab.CHAT)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = ObsidianBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = CarbonSurface,
                        tonalElevation = 0.dp
                    ) {
                        ManoNavItems(
                            activeTab = activeTab,
                            onSelectTab = viewModel::selectTab
                        ).forEach { item ->
                            NavigationBarItem(
                                selected = activeTab == item.tab,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == item.tab) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = DarkEmeraldContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = CarbonSurface
                    ) {
                        ManoNavItems(
                            activeTab = activeTab,
                            onSelectTab = viewModel::selectTab
                        ).forEach { item ->
                            NavigationRailItem(
                                selected = activeTab == item.tab,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == item.tab) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = CarbonElevated,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                when (activeTab) {
                    ManoTab.CHAT -> ChatScreen(
                        messages = messages,
                        streamingReply = streamingReply,
                        isSendingText = isSendingText,
                        isGeneratingImage = isGeneratingImage,
                        isImageMode = isImageMode,
                        attachedImagePath = attachedImagePath,
                        chatError = chatError,
                        onToggleImageMode = viewModel::toggleImageMode,
                        onSetImageMode = viewModel::setImageMode,
                        onAttachImageUri = viewModel::attachImageFromUri,
                        onClearAttachedImage = viewModel::clearAttachedImage,
                        onSendMessage = viewModel::sendMessage,
                        onNewChat = viewModel::startNewChat,
                        onDismissError = viewModel::dismissChatError,
                        onOpenKnowledgeBase = { showKnowledgeBase = true },
                        onGoToMyths = { viewModel.selectTab(ManoTab.MYTHS) },
                        onOpenVoiceMode = { showVoiceMode = true },
                        modifier = Modifier.weight(1f)
                    )

                    ManoTab.MYTHS -> MythsScreen(
                        mythState = mythState,
                        savedMyths = myths,
                        illustratingMythId = illustratingMythId,
                        mythImageError = mythImageError,
                        onGenerateMyth = viewModel::generateMyth,
                        onIllustrateMyth = viewModel::illustrateMyth,
                        onDeleteMyth = viewModel::deleteMyth,
                        onSendMythToChat = viewModel::sendMythToChat,
                        onDismissImageError = viewModel::dismissMythImageError,
                        modifier = Modifier.weight(1f)
                    )

                    ManoTab.HISTORY -> HistoryScreen(
                        sessions = sessions,
                        mythsCount = myths.size,
                        onOpenSession = viewModel::openSession,
                        onDeleteSession = viewModel::deleteSession,
                        onClearAllSessions = viewModel::clearAllHistory,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showKnowledgeBase) {
        KnowledgeBaseSheet(onDismiss = { showKnowledgeBase = false })
    }

    if (showVoiceMode) {
        ManoVoiceMode(
            streamingReply = streamingReply,
            onSendVoicePrompt = viewModel::sendVoicePrompt,
            onClose = { showVoiceMode = false }
        )
    }
}

private data class NavEntry(
    val tab: ManoTab,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)

@Composable
private fun ManoNavItems(
    activeTab: ManoTab,
    onSelectTab: (ManoTab) -> Unit
): List<NavEntry> = listOf(
    NavEntry(
        tab = ManoTab.CHAT,
        label = stringResource(R.string.nav_chat),
        selectedIcon = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline,
        testTag = "nav_tab_chat"
    ),
    NavEntry(
        tab = ManoTab.MYTHS,
        label = stringResource(R.string.nav_myths),
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_tab_myths"
    ),
    NavEntry(
        tab = ManoTab.HISTORY,
        label = stringResource(R.string.nav_history),
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_history"
    )
)
