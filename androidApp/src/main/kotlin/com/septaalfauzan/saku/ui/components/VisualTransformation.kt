package com.septaalfauzan.saku.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class NumberVisualTransformation(
    private val separator: Char = '.'
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text

        val formatted = original
            .filter { it.isDigit() }
            .reversed()
            .chunked(3)
            .joinToString(separator.toString())
            .reversed()

        return TransformedText(
            text = AnnotatedString(formatted),
            offsetMapping = object : OffsetMapping {

                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 0) return 0

                    val separatorsBefore = (offset - 1) / 3

                    return offset + separatorsBefore
                }

                override fun transformedToOriginal(offset: Int): Int {
                    if (offset <= 0) return 0

                    val separatorsBefore = offset / 4

                    return (offset - separatorsBefore)
                        .coerceAtMost(original.length)
                }
            }
        )
    }
}