package com.mysterynumbers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import com.mysterynumbers.market.FIXED_MARKET_ITEMS
import com.mysterynumbers.market.MarketCodeResult
import com.mysterynumbers.market.MarketItemKind
import com.mysterynumbers.market.buildMarketEndpoint
import com.mysterynumbers.market.marketItemForCode
import com.mysterynumbers.market.parseMarketResponse

class AppTest {
    @Test
    fun parsesTencentIndexResponse() {
        val response = "v_s_sh000001=\"x~上证指数~000001~3391.88~12.34~0.36~...\";"
        val quote = parseMarketResponse(response).single()
        assertEquals("上证指数", quote.name)
        assertEquals(3391.88, quote.price)
        assertEquals(12.34, quote.change)
        assertEquals(0.36, quote.changePercent)
    }

    @Test
    fun identifiesShanghaiAndShenzhenCodes() {
        val sh = assertIs<MarketCodeResult.Success>(marketItemForCode("600519")).item
        val star = assertIs<MarketCodeResult.Success>(marketItemForCode("688001")).item
        val sz = assertIs<MarketCodeResult.Success>(marketItemForCode("000858")).item
        val gem = assertIs<MarketCodeResult.Success>(marketItemForCode("300750")).item

        assertEquals("sh600519", sh.requestCode)
        assertEquals("sh688001", star.requestCode)
        assertEquals("sz000858", sz.requestCode)
        assertEquals("sz300750", gem.requestCode)
        assertEquals(MarketItemKind.CUSTOM, sh.kind)
    }

    @Test
    fun keepsFixedIndicesAndRejectsSubscriptionCodes() {
        val index = assertIs<MarketCodeResult.Success>(marketItemForCode("000001")).item
        assertEquals("s_sh000001", index.requestCode)
        assertEquals(MarketItemKind.FIXED, index.kind)

        val scienceIndex = assertIs<MarketCodeResult.Success>(marketItemForCode("000688")).item
        assertEquals("s_sh000688", scienceIndex.requestCode)

        val subscription = assertIs<MarketCodeResult.Failure>(marketItemForCode("787981"))
        assertTrue(subscription.message.contains("688"))
    }

    @Test
    fun buildsEndpointInDisplayOrder() {
        val first = assertIs<MarketCodeResult.Success>(marketItemForCode("600519")).item
        val second = assertIs<MarketCodeResult.Success>(marketItemForCode("000858")).item
        val endpoint = buildMarketEndpoint(listOf(second, first) + FIXED_MARKET_ITEMS)
        assertEquals(
            "https://qt.gtimg.cn/q=sz000858,sh600519,s_sh000001,s_sz399001,s_sz399006,s_sh000688",
            endpoint,
        )
    }

    @Test
    fun parsesCustomStockNameAndChangeFields() {
        val item = assertIs<MarketCodeResult.Success>(marketItemForCode("002008")).item
        val fields = MutableList(33) { "" }
        fields[1] = "大族激光"
        fields[2] = "002008"
        fields[3] = "25.10"
        fields[31] = "0.42"
        fields[32] = "1.70"
        val quote = parseMarketResponse("v_sz002008=\"${fields.joinToString("~")}\";", listOf(item)).single()

        assertEquals("大族激光", quote.name)
        assertEquals(25.10, quote.price)
        assertEquals(0.42, quote.change)
        assertEquals(1.70, quote.changePercent)
    }
}
