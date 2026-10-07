import SwiftUI

// ---------- pieces ----------

struct Panel<Content: View>: View {
    @ViewBuilder var content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 4) { content }
            .padding(12)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background(Theme.surface)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Theme.line, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

struct SectionLabel: View {
    let text: String
    var body: some View {
        Text(text).font(.system(size: 11, weight: .bold, design: .monospaced)).tracking(1.2).foregroundColor(Theme.muted)
    }
}

struct Metric: View {
    let label: String
    let value: String
    var color = Theme.fg
    var sub: String? = nil
    var body: some View {
        Panel {
            Text(label.uppercased()).font(.system(size: 10, weight: .bold, design: .monospaced)).foregroundColor(Theme.muted).lineLimit(1)
            Text(value).font(.system(size: 18, weight: .black, design: .monospaced)).foregroundColor(color).lineLimit(1).minimumScaleFactor(0.5)
            if let sub { Text(sub).font(.system(size: 10)).foregroundColor(Theme.muted).lineLimit(1) }
        }
    }
}

struct StatusPill: View {
    let text: String
    let color: Color
    var body: some View {
        Text(text).font(.system(size: 10, weight: .bold, design: .monospaced)).foregroundColor(color)
            .padding(.horizontal, 9).padding(.vertical, 5)
            .overlay(Capsule().stroke(color, lineWidth: 1))
    }
}

struct InfoRow: View {
    let label: String
    let value: String
    var color = Theme.fg
    var body: some View {
        HStack {
            Text(label).font(.subheadline).foregroundColor(Theme.muted)
            Spacer()
            Text(value).font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(color)
        }
        .padding(.vertical, 2)
    }
}

struct ActionRow: View {
    let title: String
    let sub: String
    let icon: String
    var body: some View {
        Panel {
            HStack(spacing: 12) {
                Image(systemName: icon).foregroundColor(Theme.accent).frame(width: 24)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.headline.weight(.black)).foregroundColor(Theme.fg)
                    Text(sub).font(.caption).foregroundColor(Theme.muted)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundColor(Theme.muted)
            }
        }
    }
}

struct MetricData {
    let label: String
    let value: String
    var color = Theme.fg
    var sub: String? = nil
}

/// Metric boxes in rows of the same height, so every box lines up.
struct MetricGrid: View {
    let items: [MetricData]
    let columns: Int
    var body: some View {
        let rows: [[MetricData]] = stride(from: 0, to: items.count, by: columns).map { Array(items[$0..<min($0 + columns, items.count)]) }
        VStack(spacing: 8) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 8) {
                    ForEach(Array(row.enumerated()), id: \.offset) { _, m in
                        Metric(label: m.label, value: m.value, color: m.color, sub: m.sub)
                    }
                    ForEach(0..<(columns - row.count), id: \.self) { _ in Color.clear.frame(maxWidth: .infinity) }
                }
                .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

/// Licences per category: in development until iRacing switches its data API back on.
struct Licences: View {
    @State private var cat = 0
    private let cats = ["cat_sports", "cat_formula", "cat_oval", "cat_dirt_road", "cat_dirt_oval"]
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                SectionLabel(text: t("licences"))
                Spacer()
                StatusPill(text: "WIP", color: Theme.accent)
            }
            Panel {
                Tabs(labels: cats.map { t($0) }, selected: $cat)
                MetricGrid(items: [
                    MetricData(label: t("lic_class"), value: "—", color: Theme.muted),
                    MetricData(label: t("safety"), value: "—", color: Theme.muted),
                    MetricData(label: "iRating", value: "—", color: Theme.muted),
                ], columns: 3)
                .padding(.vertical, 6)
                Text(t("lic_wip")).font(.caption).foregroundColor(Theme.accent)
            }
        }
    }
}

/// What is only in the web and PC app for now, with a link to open the web.
struct WebNote: View {
    let text: String
    var body: some View {
        Panel {
            HStack(alignment: .top, spacing: 8) {
                Image(systemName: "info.circle").foregroundColor(Theme.blue)
                VStack(alignment: .leading, spacing: 4) {
                    Text(text).font(.caption).foregroundColor(Theme.fg)
                    Link(t("open_web"), destination: webApp).font(.caption.bold())
                }
            }
        }
    }
}

struct EmptyNote: View {
    let text: String
    var body: some View { Panel { Text(text).font(.subheadline).foregroundColor(Theme.muted) } }
}

struct Tabs: View {
    let labels: [String]
    @Binding var selected: Int
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Array(labels.enumerated()), id: \.offset) { i, l in
                    Button { selected = i } label: {
                        Text(l).font(.subheadline.weight(.semibold)).padding(.horizontal, 14).padding(.vertical, 7)
                            .background(selected == i ? Theme.accent : Theme.surface2)
                            .foregroundColor(selected == i ? Theme.ink : Theme.fg)
                            .clipShape(Capsule())
                    }
                }
            }
        }
    }
}

/// Banners on top of every screen: no connection, demo data.
struct Banners: View {
    @EnvironmentObject var account: Account
    @EnvironmentObject var net: NetMonitor
    @EnvironmentObject var updates: Updates
    var body: some View {
        VStack(spacing: 0) {
            if !net.online { banner(t("showing_saved"), Theme.bad) }
            if account.demo { banner(t("demo_banner"), Theme.accent) }
            if let v = updates.version { Link(destination: updates.page) { banner(t("update_available", v), Theme.good) } }
        }
        .alert(t("update_title", updates.version ?? ""), isPresented: Binding(get: { updates.version != nil && shown != updates.version }, set: { if !$0 { shown = updates.version ?? "" } })) {
            Button(t("whats_new")) { UIApplication.shared.open(changelog(updates.version ?? "")) }
            Button(t("download")) { shown = updates.version ?? ""; UIApplication.shared.open(updates.page) }
            Button(t("later"), role: .cancel) { shown = updates.version ?? "" }
        } message: {
            Text(t("update_body", Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""))
        }
    }

    // the notice shows once per version, when the app starts
    @State private var shown = ""
    private func changelog(_ v: String) -> URL { URL(string: "https://github.com/kevincsv/PitlaneHQ-Android/blob/master/CHANGELOG.md#" + v.replacingOccurrences(of: ".", with: "") + "-beta")! }

    private func banner(_ s: String, _ c: Color) -> some View {
        Text(s).font(.system(size: 11, weight: .bold, design: .monospaced)).foregroundColor(Theme.ink)
            .frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 16).padding(.vertical, 6).background(c)
    }
}

struct Screen<Content: View>: View {
    let title: String
    var sub = ""
    @ViewBuilder var content: Content
    var body: some View {
        VStack(spacing: 0) {
            Banners()
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title.uppercased()).font(.system(size: 20, weight: .black)).foregroundColor(Theme.fg).lineLimit(2)
                        if !sub.isEmpty { Text(sub).font(.caption).foregroundColor(Theme.muted).lineLimit(2) }
                    }
                    .padding(.vertical, 4)
                    content
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 20)
            }
        }
        .background(Theme.ink.ignoresSafeArea())
        .toolbarBackground(Theme.ink, for: .navigationBar)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { ToolbarItem(placement: .topBarTrailing) { InfoButton() } }
    }
}

/// What a screen loads from the server: data, loading, an error and whether it is the saved copy.
@MainActor
final class Loader<T>: ObservableObject {
    @Published var data: T?
    @Published var loading = false
    @Published var error: String?
    @Published var stale = false

    func load(_ f: @escaping () async throws -> Got<T>) async {
        loading = true
        error = nil
        do {
            let g = try await f()
            data = g.data
            stale = g.stale
        } catch {
            self.error = (error as? AppError)?.key ?? error.localizedDescription
        }
        loading = false
    }
}

/// Loading spinner, error with a retry button, and the "saved copy" note.
struct LoadState: View {
    let loading: Bool
    let error: String?
    var stale = false
    let retry: () -> Void
    var body: some View {
        if loading { ProgressView().tint(Theme.accent).frame(maxWidth: .infinity) }
        if let error {
            Panel {
                Text(t(error)).font(.subheadline).foregroundColor(Theme.bad)
                Button(t("retry").uppercased(), action: retry).font(.subheadline.bold())
            }
        } else if stale {
            Text(t("showing_saved")).font(.caption).foregroundColor(Theme.accent)
        }
    }
}

enum Route: Hashable {
    case races
    case race(Race)
    case session(CloudSession)
    case lap(CloudSession, CloudLap, [CloudLap])
    case combo(Combo)
}

extension View {
    func routes() -> some View {
        navigationDestination(for: Route.self) { r in
            switch r {
            case .races: RacesView()
            case let .race(x): RaceView(race: x)
            case let .session(s): SessionView(session: s)
            case let .lap(s, l, all): LapView(session: s, lap: l, all: all)
            case let .combo(c): ComboView(combo: c)
            }
        }
    }
}

// ---------- sign in ----------

struct LoginView: View {
    @EnvironmentObject var account: Account
    @State private var email = ""
    @State private var password = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                Spacer(minLength: 48)
                Text("PITLANE HQ").font(.system(size: 34, weight: .black)).foregroundColor(Theme.fg)
                Text(t("companion")).font(.system(size: 12, weight: .bold, design: .monospaced)).foregroundColor(Theme.accent)
                Panel {
                    Text(t("sign_in").uppercased()).font(.system(size: 22, weight: .black))
                    Text(t("same_account")).font(.caption).foregroundColor(Theme.muted)
                    TextField(t("email"), text: $email)
                        .textContentType(.username).keyboardType(.emailAddress).textInputAutocapitalization(.never).autocorrectionDisabled()
                        .padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 6)).padding(.top, 10)
                    SecureField(t("password"), text: $password)
                        .textContentType(.password)
                        .padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 6))
                    if let e = account.error { Text(t(e)).font(.caption).foregroundColor(Theme.bad) }
                    Button {
                        Task { await account.login(email: email, password: password) }
                    } label: {
                        Text((account.busy ? t("signing_in") : t("sign_in")).uppercased()).font(.headline.weight(.black)).frame(maxWidth: .infinity).padding(12)
                            .background(Theme.accent).foregroundColor(Theme.ink).clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .disabled(account.busy || email.isEmpty || password.isEmpty)
                    .padding(.top, 6)
                    HStack {
                        Link(t("create_account"), destination: webApp).font(.caption)
                        Spacer()
                        Link(t("forgot"), destination: URL(string: server.absoluteString + "/account/forgot")!).font(.caption).foregroundColor(Theme.muted)
                    }
                    .padding(.top, 6)
                }
                Text(t("pw_note")).font(.caption2).foregroundColor(Theme.muted)
            }
            .padding(24)
        }
        .background(Theme.ink.ignoresSafeArea())
        .sheet(isPresented: $account.needCode) { CodeSheet() }
    }
}

