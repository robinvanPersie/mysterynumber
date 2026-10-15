import AppKit
import SwiftUI

@MainActor
final class MarketDashboardModel: ObservableObject {
    @Published private(set) var quotes: [MarketQuote] = []
    @Published private(set) var customItems: [MarketItem] = []
    @Published var inputCode = ""
    @Published private(set) var inputError: String?
    @Published private(set) var lastUpdated: Date?
    @Published private(set) var errorMessage: String?
    @Published private(set) var isLoading = false

    var items: [MarketItem] { customItems + fixedMarketItems }

    func quote(for item: MarketItem) -> MarketQuote? {
        quotes.first { $0.id == item.requestCode }
    }

    func refresh() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        do {
            quotes = try await MarketService.fetchQuotes(items: items)
            lastUpdated = .now
            errorMessage = nil
        } catch {
            errorMessage = "暂时无法获取行情"
        }
    }

    func addInputCode() {
        switch marketItemForCode(inputCode) {
        case .failure(let message):
            inputError = message
        case .success(let item):
            guard !items.contains(where: { $0.code == item.code }) else {
                inputError = "该股票已经添加"
                return
            }
            customItems.insert(item, at: 0)
            inputCode = ""
            inputError = nil
            refreshNow()
        }
    }

    func removeCustom(code: String) {
        customItems.removeAll { $0.code == code }
        refreshNow()
    }

    func clearInputError() {
        inputError = nil
    }

    var currentEndpoint: URL { MarketService.endpoint(for: items) }

    private func refreshNow() {
        Task { await refresh() }
    }
}

@main
struct MarketDesktopWidgetApp: App {
    @StateObject private var model = MarketDashboardModel()

    var body: some Scene {
        MenuBarExtra {
            MarketDashboardView(model: model)
        } label: {
            Label("神秘数字", systemImage: "chart.line.uptrend.xyaxis")
        }
        .menuBarExtraStyle(.window)
    }
}

private struct MarketDashboardView: View {
    @ObservedObject var model: MarketDashboardModel
    private let refreshInterval: TimeInterval = 30
    // Four 52pt rows plus the three separators between them.
    private let listViewportHeight: CGFloat = 212

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            inputBar
            Divider().padding(.vertical, 10)

            if model.isLoading && model.quotes.isEmpty {
                ProgressView("正在获取行情…")
                    .frame(maxWidth: .infinity, minHeight: 150)
            } else {
                // Keep the list mounted even while a refresh is incomplete. This
                // gives the menu bar panel a stable scroll viewport and preserves
                // the rows when an individual quote is unavailable.
                ScrollView(.vertical) {
                    LazyVStack(spacing: 0) {
                        ForEach(model.items) { item in
                            QuoteRow(item: item, quote: model.quote(for: item), onDelete: model.removeCustom)
                                .frame(height: 52)
                            if item.id != model.items.last?.id {
                                Divider().padding(.leading, 4)
                            }
                        }
                    }
                }
                .scrollIndicators(.visible)
                .frame(height: listViewportHeight)
            }

            Divider().padding(.vertical, 10)
            footer
            Divider().padding(.vertical, 10)
            Button(role: .destructive) {
                NSApplication.shared.terminate(nil)
            } label: {
                Label("退出神秘数字", systemImage: "power")
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.borderless)
        }
        .padding(16)
        .frame(width: 360)
        .task { await model.refresh() }
        .onReceive(Timer.publish(every: refreshInterval, on: .main, in: .common).autoconnect()) { _ in
            refreshNow()
        }
    }

    private var header: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 3) {
                Text("指数行情")
                    .font(.title3.weight(.semibold))
                Text("主程序每 30 秒自动刷新")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Button(action: refreshNow) {
                Image(systemName: model.isLoading ? "arrow.triangle.2.circlepath" : "arrow.clockwise")
            }
            .buttonStyle(.borderless)
            .help("立即刷新")
            .disabled(model.isLoading)
        }
    }

    private var footer: some View {
        HStack(spacing: 5) {
            Circle()
                .fill(model.errorMessage == nil ? Color.green : Color.orange)
                .frame(width: 6, height: 6)
            if let lastUpdated = model.lastUpdated {
                Text("更新于 \(lastUpdated, style: .time)")
            } else {
                Text(model.errorMessage ?? "等待更新")
            }
            Spacer()
            Link(destination: model.currentEndpoint) {
                Image(systemName: "arrow.up.right.square")
            }
            .buttonStyle(.borderless)
            .help("打开腾讯行情接口")
        }
        .font(.caption)
        .foregroundStyle(.secondary)
    }

    private var inputBar: some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack(spacing: 6) {
                TextField("输入 6 位代码", text: $model.inputCode)
                    .textFieldStyle(.roundedBorder)
                    .onChange(of: model.inputCode) { _, value in
                        let filtered = String(value.filter(\.isNumber).prefix(6))
                        if filtered != value { model.inputCode = filtered }
                        model.clearInputError()
                    }
                    .onSubmit { model.addInputCode() }
                Button {
                    model.addInputCode()
                } label: {
                    Image(systemName: "plus")
                }
                .buttonStyle(.borderless)
                .help("添加股票")
            }
            if let inputError = model.inputError {
                Text(inputError)
                    .font(.caption2)
                    .foregroundStyle(.red)
                    .padding(.leading, 4)
            }
        }
    }

    private func refreshNow() {
        Task { await model.refresh() }
    }
}

private struct QuoteRow: View {
    let item: MarketItem
    let quote: MarketQuote?
    let onDelete: (String) -> Void

    var body: some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 3) {
                Text(quote?.name ?? item.displayName ?? item.code)
                    .font(.body.weight(.medium))
                Text(quote?.symbol ?? item.code)
                    .font(.caption2.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 3) {
                Text(quote?.price.marketPrice ?? "--")
                    .font(.body.monospacedDigit().weight(.semibold))
                Text(quote.map { "\($0.signedChange)  \($0.signedPercent)" } ?? "暂无行情")
                    .font(.caption.monospacedDigit())
                    .foregroundStyle((quote?.isUp ?? false) ? .red : .green)
            }
            if item.isCustom {
                Button {
                    onDelete(item.code)
                } label: {
                    Image(systemName: "xmark")
                }
                .buttonStyle(.borderless)
                .help("删除股票")
            }
        }
        .padding(.vertical, 8)
    }
}
