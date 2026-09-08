package com.gokova.myanimelist.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes =
    Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(16.dp), // For cards and containers
        large = RoundedCornerShape(24.dp), // For buttons and larger cards
    )