/// Second step of the sign-in: the authenticator code (or a recovery code).
struct CodeSheet: View {
    @EnvironmentObject var account: Account
    @State private var code = ""
    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 12) {
                Text(t("two_factor_code")).font(.caption).foregroundColor(Theme.muted)
                TextField("123 456", text: $code).textContentType(.oneTimeCode).keyboardType(.numbersAndPunctuation).autocorrectionDisabled()
                    .font(.system(size: 24, weight: .semibold, design: .monospaced)).multilineTextAlignment(.center)
                    .padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 6))
                if let e = account.error { Text(t(e)).font(.caption).foregroundColor(Theme.bad) }
                Button {
                    Task { await account.loginCode(code) }
                } label: {
                    Text((account.busy ? t("signing_in") : t("sign_in")).uppercased()).font(.headline.weight(.black)).frame(maxWidth: .infinity).padding(12)
                        .background(Theme.accent).foregroundColor(Theme.ink).clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .disabled(account.busy || code.isEmpty)
                Spacer()
            }
            .padding(20)
            .background(Theme.ink.ignoresSafeArea())
            .navigationTitle(t("two_factor"))
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button(t("cancel")) { account.cancelCode() } } }
        }
        .presentationDetents([.medium])
    }
}

/// Two-step sign-in with an authenticator app: optional, recommended. Setup: password → link/key → first code → recovery codes once.
struct TwoFactorPanel: View {
    @EnvironmentObject var account: Account
    @State private var step = ""          // "", "pw", "scan", "codes", "off"
    @State private var pw = ""
    @State private var code = ""
    @State private var setup: Account.TwoFactorSetup?
    @State private var codes: [String] = []
    @State private var err: String?
    @State private var busy = false

    var body: some View {
        Panel {
            HStack {
                Text(t("two_factor")).font(.subheadline.bold())
                Spacer()
                Text((account.twoFactor ? t("two_factor_on") : t("recommended")).uppercased()).font(.system(size: 10, weight: .black, design: .monospaced)).foregroundColor(account.twoFactor ? Theme.good : Theme.accent)
            }
            Text(account.twoFactor ? t("two_factor_is_on", account.recoveryLeft) : t("two_factor_rec")).font(.caption).foregroundColor(Theme.muted)
            if account.twoFactor {
                Button(t("turn_off").uppercased()) { pw = ""; code = ""; err = nil; step = "off" }.font(.caption.bold()).foregroundColor(Theme.bad).padding(.top, 4)
            } else {
                Button { pw = ""; code = ""; err = nil; step = "pw" } label: {
                    Text(t("turn_on").uppercased()).font(.caption.weight(.black)).padding(.horizontal, 14).padding(.vertical, 8).background(Theme.accent).foregroundColor(Theme.ink).clipShape(RoundedRectangle(cornerRadius: 6))
                }
                .padding(.top, 4)
            }
        }
        .sheet(isPresented: Binding(get: { step != "" }, set: { if !$0 { step = "" } })) { sheet }
    }

    private func field(_ label: String, _ text: Binding<String>, secret: Bool) -> some View {
        Group {
            if secret { SecureField(label, text: text).textContentType(.password) } else { TextField(label, text: text).textContentType(.oneTimeCode).keyboardType(.numbersAndPunctuation).autocorrectionDisabled() }
        }
        .padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 6))
    }

    private var sheet: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    if step == "pw" {
                        Text(t("two_factor_pw")).font(.caption).foregroundColor(Theme.muted)
                        field(t("password"), $pw, secret: true)
                        action(t("continue"), enabled: !pw.isEmpty) { setup = try await account.setup2fa(password: pw); step = "scan" }
                    } else if step == "scan", let st = setup {
                        Text(t("two_factor_scan")).font(.caption).foregroundColor(Theme.muted)
                        Link(destination: st.url) {
                            Text(t("open_in_app").uppercased()).font(.caption.weight(.black)).frame(maxWidth: .infinity).padding(12).background(Theme.accent).foregroundColor(Theme.ink).clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                        Text(t("key") + ": " + st.secret.enumerated().map { $0.offset > 0 && $0.offset % 4 == 0 ? " \($0.element)" : String($0.element) }.joined())
                            .font(.system(size: 12, design: .monospaced)).textSelection(.enabled)
                        field(t("code"), $code, secret: false)
                        action(t("turn_on"), enabled: code.count >= 6) { codes = try await account.enable2fa(code: code); step = "codes" }
                    } else if step == "codes" {
                        Text(t("two_factor_codes")).font(.caption).foregroundColor(Theme.muted)
                        Text(codes.joined(separator: "\n")).font(.system(size: 15, design: .monospaced)).textSelection(.enabled).frame(maxWidth: .infinity, alignment: .leading).padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 8))
                        HStack {
                            Button(t("copy")) { UIPasteboard.general.string = codes.joined(separator: "\n") }.foregroundColor(Theme.muted)
                            Spacer()
                            Button(t("saved_them").uppercased()) { step = "" }.font(.caption.weight(.black)).padding(.horizontal, 14).padding(.vertical, 8).background(Theme.accent).foregroundColor(Theme.ink).clipShape(RoundedRectangle(cornerRadius: 6))
                        }
                    } else if step == "off" {
                        Text(t("two_factor_off")).font(.caption).foregroundColor(Theme.muted)
                        field(t("password"), $pw, secret: true)
                        field(t("code"), $code, secret: false)
                        action(t("turn_off"), enabled: !pw.isEmpty && !code.isEmpty, danger: true) { try await account.disable2fa(password: pw, code: code); step = "" }
                    }
                    if let e = err { Text(t(e)).font(.caption).foregroundColor(Theme.bad) }
                }
                .padding(20)
            }
            .background(Theme.ink.ignoresSafeArea())
            .navigationTitle(t("two_factor"))
            .toolbar { if step != "codes" { ToolbarItem(placement: .cancellationAction) { Button(t("cancel")) { step = "" } } } }
        }
        .interactiveDismissDisabled(step == "codes")
    }

    private func action(_ label: String, enabled: Bool, danger: Bool = false, _ run: @escaping () async throws -> Void) -> some View {
        Button {
            busy = true
            err = nil
            Task {
                do { try await run() } catch { err = (error as? AppError)?.key ?? error.localizedDescription }
                busy = false
            }
        } label: {
            Text(label.uppercased()).font(.headline.weight(.black)).frame(maxWidth: .infinity).padding(12)
                .background(danger ? Theme.bad : Theme.accent).foregroundColor(Theme.ink).clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .disabled(busy || !enabled)
    }
}

// ---------- tabs ----------

struct MainView: View {
    @State private var tab = 0
    var body: some View {
        TabView(selection: $tab) {
            NavigationStack { HomeView().routes() }.tabItem { Label(t("home"), systemImage: "house") }.tag(0)
            NavigationStack { AnalysisView().routes() }.tabItem { Label(t("analysis"), systemImage: "chart.xyaxis.line") }.tag(1)
            NavigationStack { CommunityView().routes() }.tabItem { Label(t("community"), systemImage: "person.3") }.tag(2)
            NavigationStack { LiveView() }.tabItem { Label(t("live"), systemImage: "antenna.radiowaves.left.and.right") }.tag(3)
            NavigationStack { SettingsView() }.tabItem { Label(t("settings"), systemImage: "gearshape") }.tag(4)
        }
        .onReceive(inbox.$goTab.compactMap { $0 }) { t in tab = t; inbox.goTab = nil }
    }
    @EnvironmentObject private var inbox: Inbox
}

/// One entry of Info: an alert (dismissable) or one of the permanent items (support, feedback).
private struct InfoItem: Identifiable {
    let id: String, icon: String, title: String, text: String
    var keep = false
    let acts: [(String, () -> Void)]
}

/// Info, in the header of every screen: the alerts that matter now (new version, app news, account to-dos) and,
/// always, how to support the app and send feedback. Alerts can be dismissed; the badge counts the new ones.
struct InfoButton: View {
    @EnvironmentObject var account: Account
    @EnvironmentObject var updates: Updates
    @EnvironmentObject var inbox: Inbox
    @State private var open = false

    private var alerts: [InfoItem] {
        var a: [InfoItem] = []
        if let v = updates.version {
            a.append(InfoItem(id: "upd:" + v, icon: "arrow.down.circle", title: t("update_title", v), text: t("update_body", Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""), acts: [
                (t("download"), { UIApplication.shared.open(updates.page) }),
                (t("whats_new"), { UIApplication.shared.open(URL(string: changelogURL + "#" + v.replacingOccurrences(of: ".", with: "") + "-beta")!) }),
            ]))
        }
        if !account.twoFactor { a.append(InfoItem(id: "2fa", icon: "lock", title: t("two_factor_rec_short"), text: t("two_factor_rec_sub"), acts: [(t("turn_on"), { inbox.goTab = 4 })])) }
        for n in inbox.news {
            var acts: [(String, () -> Void)] = []
            if let u = n.url { acts.append((t("open"), { UIApplication.shared.open(u) })) }
            if let v = n.view { acts.append((t("see_it"), { inbox.goTab = v == "me" ? 4 : v == "community" ? 2 : 0 })) }
            a.append(InfoItem(id: "news:" + n.id, icon: "newspaper", title: n.title, text: n.text + (n.date.isEmpty ? "" : " · " + n.date), acts: acts))
        }
        return a.filter { $0.keep || !inbox.seen.contains($0.id) }
    }

    private var always: [InfoItem] {
        var a: [InfoItem] = []
        if let u = URL(string: patreonURL), !patreonURL.isEmpty { a.append(InfoItem(id: "support", icon: "heart", title: t("support"), text: t("support_sub"), keep: true, acts: [("Patreon", { UIApplication.shared.open(u) })])) }
        a.append(InfoItem(id: "feedback", icon: "bubble.left", title: t("feedback"), text: t("feedback_sub"), keep: true, acts: [(t("send_feedback"), { UIApplication.shared.open(feedbackURL) })]))
        return a
    }

