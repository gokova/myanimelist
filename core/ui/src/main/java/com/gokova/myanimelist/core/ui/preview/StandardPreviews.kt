package com.gokova.myanimelist.core.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Phone Light",
    group = "Theme & Device",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    device = "spec:width=390dp,height=844dp,dpi=320",
)
@Preview(
    name = "Phone Dark",
    group = "Theme & Device",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=390dp,height=844dp,dpi=320",
)
@Preview(
    name = "Tablet Light",
    group = "Theme & Device",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    device = "spec:width=840dp,height=1200dp,dpi=320",
)
@Preview(
    name = "Tablet Dark",
    group = "Theme & Device",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=840dp,height=1200dp,dpi=320",
)
@Preview(
    name = "Phone Light - Large Font",
    group = "Accessibility",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    fontScale = 1.5f,
    device = "spec:width=390dp,height=844dp,dpi=320",
)
@Preview(
    name = "Phone Dark - Large Font",
    group = "Accessibility",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 1.5f,
    device = "spec:width=390dp,height=844dp,dpi=320",
)
@Preview(
    name = "Phone Landscape",
    group = "Orientation",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    device = "spec:width=844dp,height=390dp,dpi=320",
)
annotation class StandardPreviews
