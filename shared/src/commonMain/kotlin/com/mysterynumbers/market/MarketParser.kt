package com.mysterynumbers.market

const val MARKET_BASE_ENDPOINT = "https://qt.gtimg.cn/q="
const val MARKET_ENDPOINT = MARKET_BASE_ENDPOINT + "s_sh000001,s_sz399001,s_sz399006,s_sh000688"

fun buildMarketEndpoint(items: List<MarketItem>): String =
    MARKET_BASE_ENDPOINT + items.joinToString(",") { it.requestCode }

fun parseMarketResponse(
    text: String,
    items: List<MarketItem> = FIXED_MARKET_ITEMS,
): List<MarketQuote> {
    val normalized = text.replace("\uFEFF", "").replace("\u0000", "").trim()
    return items.mapNotNull { item ->
        val symbol = item.requestCode
        val prefix = "v_$symbol=\""
        val start = normalized.indexOf(prefix)
        if (start < 0) return@mapNotNull null
        val contentStart = start + prefix.length
        val end = normalized.indexOf('"', contentStart)
        if (end < 0) return@mapNotNull null
        val fields = normalized.substring(contentStart, end).split('~')
        val changeIndex = if (item.kind == MarketItemKind.FIXED) 4 else 31
        val percentIndex = if (item.kind == MarketItemKind.FIXED) 5 else 32
        if (fields.size <= percentIndex) return@mapNotNull null
        val price = fields[3].toDoubleOrNull() ?: return@mapNotNull null
        val change = fields[changeIndex].toDoubleOrNull() ?: return@mapNotNull null
        val percent = fields[percentIndex].toDoubleOrNull() ?: return@mapNotNull null
        MarketQuote(symbol, fields[1].ifBlank { item.displayName ?: symbol }, fields[2], price, change, percent, item.isCustom)
    }
}

expect fun decodeMarketResponse(bytes: ByteArray): String?