    var body: some View {
        let list = alerts
        let fresh = list.filter { !inbox.seen.contains("seen:" + $0.id) }.count
        Button { open = true } label: {
            ZStack(alignment: .topTrailing) {
                Image(systemName: "info.circle").font(.system(size: 17)).foregroundColor(Theme.muted).frame(width: 32, height: 32)
                if fresh > 0 {
                    Text("\(fresh)").font(.system(size: 10, weight: .black, design: .monospaced)).foregroundColor(Theme.ink).padding(.horizontal, 5).frame(minWidth: 16, minHeight: 16).background(Theme.accent).clipShape(Capsule()).offset(x: 4, y: -2)
                }
            }
        }
        .sheet(isPresented: $open, onDismiss: { inbox.mark(list.map { "seen:" + $0.id }) }) {
            NavigationStack {
                ScrollView {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(t("alerts").uppercased()).font(.system(size: 12, weight: .bold, design: .monospaced)).foregroundColor(Theme.muted)
                        if list.isEmpty { Text(t("nothing_new")).font(.subheadline).foregroundColor(Theme.muted).padding(.vertical, 10) }
                        ForEach(list) { it in row(it, dismissable: true) }
                        Text("PITLANE HQ").font(.system(size: 12, weight: .bold, design: .monospaced)).foregroundColor(Theme.muted).padding(.top, 8)
                        ForEach(always) { it in row(it, dismissable: false) }
                    }
                    .padding(16)
                }
                .background(Theme.ink.ignoresSafeArea())
                .navigationTitle("Info")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar { if list.contains(where: { !$0.keep }) { ToolbarItem(placement: .cancellationAction) { Button(t("clear")) { inbox.mark(list.filter { !$0.keep }.map { $0.id }) } } } }
            }
            .presentationDetents([.medium, .large])
        }
    }

    private func row(_ it: InfoItem, dismissable: Bool) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: it.icon).foregroundColor(Theme.accent).frame(width: 24)
            VStack(alignment: .leading, spacing: 4) {
                Text(it.title).font(.subheadline.bold()).foregroundColor(Theme.fg)
                Text(it.text).font(.caption).foregroundColor(Theme.muted)
                HStack(spacing: 6) {
                    ForEach(Array(it.acts.enumerated()), id: \.offset) { i, act in
                        Button { open = false; act.1() } label: {
                            Text(act.0.uppercased()).font(.system(size: 11, weight: .black)).padding(.horizontal, 12).padding(.vertical, 6)
                                .background(i == 0 ? Theme.accent : Theme.surface2).foregroundColor(i == 0 ? Theme.ink : Theme.fg).clipShape(RoundedRectangle(cornerRadius: 6))
                        }
                    }
                }
                .padding(.top, 2)
            }
            Spacer(minLength: 0)
            if dismissable && !it.keep { Button { inbox.mark([it.id]) } label: { Image(systemName: "xmark").font(.caption).foregroundColor(Theme.muted) } }
        }
        .padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 10))
    }
}

// ---------- Home ----------

struct RaceRow: View {
    let race: Race
    var body: some View {
        Panel {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(race.track).font(.headline.weight(.black)).foregroundColor(Theme.fg).lineLimit(1)
                    Text(race.car).font(.caption).foregroundColor(Theme.muted).lineLimit(1)
                    Text([day(race.when), race.official ? t("official") : t("unofficial"), race.sof > 0 ? "SOF \(race.sof)" : ""].filter { !$0.isEmpty }.joined(separator: " · "))
                        .font(.caption2).foregroundColor(Theme.muted)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(race.dnf ? t("dnf") : "P\(race.finish)/\(race.field)").font(.system(.headline, design: .monospaced).weight(.black)).foregroundColor(race.finish == 1 ? Theme.purple : Theme.fg)
                    Text("iR " + signed(race.irChange)).font(.system(.caption, design: .monospaced).bold())
                        .foregroundColor(race.irChange > 0 ? Theme.good : race.irChange < 0 ? Theme.bad : Theme.muted)
                    Text("\(race.inc)x").font(.system(.caption2, design: .monospaced)).foregroundColor(race.inc >= 8 ? Theme.bad : Theme.muted)
                }
            }
        }
    }
}

struct HomeView: View {
    @EnvironmentObject var account: Account

    var body: some View {
        let races = account.races
        let recent = Array(races.prefix(10))
        let last = races.first
        let change = recent.reduce(0) { $0 + $1.irChange }
        let wins = races.filter { $0.finish == 1 }.count
        let top5 = races.filter { (1...5).contains($0.finish) }.count
        let incSum = recent.reduce(0) { $0 + $1.inc }
        let avgInc = recent.isEmpty ? "—" : String(format: "%.1f", Double(incSum) / Double(recent.count))
        Screen(title: account.display.isEmpty ? t("driver") : account.display, sub: t("racing_companion")) {
            Licences()
            DaysDriven(races: races)
            SectionLabel(text: t("race_summary"))
            MetricGrid(items: [
                MetricData(label: t("current_ir"), value: last.map { $0.ir > 0 ? "\($0.ir + $0.irChange)" : "—" } ?? "—"),
                MetricData(label: t("ir_change"), value: recent.isEmpty ? "—" : signed(change), color: change > 0 ? Theme.good : change < 0 ? Theme.bad : Theme.fg, sub: t("last_races", recent.count)),
                MetricData(label: t("races"), value: "\(races.count)"),
                MetricData(label: t("wins"), value: "\(wins)"),
                MetricData(label: t("top5"), value: "\(top5)"),
                MetricData(label: t("avg_inc"), value: avgInc),
            ], columns: 2)
            HStack {
                SectionLabel(text: t("recent_races"))
                Spacer()
                if races.count > 5 { NavigationLink(t("see_all"), value: Route.races).font(.caption) }
            }
            if races.isEmpty { EmptyNote(text: t("no_races")) }
            ForEach(races.prefix(5)) { r in
                NavigationLink(value: Route.race(r)) { RaceRow(race: r) }.buttonStyle(.plain)
            }
            Text(t("ir_estimate")).font(.caption2).foregroundColor(Theme.muted)
            Panel {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(t("agent_note")).font(.caption)
                        Text(account.syncUpdated > 0 ? t("synced", dayTime(account.syncUpdated)) : t("never_synced")).font(.caption).foregroundColor(Theme.muted)
                        if let e = account.error { Text(t(e)).font(.caption).foregroundColor(Theme.bad) }
                    }
                    Spacer()
                    Button(account.busy ? "…" : t("sync").uppercased()) { Task { await account.sync() } }.font(.subheadline.bold()).disabled(account.busy)
                }
            }
        }
        .refreshable { await account.sync() }
    }
}

/// The days you drove, like the web and the PC: the last 26 weeks in squares, brighter the more races and sessions that day.
/// A day opens what you drove: the race summaries and the sessions.
struct DaysDriven: View {
    let races: [Race]
    @EnvironmentObject var account: Account
    @State private var sessions: [CloudSession] = []
    @State private var open: Date?
    private let weeks = 26
    private var cal: Calendar { var c = Calendar.current; c.firstWeekday = 2; return c }

    private func dayKey(_ ms: Double) -> Date { cal.startOfDay(for: Date(timeIntervalSince1970: ms / 1000)) }
    private var byRace: [Date: [Race]] { Dictionary(grouping: races) { dayKey($0.when) } }
    private var bySes: [Date: [CloudSession]] { Dictionary(grouping: sessions) { dayKey($0.started) } }
    private var today: Date { cal.startOfDay(for: Date()) }
    private var start: Date {
        let wd = (cal.component(.weekday, from: today) + 5) % 7 // days since Monday
        return cal.date(byAdding: .day, value: -(wd + (weeks - 1) * 7), to: today)!
    }
    private func count(_ d: Date) -> Int { (byRace[d]?.count ?? 0) + (bySes[d]?.count ?? 0) }

    var body: some View {
        let s = start, r = byRace, se = bySes
        let all: [Date] = (0..<(weeks * 7)).map { cal.date(byAdding: .day, value: $0, to: s)! }
        let mx: Int = max(1, all.map { (r[$0]?.count ?? 0) + (se[$0]?.count ?? 0) }.max() ?? 1)
        let driven: Int = all.filter { $0 <= today && ((r[$0]?.count ?? 0) + (se[$0]?.count ?? 0)) > 0 }.count
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                SectionLabel(text: t("days_driven"))
                Spacer()
                Text(t("days_in_6m", driven)).font(.caption2).foregroundColor(Theme.muted)
            }
            Panel {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 2) {
                        ForEach(0..<weeks, id: \.self) { w in
                            VStack(spacing: 2) {
                                ForEach(0..<7, id: \.self) { d in cell(all[w * 7 + d], mx) }
                            }
                        }
                    }
                }
            }
        }
        .task { if sessions.isEmpty, let g = try? await account.sessions() { sessions = g.data } }
        .sheet(item: Binding(get: { open.map { DayRef(day: $0) } }, set: { open = $0?.day })) { ref in
            NavigationStack {
                List {
                    ForEach(byRace[ref.day] ?? []) { x in
                        NavigationLink(value: Route.race(x)) { Text("\(x.track) · P\(x.finish)/\(x.field) · \(x.inc)x").bold() }
                    }
                    ForEach(bySes[ref.day] ?? []) { x in
                        NavigationLink(value: Route.session(x)) { Text("\(kindText(x.kind)) · \(x.track) · \(x.car) · \(lapTime(x.best))") }
                    }
                }
                .navigationTitle(ref.day.formatted(.dateTime.weekday(.wide).day().month(.wide)))
                .routes()
                .toolbar { Button(t("close")) { open = nil } }
            }
        }
    }

    @ViewBuilder private func cell(_ d: Date, _ mx: Int) -> some View {
        let n = count(d)
        let a: Double = n == 0 ? 0 : [0.35, 0.6, 0.8, 1.0][max(0, min(3, Int((Double(n) / Double(mx) * 4).rounded(.up)) - 1))]
        let fill: Color = d > today ? .clear : n == 0 ? Theme.surface2 : Theme.accent.opacity(a)
        RoundedRectangle(cornerRadius: 3).fill(fill).frame(width: 11, height: 11)
            .onTapGesture { if n > 0 { open = d } }
    }
}

private struct DayRef: Identifiable { let day: Date; var id: Double { day.timeIntervalSince1970 } }

struct RacesView: View {
    @EnvironmentObject var account: Account
    var body: some View {
        Screen(title: t("all_races")) {
            ForEach(account.races) { r in
                NavigationLink(value: Route.race(r)) { RaceRow(race: r) }.buttonStyle(.plain)
            }
        }
    }
}

