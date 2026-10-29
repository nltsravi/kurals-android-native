package com.nltsravi.kurals

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nltsravi.kurals.data.KuralRepository
import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.service.NotificationService
import com.nltsravi.kurals.ui.components.AddToCollectionModal
import com.nltsravi.kurals.ui.components.LearningModePromptModal
import com.nltsravi.kurals.ui.components.ShareModal
import com.nltsravi.kurals.ui.screens.ChapterDetailScreen
import com.nltsravi.kurals.ui.screens.ChaptersScreen
import com.nltsravi.kurals.ui.screens.CollectionDetailScreen
import com.nltsravi.kurals.ui.screens.CollectionsScreen
import com.nltsravi.kurals.ui.screens.CreateCollectionScreen
import com.nltsravi.kurals.ui.screens.HomeScreen
import com.nltsravi.kurals.ui.screens.KuralDetailScreen
import com.nltsravi.kurals.ui.screens.SearchScreen
import com.nltsravi.kurals.ui.screens.SettingsScreen
import com.nltsravi.kurals.ui.strings.LocalAppLanguage
import com.nltsravi.kurals.ui.strings.LocalAppTranslations
import com.nltsravi.kurals.ui.strings.Translations
import com.nltsravi.kurals.ui.theme.KuralsTheme
import com.nltsravi.kurals.ui.theme.LocalAppPalette
import com.nltsravi.kurals.viewmodel.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()
    private val deepLinkKuralFlow = MutableStateFlow<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractDeepLinkKural(intent)

        setContent {
            val themeMode by appViewModel.themeMode.collectAsState()
            val language by appViewModel.language.collectAsState()
            val translations = remember(language) { Translations.forLanguage(language) }
            val deepLinkKural by deepLinkKuralFlow.collectAsState()

            KuralsTheme(themeMode = themeMode) {
                CompositionLocalProvider(
                    LocalAppTranslations provides translations,
                    LocalAppLanguage provides language
                ) {
                    KuralsAppContent(
                        viewModel = appViewModel,
                        deepLinkKuralNumber = deepLinkKural,
                        onConsumedDeepLink = { deepLinkKuralFlow.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractDeepLinkKural(intent)
    }

    private fun extractDeepLinkKural(intent: Intent?) {
        if (intent == null) return
        val extraNum = intent.getIntExtra(NotificationService.EXTRA_KURAL_NUMBER, -1)
        if (extraNum in 1..1330) {
            deepLinkKuralFlow.value = extraNum
            return
        }
        val data = intent.data
        if (data != null && data.scheme == "thirukkural") {
            val pathSeg = data.lastPathSegment?.toIntOrNull()
            if (pathSeg != null && pathSeg in 1..1330) {
                deepLinkKuralFlow.value = pathSeg
            }
        }
    }
}

private data class BottomTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun KuralsAppContent(
    viewModel: AppViewModel,
    deepLinkKuralNumber: Int?,
    onConsumedDeepLink: () -> Unit
) {
    val palette = LocalAppPalette.current
    val t = LocalAppTranslations.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val collections by viewModel.collections.collectAsState()
    val randomKuralState by viewModel.randomKural.collectAsState()
    val allowModifyCardImage by viewModel.allowModifyCardImage.collectAsState()
    val selectedCardImageId by viewModel.selectedCardImageId.collectAsState()
    val cardImageTheme by viewModel.cardImageTheme.collectAsState()
    val customImageUri by viewModel.customImageUri.collectAsState()
    val notificationSettings by viewModel.notificationSettings.collectAsState()
    val isLearningModeEnabled by viewModel.isLearningModeEnabled.collectAsState()
    val isDownloadingArtifacts by viewModel.isDownloadingArtifacts.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadError by viewModel.downloadError.collectAsState()
    val cachedOlineriState by viewModel.cachedOlineriState.collectAsState()
    val showLaunchPrompt by viewModel.showLaunchPrompt.collectAsState()
    val language by viewModel.language.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val pronunciationSDK = remember(cachedOlineriState, language) {
        viewModel.createPronunciationSDK()
    }

    var bookmarkModalKuralNumber by remember { mutableStateOf<Int?>(null) }
    var shareModalKural by remember { mutableStateOf<Kural?>(null) }
    var chaptersFilterCategory by remember { mutableStateOf<Int?>(null) }

    // Deep link navigation from notification tap
    LaunchedEffect(deepLinkKuralNumber) {
        if (deepLinkKuralNumber != null) {
            navController.navigate("kural/$deepLinkKuralNumber")
            onConsumedDeepLink()
        }
    }

    val tabs = listOf(
        BottomTabItem("home", t.tabHome, Icons.Default.Home),
        BottomTabItem("search", t.tabSearch, Icons.Default.Search),
        BottomTabItem("chapters", t.tabChapters, Icons.AutoMirrored.Filled.MenuBook),
        BottomTabItem("collections", t.tabCollections, Icons.Default.Bookmarks),
        BottomTabItem("settings", t.tabSettings, Icons.Default.Settings)
    )

    fun activeBottomTabKey(route: String): String {
        return when {
            route.startsWith("search") -> "search"
            route.startsWith("chapters") || route.startsWith("chapter/") -> "chapters"
            route.startsWith("collections") || route.startsWith("collection") -> "collections"
            route.startsWith("settings") -> "settings"
            else -> "home"
        }
    }

    val activeTabKey = activeBottomTabKey(currentRoute)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding(),
        containerColor = palette.background,
        bottomBar = {
            Surface(
                color = palette.surface,
                border = BorderStroke(0.5.dp, palette.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isActive = activeTabKey == tab.route
                        val tint = if (isActive) palette.tabIconSelected else palette.tabIconDefault
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (currentRoute != tab.route) {
                                        navController.navigate(tab.route) {
                                            popUpTo("home") { saveState = false }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                                .padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = tint,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = tab.label,
                                color = tint,
                                fontSize = 11.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    val kural = randomKuralState ?: KuralRepository.getKuralByNumber(1)!!
                    val isSaved = remember(collections, kural.number) {
                        collections.any { it.kuralNumbers.contains(kural.number) }
                    }
                    HomeScreen(
                        randomKural = kural,
                        isRandomKuralSaved = isSaved,
                        isLearningModeEnabled = isLearningModeEnabled,
                        onShuffleRandomKural = { viewModel.shuffleRandomKural() },
                        onOpenSearch = { navController.navigate("search") },
                        onOpenChapters = { catId ->
                            chaptersFilterCategory = catId
                            navController.navigate("chapters")
                        },
                        onOpenCollections = { navController.navigate("collections") },
                        onOpenKuralDetail = { num, _ ->
                            navController.navigate("kural/$num")
                        },
                        onShareKural = { shareModalKural = it },
                        onBookmarkKural = { bookmarkModalKuralNumber = it }
                    )
                }

                composable("search") {
                    SearchScreen(
                        isKuralSaved = { num -> collections.any { it.kuralNumbers.contains(num) } },
                        isLearningModeEnabled = isLearningModeEnabled,
                        onOpenKuralDetail = { num, _ -> navController.navigate("kural/$num") },
                        onShareKural = { shareModalKural = it },
                        onBookmarkKural = { bookmarkModalKuralNumber = it }
                    )
                }

                composable("chapters") {
                    ChaptersScreen(
                        initialCategoryId = chaptersFilterCategory,
                        onOpenChapterDetail = { chNum -> navController.navigate("chapter/$chNum") }
                    )
                }

                composable("collections") {
                    CollectionsScreen(
                        collections = collections,
                        onOpenCollectionDetail = { id -> navController.navigate("collection/$id") },
                        onOpenCreateCollection = { navController.navigate("collection_create") },
                        onDeleteCollection = { id -> viewModel.deleteCollection(id) }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        language = language,
                        onSelectLanguage = { viewModel.setLanguage(it) },
                        themeMode = themeMode,
                        onSelectThemeMode = { viewModel.setThemeMode(it) },
                        allowModifyCardImage = allowModifyCardImage,
                        onToggleAllowModifyCardImage = { viewModel.setAllowModifyCardImage(it) },
                        selectedCardImageId = selectedCardImageId,
                        onSelectCardImageId = { viewModel.setSelectedCardImageId(it) },
                        onSelectCardImageTheme = { viewModel.setCardImageTheme(it) },
                        customImageUri = customImageUri,
                        onSelectCustomImageUri = { viewModel.setCustomImageUri(it) },
                        notificationSettings = notificationSettings,
                        onUpdateNotificationSettings = { viewModel.updateNotificationSettings(it) },
                        isLearningModeEnabled = isLearningModeEnabled,
                        isDownloadingArtifacts = isDownloadingArtifacts,
                        downloadProgress = downloadProgress,
                        downloadError = downloadError,
                        cachedOlineriState = cachedOlineriState,
                        onEnableLearningMode = { viewModel.enableLearningMode() },
                        onOpenKuralPractice = { num -> navController.navigate("kural/$num") }
                    )
                }

                composable(
                    route = "kural/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType })
                ) { entry ->
                    val kuralNum = entry.arguments?.getInt("id") ?: 1
                    val isSaved = remember(collections, kuralNum) {
                        collections.any { it.kuralNumbers.contains(kuralNum) }
                    }
                    KuralDetailScreen(
                        kuralNumber = kuralNum,
                        isBookmarked = isSaved,
                        isLearningModeEnabled = isLearningModeEnabled,
                        pronunciationSDK = pronunciationSDK,
                        onBack = { navController.popBackStack() },
                        onNavigateToKural = { nextNum ->
                            navController.navigate("kural/$nextNum") {
                                popUpTo("kural/$kuralNum") { inclusive = true }
                            }
                        },
                        onNavigateToChapter = { chNum ->
                            navController.navigate("chapter/$chNum")
                        },
                        onBookmarkClick = { bookmarkModalKuralNumber = it },
                        onShareClick = { shareModalKural = it }
                    )
                }

                composable(
                    route = "chapter/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType })
                ) { entry ->
                    val chNum = entry.arguments?.getInt("id") ?: 1
                    ChapterDetailScreen(
                        chapterNumber = chNum,
                        isKuralSaved = { num -> collections.any { it.kuralNumbers.contains(num) } },
                        isLearningModeEnabled = isLearningModeEnabled,
                        onBack = { navController.popBackStack() },
                        onNavigateToChapter = { nextCh ->
                            navController.navigate("chapter/$nextCh") {
                                popUpTo("chapter/$chNum") { inclusive = true }
                            }
                        },
                        onOpenKuralDetail = { num, _ -> navController.navigate("kural/$num") },
                        onShareKural = { shareModalKural = it },
                        onBookmarkKural = { bookmarkModalKuralNumber = it }
                    )
                }

                composable(
                    route = "collection/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entry ->
                    val colId = entry.arguments?.getString("id") ?: ""
                    val collection = remember(collections, colId) {
                        collections.find { it.id == colId }
                    }
                    CollectionDetailScreen(
                        collection = collection,
                        isLearningModeEnabled = isLearningModeEnabled,
                        onBack = { navController.popBackStack() },
                        onRenameCollection = { id, name -> viewModel.renameCollection(id, name) },
                        onDeleteCollection = { id -> viewModel.deleteCollection(id) },
                        onMoveKural = { id, from, to -> viewModel.moveKural(id, from, to) },
                        onRemoveKuralFromCollection = { id, num -> viewModel.removeKuralFromCollection(id, num) },
                        onOpenKuralDetail = { num, _ -> navController.navigate("kural/$num") },
                        onShareKural = { shareModalKural = it }
                    )
                }

                composable("collection_create") {
                    CreateCollectionScreen(
                        onBack = { navController.popBackStack() },
                        onCreateCollection = { name, desc, color ->
                            viewModel.createCollection(name, desc, color)
                        },
                        onCollectionCreated = { newId ->
                            navController.navigate("collection/$newId") {
                                popUpTo("collection_create") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }

    // Global AddToCollectionModal
    AddToCollectionModal(
        visible = bookmarkModalKuralNumber != null,
        kuralNumber = bookmarkModalKuralNumber,
        collections = collections,
        onToggleCollection = { colId, num -> viewModel.toggleKuralInCollection(colId, num) },
        onCreateAndAddCollection = { name, color, num ->
            val created = viewModel.createCollection(name = name, color = color)
            viewModel.addKuralToCollection(created.id, num)
        },
        onClose = { bookmarkModalKuralNumber = null }
    )

    // Global ShareModal
    ShareModal(
        visible = shareModalKural != null,
        kural = shareModalKural,
        allowModifyCardImage = allowModifyCardImage,
        selectedCardImageId = selectedCardImageId,
        onSelectCardImageId = { viewModel.setSelectedCardImageId(it) },
        cardImageTheme = cardImageTheme,
        onSelectCardImageTheme = { viewModel.setCardImageTheme(it) },
        customImageUri = customImageUri,
        onSelectCustomImageUri = { viewModel.setCustomImageUri(it) },
        onClose = { shareModalKural = null }
    )

    // Global Olineri Learning Mode Launch Prompt Modal
    LearningModePromptModal(
        visible = showLaunchPrompt && !isLearningModeEnabled,
        isDownloading = isDownloadingArtifacts,
        downloadProgress = downloadProgress,
        downloadError = downloadError,
        onEnableNow = { viewModel.enableLearningMode() },
        onDismissLater = { viewModel.dismissLaunchPrompt() }
    )
}

