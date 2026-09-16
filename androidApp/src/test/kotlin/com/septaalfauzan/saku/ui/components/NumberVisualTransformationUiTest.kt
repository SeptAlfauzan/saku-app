package com.septaalfauzan.saku.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.KeyboardType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class NumberVisualTransformationUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun textFieldRendersThousandsSeparator() {
        val value = mutableStateOf("")
        compose.setContent {
            TextField(
                value = value.value,
                onValueChange = { value.value = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = NumberVisualTransformation(),
            )
        }
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("1234")
        field.assertTextContains("1.234")
    }
}
