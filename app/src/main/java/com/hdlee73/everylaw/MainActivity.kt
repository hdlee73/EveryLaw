package com.hdlee73.everylaw

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.isSystemInDarkTheme
import com.hdlee73.everylaw.ui.theme.EverylawIcons
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hdlee73.everylaw.core.model.LawSummary
import com.hdlee73.everylaw.di.viewModelFactory
import com.hdlee73.everylaw.ui.library.DocumentScreen
import com.hdlee73.everylaw.ui.library.DocumentViewModel
import com.hdlee73.everylaw.ui.library.PrecedentScreen
import com.hdlee73.everylaw.ui.library.PrecedentViewModel
import com.hdlee73.everylaw.ui.library.QueryListScreen
import com.hdlee73.everylaw.ui.search.SearchScreen
import com.hdlee73.everylaw.ui.search.SearchViewModel
import com.hdlee73.everylaw.ui.settings.AboutScreen
import com.hdlee73.everylaw.ui.settings.FirstRunOcGuide
import com.hdlee73.everylaw.ui.settings.SettingsScreen
import com.hdlee73.everylaw.ui.settings.SettingsViewModel
import com.hdlee73.everylaw.ui.settings.UpdateDialog
import com.hdlee73.everylaw.ui.theme.EverylawTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val factory = (application as EverylawApp).container.viewModelFactory()
        setContent {
            EverylawTheme {
                val size = calculateWindowSizeClass(this)
                // 갤럭시 Z 폴드 펼침(약 673dp+)·태블릿은 Medium 이상 → 분할 화면, 일반 폰은 Compact → 아코디언
                val isWide = size.widthSizeClass != WindowWidthSizeClass.Compact
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    EverylawRoot(isWide = isWide, factory = factory, onExit = { finish() })
                }
            }
        }
    }
}

private enum class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "검색", EverylawIcons.Search),
    Favorites("favorites", "보관함", EverylawIcons.Folder),
    History("history", "최근", EverylawIcons.Clock),
    Settings("settings", "설정", EverylawIcons.Gear),
}

