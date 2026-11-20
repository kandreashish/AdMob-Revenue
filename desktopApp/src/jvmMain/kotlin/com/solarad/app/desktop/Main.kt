package com.solarad.app.desktop

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.solarad.app.App
import java.awt.Dimension

fun main() = application {
    val windowState = rememberWindowState(width = 1120.dp, height = 780.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "SolarAd - AdMob Dashboard",
        state = windowState,
        icon = painterResource("icons/icon.png")
    ) {
        window.minimumSize = Dimension(420, 560)
        App()
    }
}
