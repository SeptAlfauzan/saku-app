package com.septaalfauzan.saku

import android.content.Intent
import android.net.Uri
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class MainActivityShareIntentTest {

    private val imageUri = Uri.parse("content://media/images/42")

    private fun sendIntent(type: String, withStream: Boolean = true): Intent {
        val intent = Intent(Intent.ACTION_SEND).setType(type)
        if (withStream) intent.putExtra(Intent.EXTRA_STREAM, imageUri)
        return intent
    }

    @Test
    fun imageSendWithStreamExtractsUri() {
        assertEquals(imageUri, sharedImageUriFrom(sendIntent("image/jpeg")))
    }

    @Test
    fun imageSendWithoutStreamIsNull() {
        assertNull(sharedImageUriFrom(sendIntent("image/*", withStream = false)))
    }

    @Test
    fun nonImageSendIsNull() {
        assertNull(sharedImageUriFrom(sendIntent("text/plain")))
    }

    @Test
    fun wrongActionIsNull() {
        val intent = Intent(Intent.ACTION_VIEW)
            .setType("image/jpeg")
            .putExtra(Intent.EXTRA_STREAM, imageUri)
        assertNull(sharedImageUriFrom(intent))
    }
}