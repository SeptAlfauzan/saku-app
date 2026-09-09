package com.septaalfauzan.saku.ui.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

fun categoryGlyph(categoryId: String?): ImageVector = when (categoryId) {
    "food" -> Icons.Outlined.Restaurant
    "transport" -> Icons.Outlined.DirectionsTransit
    "shopping" -> Icons.Outlined.ShoppingCart
    "bills" -> Icons.Outlined.Receipt
    "entertainment" -> Icons.Outlined.Movie
    "health" -> Icons.Outlined.LocalHospital
    "education" -> Icons.Outlined.School
    "insurance" -> Icons.Outlined.Verified
    "salary" -> Icons.Outlined.AccountBalance
    "freelance" -> Icons.Outlined.Work
    "cashback" -> Icons.Outlined.Savings
    "interest" -> Icons.Outlined.Savings
    else -> Icons.Outlined.MoreHoriz
}

fun categoryLabel(categoryId: String?): String = when (categoryId) {
    "food" -> "Food"
    "transport" -> "Transport"
    "shopping" -> "Shopping"
    "bills" -> "Bills"
    "entertainment" -> "Entertainment"
    "health" -> "Health"
    "education" -> "Education"
    "insurance" -> "Insurance"
    "salary" -> "Salary"
    "freelance" -> "Freelance"
    "cashback" -> "Cashback"
    "interest" -> "Interest"
    else -> "Other"
}
