import Network
import SwiftUI

@main
struct PitlaneHQApp: App {
    @StateObject private var account = Account()
    @StateObject private var net = NetMonitor()
    @StateObject private var i18n = I18n.shared
    @StateObject private var updates = Updates()
    @StateObject private var inbox = Inbox()

    var body: some Scene {
        WindowGroup {
            Group {
                if account.signedIn { MainView() } else { LoginView() }
            }
            .id(i18n.lang)
            .environmentObject(account)
            .environmentObject(net)
            .environmentObject(i18n)
            .environmentObject(updates)
            .environmentObject(inbox)
            .task { await updates.check() }
            .task { await inbox.load() }
            .preferredColorScheme(.dark)
            .tint(Theme.accent)
        }
    }
}

/// Whether the phone has a connection, to show the offline banner and reload when it comes back.
@MainActor
final class NetMonitor: ObservableObject {
    @Published var online = true
    @Published var cameBack = 0
    private let monitor = NWPathMonitor()

    init() {
        monitor.pathUpdateHandler = { [weak self] path in
            let on = path.status == .satisfied
            Task { @MainActor in
                guard let self else { return }
                if on && !self.online { self.cameBack += 1 }
                self.online = on
            }
        }
        monitor.start(queue: DispatchQueue(label: "net"))
    }
}

/// A newer TrackIQ for this phone on GitHub (the same release the web's download buttons point to).
@MainActor
final class Updates: ObservableObject {
    @Published var version: String?
    let page = URL(string: "https://github.com/kevincsv/PitlaneHQ-Android/releases/latest")!

    private static func parts(_ v: String) -> [Int] {
        let core = v.trimmingCharacters(in: .whitespaces).replacingOccurrences(of: "v", with: "").split(separator: "-").first.map(String.init) ?? ""
        return core.split(separator: ".").map { Int($0) ?? 0 }
    }

    static func newer(_ latest: String, than current: String) -> Bool {
        let a = parts(latest), b = parts(current)
        for i in 0..<max(a.count, b.count) {
            let x = i < a.count ? a[i] : 0, y = i < b.count ? b[i] : 0
            if x != y { return x > y }
        }
        return false
    }

    func check() async {
        var r = URLRequest(url: URL(string: "https://api.github.com/repos/kevincsv/PitlaneHQ-Android/releases/latest")!, timeoutInterval: 15)
        r.setValue("application/vnd.github+json", forHTTPHeaderField: "accept")
        guard let res = try? await URLSession.shared.data(for: r),
              let j = try? JSONSerialization.jsonObject(with: res.0) as? [String: Any],
              let tag = j["tag_name"] as? String else { return }
        let current = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0"
        if Updates.newer(tag, than: current) {
            version = tag.replacingOccurrences(of: "v", with: "").split(separator: "-").first.map(String.init)
        }
    }
}

/// One piece of news of the app (web/dist/app-news.json on the server, edited by hand).
struct AppNews: Identifiable {
    let id: String, date: String, title: String, text: String, url: URL?, view: String?
}

/// The inbox: app news from the server and which items were dismissed on this phone.
@MainActor
final class Inbox: ObservableObject {
    @Published var news: [AppNews] = []
    @Published var seen: Set<String> = Set(UserDefaults.standard.stringArray(forKey: "inboxSeen") ?? [])

    func mark(_ ids: [String]) {
        seen.formUnion(ids)
        UserDefaults.standard.set(Array(seen), forKey: "inboxSeen")
    }

    func load() async {
        guard let res = try? await URLSession.shared.data(from: server.appendingPathComponent("app/app-news.json")),
              let j = try? JSONSerialization.jsonObject(with: res.0) as? [String: Any], let items = j["items"] as? [[String: Any]] else { return }
        let lang = I18n.shared.lang
        func pick(_ o: Any?) -> String { let d = o as? [String: String] ?? [:]; return d[lang] ?? d["en"] ?? "" }
        news = items.prefix(10).map { it in
            AppNews(id: it["id"] as? String ?? "", date: it["date"] as? String ?? "", title: pick(it["title"]), text: pick(it["text"]), url: (it["url"] as? String).flatMap(URL.init), view: it["view"] as? String)
        }
    }
}

enum Theme {
    static let ink = Color(red: 0x11 / 255, green: 0x15 / 255, blue: 0x1B / 255)
    static let surface = Color(red: 0x19 / 255, green: 0x20 / 255, blue: 0x2A / 255)
    static let surface2 = Color(red: 0x1E / 255, green: 0x26 / 255, blue: 0x31 / 255)
    static let line = Color(red: 0x2B / 255, green: 0x35 / 255, blue: 0x42 / 255)
    static let fg = Color(red: 0xE7 / 255, green: 0xEB / 255, blue: 0xF1 / 255)
    static let bg = Color(red: 0x11 / 255, green: 0x15 / 255, blue: 0x1B / 255)
    static let muted = Color(red: 0x8A / 255, green: 0x97 / 255, blue: 0xA9 / 255)
    static let accent = Color(red: 1, green: 0xB0 / 255, blue: 0x2E / 255)
    static let good = Color(red: 0x38 / 255, green: 0xC9 / 255, blue: 0x7C / 255)
    static let bad = Color(red: 1, green: 0x63 / 255, blue: 0x63 / 255)
    static let purple = Color(red: 0xB9 / 255, green: 0x8C / 255, blue: 1)
    static let blue = Color(red: 0x5A / 255, green: 0xA9 / 255, blue: 1)
}
