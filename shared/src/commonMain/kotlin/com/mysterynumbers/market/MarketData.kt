package com.mysterynumbers.market

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MarketQuote(
    val id: String,
    val name: String,
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val isCustom: Boolean = false,
)

enum class MarketSide { SH, SZ }

enum class MarketItemKind { FIXED, CUSTOM }

data class MarketItem(
    val code: String,
    val side: MarketSide,
    val requestCode: String,
    val kind: MarketItemKind,
    val displayName: String? = null,
) {
    val isCustom: Boolean get() = kind == MarketItemKind.CUSTOM
}

val FIXED_MARKET_ITEMS = listOf(
    MarketItem("000001", MarketSide.SH, "s_sh000001", MarketItemKind.FIXED, "上证指数"),
    MarketItem("399001", MarketSide.SZ, "s_sz399001", MarketItemKind.FIXED, "深证成指"),
    MarketItem("399006", MarketSide.SZ, "s_sz399006", MarketItemKind.FIXED, "创业板指"),
    MarketItem("000688", MarketSide.SH, "s_sh000688", MarketItemKind.FIXED, "科创50"),
)

sealed class MarketCodeResult {
    data class Success(val item: MarketItem) : MarketCodeResult()
    data class Failure(val message: String) : MarketCodeResult()
}

fun marketItemForCode(rawCode: String): MarketCodeResult {
    val code = rawCode.trim()
    if (!code.matches(Regex("\\d{6}"))) {
        return MarketCodeResult.Failure("请输入 6 位数字代码")
    }
    FIXED_MARKET_ITEMS.firstOrNull { it.code == code }?.let {
        return MarketCodeResult.Success(it)
    }
    if (code.startsWith("787")) {
        return MarketCodeResult.Failure("787 开头的是新股申购代码，请输入上市后的 688 开头股票代码")
    }
    val side = when {
        code.startsWith("600") || code.startsWith("601") || code.startsWith("603") ||
            code.startsWith("605") || code.startsWith("688") || code.startsWith("689") -> MarketSide.SH
        code.startsWith("000") || code.startsWith("001") || code.startsWith("002") ||
            code.startsWith("003") || code.startsWith("300") || code.startsWith("301") ||
            code.startsWith("399") -> MarketSide.SZ
        else -> return MarketCodeResult.Failure("暂不支持该代码")
    }
    // Tencent uses the s_ namespace for indices, but plain sh/sz symbols for stocks.
    val prefix = if (side == MarketSide.SH) "sh" else "sz"
    return MarketCodeResult.Success(
        MarketItem(code, side, "$prefix$code", MarketItemKind.CUSTOM),
    )
}

data class MarketUiState(
    val quotes: List<MarketQuote> = emptyList(),
    val customItems: List<MarketItem> = emptyList(),
    val lastUpdated: String? = null,
    val errorMessage: String? = null,
    val inputError: String? = null,
    val isLoading: Boolean = false,
)

interface MarketRepository {
    suspend fun fetchQuotes(items: List<MarketItem>): List<MarketQuote>
}

class MarketViewModel(
    private val repository: MarketRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {
    var state by mutableStateOf(MarketUiState())
        private set

    private var refreshJob: Job? = null
    private var autoRefreshJob: Job? = null

    fun refresh() {
        if (state.isLoading) return
        val items = state.customItems + FIXED_MARKET_ITEMS
        refreshJob?.cancel()
        refreshJob = scope.launch {
            state = state.copy(isLoading = true)
            try {
                val quotes = repository.fetchQuotes(items)
                state = state.copy(
                    quotes = quotes,
                    lastUpdated = currentTimeLabel(),
                    errorMessage = null,
                    isLoading = false,
                )
            } catch (_: Throwable) {
                state = state.copy(isLoading = false, errorMessage = "暂时无法获取行情")
            }
        }
    }

    fun addCode(rawCode: String): Boolean {
        return when (val result = marketItemForCode(rawCode)) {
            is MarketCodeResult.Failure -> {
                state = state.copy(inputError = result.message)
                false
            }
            is MarketCodeResult.Success -> {
                val item = result.item
                if ((state.customItems + FIXED_MARKET_ITEMS).any { it.code == item.code }) {
                    state = state.copy(inputError = "该股票已经添加")
                    false
                } else {
                    state = state.copy(
                        customItems = listOf(item) + state.customItems,
                        inputError = null,
                    )
                    refresh()
                    true
                }
            }
        }
    }

    fun removeCustom(code: String) {
        if (state.customItems.none { it.code == code }) return
        state = state.copy(
            customItems = state.customItems.filterNot { it.code == code },
            quotes = state.quotes.filterNot { it.symbol == code },
            inputError = null,
        )
        refresh()
    }

    fun clearInputError() {
        if (state.inputError != null) state = state.copy(inputError = null)
    }

    fun currentEndpoint(): String = buildMarketEndpoint(state.customItems + FIXED_MARKET_ITEMS)

    fun startAutoRefresh(intervalMillis: Long = 30_000L): Job {
        autoRefreshJob?.cancel()
        val job = scope.launch {
            refresh()
            while (true) {
                delay(intervalMillis)
                refresh()
            }
        }
        autoRefreshJob = job
        return job
    }

    fun close() {
        refreshJob?.cancel()
        autoRefreshJob?.cancel()
        scope.cancel()
    }
}

expect fun currentTimeLabel(): String
expect fun formatDecimal(value: Double, decimals: Int): String
