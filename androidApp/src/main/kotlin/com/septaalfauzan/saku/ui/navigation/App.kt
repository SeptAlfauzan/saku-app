package com.septaalfauzan.saku.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.septaalfauzan.saku.ui.addedit.AddEditRoute
import com.septaalfauzan.saku.ui.dashboard.DashboardRoute
import com.septaalfauzan.saku.ui.detail.DetailRoute
import com.septaalfauzan.saku.ui.theme.SakuTheme
import com.septaalfauzan.saku.ui.transactions.TransactionListRoute

object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val ADD = "add"
    const val EDIT = "edit/{transactionId}"
    const val DETAIL = "detail/{transactionId}"
    fun edit(id: String) = "edit/$id"
    fun detail(id: String) = "detail/$id"
}

private data class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    NavDestination(Routes.DASHBOARD, "Dashboard", Icons.Default.Home),
    NavDestination(Routes.TRANSACTIONS, "Transactions", Icons.AutoMirrored.Filled.List),
)

@Composable
fun App() {
    SakuTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val showBottomBar = currentRoute == Routes.DASHBOARD || currentRoute == Routes.TRANSACTIONS

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomDestinations.forEach { destination ->
                            val selected = backStackEntry?.destination?.hierarchy
                                ?.any { it.route == destination.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (showBottomBar) {
                    FloatingActionButton(onClick = { navController.navigate(Routes.ADD) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add transaction")
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Routes.DASHBOARD) { DashboardRoute(onAdd = { navController.navigate(Routes.ADD) }) }
                composable(Routes.TRANSACTIONS) { TransactionListRoute(onOpen = { id -> navController.navigate(Routes.detail(id)) }, onAdd = { navController.navigate(Routes.ADD) }) }
                composable(Routes.ADD) { AddEditRoute(onDone = { navController.popBackStack() }) }
                composable(
                    Routes.EDIT,
                    arguments = listOf(androidx.navigation.navArgument("transactionId") { type = androidx.navigation.NavType.StringType }),
                ) { entry -> AddEditRoute(transactionId = entry.arguments?.getString("transactionId"), onDone = { navController.popBackStack() }) }
                composable(
                    Routes.DETAIL,
                    arguments = listOf(androidx.navigation.navArgument("transactionId") { type = androidx.navigation.NavType.StringType }),
                ) { entry ->
                    val id = entry.arguments?.getString("transactionId")
                    if (id != null) DetailRoute(transactionId = id, onEdit = { navController.navigate(Routes.edit(id)) }, onDeleted = { navController.popBackStack() })
                }
            }
        }
    }
}
