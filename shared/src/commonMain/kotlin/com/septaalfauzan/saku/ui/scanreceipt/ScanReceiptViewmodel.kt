package com.septaalfauzan.saku.ui.scanreceipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.septaalfauzan.saku.domain.usecase.GetReceiptValue
import com.septaalfauzan.saku.util.Logger
import kotlinx.coroutines.launch

data class ScanReceipUiState(
    val data: String
)

class ScanReceiptViewmodel(
    private val getReceiptValue: GetReceiptValue
) : ViewModel(){
    fun scanReceipt(){
        viewModelScope.launch {
          val result =  getReceiptValue.invoke("", "image/jpeg")
            Logger.d("ScanReceiptViewmodel", "result $result")
        }
    }
}