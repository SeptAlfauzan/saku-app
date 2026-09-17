package com.septaalfauzan.saku

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.septaalfauzan.saku.ui.navigation.App

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private var sharedImageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            App(sharedImageUri)
        }
    }
    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND &&
            intent.type?.startsWith("image/") == true
        ) {
            val imageUri = intent.getParcelableExtra<Uri>(
                Intent.EXTRA_STREAM
            )

            imageUri?.let {
                sharedImageUri = it
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(null)
}
