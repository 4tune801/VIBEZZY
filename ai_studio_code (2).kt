package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.BottomTab
import com.example.ui.components.VibezzyBottomBar
import com.example.ui.components.VibezzyTopBar
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.VibezzyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            VibezzyTheme {
                VibezzyApp()
            }
        }
    }
}

@Composable
fun VibezzyApp(
    viewModel: VibezzyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbar()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VibezzyBlack)
    ) {
        val activeRoom = uiState.rooms.find { it.id == uiState.selectedRoomId }
        val activeGroup = uiState.groups.find { it.id == uiState.activeGroupChatId }

        when {
            uiState.showWalletScreen -> {
                WalletScreen(
                    uiState = uiState,
                    onBack = viewModel::closeWallet,
                    onSelectPaymentMethod = viewModel::selectPaymentMethod,
                    onBuyPackage = viewModel::openPaystackCheckout,
                    onClosePaystackCheckout = viewModel::closePaystackCheckout,
                    onConfirmPaystackPayment = viewModel::confirmPaystackPayment,
                    onUpdateTopUpForm = viewModel::updateTopUpForm,
                    onSubmitTopUp = { viewModel.submitTopUpForm() },
                    onWithdraw = viewModel::withdrawAmi,
                    onShowMessage = viewModel::showMessage
                )
            }

            uiState.showSearchScreen -> {
                SearchOverlayScreen(
                    uiState = uiState,
                    onQueryChange = viewModel::updateSearchQuery,
                    onBack = viewModel::closeSearch,
                    onRoomClick = { roomId -> viewModel.openRoom(roomId) },
                    onGroupClick = { groupId ->
                        viewModel.closeSearch()
                        viewModel.selectTab(BottomTab.CHAT)
                        viewModel.openGroupChat(groupId)
                    },
                    onVideoClick = { video -> viewModel.openVideoModal(video) }
                )
            }

            uiState.showNotificationsScreen -> {
                NotificationsOverlayScreen(
                    notifications = uiState.notifications,
                    onBack = viewModel::closeNotifications,
                    onMarkAllRead = viewModel::markAllNotificationsRead,
                    onNotificationClick = viewModel::onNotificationClick
                )
            }

            uiState.selectedTab == BottomTab.CHAT && activeGroup != null -> {
                GroupChatScreen(
                    group = activeGroup,
                    onBack = viewModel::closeGroupChat,
                    onSendMessage = { text -> viewModel.sendGroupMessage(activeGroup.id, text) }
                )
            }

            uiState.selectedTab == BottomTab.HOME && activeRoom != null -> {
                RoomDetailScreen(
                    room = activeRoom,
                    posts = uiState.posts.filter { it.roomId == activeRoom.id },
                    gameTournaments = uiState.gameTournaments,
                    gameChatMessages = uiState.gameChatMessages,
                    onBack = viewModel::closeRoom,
                    onLikePost = viewModel::toggleLikePost,
                    onCommentPost = viewModel::openPostComments,
                    onSharePost = { post ->
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "${post.authorHandle} in ${post.roomName} on VIBEZZY: ${post.caption}"
                            )
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Vibe"))
                    },
                    onJoinAudioStage = { viewModel.selectTab(BottomTab.LIVE) },
                    onQuickPostInRoom = { caption ->
                        viewModel.createPost(activeRoom.id, caption, 30, false, null)
                    },
                    onSendGameChat = viewModel::sendGameChatMessage,
                    onToggleJoinTournament = viewModel::toggleJoinTournament
                )
            }

            else -> {
                Scaffold(
                    containerColor = VibezzyBlack,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        VibezzyTopBar(
                            unreadCount = uiState.unreadNotificationCount,
                            amiBalance = uiState.amiBalance,
                            onSearchClick = viewModel::openSearch,
                            onNotificationsClick = viewModel::openNotifications,
                            onWalletClick = viewModel::openWallet
                        )
                    },
                    bottomBar = {
                        VibezzyBottomBar(
                            selectedTab = uiState.selectedTab,
                            onTabSelected = viewModel::selectTab
                        )
                    },
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(
                                containerColor = NeonGreen,
                                contentColor = VibezzyBlack,
                                snackbarData = data
                            )
                        }
                    }
                ) { innerPadding ->
                    Crossfade(
                        targetState = uiState.selectedTab,
                        animationSpec = tween(220),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        label = "mainTabCrossfade"
                    ) { tab ->
                        when (tab) {
                            BottomTab.HOME -> {
                                HomeScreen(
                                    uiState = uiState,
                                    onStoryClick = viewModel::openStory,
                                    onRoomClick = { room -> viewModel.openRoom(room.id) },
                                    onLikePost = viewModel::toggleLikePost,
                                    onCommentPost = viewModel::openPostComments,
                                    onSharePost = { post ->
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "${post.authorHandle} on VIBEZZY: ${post.caption}"
                                            )
                                        }
                                        context.startActivity(
                                            Intent.createChooser(sendIntent, "Share Vibe")
                                        )
                                    },
                                    onOpenLiveClick = { viewModel.selectTab(BottomTab.LIVE) }
                                )
                            }

                            BottomTab.EXPLORE -> {
                                ExploreScreen(
                                    uiState = uiState,
                                    onSearchClick = viewModel::openSearch,
                                    onRoomClick = { room -> viewModel.openRoom(room.id) },
                                    onVideoClick = viewModel::openVideoModal,
                                    onOpenLiveClick = { viewModel.selectTab(BottomTab.LIVE) }
                                )
                            }

                            BottomTab.CREATE -> {
                                CreatePostScreen(
                                    rooms = uiState.rooms,
                                    onPostToHome = viewModel::createPost
                                )
                            }

                            BottomTab.LIVE -> {
                                AudioLiveScreen(
                                    uiState = uiState,
                                    onSendChat = viewModel::sendLiveChatMessage,
                                    onSendGift = viewModel::sendAmiGift,
                                    onClearGiftBanner = viewModel::clearGiftBanner,
                                    onToggleRaiseHand = viewModel::toggleRaiseHand,
                                    onToggleSimulateUnder500 = viewModel::cycleFollowerPreviewMode,
                                    onAttemptHostRoom = viewModel::attemptHostLiveRoom,
                                    onOpenWallet = viewModel::openWallet
                                )
                            }

                            BottomTab.CHAT -> {
                                GroupsScreen(
                                    groups = uiState.groups,
                                    onCreateGroup = viewModel::createGroup,
                                    onOpenGroupChat = viewModel::openGroupChat,
                                    onJoinGroup = viewModel::joinGroup
                                )
                            }

                            BottomTab.PROFILE -> {
                                ProfileScreen(
                                    uiState = uiState,
                                    onTopUpClick = viewModel::openWallet,
                                    onFollowToggle = viewModel::toggleFollowProfile,
                                    onUpdateProfile = viewModel::updateProfile,
                                    onVideoClick = viewModel::openVideoModal,
                                    onCycleFollowerMode = viewModel::cycleFollowerPreviewMode
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Snackbar for overlay screens (Wallet, RoomDetail, GroupChat, etc.)
        if (uiState.showWalletScreen || activeRoom != null || activeGroup != null || uiState.showSearchScreen || uiState.showNotificationsScreen) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) { data ->
                Snackbar(
                    containerColor = NeonGreen,
                    contentColor = VibezzyBlack,
                    snackbarData = data
                )
            }
        }
    }

    // Full-screen Story Viewer Modal
    uiState.activeStory?.let { story ->
        StoryViewerModal(
            story = story,
            onDismiss = viewModel::closeStory,
            onJumpToRoom = { roomName ->
                val target = uiState.rooms.find { it.name.equals(roomName, ignoreCase = true) }
                if (target != null) viewModel.openRoom(target.id)
            },
            onSendQuickReply = viewModel::showMessage
        )
    }

    // 30s Video Player Modal
    uiState.activeVideo?.let { video ->
        VideoPlayerModal(
            video = video,
            onDismiss = viewModel::closeVideoModal,
            onLikeClick = { viewModel.toggleLikeProfileVideo(video.id) },
            onAddComment = { comment -> viewModel.addCommentToProfileVideo(video.id, comment) }
        )
    }

    // Post Comments Bottom Sheet
    uiState.activePostCommentsId?.let { postId ->
        val post = uiState.posts.find { it.id == postId }
        if (post != null) {
            CommentsBottomSheet(
                post = post,
                onDismiss = viewModel::closePostComments,
                onAddComment = { text -> viewModel.addCommentToPost(post.id, text) }
            )
        }
    }
}