package com.mysterynumbers.market

const val MARKET_ENDPOINT = "https://qt.gtimg.cn/q=s_sh000001,s_sz399001,s_sz399006,s_sh000688"

private val symbols = listOf("s_sh000001", "s_sz399001", "s_sz399006", "s_sh000688")

fun parseMarketResponse(text: String): List<MarketQuote> {
    val normalized = text.replace("\uFEFF", "").replace("\u0000", "").trim()
    return symbols.mapNotNull { symbol ->
        val prefix = "v_$symbol=\""
        val start = normalized.indexOf(prefix)
        if (start < 0) return@mapNotNull null
        val contentStart = start + prefix.length
        val end = normalized.indexOf('"', contentStart)
        if (end < 0) return@mapNotNull null
        val fields = normalized.substring(contentStart, end).split('~')
        if (fields.size <= 5) return@mapNotNull null
        val price = fields[3].toDoubleOrNull() ?: return@mapNotNull null
        val change = fields[4].toDoubleOrNull() ?: return@mapNotNull null
        val percent = fields[5].toDoubleOrNull() ?: return@mapNotNull null
        MarketQuote(symbol, fields[1].ifBlank { symbol }, fields[2], price, change, percent)
    }
}

expect fun decodeMarketResponse(bytes: ByteArray): String?
