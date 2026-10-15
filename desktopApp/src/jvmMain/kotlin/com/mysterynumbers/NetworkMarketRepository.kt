package com.mysterynumbers

import com.mysterynumbers.market.MarketItem
import com.mysterynumbers.market.MarketQuote
import com.mysterynumbers.market.MarketRepository
import com.mysterynumbers.market.decodeMarketResponse
import com.mysterynumbers.market.parseMarketResponse
import java.net.HttpURLConnection
import java.net.URI

class NetworkMarketRepository : MarketRepository {
    override suspend fun fetchQuotes(items: List<MarketItem>): List<MarketQuote> {
        val endpoint = com.mysterynumbers.market.buildMarketEndpoint(items)
        val bytes = fetchBytes(URI(endpoint).toURL())
        val text = decodeMarketResponse(bytes) ?: error("无法解码行情响应")
        return parseMarketResponse(text, items).also { if (it.isEmpty()) error("行情响应格式异常") }
    }

    private fun fetchBytes(url: java.net.URL): ByteArray {
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X)")
        connection.setRequestProperty("Referer", "https://gu.qq.com/")
        return try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            connection.inputStream.use { input -> input.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}
