package com.mysterynumbers.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarketDashboard(viewModel: MarketViewModel, onOpenEndpoint: () -> Unit, onExit: () -> Unit) {
    val state = viewModel.state
    var inputCode by remember { mutableStateOf("") }
    // Four 52dp rows plus the three separators between them.
    val listViewportHeight = 212.dp
    val items = state.customItems + FIXED_MARKET_ITEMS
    val quotesByCode = state.quotes.associateBy { it.symbol }

    fun submitCode() {
        if (viewModel.addCode(inputCode)) inputCode = ""
    }

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
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = {
                        inputCode = it.filter(Char::isDigit).take(6)
                        viewModel.clearInputError()
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("输入 6 位代码", fontSize = 13.sp) },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitCode() }),
                )
                TextButton(
                    onClick = ::submitCode,
                    modifier = Modifier.height(42.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF3F3F3F)),
                ) { Text("+", fontSize = 25.sp) }
            }
            if (state.inputError != null) {
                Text(
                    state.inputError,
                    color = Color(0xFFB3261E),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }
            Divider(Modifier.padding(vertical = 10.dp))
            Box(Modifier.fillMaxWidth().height(listViewportHeight)) {
                if (state.quotes.isEmpty() && state.isLoading) {
                    Row(Modifier.fillMaxWidth().height(listViewportHeight), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.width(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("正在获取行情…")
                    }
                } else if (state.quotes.isEmpty()) {
                    Column(Modifier.fillMaxWidth().height(listViewportHeight), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("暂无行情", fontWeight = FontWeight.Medium)
                        Text(state.errorMessage ?: "请检查网络连接", color = Color.Gray, fontSize = 14.sp)
                        TextButton(onClick = viewModel::refresh, modifier = Modifier.padding(top = 4.dp)) { Text("重新获取") }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().height(listViewportHeight)) {
                        itemsIndexed(items, key = { _, item -> item.code }) { index, item ->
                            QuoteRow(item, quotesByCode[item.code], viewModel::removeCustom)
                            if (index < items.lastIndex) Divider()
                        }
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
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
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
private fun QuoteRow(item: MarketItem, quote: MarketQuote?, onDelete: (String) -> Unit) {
    val up = quote?.change?.let { it >= 0 } ?: false
    val sign = if (up) "+" else ""
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(quote?.name ?: item.displayName ?: item.code, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(item.code, color = Color(0xFF454545), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(quote?.let { formatDecimal(it.price, 2) } ?: "--", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
            Text(quote?.let { "$sign${formatDecimal(it.change, 2)}  $sign${formatDecimal(it.changePercent, 2)}%" } ?: "暂无行情", color = if (up) Color(0xFFD93025) else Color(0xFF008A2E), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        }
        if (item.isCustom) {
            TextButton(
                onClick = { onDelete(item.code) },
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF777777)),
            ) { Text("×", fontSize = 20.sp) }
        }
    }
}
