import Foundation
import CoreFoundation

struct MarketQuote: Identifiable, Codable, Hashable {
    let id: String
    let name: String
    let symbol: String
    let price: Double
    let change: Double
    let changePercent: Double
    let updatedAt: Date

    var isUp: Bool { change >= 0 }
    var signedChange: String { String(format: "%@%.2f", isUp ? "+" : "", change) }
    var signedPercent: String { String(format: "%@%.2f%%", isUp ? "+" : "", changePercent) }
}

enum MarketService {
    static let endpoint = URL(string: "https://qt.gtimg.cn/q=s_sh000001,s_sz399001,s_sz399006,s_sh000688")!
    private static let fallbackEndpoint = URL(string: "http://qt.gtimg.cn/q=s_sh000001,s_sz399001,s_sz399006,s_sh000688")!
    static let symbols = ["s_sh000001", "s_sz399001", "s_sz399006", "s_sh000688"]
    private static let indexSymbols = Set(symbols)

    static func fetchQuotes() async throws -> [MarketQuote] {
        var lastError: Error = URLError(.cannotLoadFromNetwork)
        for url in [endpoint, fallbackEndpoint] {
            do {
                var request = URLRequest(url: url)
                request.timeoutInterval = 8
                request.cachePolicy = .reloadIgnoringLocalCacheData
                request.setValue("Mozilla/5.0 (Macintosh; Intel Mac OS X)", forHTTPHeaderField: "User-Agent")
                request.setValue("https://gu.qq.com/", forHTTPHeaderField: "Referer")
                let (data, response) = try await URLSession.shared.data(for: request)
                guard let http = response as? HTTPURLResponse, 200..<300 ~= http.statusCode else {
                    throw URLError(.badServerResponse)
                }
                let quotes = parse(data: data)
                if !quotes.isEmpty { return quotes }
                throw URLError(.cannotParseResponse)
            } catch {
                lastError = error
            }
        }
        throw lastError
    }

    static func parse(data: Data, now: Date = .now) -> [MarketQuote] {
        guard let text = decodeResponse(data) else { return [] }
        var quotes: [MarketQuote] = []
        for symbol in symbols {
            let prefix = "v_\(symbol)=\""
            guard let start = text.range(of: prefix)?.upperBound,
                  let end = text[start...].firstIndex(of: "\"") else { continue }
            let fields = String(text[start..<end]).split(separator: "~", omittingEmptySubsequences: false).map(String.init)
            guard fields.count > 5,
                  let price = number(fields[3]) else { continue }

            // Tencent returns a compact index record for these symbols:
            // [3] current price, [4] absolute change, [5] percent change.
            // A full stock record uses [31]/[32], so keep that format as a
            // fallback for callers that later add stock symbols.
            let change: Double
            let percent: Double
            if indexSymbols.contains(symbol) {
                guard let indexChange = number(fields[4]),
                      let indexPercent = number(fields[5]) else { continue }
                change = indexChange
                percent = indexPercent
            } else {
                guard fields.count > 32,
                      let stockChange = number(fields[31]),
                      let stockPercent = number(fields[32]) else { continue }
                change = stockChange
                percent = stockPercent
            }
            quotes.append(MarketQuote(
                id: symbol,
                name: fields[1].isEmpty ? symbol : fields[1],
                symbol: fields[2],
                price: price,
                change: change,
                changePercent: percent,
                updatedAt: now
            ))
        }
        return quotes
    }

    private static func number(_ value: String) -> Double? {
        Double(value.trimmingCharacters(in: .whitespacesAndNewlines))
    }

    private static func decodeResponse(_ data: Data) -> String? {
        let gb18030 = String.Encoding(rawValue: CFStringConvertEncodingToNSStringEncoding(
            CFStringEncoding(CFStringEncodings.GB_18030_2000.rawValue)
        ))
        // GB18030 is a superset of the GBK byte sequences used by this
        // endpoint. Prefer valid UTF-8, then GB18030, and remove transport
        // padding before parsing.
        let decoded = String(data: data, encoding: .utf8)
            ?? String(data: data, encoding: gb18030)
        return decoded?.trimmingCharacters(in: CharacterSet(charactersIn: "\u{FEFF}\u{0000}\r\n"))
    }
}

extension Double {
    var marketPrice: String { String(format: "%.2f", self) }
}
