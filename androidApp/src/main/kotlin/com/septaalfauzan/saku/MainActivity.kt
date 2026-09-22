package com.septaalfauzan.saku

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.septaalfauzan.saku.ui.navigation.App

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private var sharedImageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        handleIntent(intent)
        setContent {
            App(sharedImageUri)
        }
    }

    private fun handleIntent(intent: Intent) {
        sharedImageUri = sharedImageUriFrom(intent)
    }
}

internal fun sharedImageUriFrom(intent: Intent): Uri? {
    if (intent.action != Intent.ACTION_SEND ||
        intent.type?.startsWith("image/") != true
    ) {
        return null
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(Intent.EXTRA_STREAM)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(null)
}