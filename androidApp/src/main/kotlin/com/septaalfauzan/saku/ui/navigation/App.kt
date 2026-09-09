package com.septaalfauzan.saku.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.septaalfauzan.saku.ui.addedit.AddEditRoute
import com.septaalfauzan.saku.ui.components.PillDestination
import com.septaalfauzan.saku.ui.components.PillNavigation
import com.septaalfauzan.saku.ui.components.QuickActionSheet
import com.septaalfauzan.saku.ui.dashboard.DashboardRoute
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import com.septaalfauzan.saku.ui.detail.DetailRoute
import com.septaalfauzan.saku.ui.review.ReviewQueueRoute
import com.septaalfauzan.saku.ui.scanner.ScannerScreen
import com.septaalfauzan.saku.ui.tracking.TrackingRoute
import com.septaalfauzan.saku.ui.transactions.TransactionListRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.septaalfauzan.saku.ui.designsystem.SakuIcons

object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val ADD = "add"
    const val EDIT = "edit/{transactionId}"
    const val DETAIL = "detail/{transactionId}"
    const val REVIEW = "review"
    const val TRACKING = "tracking"
    const val RULES = "rules"
    const val SCANNER = "scanner"
    fun edit(id: String) = "edit/$id"
    fun detail(id: String) = "detail/$id"
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    // 1. Track the scroll state of the list
    val listState = rememberLazyListState()

    // 2. Track the previous index and offset to determine scroll direction
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    // 3. Derive whether the user is scrolling down
    val isFabVisible by remember {
        derivedStateOf {
            val currentIndex = listState.firstVisibleItemIndex
            val currentOffset = listState.firstVisibleItemScrollOffset

            val isScrollingDown = if (currentIndex == previousIndex) {
                currentOffset > previousScrollOffset
            } else {
                currentIndex > previousIndex
            }

            // Update tracking variables
            previousIndex = currentIndex
            previousScrollOffset = currentOffset

            // FAB is visible only when NOT scrolling down (or at the top)
            !isScrollingDown || (currentIndex == 0 && currentOffset == 0)
        }
    }

    SakuTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        var showScanSheet by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState()
        Scaffold(
floatingActionButton = {
    AnimatedVisibility(
        visible = isFabVisible && currentRoute == Routes.DASHBOARD,
        enter = scaleIn(),
        exit = scaleOut()
    ) {
    FloatingActionButton(
        containerColor = SakuTheme.palette.ink,
        onClick = {
            showScanSheet = true
        }
    ) {
        Icon(
            SakuIcons.Add,
            contentDescription = "Add",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )

    }
    }
}
        ){ _ ->

        Box(Modifier.fillMaxSize().padding(top = 24.dp)) {
            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardRoute(
                        onAdd = { navController.navigate(Routes.ADD) },
                        onOpenReview = { navController.navigate(Routes.REVIEW) },
                        onOpenTracking = { navController.navigate(Routes.TRACKING) },
                        onOpenTransaction = { id -> navController.navigate(Routes.detail(id)) },
                        onOpenAll = { navController.navigate(Routes.TRANSACTIONS) },
                    )
                }
                composable(Routes.TRANSACTIONS) {
                    TransactionListRoute(
                        onOpen = { id -> navController.navigate(Routes.detail(id)) },
                        onAdd = { navController.navigate(Routes.ADD) },
                    )
                }
                composable(Routes.ADD) { AddEditRoute(onDone = { navController.popBackStack() }) }
                composable(
                    Routes.EDIT,
                    arguments = listOf(androidx.navigation.navArgument("transactionId") {
                        type = androidx.navigation.NavType.StringType
                    }),
                ) { entry ->
                    AddEditRoute(
                        transactionId = entry.arguments?.getString("transactionId"),
                        onDone = { navController.popBackStack() },
                    )
                }
                composable(
                    Routes.DETAIL,
                    arguments = listOf(androidx.navigation.navArgument("transactionId") {
                        type = androidx.navigation.NavType.StringType
                    }),
                ) { entry ->
                    val id = entry.arguments?.getString("transactionId")
                    if (id != null) {
                        DetailRoute(
                            transactionId = id,
                            onEdit = { navController.navigate(Routes.edit(id)) },
                            onDeleted = { navController.popBackStack() },
                        )
                    }
                }
                composable(Routes.REVIEW) {
                    ReviewQueueRoute(
                        onEdit = { id -> navController.navigate(Routes.edit(id)) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.TRACKING) {
                    TrackingRoute(onBack = { navController.popBackStack() })
                }
                composable(Routes.RULES) {
                    TrackingRoute(onBack = null)
                }
                composable(Routes.SCANNER) {
                    ScannerScreen(onBack = { navController.popBackStack() })
                }
            }

//                Box(
//                    modifier = Modifier
//                        .align(Alignment.BottomCenter)
//                        .navigationBarsPadding()
//                        .padding(horizontal = 24.dp, vertical = 16.dp),
//                ) {
//                    PillNavigation(
//                        currentRoute = currentRoute,
//                        onSelect = { dest ->
//                            navController.navigate(dest.route) {
//                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
//                                launchSingleTop = true
//                                restoreState = true
//                            }
//                        },
//                        onScanAdd = { showScanSheet = true },
//                    )
//                }
        }
        }

        if (showScanSheet) {
            QuickActionSheet(
                sheetState = sheetState,
                onDismiss = { showScanSheet = false },
                onScanReceipt = {
                    showScanSheet = false
                    navController.navigate(Routes.SCANNER)
                },
                onManualEntry = {
                    showScanSheet = false
                    navController.navigate(Routes.ADD)
                },
            )
        }
    }
}