struct RaceView: View {
    let race: Race
    var body: some View {
        let x = race
        let best = x.laps.filter { $0.time > 0 && !$0.cut }.map(\.time).min()
        Screen(title: x.track, sub: x.car + " · " + dayTime(x.when)) {
            MetricGrid(items: [
                MetricData(label: t("start"), value: "P\(x.start)"),
                MetricData(label: t("finish"), value: x.dnf ? t("dnf") : "P\(x.finish)", color: x.finish == 1 ? Theme.purple : Theme.fg),
                MetricData(label: "iRating", value: signed(x.irChange), color: x.irChange > 0 ? Theme.good : x.irChange < 0 ? Theme.bad : Theme.fg, sub: x.ir > 0 ? "\(x.ir) → \(x.ir + x.irChange)" : nil),
                MetricData(label: t("incidents"), value: "\(x.inc)x", color: x.inc >= 8 ? Theme.bad : Theme.fg),
                MetricData(label: t("pos_gain"), value: signed(x.start - x.finish), color: x.start > x.finish ? Theme.good : x.start < x.finish ? Theme.bad : Theme.fg),
                MetricData(label: t("sof"), value: x.sof > 0 ? "\(x.sof)" : "—"),
            ], columns: 3)
            Panel {
                InfoRow(label: t("best_lap"), value: lapTime(x.best), color: Theme.purple)
                InfoRow(label: t("field_best"), value: lapTime(x.fieldBest))
                InfoRow(label: t("average"), value: lapTime(x.avg))
                InfoRow(label: t("consistency"), value: x.consistency.map { String(format: "±%.3f s", $0) } ?? "—")
                InfoRow(label: t("pits"), value: "\(x.pits)")
                InfoRow(label: t("fuel_used"), value: x.fuelUsed.map { String(format: "%.1f L", $0) } ?? "—")
                InfoRow(label: t("laps"), value: "\(x.laps.count) · " + t("drivers", x.field))
            }
            if !x.laps.isEmpty {
                SectionLabel(text: t("laps").uppercased())
                Panel {
                    ForEach(x.laps, id: \.n) { l in
                        HStack {
                            Text("L\(l.n)").foregroundColor(Theme.muted).frame(width: 44, alignment: .leading)
                            Text(lapTime(l.time)).foregroundColor(l.cut ? Theme.muted : l.time == best ? Theme.purple : Theme.fg).strikethrough(l.cut).opacity(l.cut ? 0.6 : 1)
                            Spacer()
                            Text("P\(l.pos)").foregroundColor(Theme.muted).strikethrough(l.cut).frame(width: 44, alignment: .leading)
                            Text([l.cut ? t("invalid").lowercased() : nil, l.pit ? t("pit") : nil, l.inc > 0 ? "\(l.inc)x" : nil].compactMap { $0 }.joined(separator: " · "))
                                .font(.system(size: 11, design: .monospaced)).foregroundColor(l.pit && !l.cut ? Theme.blue : Theme.bad).frame(minWidth: 40, alignment: .leading)
                        }
                        .font(.system(size: 12, design: .monospaced))
                    }
                }
            }
            if !x.results.isEmpty {
                SectionLabel(text: t("results"))
                Panel {
                    ForEach(x.results, id: \.pos) { p in
                        let me = p.pos == x.finish
                        HStack {
                            Text("\(p.pos)").foregroundColor(me ? Theme.accent : Theme.muted).frame(width: 28, alignment: .leading)
                            Text(p.name).fontWeight(me ? .black : .regular).foregroundColor(me ? Theme.accent : Theme.fg).lineLimit(1)
                            Spacer()
                            Text(p.ir > 0 ? "\(p.ir)" : "").foregroundColor(Theme.muted).frame(width: 44, alignment: .trailing)
                            Text(lapTime(p.best)).frame(width: 66, alignment: .trailing)
                            Text("\(p.inc)x").foregroundColor(Theme.muted).frame(width: 30, alignment: .trailing)
                        }
                        .font(.system(size: 12, design: .monospaced))
                    }
                }
            }
        }
    }
}

// ---------- Analysis ----------

struct AnalysisView: View {
    @EnvironmentObject var account: Account
    @EnvironmentObject var net: NetMonitor
    @StateObject private var sessions = Loader<[CloudSession]>()
    @StateObject private var bests = Loader<[PersonalBest]>()
    @State private var tab = 0

    private func load() async {
        if tab == 0 { await sessions.load { try await account.sessions() } } else { await bests.load { try await account.bests() } }
    }

    var body: some View {
        Screen(title: t("my_laps"), sub: t("uploaded_by_pc")) {
            WebNote(text: t("coach_web"))
            Tabs(labels: [t("sessions"), t("bests")], selected: $tab)
            if tab == 0 {
                LoadState(loading: sessions.loading, error: sessions.error, stale: sessions.stale) { Task { await load() } }
                if let s = sessions.data, s.isEmpty { EmptyNote(text: t("no_sessions")) }
                ForEach(sessions.data ?? []) { x in
                    NavigationLink(value: Route.session(x)) {
                        Panel {
                            Text(x.track + (x.trackConfig.isEmpty ? "" : " · " + x.trackConfig)).font(.headline.weight(.black)).foregroundColor(Theme.fg).lineLimit(1)
                            Text(x.car).font(.caption).foregroundColor(Theme.muted).lineLimit(1)
                            HStack {
                                Text([kindText(x.kind), day(x.started), t("laps_n", x.laps)].filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption).foregroundColor(Theme.muted)
                                Spacer()
                                Text(lapTime(x.best)).font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(Theme.purple)
                            }
                            .padding(.top, 4)
                        }
                    }
                    .buttonStyle(.plain)
                }
            } else {
                LoadState(loading: bests.loading, error: bests.error, stale: bests.stale) { Task { await load() } }
                if let b = bests.data, b.isEmpty { EmptyNote(text: t("no_sessions")) }
                ForEach(bests.data ?? []) { x in
                    let s = x.bestSessionId.map { CloudSession(id: $0, started: x.last, track: x.track, trackConfig: x.trackConfig, car: x.car, kind: "", laps: x.laps, best: x.best) }
                    NavigationLink(value: s.map { Route.session($0) } ?? Route.races) {
                        Panel {
                            Text(x.track + (x.trackConfig.isEmpty ? "" : " · " + x.trackConfig)).font(.headline.weight(.black)).foregroundColor(Theme.fg).lineLimit(1)
                            Text(x.car).font(.caption).foregroundColor(Theme.muted).lineLimit(1)
                            HStack {
                                Text([day(x.last), t("laps_n", x.laps)].filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption).foregroundColor(Theme.muted)
                                Spacer()
                                Text(lapTime(x.best)).font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(Theme.purple)
                            }
                            .padding(.top, 4)
                        }
                    }
                    .buttonStyle(.plain)
                    .disabled(s == nil)
                }
            }
        }
        .refreshable { await load() }
        .task(id: "\(tab)-\(account.demo)") {
            if (tab == 0 ? sessions.data == nil : bests.data == nil) || account.demo != lastDemo { lastDemo = account.demo; await load() }
        }
        .onChange(of: net.cameBack) { _ in Task { await load() } }
    }

    @State private var lastDemo = false
}

struct SessionView: View {
    @EnvironmentObject var account: Account
    let session: CloudSession
    @StateObject private var laps = Loader<[CloudLap]>()

    var body: some View {
        let all = laps.data ?? []
        let valid = all.filter { $0.valid && $0.time > 0 }
        let best = valid.map(\.time).min()
        let nSec = all.map(\.sectors.count).max() ?? 0
        // fastest time of each sector over the valid laps, in purple like in the sim
        let bestSec: [Double?] = (0..<nSec).map { i in valid.compactMap { i < $0.sectors.count && $0.sectors[i] > 0 ? $0.sectors[i] : nil }.min() }
        let ideal: Double? = nSec > 0 && bestSec.allSatisfy({ $0 != nil }) ? bestSec.compactMap { $0 }.reduce(0, +) : nil
        return Screen(title: session.track, sub: session.car) {
            LoadState(loading: laps.loading, error: laps.error, stale: laps.stale) { Task { await laps.load { try await account.laps(session.id) } } }
            HStack(spacing: 8) {
                Metric(label: t("best"), value: lapTime(best), color: Theme.purple)
                Metric(label: t("best_sectors"), value: lapTime(ideal))
            }
            .fixedSize(horizontal: false, vertical: true)
            HStack {
                SectionLabel(text: t("laps").uppercased())
                Spacer()
                Text(t("tap_lap")).font(.caption2).foregroundColor(Theme.muted)
            }
            ForEach(all) { lap in
                NavigationLink(value: Route.lap(session, lap, all)) {
                    Panel {
                        HStack {
                            Text("L\(lap.n)").font(.system(.subheadline, design: .monospaced)).foregroundColor(Theme.muted).frame(width: 44, alignment: .leading)
                            Text(lapTime(lap.time)).font(.system(.subheadline, design: .monospaced).bold())
                                .foregroundColor(!lap.valid ? Theme.muted : lap.time == best ? Theme.purple : Theme.fg).strikethrough(!lap.valid)
                            Spacer()
                            if let best, lap.valid, lap.time > best { Text(String(format: "+%.3f", lap.time - best)).font(.system(size: 11, design: .monospaced)).foregroundColor(Theme.muted) }
                            if lap.inc > 0 { Text("\(lap.inc)x").font(.system(size: 11, design: .monospaced)).foregroundColor(Theme.bad) }
                            if !lap.valid { Text(t("invalid")).font(.system(size: 10, design: .monospaced)).foregroundColor(Theme.bad) }
                            Image(systemName: "chevron.right").foregroundColor(Theme.muted)
                        }
                        if !lap.sectors.isEmpty {
                            HStack(spacing: 10) {
                                ForEach(Array(lap.sectors.enumerated()), id: \.offset) { i, s in
                                    Text("S\(i + 1) " + String(format: "%.3f", s)).font(.system(size: 11, design: .monospaced))
                                        .foregroundColor(lap.valid && i < bestSec.count && bestSec[i] == s ? Theme.purple : Theme.muted)
                                }
                            }
                        }
                    }
                }
                .buttonStyle(.plain)
            }
        }
        .task { if laps.data == nil { await laps.load { try await account.laps(session.id) } } }
    }
}

/// A lap next to the lap it is compared with: your best lap of the session or the community's fastest.
struct LapView: View {
    @EnvironmentObject var account: Account
    let session: CloudSession
    let lap: CloudLap
    let all: [CloudLap]
    @State private var community = false
    @State private var loading = false
    @State private var error: String?
    @State private var trace: Trace?
    @State private var ref: Trace?
    @State private var refLabel = ""
    @State private var refTime: Double?
    @State private var refSectors: [Double] = []
    @State private var pick: Double?

    private func load() async {
        loading = true
        error = nil
        ref = nil
        refLabel = ""
        refTime = nil
        refSectors = []
        do {
            trace = try await account.lapTrace(lap.id)
            if !community {
                if let best = all.filter({ $0.valid && $0.time > 0 && $0.id != lap.id }).min(by: { $0.time < $1.time }) {
                    ref = try? await account.lapTrace(best.id)
                    refLabel = "L\(best.n)"
                    refTime = best.time
                    refSectors = best.sectors
                }
            } else {
                // the community leaderboard of this track and car, matched by name
                let combos = (try? await account.combos().data) ?? []
                let c = combos.first { $0.track.caseInsensitiveCompare(session.track) == .orderedSame && $0.car.caseInsensitiveCompare(session.car) == .orderedSame }
                    ?? combos.first { session.track.lowercased().hasPrefix($0.track.lowercased()) && $0.car.caseInsensitiveCompare(session.car) == .orderedSame }
                if let c, let top = (try? await account.leaderboard(trackId: c.trackId, carId: c.carId).data)?.first(where: { $0.hasTrace }) {
                    ref = try? await account.communityTrace(top.id)
                    refLabel = top.alias
                    refTime = top.time
                    refSectors = top.sectors
                }
            }
        } catch {
            self.error = (error as? AppError)?.key ?? error.localizedDescription
        }
        loading = false
    }

    private func speedSeries(_ c: Compared, _ refName: String) -> [Series] {
        var out = [Series(label: t("you"), values: c.speedA, color: Theme.accent)]
        if let b = c.speedB { out.append(Series(label: refName, values: b, color: Theme.blue)) }
        return out
    }

