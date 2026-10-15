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
)

data class MarketUiState(
    val quotes: List<MarketQuote> = emptyList(),
    val lastUpdated: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
)

interface MarketRepository {
    suspend fun fetchQuotes(): List<MarketQuote>
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
        refreshJob?.cancel()
        refreshJob = scope.launch {
            state = state.copy(isLoading = true)
            try {
                val quotes = repository.fetchQuotes()
                state = MarketUiState(quotes = quotes, lastUpdated = currentTimeLabel())
            } catch (_: Throwable) {
                state = state.copy(isLoading = false, errorMessage = "暂时无法获取行情")
            }
        }
    }

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
