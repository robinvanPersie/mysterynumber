package com.mysterynumbers

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