    var body: some View {
        let d = refTime.map { lap.time - $0 }
        Screen(title: t("lap_analysis") + " · L\(lap.n)", sub: session.track + " · " + session.car) {
            HStack(spacing: 8) {
                Text(t("compare_with")).font(.caption).foregroundColor(Theme.muted)
                Tabs(labels: [t("my_best"), t("community_fastest")], selected: Binding(get: { community ? 1 : 0 }, set: { community = $0 == 1 }))
            }
            LoadState(loading: loading, error: error) { Task { await load() } }
            WebNote(text: t("coach_web"))
            HStack(spacing: 8) {
                Metric(label: t("you"), value: lapTime(lap.time), color: Theme.accent)
                Metric(label: refLabel.isEmpty ? t("ref") : refLabel, value: lapTime(refTime), color: Theme.blue)
                Metric(label: t("delta"), value: d.map { String(format: "%+.3f", $0) } ?? "—", color: d == nil ? Theme.fg : d! <= 0 ? Theme.good : Theme.bad)
            }
            .fixedSize(horizontal: false, vertical: true)
            if !loading {
                if let trace {
                    let c = compare(trace, ref)
                    if ref == nil { Text(t("no_reference")).font(.caption).foregroundColor(Theme.muted) }
                    let refName = refLabel.isEmpty ? t("ref") : refLabel
                    let card: (Int) -> AnyView = { i in AnyView(PointCard(c: c, i: i, refName: refName, sec: lap.sectors, refSec: refSectors)) }
                    if trace.hasShape { TrackMapView(c: c, trace: trace, pick: $pick) }
                    Chart(title: t("speed") + " (km/h)", series: speedSeries(c, refName), step: c.step, fmt: { String(format: "%.0f", $0) },
                          pick: $pick, card: card)
                    if let delta = c.delta {
                        Chart(title: t("delta") + " (s)", series: [Series(label: t("delta"), values: delta, color: Theme.purple)], step: c.step, fmt: { String(format: "%+.3f", $0) }, zero: true,
                              pick: $pick)
                    }
                    Chart(title: t("inputs"), series: [Series(label: t("throttle"), values: c.thrA, color: Theme.good), Series(label: t("brake"), values: c.brkA, color: Theme.bad)],
                          step: c.step, fmt: { String(format: "%.0f%%", $0 * 100) }, fixedMax: 1,
                          pick: $pick)
                    if ref != nil {
                        PhaseCoach(c: c)
                    }
                } else if error == nil {
                    EmptyNote(text: t("no_trace"))
                }
            }
            if !lap.sectors.isEmpty {
                SectionLabel(text: t("sectors"))
                Panel {
                    ForEach(Array(lap.sectors.enumerated()), id: \.offset) { i, s in
                        let r = i < refSectors.count ? refSectors[i] : nil
                        HStack {
                            Text("S\(i + 1)").foregroundColor(Theme.muted).frame(width: 40, alignment: .leading)
                            Text(String(format: "%.3f", s)).foregroundColor(Theme.accent)
                            Spacer()
                            Text(r.map { String(format: "%.3f", $0) } ?? "—").foregroundColor(Theme.blue)
                            Spacer()
                            Text(r.map { String(format: "%+.3f", s - $0) } ?? "").foregroundColor(r != nil && s <= r! ? Theme.good : Theme.bad)
                        }
                        .font(.system(size: 13, design: .monospaced))
                    }
                }
            }
        }
        .task(id: community) { await load() }
    }
}

struct Series {
    let label: String
    let values: [Double]
    let color: Color
}

/// The track, drawn from where the car was on this lap (Pitlane HQ records it), coloured where you gain
/// (green) or lose (red) time against the reference, like the map in the web and the PC app. Touch
/// or drag on it to read that point: the charts follow it (`pick` is the fraction of the lap).
struct TrackMapView: View {
    let c: Compared
    let trace: Trace
    @Binding var pick: Double?
    @State private var showInc = true
    @State private var showBrk = true
    @State private var showCoach = true
    private let xs: [Double], ys: [Double]
    private let brkA: [Double], brkB: [Double]
    private let rings: [Corner]
    private let minX: Double, maxX: Double, minY: Double, maxY: Double

    init(c: Compared, trace: Trace, pick: Binding<Double?>) {
        self.c = c
        self.trace = trace
        self._pick = pick
        xs = trace.x ?? []
        ys = trace.y ?? []
        brkA = brakePoints(c.brkA, c.step)
        brkB = brakePoints(c.brkB, c.step)
        rings = c.delta != nil ? corners(c).filter { $0.lost > 0.05 } : []
        minX = xs.min() ?? 0; maxX = xs.max() ?? 1; minY = ys.min() ?? 0; maxY = ys.max() ?? 1
    }

    private var n: Int { xs.count }
    private var m: Int { c.speedA.count }

    private func point(_ i: Int, _ size: CGSize) -> CGPoint {
        guard n > 1, i >= 0, i < n else { return .zero }
        let pad: Double = 18
        let k: Double = min((size.width - 2 * pad) / max(1e-6, maxX - minX), (size.height - 2 * pad) / max(1e-6, maxY - minY))
        let ox: Double = (size.width - 2 * pad - (maxX - minX) * k) / 2
        let oy: Double = (size.height - 2 * pad - (maxY - minY) * k) / 2
        return CGPoint(x: pad + (xs[i] - minX) * k + ox, y: size.height - pad - (ys[i] - minY) * k - oy)
    }

    private func path(_ from: Int, _ to: Int, _ size: CGSize, close: Bool = false) -> Path {
        var p = Path()
        for i in from...to {
            let q = point(i, size)
            if i == from { p.move(to: q) } else { p.addLine(to: q) }
        }
        if close { p.closeSubpath() }
        return p
    }

    private func pickAt(_ loc: CGPoint, _ size: CGSize) {
        var bi = 0
        var bd: Double = .greatestFiniteMagnitude
        for i in 0..<n {
            let q = point(i, size)
            let d: Double = (q.x - loc.x) * (q.x - loc.x) + (q.y - loc.y) * (q.y - loc.y)
            if d < bd { bd = d; bi = i }
        }
        if bd < 60 * 60 { pick = Double(bi) / Double(max(1, n - 1)) }
    }

    private var selected: Int? {
        guard let f = pick, m > 1 else { return nil }
        return max(0, min(m - 1, Int((f * Double(m - 1)).rounded())))
    }

    private struct Seg: Identifiable {
        let id: Int
        let from: Int
        let to: Int
        let color: Color
    }

    private var stroke: StrokeStyle { StrokeStyle(lineWidth: 7, lineCap: .round, lineJoin: .round) }

    /// 48 stretches of the lap, coloured where lap A gains or loses time against the reference.
    private var segments: [Seg] {
        guard let delta = c.delta, m > 2, n > 2 else { return [] }
        let segs = 48
        var vals: [Double] = []
        for j in 0..<segs {
            let a: Int = j * (m - 1) / segs
            let b: Int = (j + 1) * (m - 1) / segs
            vals.append(delta[b] - delta[a])
        }
        var mx: Double = 0.01
        for v in vals { mx = max(mx, abs(v)) }
        var out: [Seg] = []
        for j in 0..<segs {
            let v: Double = vals[j]
            if abs(v) < 0.003 { continue }
            let alpha: Double = 0.3 + 0.7 * min(1, abs(v) / mx)
            let base: Color = v > 0 ? Theme.bad : Theme.good
            out.append(Seg(id: j, from: j * (n - 1) / segs, to: (j + 1) * (n - 1) / segs, color: base.opacity(alpha)))
        }
        return out
    }

    private var selectedRow: Int? {
        guard let s = selected else { return nil }
        let i: Int = Int(((Double(s) * c.step) / trace.bin).rounded())
        return max(0, min(n - 1, i))
    }

