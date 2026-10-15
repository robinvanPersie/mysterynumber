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

enum MarketSide {
    case sh
    case sz
}

enum MarketItemKind {
    case fixed
    case custom
}

struct MarketItem: Identifiable, Hashable {
    let code: String
    let side: MarketSide
    let requestCode: String
    let kind: MarketItemKind
    let displayName: String?

    var id: String { code }
    var isCustom: Bool { kind == .custom }
}

let fixedMarketItems: [MarketItem] = [
    MarketItem(code: "000001", side: .sh, requestCode: "s_sh000001", kind: .fixed, displayName: "上证指数"),
    MarketItem(code: "399001", side: .sz, requestCode: "s_sz399001", kind: .fixed, displayName: "深证成指"),
    MarketItem(code: "399006", side: .sz, requestCode: "s_sz399006", kind: .fixed, displayName: "创业板指"),
    MarketItem(code: "000688", side: .sh, requestCode: "s_sh000688", kind: .fixed, displayName: "科创50"),
]

enum MarketCodeResult {
    case success(MarketItem)
    case failure(String)
}

func marketItemForCode(_ rawCode: String) -> MarketCodeResult {
    let code = rawCode.trimmingCharacters(in: .whitespacesAndNewlines)
    guard code.count == 6, code.allSatisfy(\.isNumber) else {
        return .failure("请输入 6 位数字代码")
    }
    if let fixed = fixedMarketItems.first(where: { $0.code == code }) {
        return .success(fixed)
    }
    if code.hasPrefix("787") {
        return .failure("787 开头的是新股申购代码，请输入上市后的 688 开头股票代码")
    }
    let side: MarketSide
    if ["600", "601", "603", "605", "688", "689"].contains(where: code.hasPrefix) {
        side = .sh
    } else if ["000", "001", "002", "003", "300", "301", "399"].contains(where: code.hasPrefix) {
        side = .sz
    } else {
        return .failure("暂不支持该代码")
    }
    // Tencent uses the s_ namespace for indices, but plain sh/sz symbols for stocks.
    let prefix = side == .sh ? "sh" : "sz"
    return .success(MarketItem(code: code, side: side, requestCode: "\(prefix)\(code)", kind: .custom, displayName: nil))
}

enum MarketService {
    static let baseEndpoint = "https://qt.gtimg.cn/q="

    static func endpoint(for items: [MarketItem]) -> URL {
        URL(string: baseEndpoint + items.map(\.requestCode).joined(separator: ","))!
    }

    static func fetchQuotes(items: [MarketItem]) async throws -> [MarketQuote] {
        let httpsEndpoint = endpoint(for: items)
        let httpEndpoint = URL(string: httpsEndpoint.absoluteString.replacingOccurrences(of: "https://", with: "http://"))!
        var lastError: Error = URLError(.cannotLoadFromNetwork)
        for url in [httpsEndpoint, httpEndpoint] {
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
                let quotes = parse(data: data, items: items)
                if !quotes.isEmpty { return quotes }
                throw URLError(.cannotParseResponse)
            } catch {
                lastError = error
            }
        }
        throw lastError
    }

    static func parse(data: Data, items: [MarketItem], now: Date = .now) -> [MarketQuote] {
        guard let text = decodeResponse(data) else { return [] }
        var quotes: [MarketQuote] = []
        for item in items {
            let symbol = item.requestCode
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
            if item.kind == .fixed {
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
                name: fields[1].isEmpty ? (item.displayName ?? symbol) : fields[1],
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
