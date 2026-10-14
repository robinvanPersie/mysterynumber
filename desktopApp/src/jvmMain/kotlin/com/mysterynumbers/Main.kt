package com.mysterynumbers

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "神秘数字",
    ) {
        App()
    }
}