    var body: some View {
        Panel {
            SectionLabel(text: t("track_map").uppercased())
            GeometryReader { g in
                map(g.size)
            }
            .frame(height: 230)
            Text(readout).font(.system(size: 11, design: .monospaced)).foregroundColor(Theme.muted)
            // the same switches as the web and the race summary: braking, incidents, coach
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    chip(t("map_braking"), $showBrk)
                    if !incidents.isEmpty { chip("✕ " + t("map_incidents"), $showInc) }
                    if !rings.isEmpty { chip("Coach", $showCoach) }
                }
            }
            if !incidents.isEmpty { Text(incSummary).font(.caption).foregroundColor(Theme.bad) }
            if showInc, let s = selected {
                let d: Double = Double(s) * c.step
                ForEach(Array(incidents.filter { abs($0.d - d) < 60 }.enumerated()), id: \.offset) { _, e in
                    Text(incName(e.kind) + " (\(e.pts)x)").font(.system(size: 11, design: .monospaced)).foregroundColor(Theme.bad)
                }
            }
        }
    }

    private func map(_ size: CGSize) -> some View {
        ZStack {
            path(0, n - 1, size, close: true).stroke(Theme.line, style: stroke)
            ForEach(segments) { s in
                path(s.from, s.to, size).stroke(s.color, style: stroke)
            }
            brakeMarks(size)
            incMarks(size)
            dots(size)
        }
        .contentShape(Rectangle())
        .onTapGesture { loc in pickAt(loc, size) }
        .simultaneousGesture(DragGesture(minimumDistance: 6).onChanged { v in pickAt(v.location, size) })
    }

    private var incidents: [(d: Double, pts: Int, kind: String)] { trace.incidents }

    /// An incident as the game names it, from its points: 1x off track, 2x loss of control (or a slight contact), 4x car contact.
    private func incName(_ kind: String) -> String { t(kind == "contact" ? "inc_contact" : kind == "light" ? "inc_light" : kind == "loss" ? "inc_loss" : "inc_off") }

    private func cross(_ q: CGPoint) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: q.x - 6, y: q.y - 6)); p.addLine(to: CGPoint(x: q.x + 6, y: q.y + 6))
        p.move(to: CGPoint(x: q.x + 6, y: q.y - 6)); p.addLine(to: CGPoint(x: q.x - 6, y: q.y + 6))
        return p
    }

    private func row(_ d: Double) -> Int { max(0, min(n - 1, Int((d / trace.bin).rounded()))) }

    @ViewBuilder private func incMarks(_ size: CGSize) -> some View {
        if showInc {
            ForEach(Array(incidents.enumerated()), id: \.offset) { _, e in
                let q = point(row(e.d), size)
                cross(q).stroke(Theme.bad, style: StrokeStyle(lineWidth: 3.5, lineCap: .round))
                Text("\(e.pts)x").font(.system(size: 9, weight: .black, design: .monospaced)).foregroundColor(Theme.bad).position(x: q.x + 14, y: q.y - 10)
            }
        }
    }

    /// Where each lap brakes (you in the accent colour, the reference in blue) and the coach's corners.
    @ViewBuilder private func brakeMarks(_ size: CGSize) -> some View {
        if showCoach {
            ForEach(Array(rings.enumerated()), id: \.offset) { _, k in
                let q = point(row(Double(k.atM)), size)
                Circle().fill(Theme.purple.opacity(0.18)).frame(width: 28, height: 28).position(q)
                Circle().stroke(Theme.purple, lineWidth: 2.5).frame(width: 28, height: 28).position(q)
            }
        }
        if showBrk {
            ForEach(Array(brkB.enumerated()), id: \.offset) { _, d in
                Circle().fill(Theme.blue).frame(width: 10, height: 10).overlay(Circle().stroke(Theme.bg, lineWidth: 2)).position(point(row(d), size))
            }
            ForEach(Array(brkA.enumerated()), id: \.offset) { _, d in
                Circle().fill(Theme.accent).frame(width: 12, height: 12).overlay(Circle().stroke(Theme.bg, lineWidth: 2)).position(point(row(d), size))
            }
        }
    }

    private func chip(_ label: String, _ on: Binding<Bool>) -> some View {
        Button { on.wrappedValue.toggle() } label: {
            Text(label).font(.system(size: 11, weight: .bold)).foregroundColor(on.wrappedValue ? Theme.bg : Theme.muted)
                .padding(.horizontal, 10).padding(.vertical, 5).background(on.wrappedValue ? Theme.accent : Theme.surface2).clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private var incSummary: String {
        let pts: Int = incidents.reduce(0) { $0 + $1.pts }
        var by: [String: Int] = [:]
        for e in incidents { by[incName(e.kind), default: 0] += 1 }
        let parts: [String] = by.keys.sorted().map { "\(by[$0]!) \($0.lowercased())" }
        return t("incidents_sum", incidents.count, pts) + ": " + parts.joined(separator: " · ")
    }

    /// A short line across the track at row `i` (the start/finish line, a sector cut).
    private func bar(_ i: Int, _ len: Double, _ size: CGSize) -> Path {
        let q = point(i, size), q2 = point(min(n - 1, i + 3), size)
        let tx = q2.x - q.x, ty = q2.y - q.y, tm = max(1e-3, (tx * tx + ty * ty).squareRoot()), nx = -ty / tm, ny = tx / tm
        var p = Path()
        p.move(to: CGPoint(x: q.x - nx * len, y: q.y - ny * len)); p.addLine(to: CGPoint(x: q.x + nx * len, y: q.y + ny * len))
        return p
    }

    /// Where the S1, S2, S3 label of sector `k` goes: the middle of the sector, on the inside of the track.
    private func secLabel(_ k: Int, _ size: CGSize) -> CGPoint {
        let i = max(0, min(n - 1, Int((Double(k) + 0.5) * Double(n - 1) / 3)))
        let q = point(i, size), q2 = point(min(n - 1, i + 3), size)
        let tx = q2.x - q.x, ty = q2.y - q.y, tm = max(1e-3, (tx * tx + ty * ty).squareRoot())
        var nx = -ty / tm, ny = tx / tm
        if nx * (q.x - size.width / 2) + ny * (q.y - size.height / 2) > 0 { nx = -nx; ny = -ny }
        return CGPoint(x: q.x + nx * 22, y: q.y + ny * 22)
    }

    // the sectors like the web: the start/finish line, a cut at each sector change, S1, S2, S3 in the middle of each
    @ViewBuilder private func dots(_ size: CGSize) -> some View {
        bar(0, 10, size).stroke(Theme.fg, style: StrokeStyle(lineWidth: 4, dash: [3, 3]))
        bar((n - 1) / 3, 8, size).stroke(Theme.fg.opacity(0.8), style: StrokeStyle(lineWidth: 2.5, lineCap: .round))
        bar(2 * (n - 1) / 3, 8, size).stroke(Theme.fg.opacity(0.8), style: StrokeStyle(lineWidth: 2.5, lineCap: .round))
        ForEach(0..<3, id: \.self) { k in
            Text("S\(k + 1)").font(.system(size: 11, weight: .black, design: .monospaced)).foregroundColor(Theme.accent).position(secLabel(k, size))
        }
        if let i = selectedRow {
            Circle().fill(Theme.fg).frame(width: 18, height: 18).position(point(i, size))
            Circle().fill(Theme.accent).frame(width: 12, height: 12).position(point(i, size))
        }
    }

    private var readout: String {
        guard let s = selected else { return c.delta != nil ? t("map_hint") : t("map_hint_a") }
        var out = "\(Int((Double(s) * c.step).rounded())) m · \(String(format: "%.0f", c.speedA[s])) km/h"
        if let b = c.speedB, s < b.count { out += " · " + t("ref") + " " + String(format: "%.0f", b[s]) }
        if let d = c.delta, s < d.count { out += String(format: " · %+.3f s", d[s]) }
        return out
    }
}

/// The coach in four phases (braking, entry, apex, exit), like the web's and Android's.
struct PhaseCoach: View {
    let c: Compared

    var body: some View {
        let cs = corners(c)
        VStack(alignment: .leading, spacing: 10) {
            if cs.isEmpty { fallback } else { phases(cs) }
        }
    }

    // traces without the reference's pedals: where time goes in 250 m stretches
    @ViewBuilder private var fallback: some View {
        let ls = losses(c)
        SectionLabel(text: t("where_time"))
        if ls.isEmpty { EmptyNote(text: t("all_clean")) }
        ForEach(ls, id: \.self) { lo in
            Panel {
                HStack {
                    Text(t("at_m", lo.fromM)).font(.headline.weight(.black))
                    Spacer()
                    Text(t("lost", String(format: "%.3f", lo.lost))).font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(Theme.bad)
                }
                Text(t("min_speed", String(format: "%.0f", lo.minA), String(format: "%.0f", lo.minB))).font(.caption).foregroundColor(Theme.muted)
            }
        }
    }

    private func total(_ cs: [Corner], _ k: String) -> Double { cs.reduce(0) { $0 + max(0, $1.phases[k] ?? 0) } }

    @ViewBuilder private func phases(_ cs: [Corner]) -> some View {
        let totals: [(String, Double)] = phaseKeys.map { ($0, total(cs, $0)) }
        let worst = totals.max { $0.1 < $1.1 }
        let ca = cs.reduce(0) { $0 + $1.coastA }, cb = cs.reduce(0) { $0 + $1.coastB }
        let top = Array(cs.filter { $0.tip != nil }.sorted { $0.lost > $1.lost }.prefix(5))
        SectionLabel(text: t("by_phase"))
        MetricGrid(items: totals.map { kv in MetricData(label: t("phase_" + kv.0), value: (kv.1 > 0 ? "+" : "") + String(format: "%.2f", kv.1), color: kv.1 > 0.03 ? Theme.bad : Theme.muted) }, columns: 4)
        if let w = worst, w.1 > 0.05 { Text(t("most_phase", t("phase_" + w.0).lowercased())).font(.caption).foregroundColor(Theme.muted) }
        else { Text(t("no_phase")).font(.caption).foregroundColor(Theme.muted) }
        Panel { InfoRow(label: t("coasting"), value: "\(Int(ca)) m · " + t("ref") + " \(Int(cb)) m", color: ca - cb > 20 ? Theme.bad : Theme.fg) }
        SectionLabel(text: t("where_time"))
        if top.isEmpty { EmptyNote(text: t("all_clean")) }
        ForEach(top, id: \.self) { k in
            Panel {
                HStack {
                    Text(t("corner_n", k.n)).font(.subheadline.weight(.black)).foregroundColor(Theme.accent)
                    Text(t("at_m", k.atM) + " · " + t("phase_" + (k.phase ?? ""))).font(.caption).foregroundColor(Theme.muted)
                    Spacer()
                    Text(String(format: "+%.2f", k.lost)).font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(Theme.bad)
                }
                Text(tipText(k)).font(.subheadline)
            }
        }
    }

    private func tipText(_ k: Corner) -> String {
        var s = t(k.tip ?? "")
        for (i, a) in k.args.enumerated() { s = s.replacingOccurrences(of: "{\(i)}", with: a) }
        return s
    }
}

/// Everything at one point of the lap, like the box that follows the mouse on the web and the PC:
/// distance and sector, speed, throttle, brake and gear of both laps, the gap there and the sector
/// times.
struct PointCard: View {
    let c: Compared
    let i: Int
    let refName: String
    let sec: [Double]
    let refSec: [Double]

    private func at(_ a: [Double]?) -> Double? {
        guard let a, i < a.count else { return nil }
        return a[i]
    }

    private func row(_ name: String, _ color: Color, _ spd: Double?, _ thr: Double?, _ brk: Double?, _ gear: Double?) -> some View {
        let vals: [String] = [
            spd.map { String(format: "%.0f", $0) } ?? "–",
            thr.map { String(format: "%.0f%%", $0 * 100) } ?? "–",
            brk.map { String(format: "%.0f%%", $0 * 100) } ?? "–",
            gear.map { String(format: "%.0f", $0) } ?? "–",
        ]
        return HStack(spacing: 4) {
            Text(name).foregroundColor(color).bold().lineLimit(1).frame(maxWidth: .infinity, alignment: .leading)
            ForEach(0..<4, id: \.self) { k in
                Text(vals[k]).frame(maxWidth: .infinity, alignment: .trailing)
            }
        }
        .font(.system(size: 12, design: .monospaced))
    }

    var body: some View {
        let n: Int = c.speedA.count
        let ns: Int = max(1, min(3, sec.count))
        let s: Int = min(ns, i / max(1, n / ns) + 1)
        let d: Double? = at(c.delta)
        let a: Double? = s - 1 < sec.count ? sec[s - 1] : nil
        let b: Double? = s - 1 < refSec.count ? refSec[s - 1] : nil
        let heads: [String] = ["km/h", t("throttle"), t("brake"), t("gear")]
        VStack(alignment: .leading, spacing: 3) {
            HStack {
                Text("\(Int((Double(i) * c.step).rounded())) m · S\(s)").font(.system(size: 13, weight: .black, design: .monospaced))
                Spacer()
                if let d {
                    Text(String(format: "%+.3f s", d)).font(.system(size: 13, weight: .bold, design: .monospaced)).foregroundColor(d <= 0 ? Theme.good : Theme.bad)
                }
            }
            HStack(spacing: 4) {
                Text(" ").frame(maxWidth: .infinity)
                ForEach(0..<4, id: \.self) { k in
                    Text(heads[k]).lineLimit(1).frame(maxWidth: .infinity, alignment: .trailing)
                }
            }
            .font(.system(size: 10)).foregroundColor(Theme.muted)
            row(t("you"), Theme.accent, at(c.speedA), at(c.thrA), at(c.brkA), at(c.gearA))
            if c.speedB != nil {
                row(refName, Theme.blue, at(c.speedB), at(c.thrB), at(c.brkB), at(c.gearB))
            }
            if let a {
                HStack {
                    Text("S\(s)").foregroundColor(Theme.muted)
                    Text(String(format: "%.3f", a) + (b.map { " · " + String(format: "%.3f", $0) } ?? ""))
                    Spacer()
                    if let b {
                        Text(String(format: "%+.3f", a - b)).bold().foregroundColor(a <= b ? Theme.good : Theme.bad)
                    }
                }
                .font(.system(size: 12, design: .monospaced))
            }
        }
        .padding(.horizontal, 10).padding(.vertical, 8)
        .frame(maxWidth: .infinity, minHeight: 108, alignment: .leading)
        .background(Theme.surface2)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

/// Practice, qualifying or race, from iRacing's session type.
func kindText(_ k: String) -> String {
    let x = k.lowercased()
    if x.contains("race") { return t("kind_race") }
    if x.contains("qual") { return t("kind_qual") }
    if x.contains("warm") { return t("kind_warm") }
    if x.contains("practice") || x.contains("test") || x.contains("offline") { return t("kind_prac") }
    return k
}

/// A line chart over the lap distance. Tap, or touch and hold and drag, to read the values at
/// that point, like hovering in the web and PC app. The charts of a lap share the point ([pick],
/// a fraction of the lap); the first one shows [card], everything at that point, the others the line
/// and their values.
struct Chart: View {
    let title: String
    let series: [Series]
    let step: Double
    let fmt: (Double) -> String
    var zero = false
    var fixedMax: Double? = nil
    var pick: Binding<Double?>? = nil
    var card: ((Int) -> AnyView)? = nil
    @State private var own: Double?

    private var count: Int { series.map { $0.values.count }.max() ?? 0 }

    private var frac: Double? { pick != nil ? pick!.wrappedValue : own }

    private func set(_ x: CGFloat, _ width: CGFloat) {
        let f: Double = Double(max(0, min(1, x / max(1, width))))
        if let pick { pick.wrappedValue = f } else { own = f }
    }

    private func index(_ width: CGFloat) -> Int? {
        guard let f = frac, count > 1 else { return nil }
        return max(0, min(count - 1, Int((f * Double(count - 1)).rounded())))
    }

    var body: some View {
        let all: [Double] = series.flatMap { $0.values }.filter { $0.isFinite }
        let mn: Double = all.min() ?? 0
        let mx: Double = all.max() ?? 1
        let lo: Double = fixedMax != nil ? 0 : (zero ? min(mn, 0) : mn)
        let hi: Double = fixedMax ?? (zero ? max(mx, 0) : mx)
        let span: Double = hi - lo > 1e-9 ? hi - lo : 1
        Panel {
            SectionLabel(text: title.uppercased())
            if let card {
                // its place is kept before you touch, so the chart does not move under your finger
                if let i = index(1) {
                    card(i)
                } else {
                    Text(t("hold_hint")).font(.caption).foregroundColor(Theme.muted)
                        .frame(maxWidth: .infinity, minHeight: 108)
                        .background(Theme.surface2)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
            }
            GeometryReader { g in
                let w: Double = Double(g.size.width)
                let h: Double = Double(g.size.height)
                let idx = index(g.size.width)
                VStack(alignment: .leading, spacing: 4) {
                    if card == nil || idx == nil { readout(idx).frame(height: 14) }
                    ZStack(alignment: .topLeading) {
                        if zero {
                            Path { p in
                                let y: Double = (h - 18) * (1 - (0 - lo) / span)
                                p.move(to: CGPoint(x: 0, y: y))
                                p.addLine(to: CGPoint(x: w, y: y))
                            }
                            .stroke(Theme.line, lineWidth: 1)
                        }
                        ForEach(Array(series.enumerated()), id: \.offset) { _, s in
                            line(s.values, w, h - 18, lo, span).stroke(s.color, lineWidth: 1.5)
                        }
                        if let idx {
                            let x: Double = w * Double(idx) / Double(max(1, count - 1))
                            Path { p in
                                p.move(to: CGPoint(x: x, y: 0))
                                p.addLine(to: CGPoint(x: x, y: h - 18))
                            }
                            .stroke(Theme.fg.opacity(0.6), lineWidth: 1)
                            ForEach(Array(series.enumerated()), id: \.offset) { _, s in
                                if idx < s.values.count {
                                    let y: Double = (h - 18) * (1 - (s.values[idx] - lo) / span)
                                    Circle().fill(s.color).frame(width: 7, height: 7).position(x: x, y: y)
                                }
                            }
                        }
                    }
                    .contentShape(Rectangle())
                    // at once: a tap picks the point, sliding sideways follows it (up and down still scrolls)
                    .onTapGesture { location in set(location.x, g.size.width) }
                    .simultaneousGesture(
                        DragGesture(minimumDistance: 6)
                            .onChanged { v in
                                let dx: CGFloat = abs(v.translation.width)
                                let dy: CGFloat = abs(v.translation.height)
                                if dx > dy { set(v.location.x, g.size.width) }
                            }
                    )
                }
            }
            .frame(height: 150)
            HStack {
                Text(fixedMax == 1 ? "0%" : fmt(lo))
                Spacer()
                Text(String(format: "%.1f km", Double(max(0, count - 1)) * step / 1000))
                Spacer()
                Text(fixedMax == 1 ? "100%" : fmt(hi))
            }
            .font(.system(size: 10)).foregroundColor(Theme.muted)
        }
    }

    // the values under the finger
    @ViewBuilder private func readout(_ idx: Int?) -> some View {
        if let idx {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    Text("\(Int((Double(idx) * step).rounded())) m").foregroundColor(Theme.muted)
                    ForEach(Array(series.enumerated()), id: \.offset) { _, s in
                        if idx < s.values.count { Text(s.label + " " + fmt(s.values[idx])).foregroundColor(s.color).bold() }
                    }
                }
                .font(.system(size: 11, design: .monospaced))
            }
        } else {
            Text(" ").font(.system(size: 11))
        }
    }

    private func line(_ v: [Double], _ w: Double, _ h: Double, _ lo: Double, _ span: Double) -> Path {
        Path { p in
            guard v.count > 1 else { return }
            let last: Double = Double(v.count - 1)
            for i in 0..<v.count {
                let px: Double = w * Double(i) / last
                let py: Double = h * (1 - (v[i] - lo) / span)
                if i == 0 { p.move(to: CGPoint(x: px, y: py)) } else { p.addLine(to: CGPoint(x: px, y: py)) }
            }
        }
    }
}

// ---------- Community ----------

struct CommunityView: View {
    @EnvironmentObject var account: Account
    @EnvironmentObject var net: NetMonitor
    @StateObject private var combos = Loader<[Combo]>()
    @StateObject private var reports = Loader<[SharedReport]>()
    @StateObject private var setups = Loader<[SharedSetup]>()
    @State private var tab = 0
    @State private var q = ""
    @State private var lastDemo = false

    private func match(_ s: String...) -> Bool { q.isEmpty || s.contains { $0.localizedCaseInsensitiveContains(q) } }

    private func load() async {
        switch tab {
        case 0: await combos.load { try await account.combos() }
        case 1: await reports.load { try await account.reports() }
        default: await setups.load { try await account.setups() }
        }
    }

    private var empty: Bool { tab == 0 ? combos.data == nil : tab == 1 ? reports.data == nil : setups.data == nil }

    var body: some View {
        Screen(title: t("community"), sub: t("shared_by")) {
            WebNote(text: t("community_web"))
            // setups and shared race analyses are switched off for now: only the leaderboards
            TextField(t("search"), text: $q).padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 6))
            switch tab {
            case 0:
                let list = (combos.data ?? []).filter { match($0.track, $0.car) }
                LoadState(loading: combos.loading, error: combos.error, stale: combos.stale) { Task { await load() } }
                if combos.data != nil && list.isEmpty { EmptyNote(text: t("nothing_found")) }
                ForEach(list) { x in
                    NavigationLink(value: Route.combo(x)) {
                        Panel {
                            Text(x.track).font(.headline.weight(.black)).foregroundColor(Theme.fg).lineLimit(1)
                            Text(x.car).font(.caption).foregroundColor(Theme.muted).lineLimit(1)
                            HStack {
                                Text(t("laps_n", x.laps)).font(.caption).foregroundColor(Theme.muted)
                                Spacer()
                                Text(lapTime(x.best)).font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(Theme.purple)
                            }
                            .padding(.top, 4)
                        }
                    }
                    .buttonStyle(.plain)
                }
            case 1:
                let list = (reports.data ?? []).filter { match($0.track, $0.car, $0.alias) }
                LoadState(loading: reports.loading, error: reports.error, stale: reports.stale) { Task { await load() } }
                if reports.data != nil && list.isEmpty { EmptyNote(text: t("nothing_found")) }
                ForEach(list) { x in
                    Panel {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(x.track).font(.headline.weight(.black)).lineLimit(1)
                                Text(x.car).font(.caption).foregroundColor(Theme.muted).lineLimit(1)
                                Text(x.alias + " · " + day(x.created)).font(.caption2).foregroundColor(Theme.muted)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 2) {
                                Text(x.finish > 0 ? "P\(x.finish)/\(x.field)" : "—").font(.system(.headline, design: .monospaced).weight(.black))
                                Text(lapTime(x.best)).font(.system(.caption, design: .monospaced)).foregroundColor(Theme.purple)
                            }
                        }
                    }
                }
            default:
                let list = (setups.data ?? []).filter { match($0.track, $0.car, $0.name, $0.alias) }
                LoadState(loading: setups.loading, error: setups.error, stale: setups.stale) { Task { await load() } }
                if setups.data != nil && list.isEmpty { EmptyNote(text: t("nothing_found")) }
                ForEach(list) { x in
                    Panel {
                        Text(x.name).font(.headline.weight(.black)).lineLimit(1)
                        Text([x.car, x.track].filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption).foregroundColor(Theme.muted).lineLimit(2)
                        if !x.notes.isEmpty { Text(x.notes).font(.caption).lineLimit(3) }
                        Text(x.alias + " · " + t("downloads", x.downloads) + " · " + day(x.created)).font(.caption2).foregroundColor(Theme.muted)
                    }
                }
            }
        }
        .refreshable { await load() }
        .task(id: "\(tab)-\(account.demo)") {
            if empty || account.demo != lastDemo { lastDemo = account.demo; await load() }
        }
        .onChange(of: net.cameBack) { _ in Task { await load() } }
    }
}

