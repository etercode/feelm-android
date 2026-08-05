package org.feelm.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.feelm.app.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import org.feelm.app.di.AppContainer
import org.feelm.app.ui.account.AccountScreen
import org.feelm.app.ui.browse.BrowseScreen
import org.feelm.app.ui.detail.DetailScreen
import org.feelm.app.ui.feed.FeedScreen
import org.feelm.app.ui.home.HomeScreen
import org.feelm.app.ui.person.PersonScreen
import org.feelm.app.ui.profile.FollowListScreen
import org.feelm.app.ui.profile.ProfileScreen
import org.feelm.app.ui.settings.SettingsScreen
import org.feelm.app.ui.search.SearchScreen
import org.feelm.app.ui.shelf.ShelfScreen
import org.feelm.app.ui.theme.FeelmTheme

object Routes {
    const val HOME = "home"
    const val BROWSE = "browse"
    const val SEARCH = "search"
    const val SHELF = "shelf"
    const val PROFILE = "profile"
    const val WORK = "work/{type}/{slug}"
    const val PERSON = "person/{slug}"
    const val USER = "user/{username}"
    const val FOLLOWS = "follows/{username}/{following}"
    const val FEED = "feed"
    const val SETTINGS = "settings"

    fun work(type: String, slug: String) = "work/$type/$slug"
    fun person(slug: String) = "person/$slug"
    fun user(username: String) = "user/$username"
    fun follows(username: String, following: Boolean) = "follows/$username/$following"
}

private data class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.app_navHome, Icons.Filled.Home),
    Tab(Routes.BROWSE, R.string.app_navBrowse, Icons.Filled.GridView),
    Tab(Routes.SEARCH, R.string.app_navSearch, Icons.Filled.Search),
    Tab(Routes.SHELF, R.string.app_navShelf, Icons.Filled.Bookmarks),
    Tab(Routes.PROFILE, R.string.app_navYou, Icons.Filled.Person),
)

@Composable
fun FeelmApp(container: AppContainer) {
    val darkTheme by container.settings.darkTheme.collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    // Edge-to-edge means the status bar sits over the app's own background, so
    // its icons have to be told which way to contrast — the system cannot work
    // that out from a theme it does not know about.
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    val seenState by container.seen.state.collectAsStateWithLifecycle()

    // Signing in or out changes whose catch-up point applies, and signing out
    // has to clear it — otherwise the badges of the last account stay on the
    // posters of the next one.
    val signedIn by container.repository.isSignedIn.collectAsStateWithLifecycle(false)
    LaunchedEffect(signedIn) { container.seen.refresh() }

    FeelmTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalSeenState provides seenState) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val destination = backStackEntry?.destination

        // A title opened from any tab takes the whole screen. The bar is for
        // moving between sections, and a detail page is not one.
        val onTab = tabs.any { tab -> destination?.hierarchy?.any { it.route == tab.route } == true }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            // The screens own their system-bar insets; this Scaffold only
            // contributes the height of the bar itself. Two Scaffolds both
            // applying the status-bar inset would indent every screen twice.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (onTab) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEach { tab ->
                            val selected =
                                destination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navController.switchTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = stringResource(tab.label)) },
                                label = { Text(stringResource(tab.label)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(padding),
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenWork = { type, slug ->
                            navController.navigate(Routes.work(type, slug))
                        },
                        onOpenAccount = { navController.switchTab(Routes.PROFILE) },
                        darkTheme = darkTheme,
                        onToggleTheme = {
                            scope.launch { container.settings.setDarkTheme(!darkTheme) }
                        },
                        onCatchUp = container.seen::catchUp,
                    )
                }

                composable(Routes.BROWSE) {
                    BrowseScreen(
                        onOpenWork = { type, slug ->
                            navController.navigate(Routes.work(type, slug))
                        },
                    )
                }

                composable(Routes.SEARCH) {
                    SearchScreen(
                        onOpenWork = { type, slug ->
                            navController.navigate(Routes.work(type, slug))
                        },
                    )
                }

                composable(Routes.SHELF) {
                    ShelfScreen(
                        onOpenWork = { type, slug ->
                            navController.navigate(Routes.work(type, slug))
                        },
                        onSignIn = { navController.switchTab(Routes.PROFILE) },
                    )
                }

                composable(Routes.PROFILE) {
                    AccountScreen(
                        darkTheme = darkTheme,
                        onToggleTheme = {
                            scope.launch { container.settings.setDarkTheme(!darkTheme) }
                        },
                        onOpenProfile = { username -> navController.navigate(Routes.user(username)) },
                        onOpenFollows = { name, following ->
                            navController.navigate(Routes.follows(name, following))
                        },
                        onOpenFeed = { navController.navigate(Routes.FEED) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }

                composable(
                    route = Routes.USER,
                    arguments = listOf(navArgument("username") { type = NavType.StringType }),
                ) { entry ->
                    ProfileScreen(
                        username = entry.arguments?.getString("username").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onOpenWork = { type, slug ->
                            navController.navigate(Routes.work(type, slug))
                        },
                        onOpenFollows = { name, following ->
                            navController.navigate(Routes.follows(name, following))
                        },
                    )
                }

                composable(
                    route = Routes.FOLLOWS,
                    arguments = listOf(
                        navArgument("username") { type = NavType.StringType },
                        navArgument("following") { type = NavType.BoolType },
                    ),
                ) { entry ->
                    FollowListScreen(
                        username = entry.arguments?.getString("username").orEmpty(),
                        following = entry.arguments?.getBoolean("following") == true,
                        onBack = { navController.popBackStack() },
                        onOpenProfile = { name -> navController.navigate(Routes.user(name)) },
                    )
                }

                composable(Routes.FEED) {
                    FeedScreen(
                        onBack = { navController.popBackStack() },
                        onOpenWork = { type, slug ->
                            navController.navigate(Routes.work(type, slug))
                        },
                        onOpenProfile = { username ->
                            navController.navigate(Routes.user(username))
                        },
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }

                composable(
                    route = Routes.WORK,
                    arguments = listOf(
                        navArgument("type") { type = NavType.StringType },
                        navArgument("slug") { type = NavType.StringType },
                    ),
                ) { entry ->
                    DetailScreen(
                        type = entry.arguments?.getString("type").orEmpty(),
                        slug = entry.arguments?.getString("slug").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onSignIn = { navController.switchTab(Routes.PROFILE) },
                        onOpenWork = { type, slug ->
                            navController.navigate(Routes.work(type, slug))
                        },
                        onOpenPerson = { slug -> navController.navigate(Routes.person(slug)) },
                        onSeen = container.seen::markSeen,
                    )
                }

                composable(
                    route = Routes.PERSON,
                    arguments = listOf(navArgument("slug") { type = NavType.StringType }),
                ) { entry ->
                    PersonScreen(
                        slug = entry.arguments?.getString("slug").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onOpenWork = { type, slug ->
                            navController.navigate(Routes.work(type, slug))
                        },
                    )
                }
            }
        }
        }
    }
}

/**
 * Move to a tab the way a tab bar should.
 *
 * `saveState`/`restoreState` are what stop a tab from being rebuilt every time
 * it is revisited — without them, scrolling half way down Browse, glancing at
 * Search and coming back puts you at the top of a freshly refetched list.
 * `launchSingleTop` keeps a second copy of a tab off the back stack when its
 * own icon is tapped again.
 */
private fun androidx.navigation.NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
