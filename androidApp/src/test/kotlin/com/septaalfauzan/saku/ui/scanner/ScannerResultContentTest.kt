package com.septaalfauzan.saku.ui.scanner

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.septaalfauzan.saku.domain.model.Receipt
import com.septaalfauzan.saku.domain.model.ReceiptItem
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import com.septaalfauzan.saku.ui.state.StateUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ScannerResultContentTest {

    @get:Rule
    val compose = createComposeRule()

    private fun sampleReceipt(paymentMethod: String = "-") = Receipt(
        merchantName = "Alfamart",
        transactionDate = "16-09-2026",
        transactionTime = "10:00",
        currency = "IDR",
        subtotal = 10000,
        tax = 0,
        discount = 500,
        total = 9500,
        paymentMethod = paymentMethod,
        items = listOf(ReceiptItem("Nasi", 1, 10000, 10000)),
    )

    private fun setContent(
        state: StateUi<Receipt>,
        onApprove: () -> Unit = {},
        onEdit: (Receipt) -> Unit = {},
        onRetake: () -> Unit = {},
    ) {
        compose.setContent {
            SakuTheme {
                ScannerResultContent(
                    capturedPath = null,
                    scanOcrState = state,
                    onApprove = onApprove,
                    onEdit = onEdit,
                    onRetake = onRetake,
                )
            }
        }
    }

    @Test
    fun successStateRendersReceiptFields() {
        setContent(StateUi.Success(sampleReceipt()))
        compose.onNodeWithText("Alfamart").assertExists()
        compose.onNodeWithText("16-09-2026").assertExists()
        compose.onNodeWithText("-").assertExists()
        compose.onNodeWithText("Rp9.500").assertExists()
    }

    @Test
    fun successStateShowsPaymentMethodFallbackDash() {
        setContent(StateUi.Success(sampleReceipt(paymentMethod = "-")))
        compose.onNodeWithText("-").assertExists()
    }

    @Test
    fun errorStateRendersMessageAndKeepsButtonsEnabled() {
        setContent(StateUi.Error("bad image"))
        compose.onNodeWithText("BAD IMAGE").assertExists()
        compose.onNodeWithTag("approve_button").assertIsEnabled()
        compose.onNodeWithTag("rescan_button").assertIsEnabled()
    }

    @Test
    fun loadingStateShowsProcessingAndDisablesButtons() {
        setContent(StateUi.Loading)
        compose.onNodeWithText("Memproses Struk").assertExists()
        compose.onNodeWithTag("approve_button").assertIsNotEnabled()
        compose.onNodeWithTag("rescan_button").assertIsNotEnabled()
    }

    @Test
    fun approveAndRetakeCallbacksFire() {
        var approved = false
        var retaken = false
        setContent(
            StateUi.Success(sampleReceipt()),
            onApprove = { approved = true },
            onRetake = { retaken = true },
        )
        compose.onNodeWithTag("rescan_button").assertIsEnabled().performScrollTo().performClick()
        compose.onNodeWithTag("approve_button").assertIsEnabled().performScrollTo().performClick()
        org.junit.Assert.assertTrue(approved)
        org.junit.Assert.assertTrue(retaken)
    }

    @Test
    fun successStateShowsItemsFromItemsFallbackWhenNoNote() {
        val receipt = sampleReceipt().copy(
            note = null,
            items = listOf(
                ReceiptItem("Nasi", 2, 20000, 20000),
                ReceiptItem("Teh", 1, 5000, 5000),
            ),
        )
        setContent(StateUi.Success(receipt))
        compose.onNodeWithText("2x Nasi (Rp20.000), 1x Teh (Rp5.000)").assertExists()
    }

    @Test
    fun successStatePrefersNoteOverItems() {
        val receipt = sampleReceipt().copy(note = "Catatan belanja")
        setContent(StateUi.Success(receipt))
        compose.onNodeWithText("Catatan belanja").assertExists()
    }

    @Test
    fun successStateShowsDiscount() {
        val receipt = sampleReceipt().copy(discount = 10000)
        setContent(StateUi.Success(receipt))
        compose.onNodeWithText("Rp10.000").assertExists()
    }

    @Test
    fun editButtonClickFiresOnEditWithReceipt() {
        val receipt = sampleReceipt()
        var edited: Receipt? = null
        setContent(StateUi.Success(receipt), onEdit = { edited = it })
        compose.onNodeWithTag("edit_button").assertIsEnabled().performScrollTo().performClick()
        org.junit.Assert.assertEquals(receipt, edited)
    }
}
