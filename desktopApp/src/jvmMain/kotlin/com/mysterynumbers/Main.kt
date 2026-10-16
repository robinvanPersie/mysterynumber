package com.mysterynumbers

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.mysterynumbers.market.MarketViewModel
import com.mysterynumbers.NetworkMarketRepository
import java.awt.EventQueue
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.net.URI
import java.awt.Desktop
import java.awt.Font
import java.awt.Window as AwtWindow
import java.awt.event.WindowAdapter

fun main() = application {
    val macOs = remember { isMacOs() }
    var windowVisible by remember { mutableStateOf(!macOs) }
    val viewModel = remember { MarketViewModel(NetworkMarketRepository()) }
    val windowState = rememberWindowState(size = DpSize(360.dp, 480.dp))

    Window(
        state = windowState,
        visible = windowVisible,
        onCloseRequest = { windowVisible = false },
        title = "神秘数字",
        undecorated = macOs,
        resizable = !macOs,
        alwaysOnTop = macOs,
        transparent = macOs,
    ) {
        if (macOs) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xD9D9D9D9))
                    .border(1.dp, Color(0xFFF5F5F5), RoundedCornerShape(20.dp)),
            ) {
                App(viewModel, { openEndpoint(viewModel.currentEndpoint()) }, ::exitApplication)
            }
        } else {
            App(viewModel, { openEndpoint(viewModel.currentEndpoint()) }, ::exitApplication)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startAutoRefresh()
    }

    DisposableEffect(Unit) {
        val focusListener = if (macOs) WindowFocusHider { windowVisible = false } else null
        if (focusListener != null) {
            EventQueue.invokeLater {
                AwtWindow.getWindows()
                    .filterIsInstance<java.awt.Frame>()
                    .firstOrNull { it.title == "神秘数字" }
                    ?.addWindowFocusListener(focusListener)
            }
        }
        val tray = createTray(
            macOs = macOs,
            onToggle = {
                EventQueue.invokeLater {
                    if (macOs) {
                        val pointer = java.awt.MouseInfo.getPointerInfo()?.location
                        if (pointer != null) {
                            windowState.position = WindowPosition.Absolute((pointer.x - 164).dp, 28.dp)
                        }
                    }
                    windowVisible = !windowVisible
                }
            },
            onShow = {
                EventQueue.invokeLater {
                    if (macOs) {
                        val pointer = java.awt.MouseInfo.getPointerInfo()?.location
                        if (pointer != null) {
                            windowState.position = WindowPosition.Absolute((pointer.x - 164).dp, 28.dp)
                        }
                    }
                    windowVisible = true
                }
            },
            onRefresh = viewModel::refresh,
            onExit = ::exitApplication,
        )
        onDispose {
            if (focusListener != null) {
                AwtWindow.getWindows()
                    .filterIsInstance<java.awt.Frame>()
                    .firstOrNull { it.title == "神秘数字" }
                    ?.removeWindowFocusListener(focusListener)
            }
            tray?.let { SystemTray.getSystemTray().remove(it) }
            viewModel.close()
        }
    }
}

private class WindowFocusHider(private val hide: () -> Unit) : WindowAdapter() {
    override fun windowLostFocus(event: java.awt.event.WindowEvent) {
        hide()
    }
}

private fun isMacOs(): Boolean = System.getProperty("os.name").contains("Mac", ignoreCase = true)

private fun openEndpoint(endpoint: String) {
    runCatching { Desktop.getDesktop().browse(URI(endpoint)) }
}

private fun createTray(
    macOs: Boolean,
    onToggle: () -> Unit,
    onShow: () -> Unit,
    onRefresh: () -> Unit,
    onExit: () -> Unit,
): TrayIcon? {
    if (!SystemTray.isSupported()) return null
    val menu = if (macOs) null else java.awt.PopupMenu().apply {
        val menuFont = windowsTrayMenuFont()
        font = menuFont
        add(java.awt.MenuItem("显示神秘数字").apply {
            font = menuFont
            addActionListener { onShow() }
        })
        add(java.awt.MenuItem("立即刷新").apply {
            font = menuFont
            addActionListener { onRefresh() }
        })
        addSeparator()
        add(java.awt.MenuItem("退出神秘数字").apply {
            font = menuFont
            addActionListener { onExit() }
        })
    }
    val trayIcon = TrayIcon(createTrayImage(), "神秘数字", menu).apply {
        isImageAutoSize = true
        addActionListener { onToggle() }
    }
    return runCatching {
        SystemTray.getSystemTray().add(trayIcon)
        trayIcon
    }.getOrNull()
}

private fun windowsTrayMenuFont(): Font {
    // AWT tray menus use the native Windows menu peer. Checking only the
    // enumerated family names can select a logical font that has no CJK glyphs
    // in a packaged JRE, which renders Chinese labels as empty squares.
    val sample = "显示神秘数字立即刷新退出"
    val candidates = listOf(
        "Microsoft YaHei UI",
        "Microsoft YaHei",
        "SimSun",
        "NSimSun",
        "宋体",
        "Dialog",
        Font.SANS_SERIF,
    )
    return candidates
        .asSequence()
        .map { Font(it, Font.PLAIN, 13) }
        .firstOrNull { it.canDisplayUpTo(sample) == -1 }
        ?: Font(Font.SANS_SERIF, Font.PLAIN, 13)
}

private fun createTrayImage(): BufferedImage {
    val bundledSymbol = Thread.currentThread().contextClassLoader
        .getResourceAsStream("icons/menu-chart-symbol.png")
        ?.use(ImageIO::read)
    if (bundledSymbol != null) return bundledSymbol

    val image = BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.color = java.awt.Color.BLACK
    graphics.stroke = java.awt.BasicStroke(1.7f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND)
    graphics.drawLine(3, 14, 3, 4)
    graphics.drawLine(3, 14, 15, 14)
    graphics.drawLine(5, 11, 8, 8)
    graphics.drawLine(8, 8, 11, 10)
    graphics.drawLine(11, 10, 15, 5)
    graphics.drawLine(13, 5, 15, 5)
    graphics.drawLine(15, 5, 15, 7)
    graphics.dispose()
    return image
}