struct ComboView: View {
    @EnvironmentObject var account: Account
    let combo: Combo
    @StateObject private var board = Loader<[CommunityLap]>()

    var body: some View {
        let laps = board.data ?? []
        let top = laps.first?.time
        let mine = laps.firstIndex { !account.display.isEmpty && $0.alias.caseInsensitiveCompare(account.display) == .orderedSame }
        Screen(title: combo.track, sub: combo.car) {
            LoadState(loading: board.loading, error: board.error, stale: board.stale) { Task { await board.load { try await account.leaderboard(trackId: combo.trackId, carId: combo.carId) } } }
            if board.data != nil {
                Panel {
                    if let mine { Text(t("your_position", mine + 1, laps.count, lapTime(laps[mine].time))).font(.subheadline.bold()).foregroundColor(Theme.accent) }
                    else { Text(t("not_on_board")).font(.caption).foregroundColor(Theme.muted) }
                    Text(t("drivers", laps.count)).font(.caption2).foregroundColor(Theme.muted)
                }
            }
            SectionLabel(text: t("fastest_drivers"))
            ForEach(Array(laps.enumerated()), id: \.element.id) { i, lap in
                let me = i == mine
                Panel {
                    HStack {
                        Text("\(i + 1)").font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(i == 0 ? Theme.purple : me ? Theme.accent : Theme.muted).frame(width: 32, alignment: .leading)
                        VStack(alignment: .leading, spacing: 1) {
                            Text(lap.alias).font(.subheadline.bold()).foregroundColor(me ? Theme.accent : Theme.fg).lineLimit(1)
                            if !lap.sectors.isEmpty { Text(lap.sectors.map { String(format: "%.3f", $0) }.joined(separator: "  ")).font(.system(size: 10, design: .monospaced)).foregroundColor(Theme.muted) }
                            if !lap.hasTrace { Text(t("trace_not_shared")).font(.system(size: 10)).foregroundColor(Theme.muted) }
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 0) {
                            Text(lapTime(lap.time)).font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(i == 0 ? Theme.purple : Theme.fg)
                            if let top, i > 0 { Text(String(format: "+%.3f", lap.time - top)).font(.system(size: 11, design: .monospaced)).foregroundColor(Theme.muted) }
                        }
                    }
                }
            }
        }
        .task { if board.data == nil { await board.load { try await account.leaderboard(trackId: combo.trackId, carId: combo.carId) } } }
    }
}

