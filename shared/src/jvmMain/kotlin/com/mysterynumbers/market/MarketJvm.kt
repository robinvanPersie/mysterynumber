package com.mysterynumbers.market

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

actual fun currentTimeLabel(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
actual fun formatDecimal(value: Double, decimals: Int): String = "%1$.${decimals}f".format(Locale.getDefault(), value)

actual fun decodeMarketResponse(bytes: ByteArray): String? {
    val utf8 = runCatching {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(java.nio.ByteBuffer.wrap(bytes))
            .toString()
    }.getOrNull()?.cleanResponse()
    if (utf8?.looksUsable() == true) return utf8

    return runCatching { bytes.toString(Charset.forName("GB18030")) }
        .getOrNull()
        ?.cleanResponse()
        ?.takeIf { it.looksUsable() }
}

private fun String.cleanResponse(): String = trim('\uFEFF', '\u0000', '\r', '\n', ' ', '\t')
private fun String.looksUsable(): Boolean =
    contains("v_s_sh000001=") && contains('~') && !contains('\uFFFD')
