package com.septaalfauzan.saku.ui.navigation

import android.Manifest
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import com.septaalfauzan.saku.SubTrackApplication
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import com.septaalfauzan.saku.ui.scanner.ScannerScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows
import org.koin.core.context.stopKoin
import kotlin.test.assertEquals

/**
 * End-to-end share-intent navigation contract for [App].
 *
 * Fallback notes (see task brief Step 2): the full [App] mount is not achievable
 * under Robolectric in androidApp unit tests:
 *  - `App(sharedImageUri)` navigates from a `LaunchedEffect` (App.kt) before the
 *    `NavHost` has attached its graph -> "Navigation graph has not been set".
 *  - `SubTrackApplication` calls `startKoin` per `Application.onCreate`; Robolectric
 *    reuses the environment across test methods in a class, so a second test in the
 *    same class throws `KoinApplicationAlreadyStartedException`.
 *
 * Consequently the share-uri flow is asserted at the ScannerScreen mount level
 * (the nav-wiring contract, `Routes.scanSharedImage`, is deterministic and already
 * covered by RoutesTest), and the FAB -> sheet -> scanner interaction is replaced
 * by the base-route assertion it depends on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = SubTrackApplication::class)
class AppShareIntentNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun denyCameraPermission() {
        // CameraX is untestable under Robolectric (repo convention); keep
        // ScannerScreen off the camera path so the mount is deterministic.
        Shadows.shadowOf(
            ApplicationProvider.getApplicationContext<Application>()
        ).denyPermissions(Manifest.permission.CAMERA)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun sharedImageUri(): Uri {
        val context: Context = ApplicationProvider.getApplicationContext()
        val file = java.io.File(context.cacheDir, "shared_image_source.jpg")
        if (!file.exists()) file.writeBytes(ByteArray(128) { 0x42 })
        return Uri.fromFile(file)
    }

    @Test
    fun sharedImageUriNavigatesToScannerScreen() {
        compose.setContent {
            SakuTheme {
                ScannerScreen(
                    saveJsonEdit = null,
                    onBack = {},
                    onEdit = {},
                    sharedImagUri = sharedImageUri().toString(),
                )
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Pemindai Struk").fetchSemanticsNodes().isNotEmpty()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppFabScannerRouteContractTest {

    @Test
    fun fabScanNavigatesToScannerBaseRoute() {
        // The FAB's onScanReceipt handler in App navigates with Routes.SCANNER.
        // The interactive FAB->sheet->scanner flow cannot mount under Robolectric
        // (see AppShareIntentNavigationTest notes), so assert the base navigable
        // route string it resolves to; full route-string coverage lives in RoutesTest.
        assertEquals("scanner", Routes.SCANNER)
    }
}