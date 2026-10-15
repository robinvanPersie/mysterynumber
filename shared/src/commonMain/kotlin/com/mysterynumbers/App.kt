package com.mysterynumbers

import androidx.compose.runtime.Composable
import com.mysterynumbers.market.MarketDashboard
import com.mysterynumbers.market.MarketViewModel

@Composable
fun App(
    viewModel: MarketViewModel,
    onOpenEndpoint: () -> Unit,
    onExit: () -> Unit,
) {
    MarketDashboard(viewModel, onOpenEndpoint, onExit)
}
