package com.septaalfauzan.saku.ui.navigation

import android.net.Uri
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class RoutesTest {

    @Test
    fun scanSharedImageEncodesUri() {
        val route = Routes.scanSharedImage(
            Uri.parse("content://com.example.provider/images/1")
        )
        assertEquals(
            "scanner?imageUri=content%3A%2F%2Fcom.example.provider%2Fimages%2F1",
            route,
        )
    }

    @Test
    fun scanSharedImageNullFallsBackToBaseRoute() {
        assertEquals("scanner", Routes.scanSharedImage(null))
    }

    @Test
    fun scannerBaseRouteIsPlainScanner() {
        assertEquals("scanner", Routes.SCANNER)
    }

    @Test
    fun scannerPatternCarriesImageUriArg() {
        assertEquals("scanner?imageUri={imageUri}", Routes.SCANNER_PATTERN)
    }

    @Test
    fun successRouteHasTitleAndDescriptionParams() {
        assertEquals("success?title={title}&description={description}", Routes.SUCCESS)
    }

    @Test
    fun successScreenBuildsRouteWithArgs() {
        assertEquals(
            "success?title=Berhasil!&description=Transaksi berhasil ditambahkan.",
            Routes.successScreen("Berhasil!", "Transaksi berhasil ditambahkan."),
        )
    }

    @Test
    fun successScreenHandlesNullArgs() {
        assertEquals(
            "success?title=Berhasil!&description=null",
            Routes.successScreen("Berhasil!", null),
        )
    }
}