// ---------- Live ----------

struct LiveView: View {
    @EnvironmentObject var account: Account
    @StateObject private var live = Live()
    @Environment(\.scenePhase) private var phase

    private var status: (String, Color) {
        switch live.link {
        case .off: return (t("offline_s"), Theme.muted)
        case .connecting: return (t("connecting"), Theme.muted)
        case .open:
            if !live.pcOnline { return (t("pc_offline"), Theme.muted) }
            if !live.simConnected { return (t("pc_no_sim"), Theme.accent) }
            return (t("live_s"), Theme.good)
        }
    }

    private func start() { live.start(token: account.token, key: account.dataKey, demo: account.demo) }

    var body: some View {
        let gear = live.num("Gear").map { Int($0) }
        let delta = live.num("LapDeltaToBestLap")
        Screen(title: t("live_title"), sub: t("live_sub")) {
            StatusPill(text: status.0, color: status.1)
            WebNote(text: t("live_web"))
            if let m = live.message { Text(t(m)).font(.caption).foregroundColor(Theme.bad) }
            if live.link == .open && !live.pcOnline { EmptyNote(text: t("open_pc")) }
            HStack(spacing: 8) {
                Metric(label: t("speed"), value: live.num("Speed").map { String(format: "%.0f", $0 * 3.6) } ?? "—")
                Metric(label: t("gear"), value: gear.map { $0 < 0 ? "R" : $0 == 0 ? "N" : "\($0)" } ?? "—")
                Metric(label: "RPM", value: live.num("RPM").map { String(format: "%.0f", $0) } ?? "—")
            }
            .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: 8) {
                Metric(label: t("lap"), value: live.num("Lap").map { "\(Int($0))" } ?? "—")
                Metric(label: t("pos"), value: live.num("PlayerCarPosition").flatMap { $0 > 0 ? "P\(Int($0))" : nil } ?? "—")
                Metric(label: t("fuel"), value: live.num("FuelLevel").map { String(format: "%.1f L", $0) } ?? "—")
            }
            .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: 8) {
                Metric(label: t("current"), value: lapTime(live.num("LapCurrentLapTime")))
                Metric(label: t("last"), value: lapTime(live.num("LapLastLapTime")))
                Metric(label: t("best"), value: lapTime(live.num("LapBestLapTime")), color: Theme.purple)
            }
            .fixedSize(horizontal: false, vertical: true)
            Metric(label: t("delta_best"), value: delta.map { String(format: "%+.3f", $0) } ?? "—", color: delta == nil ? Theme.fg : delta! <= 0 ? Theme.good : Theme.bad)
            Panel {
                SectionLabel(text: t("inputs").uppercased())
                bar(t("throttle"), live.num("Throttle"), Theme.good)
                bar(t("brake"), live.num("Brake"), Theme.bad)
            }
            Text(t("e2e")).font(.caption2).foregroundColor(Theme.muted)
        }
        .onAppear { start() }
        .onDisappear { live.stop() }
        .onChange(of: phase) { p in
            if p == .active { start() } else { live.stop() }
        }
    }

    private func bar(_ label: String, _ v: Double?, _ c: Color) -> some View {
        HStack {
            Text(label).font(.system(size: 11, design: .monospaced)).foregroundColor(Theme.muted).frame(width: 90, alignment: .leading)
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    Capsule().fill(Theme.surface2)
                    Capsule().fill(c).frame(width: g.size.width * min(max(v ?? 0, 0), 1))
                }
            }
            .frame(height: 10)
        }
        .padding(.top, 6)
    }
}

// ---------- DRINKS mode (admins) ----------

struct DrinksView: View {
    @EnvironmentObject var account: Account
    @StateObject private var live = Live()
    @State private var name = ""

    var body: some View {
        Screen(title: t("drinks"), sub: t("drinks_sub")) {
            StatusPill(text: live.pcOnline ? t("pc_online") : live.link == .connecting ? t("connecting") : t("pc_offline"), color: live.pcOnline ? Theme.good : Theme.muted)
            if let m = live.message { Text(t(m)).font(.caption).foregroundColor(Theme.bad) }
            Panel { Text(t("drinks_note")).font(.caption).foregroundColor(Theme.muted) }
            if account.demo {
                EmptyNote(text: t("drinks_demo"))
            } else if let d = live.drinks {
                if !d.admin {
                    EmptyNote(text: t("drinks_pc_admin"))
                } else {
                    controls(d)
                }
            } else {
                EmptyNote(text: t("drinks_wait"))
            }
        }
        .onAppear { live.start(token: account.token, key: account.dataKey, demo: false) }
        .onDisappear { live.stop() }
    }

    @ViewBuilder private func controls(_ d: Drinks) -> some View {
        Panel {
            Toggle(t("drinks_on"), isOn: Binding(get: { d.on }, set: { live.setDrinks(on: $0, guest: d.guest, guestAuto: d.guestAuto) })).font(.subheadline.bold())
            Text(t("drinks_now", d.driver.isEmpty ? t("you") : d.driver)).font(.subheadline.bold()).foregroundColor(d.on ? Theme.accent : Theme.muted)
        }
        if d.on {
            Panel {
                Toggle(t("drinks_auto"), isOn: Binding(get: { d.guestAuto }, set: { live.setDrinks(on: true, guest: d.guest, guestAuto: $0) })).font(.subheadline)
                if !d.guestAuto {
                    TextField(t("drinks_name"), text: $name).padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 6))
                    HStack(spacing: 8) {
                        Button(t("drinks_set")) {
                            live.setDrinks(on: true, guest: String(name.trimmingCharacters(in: .whitespaces).prefix(32)), guestAuto: false)
                            name = ""
                        }
                        .buttonStyle(.borderedProminent).foregroundColor(Theme.ink)
                        .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                        Button(t("drinks_me")) { live.setDrinks(on: true, guest: "", guestAuto: false) }.buttonStyle(.bordered)
                    }
                }
            }
            if !d.guestAuto && !d.guests.isEmpty {
                SectionLabel(text: t("drinks_recent"))
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(d.guests, id: \.self) { g in
                            let sel = g.caseInsensitiveCompare(d.guest) == .orderedSame
                            Button { live.setDrinks(on: true, guest: g, guestAuto: false) } label: {
                                Text(g).font(.subheadline).padding(.horizontal, 12).padding(.vertical, 6)
                                    .background(sel ? Theme.accent : Theme.surface2).foregroundColor(sel ? Theme.ink : Theme.fg).clipShape(Capsule())
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Settings ----------

struct SettingsView: View {
    @EnvironmentObject var account: Account
    @EnvironmentObject var i18n: I18n
    @StateObject private var devices = Loader<[Device]>()

    private var version: String { (Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "") + " beta" }

    var body: some View {
        Screen(title: t("settings")) {
            SectionLabel(text: t("account"))
            Panel {
                Text(account.display.isEmpty ? t("driver") : account.display).font(.title3.weight(.black))
                Text(account.email).font(.caption).foregroundColor(Theme.muted)
                Text(account.verified ? t("verified") : t("not_verified")).font(.caption2).foregroundColor(account.verified ? Theme.good : Theme.accent)
            }
            TwoFactorPanel()
            Link(destination: webApp) { ActionRow(title: t("web").uppercased(), sub: t("web_sub"), icon: "safari") }
            if let u = URL(string: patreonURL), !patreonURL.isEmpty {
                Link(destination: u) { ActionRow(title: t("support").uppercased(), sub: t("support_sub"), icon: "heart") }
            }
            SectionLabel(text: t("devices"))
            LoadState(loading: devices.loading, error: devices.error) { Task { await devices.load { try await account.devices() } } }
            ForEach(devices.data ?? []) { d in
                Panel {
                    HStack {
                        Image(systemName: d.device.localizedCaseInsensitiveContains("iphone") || d.device.localizedCaseInsensitiveContains("android") ? "iphone" : "desktopcomputer").foregroundColor(Theme.muted)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(d.current ? t("this_device") : (d.device.isEmpty ? "—" : d.device)).font(.subheadline.bold())
                            Text(dayTime(d.lastSeen)).font(.caption2).foregroundColor(Theme.muted)
                        }
                        Spacer()
                        if !d.current {
                            Button(t("sign_out_device")) { revoke(d.id) }.font(.caption).foregroundColor(Theme.bad)
                        }
                    }
                }
            }
            if (devices.data?.count ?? 0) > 1 {
                Button(t("sign_out_others")) { revoke("others") }.foregroundColor(Theme.bad)
            }
            SectionLabel(text: t("language"))
            Panel {
                ForEach(["system", "en", "es"], id: \.self) { k in
                    Button { i18n.choice = k } label: {
                        HStack {
                            Image(systemName: i18n.choice == k ? "largecircle.fill.circle" : "circle").foregroundColor(Theme.accent)
                            Text(k == "system" ? t("lang_system") : k == "en" ? "English" : "Español").foregroundColor(Theme.fg)
                            Spacer()
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
            if account.admin {
                SectionLabel(text: t("admin_tools"))
                NavigationLink { DrinksView() } label: { ActionRow(title: t("drinks").uppercased(), sub: t("drinks_sub"), icon: "wineglass") }
                    .buttonStyle(.plain)
                Panel {
                    Toggle(t("demo_mode"), isOn: $account.demo).font(.subheadline.bold())
                    Text(t("demo_note")).font(.caption).foregroundColor(Theme.muted)
                }
            }
            SectionLabel(text: t("your_data"))
            Panel { Text(t("privacy")).font(.caption).foregroundColor(Theme.muted) }
            Button {
                Task { await account.logout() }
            } label: {
                Text(t("sign_out").uppercased()).font(.headline.bold()).frame(maxWidth: .infinity).padding(12)
                    .background(Theme.surface2).foregroundColor(Theme.bad).clipShape(RoundedRectangle(cornerRadius: 8))
            }
            Text(t("version", version)).font(.caption2).foregroundColor(Theme.muted)
        }
        .task { if devices.data == nil { await devices.load { try await account.devices() } } }
    }

    private func revoke(_ id: String) {
        Task {
            do { try await account.revoke(id) } catch { devices.error = (error as? AppError)?.key ?? error.localizedDescription }
            await devices.load { try await account.devices() }
        }
    }
}
