package com.gokova.myanimelist.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp), // Chips, tooltips, tags
        small = RoundedCornerShape(8.dp), // Small badges, status indicators
        medium = RoundedCornerShape(16.dp), // Cards and content containers
        large = RoundedCornerShape(24.dp), // Buttons and larger cards
        extraLarge = RoundedCornerShape(32.dp), // Dialogs, bottom sheets, full modals
    )