@Composable
private fun EverylawRoot(
    isWide: Boolean,
    factory: androidx.lifecycle.ViewModelProvider.Factory,
    onExit: () -> Unit,
) {
    val nav = rememberNavController()
    // 검색 상태는 탭을 오가도 유지되도록 Activity 범위로 둔다
    val searchVm: SearchViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val backEntry by nav.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route
    val current = TopLevel.entries.firstOrNull { it.route == route }

    // 홈 위쪽은 남색 머리띠라 상태 표시줄 아이콘을 밝게, 다른 화면은 밝은 바탕이라 어둡게(라이트 테마 기준)
    val view = androidx.compose.ui.platform.LocalView.current
    val dark = isSystemInDarkTheme()
    androidx.compose.runtime.SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark && current != TopLevel.Home
    }

    LaunchedEffect(Unit) { settingsVm.autoCheckIfDue() }
    UpdateDialog(settingsVm)

    // 뒤로가기: ① 화면 안 이전 결과 → ② 이전 화면(NavHost) → ③ 홈 첫 화면에서는 두 번 눌러야 종료
    val canGoBackInSearch by searchVm.canGoBack.collectAsStateWithLifecycle()
    BackHandler(enabled = current == TopLevel.Home && canGoBackInSearch) { searchVm.back() }
    val context = LocalContext.current
    val lastBack = remember { mutableLongStateOf(0L) }
    BackHandler(enabled = current == TopLevel.Home && !canGoBackInSearch) {
        val now = System.currentTimeMillis()
        if (now - lastBack.longValue < 2000) onExit()
        else {
            lastBack.longValue = now
            Toast.makeText(context, "한 번 더 누르면 종료돼요", Toast.LENGTH_SHORT).show()
        }
    }

    val goTop: (TopLevel) -> Unit = { dest ->
        nav.navigate(dest.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val runQuery: (String) -> Unit = { q ->
        searchVm.submit(q)
        goTop(TopLevel.Home)
    }

    // 첫 실행 한 번: 기본(공용) 인증키와 본인 OC 발급 안내
    FirstRunOcGuide(settingsVm, onOpenSettings = { goTop(TopLevel.Settings) })

    Row(Modifier.fillMaxSize()) {
        if (isWide && current != null) {
            NavigationRail(Modifier.safeDrawingPadding().padding(top = 12.dp)) {
                TopLevel.entries.forEach { t ->
                    NavigationRailItem(
                        selected = t == current,
                        onClick = { goTop(t) },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            bottomBar = {
                if (!isWide && current != null) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    TopLevel.entries.forEach { t ->
                        NavigationBarItem(
                            selected = t == current,
                            onClick = { goTop(t) },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
        ) { pad ->
            // 바깥 Scaffold가 이미 시스템 바 여백을 줬으니 안쪽 화면의 Scaffold가 중복 적용하지 않도록 소비 처리
            Box(Modifier.padding(pad).consumeWindowInsets(pad)) {
                com.hdlee73.everylaw.ui.ProvideCitationHandler(onCite = runQuery) {
                    EverylawNavHost(nav, isWide, factory, searchVm, settingsVm, runQuery)
                }
            }
        }
    }
}

@Composable
private fun EverylawNavHost(
    nav: NavHostController,
    isWide: Boolean,
    factory: androidx.lifecycle.ViewModelProvider.Factory,
    searchVm: SearchViewModel,
    settingsVm: SettingsViewModel,
    runQuery: (String) -> Unit,
) {
    NavHost(nav, startDestination = TopLevel.Home.route) {
        composable(TopLevel.Home.route) {
            SearchScreen(
                vm = searchVm,
                isWide = isWide,
                onOpenPrecedent = { nav.navigate("precedent/${Uri.encode(it.serialNo)}") },
                onOpenLaw = { law ->
                    nav.navigate(
                        "law/${Uri.encode(law.mst)}?id=${Uri.encode(law.id)}&name=${Uri.encode(law.name)}&kind=${Uri.encode(law.kind.orEmpty())}"
                    )
                },
                onOpenAdminRule = { nav.navigate("adminrule/${Uri.encode(it.serialNo)}") },
                onOpenSettings = { nav.navigate(TopLevel.Settings.route) },
            )
        }
        composable(TopLevel.Favorites.route) {
            val favorites by searchVm.favorites.collectAsStateWithLifecycle()
            QueryListScreen(
                title = "보관함",
                items = favorites,
                emptyText = "검색 결과 화면의 ☆ 버튼으로 자주 보는 조문을 저장하세요.",
                isFavorites = true,
                onPick = runQuery,
                onRemove = searchVm::toggleFavorite,
                onClearAll = null,
            )
        }
        composable(TopLevel.History.route) {
            val recent by searchVm.recent.collectAsStateWithLifecycle()
            QueryListScreen(
                title = "최근",
                items = recent,
                emptyText = "아직 검색 기록이 없어요.",
                isFavorites = false,
                onPick = runQuery,
                onRemove = null,
                onClearAll = searchVm::clearRecent,
            )
        }
        composable(TopLevel.Settings.route) {
            SettingsScreen(settingsVm, onOpenAbout = { nav.navigate("about") })
        }
        composable("about") {
            AboutScreen(settingsVm, onBack = { nav.popBackStack() })
        }
        composable("precedent/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val vm: PrecedentViewModel = viewModel(factory = factory)
            PrecedentScreen(vm, entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable(
            "law/{mst}?id={id}&name={name}&kind={kind}",
            arguments = listOf(
                navArgument("mst") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType; defaultValue = "" },
                navArgument("name") { type = NavType.StringType; defaultValue = "" },
                navArgument("kind") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val a = entry.arguments!!
            val law = LawSummary(
                id = a.getString("id").orEmpty(),
                mst = a.getString("mst").orEmpty(),
                name = a.getString("name").orEmpty(),
                abbreviation = null,
                kind = a.getString("kind")?.ifBlank { null },
                effectiveDate = null,
            )
            val vm: DocumentViewModel = viewModel(factory = factory)
            DocumentScreen(vm, load = { loadLaw(law) }, onBack = { nav.popBackStack() }, onOpenTree = runQuery)
        }
        composable("adminrule/{serial}", arguments = listOf(navArgument("serial") { type = NavType.StringType })) { entry ->
            val serial = entry.arguments?.getString("serial").orEmpty()
            val vm: DocumentViewModel = viewModel(factory = factory)
            DocumentScreen(vm, load = { loadAdminRule(serial) }, onBack = { nav.popBackStack() }, onOpenTree = runQuery)
        }
    }
}
