import AppKit
import SwiftUI

@MainActor
final class MarketDashboardModel: ObservableObject {
    @Published private(set) var quotes: [MarketQuote] = []
    @Published private(set) var lastUpdated: Date?
    @Published private(set) var errorMessage: String?
    @Published private(set) var isLoading = false

    func refresh() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        do {
            quotes = try await MarketService.fetchQuotes()
            lastUpdated = .now
            errorMessage = nil
        } catch {
            errorMessage = "暂时无法获取行情"
        }
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

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            Divider().padding(.vertical, 10)

            if model.isLoading && model.quotes.isEmpty {
                ProgressView("正在获取行情…")
                    .frame(maxWidth: .infinity, minHeight: 150)
            } else if model.quotes.isEmpty {
                ContentUnavailableView {
                    Label("暂无行情", systemImage: "wifi.exclamationmark")
                } description: {
                    Text(model.errorMessage ?? "请检查网络连接")
                } actions: {
                    Button("重新获取") { refreshNow() }
                }
                .frame(minHeight: 150)
            } else {
                VStack(spacing: 0) {
                    ForEach(model.quotes) { quote in
                        QuoteRow(quote: quote)
                        if quote.id != model.quotes.last?.id {
                            Divider().padding(.leading, 4)
                        }
                    }
                }
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
            Link(destination: MarketService.endpoint) {
                Image(systemName: "arrow.up.right.square")
            }
            .buttonStyle(.borderless)
            .help("打开腾讯行情接口")
        }
        .font(.caption)
        .foregroundStyle(.secondary)
    }

    private func refreshNow() {
        Task { await model.refresh() }
    }
}

private struct QuoteRow: View {
    let quote: MarketQuote

    var body: some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 3) {
                Text(quote.name)
                    .font(.body.weight(.medium))
                Text(quote.symbol)
                    .font(.caption2.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 3) {
                Text(quote.price.marketPrice)
                    .font(.body.monospacedDigit().weight(.semibold))
                Text("\(quote.signedChange)  \(quote.signedPercent)")
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(quote.isUp ? .red : .green)
            }
        }
        .padding(.vertical, 8)
    }
}
