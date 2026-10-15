package com.mysterynumbers.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarketDashboard(viewModel: MarketViewModel, onOpenEndpoint: () -> Unit, onExit: () -> Unit) {
    val state = viewModel.state
    MaterialTheme {
        Column(Modifier.padding(16.dp).width(328.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("指数行情", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("主程序每 30 秒自动刷新", color = Color(0xFF4A4A4A), fontSize = 14.sp)
                }
                TextButton(
                    onClick = viewModel::refresh,
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF3F3F3F)),
                ) {
                    Text(if (state.isLoading) "↻" else "↻", fontSize = 27.sp)
                }
            }
            Divider(Modifier.padding(vertical = 10.dp))
            if (state.quotes.isEmpty() && state.isLoading) {
                Row(Modifier.fillMaxWidth().heightIn(min = 150.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.width(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("正在获取行情…")
                }
            } else if (state.quotes.isEmpty()) {
                Column(Modifier.fillMaxWidth().heightIn(min = 150.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("暂无行情", fontWeight = FontWeight.Medium)
                Text(state.errorMessage ?: "请检查网络连接", color = Color.Gray, fontSize = 14.sp)
                    Button(onClick = viewModel::refresh, Modifier.padding(top = 10.dp)) { Text("重新获取") }
                }
            } else {
                LazyColumn {
                    itemsIndexed(state.quotes) { index, quote ->
                        QuoteRow(quote)
                        if (index < state.quotes.lastIndex) Divider()
                    }
                }
            }
            Divider(Modifier.padding(vertical = 10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("●", color = if (state.errorMessage == null) Color(0xFF35A853) else Color(0xFFF29900), fontSize = 10.sp)
                Spacer(Modifier.width(5.dp))
                Text(state.lastUpdated?.let { "更新于 $it" } ?: (state.errorMessage ?: "等待更新"), color = Color(0xFF4A4A4A), fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onOpenEndpoint,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF4A4A4A)),
                ) { Text("↗", fontSize = 18.sp) }
            }
            Divider(Modifier.padding(vertical = 10.dp))
            TextButton(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF454545)),
            ) { Text("◯  退出神秘数字", fontSize = 16.sp) }
        }
    }
}

@Composable
private fun QuoteRow(quote: MarketQuote) {
    val up = quote.change >= 0
    val sign = if (up) "+" else ""
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(quote.name, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(quote.symbol, color = Color(0xFF454545), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatDecimal(quote.price, 2), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
            Text("$sign${formatDecimal(quote.change, 2)}  $sign${formatDecimal(quote.changePercent, 2)}%", color = if (up) Color(0xFFD93025) else Color(0xFF008A2E), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
