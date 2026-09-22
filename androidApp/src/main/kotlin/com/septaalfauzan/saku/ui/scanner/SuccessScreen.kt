package com.septaalfauzan.saku.ui.scanner

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState

@Composable
fun SuccessScreen(
    title: String,
    description: String? = null,
    onOkText: String? = null,
    onOk: () -> Unit,
) {
    val context = LocalContext.current
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            context.assets.open("files/success.json")
                .readBytes()
                .decodeToString()
        )
    }

    val progress by animateLottieCompositionAsState(
        composition = composition
    )

    val textStyle = TextStyle(fontSize = 24.sp)

    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxWidth()
//            .background(Color(0xFF30AE6E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Image(
            painter = rememberLottiePainter(
                composition = composition,
                progress = { progress }
            ),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .scale(1.4f)
            ,
            contentScale = ContentScale.Fit
        )
        Text(title, style = textStyle)
        if (description != null) Text(description, style = textStyle.copy(fontSize = 14.sp))
        Spacer(Modifier.weight(1f))
        Button(onOk, modifier = Modifier.padding(top = 24.dp).fillMaxWidth()) {
            Text(onOkText ?: "Go Home")
        }
    }
}

@Preview
@Composable
private fun Preview() {
    SuccessScreen(
        title = "test",
        description = TODO(),
        onOkText = TODO(),
        onOk = TODO(),
    )
}