package com.septaalfauzan.saku.ui.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.domain.model.AddEditUiState
import com.septaalfauzan.saku.domain.model.Receipt
import com.septaalfauzan.saku.ui.addedit.AddEditRoute
import com.septaalfauzan.saku.ui.addedit.ScanPrefill
import com.septaalfauzan.saku.ui.components.QuickActionSheet
import com.septaalfauzan.saku.ui.dashboard.DashboardRoute
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import com.septaalfauzan.saku.ui.detail.DetailRoute
import com.septaalfauzan.saku.ui.review.ReviewQueueRoute
import com.septaalfauzan.saku.ui.scanner.ScannerScreen
import com.septaalfauzan.saku.ui.configure.ConfigureParserRoute
import com.septaalfauzan.saku.ui.tracking.TrackingRoute
import com.septaalfauzan.saku.ui.transactions.TransactionListRoute
import com.septaalfauzan.saku.ui.settings.SettingsRoute
import com.septaalfauzan.saku.ui.importexport.ExportCsvRoute
import com.septaalfauzan.saku.ui.importexport.ImportCsvRoute
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.scanner.SuccessScreen
import kotlinx.serialization.json.Json

object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val ADD = "add?prefill={prefill}"
    const val EDIT = "edit/{transactionId}?prefill={prefill}&editingScan={editingScan}"
    const val DETAIL = "detail/{transactionId}"
    const val REVIEW = "review"
    const val TRACKING = "tracking"
    const val RULES = "rules"
    const val SCANNER = "scanner"
    const val SCANNER_PATTERN = "$SCANNER?imageUri={imageUri}"
    const val CONFIGURE_PARSER = "configure-parser"
    const val SETTINGS = "settings"
    const val EXPORT = "export"
    const val IMPORT = "import"
    const val SUCCESS = "success?title={title}&description={description}"
    fun edit(id: String) = "edit/$id"
    fun detail(id: String) = "detail/$id"
    fun editScan(receipt: Receipt): String {
        val json = Json.encodeToString(
            ScanPrefill.serializer(),
            ScanPrefill.fromReceipt(receipt),
        )
        return "edit/editscan?prefill=${Uri.encode(json)}&editingScan=true"
    }

    fun scanSharedImage(sharedImageUri: Uri?): String {
        return sharedImageUri?.let {
            "$SCANNER?imageUri=${Uri.encode(it.toString())}"
        } ?: SCANNER
    }

    fun successScreen(title: String?, description: String?): String {
        return "success?title=$title&description=$description"
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(sharedImageUri: Uri?) {
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
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var showScanSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(sharedImageUri) {
        sharedImageUri?.let { uri ->
            navController.navigate(
                Routes.scanSharedImage(uri)
            )
        }
    }


    SakuTheme {
        Scaffold(
            floatingActionButton = {
                AnimatedVisibility(
                    visible = isFabVisible && currentRoute == Routes.DASHBOARD,
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    FloatingActionButton(
                        containerColor = SakuTheme.palette.ink,
                        contentColor = if (isSystemInDarkTheme()) SakuTheme.palette.crimson else Color.White,
                        onClick = {
                            showScanSheet = true
                        }
                    ) {
                        Icon(
                            SakuIcons.Add,
                            contentDescription = stringResource(R.string.common_add),
                            modifier = Modifier.size(22.dp)
                        )

                    }
                }
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(top = 24.dp)
            ) {
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
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        )
                    }
                    composable(Routes.TRANSACTIONS) {
                        TransactionListRoute(
                            onOpen = { id -> navController.navigate(Routes.detail(id)) },
                            onAdd = { navController.navigate(Routes.ADD) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        Routes.ADD,
                        arguments = listOf(
                            navArgument("prefill") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                        ),
                    ) { entry ->
                        val prefill = entry.arguments?.getString("prefill")
                        AddEditRoute(
                            prefillJson = prefill,
                            onBack = {
                                navController.popBackStack()
                            },
                            onDone = { formUiState ->
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(
                                        "save_result",
                                        Json.encodeToString(
                                            AddEditUiState.serializer(),
                                            formUiState
                                        ),
                                    )
                                navController.navigate(
                                    Routes.successScreen(
                                        title = "Berhasil!",
                                        description = if (prefill != null) "Transaksi berhasil diubah." else "Transaksi baru berhasil ditambahkan."
                                    )
                                )
                            },
                        )
                    }
                    composable(
                        Routes.EDIT,
                        arguments = listOf(
                            navArgument("transactionId") {
                                type = NavType.StringType
                            },
                            navArgument("prefill") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                            navArgument("editingScan") {
                                type = NavType.BoolType
                                defaultValue = false
                            },
                        ),
                    ) { entry ->
                        val transactionId = entry.arguments?.getString("transactionId")
                        val prefill = entry.arguments?.getString("prefill")
                        val editingScan = entry.arguments?.getBoolean("editingScan") ?: false
                        AddEditRoute(
                            transactionId = transactionId,
                            prefillJson = prefill,
                            editingScan = editingScan,
                            onBack = {
                                navController.popBackStack()
                            },
                            onDone = { formUiState ->
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(
                                        "save_result",
                                        Json.encodeToString(
                                            AddEditUiState.serializer(),
                                            formUiState
                                        ),
                                    )
                                if (editingScan) navController.popBackStack() else navController.navigate(
                                    Routes.successScreen(
                                        title = "Berhasil!",
                                        description = if (prefill != null) "Transaksi berhasil diubah." else "Transaksi baru berhasil ditambahkan."
                                    )
                                )
                            },
                        )
                    }
                    composable(
                        Routes.SUCCESS,
                        arguments = listOf(
                            navArgument("title") {
                                type = NavType.StringType
                            },
                            navArgument("description") {
                                type = NavType.StringType
                            },
                        ),
                    ) { entry ->
                        SuccessScreen(
                            title = entry.arguments?.getString("title") ?: "Berhasil!",
                            description = entry.arguments?.getString("description"),
                            onOkText = "Kembali ke Halaman Utama",
                            onOk = {
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                }
                            },
                        )
                    }
                    composable(
                        Routes.DETAIL,
                        arguments = listOf(navArgument("transactionId") {
                            type = NavType.StringType
                        }),
                    ) { entry ->
                        val id = entry.arguments?.getString("transactionId")
                        if (id != null) {
                            DetailRoute(
                                transactionId = id,
                                onEdit = { navController.navigate(Routes.edit(id)) },
                                onDeleted = { navController.popBackStack() },
                                onBack = { navController.popBackStack() },
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
                        TrackingRoute(
                            onBack = { navController.popBackStack() },
                            onConfigureParser = { navController.navigate(Routes.CONFIGURE_PARSER) },
                        )
                    }
                    composable(Routes.CONFIGURE_PARSER) {
                        ConfigureParserRoute(onBack = { navController.popBackStack() })
                    }
                    composable(Routes.RULES) {
                        TrackingRoute(onBack = null)
                    }
                    composable(
                        Routes.SCANNER_PATTERN,
                        arguments = listOf(navArgument("imageUri") {
                            type = NavType.StringType
                            defaultValue = ""
                        }),
                    ) { entry ->
                        val saveJson = navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.get<String>("save_result")
                        val sharedImage = entry.arguments?.getString("imageUri")

                        ScannerScreen(
                            sharedImagUri = sharedImage,
                            onBack = {
                                navController.navigate(
                                    Routes.successScreen(
                                        title = "Berhasil!",
                                        description = "Transaksi baru berhasil ditambahkan."
                                    )
                                )

                            },
                            saveJsonEdit = saveJson,
                            onEdit = { receipt ->
                                navController.navigate(Routes.editScan(receipt))
                            },
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsRoute(
                            onBack = { navController.popBackStack() },
                            onOpenExport = { navController.navigate(Routes.EXPORT) },
                            onOpenImport = { navController.navigate(Routes.IMPORT) },
                        )
                    }
                    composable(Routes.EXPORT) {
                        ExportCsvRoute(onBack = { navController.popBackStack() })
                    }
                    composable(Routes.IMPORT) {
                        ImportCsvRoute(onBack = { navController.popBackStack() })
                    }
                }

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
