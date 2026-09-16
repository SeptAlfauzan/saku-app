package com.septaalfauzan.saku.ui.scanner

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ScannerViewfinderContentTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setContent(
        hasPermission: Boolean,
        scanning: Boolean,
        onGrantPermission: () -> Unit = {},
        onShutter: () -> Unit = {},
        onPickImage: () -> Unit = {},
    ) {
        compose.setContent {
            SakuTheme {
                ScannerViewfinderContent(
                    hasPermission = hasPermission,
                    flashMode = ScannerFlash.OFF,
                    shutterToken = 0,
                    scanning = scanning,
                    onCaptured = {},
                    onGrantPermission = onGrantPermission,
                    onShutter = onShutter,
                    onPickImage = onPickImage,
                )
            }
        }
    }

    @Test
    fun permissionPromptShownWhenNoPermission() {
        setContent(hasPermission = false, scanning = false)
        compose.onNodeWithText("Izin kamera diperlukan").assertExists()
        compose.onNodeWithText("Beri Izin").assertExists()
    }

    @Test
    fun grantButtonInvokesCallback() {
        var granted = false
        setContent(hasPermission = false, scanning = false, onGrantPermission = { granted = true })
        compose.onNodeWithText("Beri Izin").performClick()
        assertTrue(granted)
    }

    @Test
    fun galleryButtonInvokesPickImage() {
        var picked = false
        setContent(hasPermission = false, scanning = false, onPickImage = { picked = true })
        compose.onNodeWithTag("gallery_button").performClick()
        assertTrue(picked)
    }

    @Test
    fun shutterDisabledWhileScanning() {
        setContent(hasPermission = false, scanning = true)
        compose.onNodeWithTag("shutter_button").assertIsNotEnabled()
    }

    @Test
    fun shutterInvokesCallbackWhenNotScanning() {
        var shutter = 0
        setContent(hasPermission = false, scanning = false, onShutter = { shutter++ })
        compose.onNodeWithTag("shutter_button").performClick()
        assertTrue(shutter == 1)
    }
}