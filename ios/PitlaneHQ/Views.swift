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
    private func changelog(_ v: String) -> URL { URL(string: changelogURL + "#" + v.replacingOccurrences(of: ".", with: "") + "-beta")! }

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
    case profile(String?)
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
            case let .profile(id): ProfileView(lapId: id)
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
            NavigationStack { SettingsView() }.tabItem { Label(t("account_tab"), systemImage: "person") }.tag(4)
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
            if account.admin || (account.supporter && !account.supporterHidden) { HStack(spacing: 6) { MyBadges(); Spacer() } }
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
    @State private var fCat = ""
    @State private var fCar = ""
    @State private var fTrack = ""
    @State private var fKind = ""
    @State private var fDay = ""

    private func load() async {
        if tab == 0 { await sessions.load { try await account.sessions() } } else { await bests.load { try await account.bests() } }
    }
    // the car and the track are two filters: with many of each, one list of every pair gets long
    private func trackName(_ x: CloudSession) -> String { x.track + (x.trackConfig.isEmpty ? "" : " · " + x.trackConfig) }
    /// the day of a session, as the calendar keys it ("y-m-d")
    private func dayKey(_ x: CloudSession) -> String {
        let c = Calendar.current.dateComponents([.year, .month, .day], from: Date(timeIntervalSince1970: x.started / 1000))
        return "\(c.year ?? 0)-\(c.month ?? 0)-\(c.day ?? 0)"
    }
    /// every filter but one (what that one can still choose)
    private func match(_ x: CloudSession, skip: String = "") -> Bool {
        (skip == "cat" || fCat.isEmpty || x.cat == fCat) && (skip == "car" || fCar.isEmpty || x.car == fCar) && (skip == "track" || fTrack.isEmpty || trackName(x) == fTrack) &&
            (skip == "kind" || fKind.isEmpty || kindOf(x.kind) == fKind) && (skip == "day" || fDay.isEmpty || dayKey(x) == fDay)
    }

    var body: some View {
        Screen(title: t("analysis"), sub: t("uploaded_by_pc")) {
            WebNote(text: t("coach_web"))
            Tabs(labels: [t("sessions"), t("bests")], selected: $tab)
            if tab == 0 {
                LoadState(loading: sessions.loading, error: sessions.error, stale: sessions.stale) { Task { await load() } }
                // many cars, tracks and sessions make a long list: the discipline, the kind of session, the car and track,
                // a day you drove (the calendar), like the web's "Choose a session"
                let all = sessions.data ?? []
                let cats = DISCS.filter { k in all.contains { $0.cat == k } }
                let forCat = all.filter { match($0, skip: "cat") }, forKind = all.filter { match($0, skip: "kind") }
                let forCar = all.filter { match($0, skip: "car") }, forTrack = all.filter { match($0, skip: "track") }, forDay = all.filter { match($0, skip: "day") }
                let cars = Array(Set(forCar.map { $0.car })).sorted(), tracks = Array(Set(forTrack.map { trackName($0) })).sorted()
                if !cats.isEmpty && all.count > 1 {
                    Pills(items: [("", t("lic_all_full") + " (\(forCat.count))")] + cats.map { k in (k, discName(k) + " (\(forCat.filter { $0.cat == k }.count))") }, selected: $fCat)
                }
                if all.count > 1 {
                    Pills(items: [("", t("kind_all") + " (\(forKind.count))")] + ["race", "qual", "prac", "test"].compactMap { (k: String) -> (String, String)? in
                        let n = forKind.filter { kindOf($0.kind) == k }.count
                        return n > 0 || fKind == k ? (k, t("filt_" + k) + " (\(n))") : nil
                    }, selected: $fKind)
                }
                if cars.count > 1 || !fCar.isEmpty { Pills(items: [("", t("every_car") + " (\(forCar.count))")] + cars.map { k in (k, k + " (\(forCar.filter { $0.car == k }.count))") }, selected: $fCar) }
                if tracks.count > 1 || !fTrack.isEmpty { Pills(items: [("", t("every_track") + " (\(forTrack.count))")] + tracks.map { k in (k, k + " (\(forTrack.filter { trackName($0) == k }.count))") }, selected: $fTrack) }
                if all.count > 1 {
                    Panel {
                        HStack {
                            Text(t("days_drove")).font(.subheadline.bold())
                            Spacer()
                            if fDay.isEmpty { Text(t("press_day")).font(.caption).foregroundColor(Theme.muted) } else { Button(t("every_day")) { fDay = "" }.font(.caption.bold()).foregroundColor(Theme.accent) }
                        }
                        DayGrid(days: forDay.reduce(into: [String: Int]()) { $0[dayKey($1), default: 0] += 1 }, selected: fDay) { k in fDay = fDay == k ? "" : k }
                    }
                }
                let list = all.filter { match($0) }
                if all.count > 1 && !(fCat + fCar + fTrack + fKind + fDay).isEmpty {
                    Button(t("clear_filters") + " · \(list.count)/\(all.count)") { fCat = ""; fCar = ""; fTrack = ""; fKind = ""; fDay = "" }.font(.caption.bold()).foregroundColor(Theme.accent)
                }
                if sessions.data != nil && list.isEmpty { EmptyNote(text: t("no_sessions")) }
                ForEach(list) { x in
                    NavigationLink(value: Route.session(x)) {
                        Panel {
                            Text(x.track + (x.trackConfig.isEmpty ? "" : " · " + x.trackConfig)).font(.headline.weight(.black)).foregroundColor(Theme.fg).lineLimit(1)
                            Text(x.car).font(.caption).foregroundColor(Theme.muted).lineLimit(1)
                            HStack {
                                Text([kindText(x.kind), day(x.started), t("laps_n", x.laps)].filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption).foregroundColor(Theme.muted).lineLimit(1)
                                LicBadge(k: x.lic)
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
        // the average of the valid laps: a real figure, not a lap made of the best sectors
        let avg: Double? = valid.isEmpty ? nil : valid.map(\.time).reduce(0, +) / Double(valid.count)
        return Screen(title: session.track, sub: session.car) {
            LoadState(loading: laps.loading, error: laps.error, stale: laps.stale) { Task { await laps.load { try await account.laps(session.id) } } }
            if session.cat != nil {
                HStack(spacing: 6) { Text(t("viewing", session.car, discName(session.cat))).font(.caption).foregroundColor(Theme.muted); LicBadge(k: session.lic); Spacer() }
            }
            HStack(spacing: 8) {
                Metric(label: t("best"), value: lapTime(best), color: Theme.purple)
                Metric(label: t("average"), value: lapTime(avg))
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

// the disciplines iRacing splits its licenses into, and the license classes, like on the web
let DISCS = ["oval", "sports_car", "formula_car", "dirt_oval", "dirt_road"]
/// The kind of a session for the filter: race, qual(ifying), prac(tice) or test (drive).
func kindOf(_ k: String) -> String {
    let x = k.lowercased()
    return x.contains("race") ? "race" : x.contains("qual") ? "qual" : x.contains("test") ? "test" : (x.contains("prac") || x.contains("warm") || x.contains("offline")) ? "prac" : ""
}
let LICS = ["R", "D", "C", "B", "A", "P"]
func discName(_ k: String?) -> String { k.map { DISCS.contains($0) ? t("disc_" + $0) : "" } ?? "" }
private func licColor(_ k: String) -> Color {
    switch k {
    case "R": return Color(red: 1, green: 0x63 / 255, blue: 0x63 / 255)
    case "D": return Color(red: 1, green: 0x8F / 255, blue: 0x45 / 255)
    case "C": return Color(red: 0xF2 / 255, green: 0xC9 / 255, blue: 0x4C / 255)
    case "B": return Color(red: 0x38 / 255, green: 0xC9 / 255, blue: 0x7C / 255)
    case "A": return Color(red: 0x5C / 255, green: 0x9D / 255, blue: 1)
    default: return Color(red: 0xC9 / 255, green: 0xD1 / 255, blue: 0xDC / 255)
    }
}

/// The license class as a small coloured square (R, D, C, B, A, Pro).
struct LicBadge: View {
    let k: String?
    var body: some View {
        if let k, LICS.contains(k) {
            Text(k == "P" ? "Pro" : k).font(.system(size: 10, weight: .black)).foregroundColor(Theme.fg).padding(.horizontal, 5).padding(.vertical, 1)
                .background(licColor(k).opacity(0.55)).overlay(RoundedRectangle(cornerRadius: 4).stroke(licColor(k), lineWidth: 1.5)).clipShape(RoundedRectangle(cornerRadius: 4))
        }
    }
}

/// Your badges, wherever your name shows: Admin, and Supporter unless you hid it.
struct MyBadges: View {
    @EnvironmentObject var account: Account
    var body: some View {
        HStack(spacing: 6) {
            if account.admin { StatusPill(text: "ADMIN", color: Theme.accent) }
            if account.supporter && !account.supporterHidden { SupBadge() }
        }
    }
}

/// The supporter badge: people who donate, given by hand by the owner of Pitlane HQ.
struct SupBadge: View {
    var body: some View {
        Text("♥ SUPPORTER").font(.system(size: 9, weight: .black)).foregroundColor(Color(red: 1, green: 0.84, blue: 0.9)).padding(.horizontal, 6).padding(.vertical, 1)
            .background(Color(red: 0.91, green: 0.24, blue: 0.55).opacity(0.33)).overlay(Capsule().stroke(Color(red: 0.91, green: 0.24, blue: 0.55).opacity(0.65), lineWidth: 1)).clipShape(Capsule())
    }
}

/// A row of small filters that scrolls sideways.
struct Pills: View {
    let items: [(String, String)] // value, label
    @Binding var selected: String
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(items, id: \.0) { it in
                    Button { selected = selected == it.0 && !it.0.isEmpty ? "" : it.0 } label: {
                        Text(it.1).font(.caption.bold()).padding(.horizontal, 10).padding(.vertical, 5)
                            .background(selected == it.0 ? Theme.accent : Theme.surface2).foregroundColor(selected == it.0 ? Theme.ink : Theme.muted).clipShape(Capsule())
                    }
                }
            }
        }
    }
}

/// Leagues (in development, admins only): explore by discipline, post one with its Discord invite, edit or remove yours.
struct LeaguesAdmin: View {
    @EnvironmentObject var account: Account
    @Environment(\.openURL) private var openURL
    @State private var list: [League]?
    @State private var err: String?
    @State private var tab = 0
    @State private var cat = ""
    @State private var editId: String?
    @State private var f = League(id: "", name: "", about: "", cat: nil, discord: "", web: "", schedule: "", cars: "", lang: "")
    @State private var fCat = ""
    @State private var fErr: String?

    private func load() async { do { list = try await account.leagues(); err = nil } catch { err = (error as? AppError)?.key ?? error.localizedDescription; list = list ?? [] } }
    private func field(_ label: String, _ v: Binding<String>) -> some View {
        TextField(label, text: v).padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 6))
    }
    private func bind(_ k: WritableKeyPath<League, String>) -> Binding<String> { Binding(get: { f[keyPath: k] }, set: { f[keyPath: k] = $0 }) }

    var body: some View {
        let all = list ?? []
        VStack(alignment: .leading, spacing: 10) {
            Tabs(labels: [t("leagues_explore"), editId != nil ? t("leagues_edit") : t("leagues_post"), t("leagues_mine")], selected: $tab)
            if let err { Text(t(err)).font(.caption).foregroundColor(Theme.bad) }
            if tab == 1 {
                Panel {
                    field(t("league_name"), bind(\.name))
                    Pills(items: DISCS.map { ($0, discName($0)) }, selected: $fCat)
                    field("https://discord.gg/…", bind(\.discord))
                    field(t("league_web"), bind(\.web))
                    field(t("league_when"), bind(\.schedule))
                    field(t("league_cars"), bind(\.cars))
                    field(t("league_lang"), bind(\.lang))
                    field(t("league_about"), bind(\.about))
                    if let fErr { Text(t(fErr)).font(.caption).foregroundColor(Theme.bad) }
                    Button(editId != nil ? t("save") : t("league_publish")) {
                        let ok = f.discord.range(of: "^https://(discord\\.gg|(www\\.)?discord\\.com/invite)/[A-Za-z0-9-]{2,40}/?$", options: .regularExpression) != nil
                        if f.name.trimmingCharacters(in: .whitespaces).count < 3 { fErr = "league_need_name"; return }
                        if !ok { fErr = "league_need_discord"; return }
                        let l = League(id: "", name: f.name.trimmingCharacters(in: .whitespaces), about: f.about, cat: fCat.isEmpty ? nil : fCat, discord: f.discord.trimmingCharacters(in: .whitespaces), web: f.web, schedule: f.schedule, cars: f.cars, lang: f.lang)
                        Task {
                            do { try await account.saveLeague(editId, l); editId = nil; fErr = nil; f = League(id: "", name: "", about: "", cat: nil, discord: "", web: "", schedule: "", cars: "", lang: ""); fCat = ""; tab = 2; await load() }
                            catch { fErr = (error as? AppError)?.key ?? error.localizedDescription }
                        }
                    }.buttonStyle(.borderedProminent).tint(Theme.accent)
                    Text(t("league_note")).font(.caption2).foregroundColor(Theme.muted)
                }
            } else {
                if tab == 0 { Pills(items: [("", t("disc_all") + " (\(all.count))")] + DISCS.map { k in (k, discName(k) + " (\(all.filter { $0.cat == k }.count))") }, selected: $cat) }
                let shown = tab == 2 ? all.filter { $0.mine } : all.filter { cat.isEmpty || $0.cat == cat }
                if list != nil && shown.isEmpty { EmptyNote(text: t(tab == 2 ? "leagues_none_mine" : "leagues_none")) }
                ForEach(shown) { x in
                    Panel {
                        Text(x.name).font(.headline.weight(.black))
                        Text([discName(x.cat), x.lang, t("by_name", x.by)].filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption).foregroundColor(Theme.muted)
                        if !x.about.isEmpty { Text(x.about).font(.subheadline) }
                        if !x.schedule.isEmpty || !x.cars.isEmpty { Text([x.schedule, x.cars].filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption).foregroundColor(Theme.muted) }
                        HStack(spacing: 10) {
                            Button(t("join_discord")) { if let u = URL(string: x.discord) { openURL(u) } }.buttonStyle(.borderedProminent).tint(Theme.accent)
                            if let u = URL(string: x.web), !x.web.isEmpty { Button(t("league_site")) { openURL(u) }.foregroundColor(Theme.accent) }
                        }
                        if x.mine || account.admin {
                            HStack(spacing: 14) {
                                Button(t("edit")) { editId = x.id; f = x; fCat = x.cat ?? ""; fErr = nil; tab = 1 }.foregroundColor(Theme.accent)
                                Button(t("remove")) { Task { try? await account.deleteLeague(x.id); await load() } }.foregroundColor(Theme.bad)
                            }.font(.caption.bold())
                        }
                    }
                }
            }
        }
        .task { if list == nil { await load() } }
    }
}

struct CommunityView: View {
    @EnvironmentObject var account: Account
    @EnvironmentObject var net: NetMonitor
    @StateObject private var combos = Loader<[Combo]>()
    @StateObject private var reports = Loader<[SharedReport]>()
    @StateObject private var setups = Loader<[SharedSetup]>()
    @State private var tab = 0
    @State private var q = ""
    @State private var lastDemo = false
    @State private var sec = 0
    @State private var cat = ""

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
            Tabs(labels: [t("leaderboards"), t("leagues")], selected: $sec)
            if sec == 1 {
                // leagues: in development, only the admins get in; everyone else reads that we are working on it
                Panel {
                    HStack { Text(t("leagues")).font(.headline.weight(.black)); Spacer(); StatusPill(text: t("in_development"), color: Theme.accent) }
                    Text(account.admin ? t("leagues_admin") : t("leagues_wip")).font(.subheadline).foregroundColor(Theme.muted).padding(.top, 4)
                }
                if account.admin { LeaguesAdmin() }
            } else {
            WebNote(text: t("community_web"))
            // setups and shared race analyses are switched off for now: only the leaderboards, by lap time, per discipline
            Pills(items: [("", t("disc_all"))] + DISCS.map { k in (k, discName(k) + " (\((combos.data ?? []).filter { $0.cat == k }.count))") }, selected: $cat)
            TextField(t("search"), text: $q).padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 6))
            switch tab {
            case 0:
                let list = (combos.data ?? []).filter { match($0.track, $0.car) && (cat.isEmpty || $0.cat == cat) }
                LoadState(loading: combos.loading, error: combos.error, stale: combos.stale) { Task { await load() } }
                if combos.data != nil && list.isEmpty { EmptyNote(text: t("nothing_found")) }
                ForEach(list) { x in
                    NavigationLink(value: Route.combo(x)) {
                        Panel {
                            Text(x.track).font(.headline.weight(.black)).foregroundColor(Theme.fg).lineLimit(1)
                            Text(x.car).font(.caption).foregroundColor(Theme.muted).lineLimit(1)
                            HStack {
                                Text([discName(x.cat), t("laps_n", x.laps)].filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption).foregroundColor(Theme.muted)
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
                                Text((x.alias == "Anonymous" ? t("anonymous") : x.alias) + (x.mine ? " · " + t("you_badge") : "") + " · " + day(x.created)).font(.caption2).foregroundColor(x.mine ? Theme.accent : Theme.muted)
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
        let shown = laps
        let top = laps.first?.time
        // the server marks the signed-in driver's own lap (anonymous ones too); older servers: by the public name
        let mine = laps.firstIndex { $0.mine } ?? laps.firstIndex { !account.display.isEmpty && $0.alias.caseInsensitiveCompare(account.display) == .orderedSame }
        Screen(title: combo.track, sub: combo.car) {
            LoadState(loading: board.loading, error: board.error, stale: board.stale) { Task { await board.load { try await account.leaderboard(trackId: combo.trackId, carId: combo.carId) } } }
            if board.data != nil {
                Panel {
                    if mine == 0 && laps.count > 1 {
                        Text("🏆 " + t("you_fastest")).font(.subheadline.weight(.black)).foregroundColor(Theme.purple)
                        Text(t("you_fastest_sub", laps.count, lapTime(laps[0].time))).font(.caption).foregroundColor(Theme.muted)
                    } else if let mine { Text(t("your_position", mine + 1, laps.count, lapTime(laps[mine].time))).font(.subheadline.bold()).foregroundColor(Theme.accent) }
                    else { Text(t("not_on_board")).font(.caption).foregroundColor(Theme.muted) }
                    Text([discName(combo.cat), t("drivers", laps.count)].filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption2).foregroundColor(Theme.muted)
                }
            }
            SectionLabel(text: t("fastest_drivers"))
            ForEach(shown) { lap in
                let i = laps.firstIndex(of: lap) ?? 0
                let me = i == mine
                NavigationLink(value: Route.profile(lap.id)) {
                Panel {
                    HStack {
                        Text("\(i + 1)").font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(i == 0 ? Theme.purple : me ? Theme.accent : Theme.muted).frame(width: 32, alignment: .leading)
                        VStack(alignment: .leading, spacing: 1) {
                            HStack(spacing: 6) {
                                Text(lap.alias == "Anonymous" ? t("anonymous") : lap.alias).font(.subheadline.bold()).foregroundColor(me ? Theme.accent : Theme.fg).lineLimit(1).underline(lap.prof)
                                if lap.sup { SupBadge() }
                                if lap.mine { Text(t("you_badge")).font(.system(size: 10, weight: .bold)).foregroundColor(Theme.accent) }
                            }
                            if lap.field && !lap.mine { Text(t("rival_race")).font(.system(size: 10)).foregroundColor(Theme.muted) }
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
                .buttonStyle(.plain)
                .disabled(!lap.prof)
            }
        }
        .task { if board.data == nil { await board.load { try await account.leaderboard(trackId: combo.trackId, carId: combo.carId) } } }
    }
}

/// The last 26 weeks in squares, brighter the more sessions that day (keys "y-m-d" like the web).
struct DayGrid: View {
    let days: [String: Int]
    /// Analysis: the day chosen, and what pressing a day with sessions does
    var selected = ""
    var onDay: ((String) -> Void)? = nil
    var body: some View {
        let cal = Calendar.current, today = cal.startOfDay(for: Date())
        let wd = (cal.component(.weekday, from: today) + 5) % 7 // Monday = 0
        let start = cal.date(byAdding: .day, value: -(wd + 25 * 7), to: today) ?? today
        let mx = max(1, days.values.max() ?? 1)
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 3) {
                ForEach(0..<26, id: \.self) { w in
                    VStack(spacing: 3) {
                        ForEach(0..<7, id: \.self) { d in
                            let day = cal.date(byAdding: .day, value: w * 7 + d, to: start) ?? start
                            let c = cal.dateComponents([.year, .month, .day], from: day)
                            let k = "\(c.year ?? 0)-\(c.month ?? 0)-\(c.day ?? 0)"
                            let n = day > today ? -1 : days[k] ?? 0
                            let lv = n > 0 ? min(4, max(1, Int((Double(n) / Double(mx) * 4).rounded(.up)))) : 0
                            let side: CGFloat = onDay == nil ? 10 : 14
                            RoundedRectangle(cornerRadius: 2).fill(n < 0 ? Color.clear : lv == 0 ? Theme.surface2 : Theme.accent.opacity(0.25 + Double(lv) * 0.18)).frame(width: side, height: side)
                                .overlay(RoundedRectangle(cornerRadius: 2).stroke(k == selected ? Theme.fg : Color.clear, lineWidth: 2))
                                .onTapGesture { if n > 0 { onDay?(k) } }
                        }
                    }
                }
            }
        }
    }
}

/// A driver's profile: nickname (never the iRacing name), license classes, recent races, laps on the leaderboards.
struct ProfileView: View {
    @EnvironmentObject var account: Account
    let lapId: String?
    @State private var p: DriverProfile?
    @State private var error: String?

    private func load() async {
        do { p = try await account.profile(lapId); error = nil } catch { self.error = (error as? AppError)?.key ?? error.localizedDescription }
    }

    var body: some View {
        Screen(title: p?.name ?? t("profile"), sub: p.map { t("since", day($0.since)) } ?? "") {
            if let p {
                Panel {
                    HStack(spacing: 8) {
                        Text(p.name).font(.title3.weight(.black)).lineLimit(2)
                        if p.supporter && !(p.mine && p.supporterHidden) { SupBadge() }
                        Spacer()
                    }
                    if p.anonymous { Text("🔒 " + t(p.mine ? "anon_mine" : "anon_admins_only")).font(.caption).foregroundColor(Theme.muted) }
                    if p.mine && p.supporter && p.supporterHidden { Text(t("badge_hidden")).font(.caption).foregroundColor(Theme.muted) }
                    let ls = DISCS.filter { p.lics[$0] != nil }
                    if !ls.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 10) { ForEach(ls, id: \.self) { k in HStack(spacing: 4) { LicBadge(k: p.lics[k]); Text(discName(k)).font(.caption) } } }
                        }.padding(.top, 6)
                    }
                    if p.mine && p.supporter {
                        Button(t(p.supporterHidden ? "show_badge" : "hide_badge")) { Task { try? await account.setBadgeHidden(!p.supporterHidden); await load() } }
                            .font(.caption.bold()).foregroundColor(Theme.accent).padding(.top, 6)
                    }
                }
                SectionLabel(text: t("days_driven").uppercased() + " · " + t("days_in_6m", p.days.count))
                Panel { DayGrid(days: p.days) }
                SectionLabel(text: t("recent_races").uppercased())
                if p.races.isEmpty { EmptyNote(text: t("no_races_yet")) }
                ForEach(p.races) { r in
                    Panel {
                        HStack(spacing: 6) {
                            Text([day(r.when), r.official ? t("official") : "", discName(r.cat)].filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption2).foregroundColor(Theme.muted)
                            LicBadge(k: r.lic)
                        }
                        HStack {
                            VStack(alignment: .leading, spacing: 1) {
                                Text(r.track).font(.subheadline.bold()).lineLimit(1)
                                Text(r.car).font(.caption).foregroundColor(Theme.muted).lineLimit(1)
                            }
                            Spacer()
                            Text(r.dnf ? "DNF" : r.finish > 0 ? "P\(r.finish)/\(r.field)" : "—").font(.system(.headline, design: .monospaced).weight(.black))
                        }
                        let dif = r.start > 0 && r.finish > 0 ? r.start - r.finish : 0
                        Text([dif != 0 ? (dif > 0 ? "+" : "") + "\(dif) " + t("places") : "", "\(r.inc)x", r.irChange != 0 ? "iR " + (r.irChange > 0 ? "+" : "") + "\(r.irChange)" : "", r.best.map { t("best_short") + " " + lapTime($0) } ?? ""].filter { !$0.isEmpty }.joined(separator: " · "))
                            .font(.caption2).foregroundColor(Theme.muted)
                    }
                }
                SectionLabel(text: t("on_leaderboards").uppercased())
                if p.laps.isEmpty { EmptyNote(text: t("no_laps_yet")) }
                ForEach(p.laps) { l in
                    NavigationLink(value: Route.combo(Combo(trackId: l.trackId, track: l.track, carId: l.carId, car: l.car, laps: 0, best: l.time, cat: l.cat))) {
                    Panel {
                        HStack(spacing: 6) {
                            LicBadge(k: l.lic)
                            VStack(alignment: .leading, spacing: 1) {
                                Text(l.track).font(.subheadline.bold()).lineLimit(1)
                                Text([l.car, discName(l.cat)].filter { !$0.isEmpty }.joined(separator: " · ") + (l.anon ? " · 🔒 " + t(p.mine ? "anon_lap_mine" : "anon_lap_admins") : "")).font(.caption2).foregroundColor(Theme.muted).lineLimit(2)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 1) {
                                Text(lapTime(l.time)).font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(Theme.fg)
                                if l.pos > 0 { Text("P\(l.pos)" + (l.of > 0 ? "/\(l.of)" : "")).font(.system(size: 11, design: .monospaced)).foregroundColor(l.pos == 1 ? Theme.purple : Theme.muted) }
                            }
                        }
                    }
                    }
                    .buttonStyle(.plain)
                    .disabled(l.trackId == 0 || l.carId == 0)
                }
            } else if let error {
                EmptyNote(text: error.localizedCaseInsensitiveContains("anonymous") ? t("anonymous_private") : t(error))
            } else {
                EmptyNote(text: t("loading"))
            }
        }
        .task { if p == nil { await load() } }
    }
}

// ---------- Live ----------

struct LiveView: View {
    @EnvironmentObject var account: Account
    @StateObject private var live = Live()
    @Environment(\.scenePhase) private var phase
    @State private var codeIn = ""
    @State private var codeErr = false

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
            // your PC, from anywhere: Connect when it is online, Disconnect to stop the live data on this phone
            Panel {
                Text(t("your_pc_anywhere")).font(.headline.weight(.black))
                switch live.mode {
                case .own where live.pcOnline && live.ask == "wait":
                    // your PC asks you there: Accept / Decline
                    TimelineView(.periodic(from: .now, by: 5)) { tl in
                        let late = tl.date.timeIntervalSince(live.askAt) >= 60
                        VStack(alignment: .leading, spacing: 6) {
                            Text(late ? t("pc_no_answer") : t("accept_on_pc")).font(.subheadline).foregroundColor(Theme.muted).padding(.vertical, 4)
                            HStack(spacing: 14) {
                                if late { Button(t("try_again")) { live.askAgain() }.buttonStyle(.borderedProminent).tint(Theme.accent) }
                                Button(t("cancel")) { live.watch(.idle) }.foregroundColor(Theme.bad).font(.subheadline.bold())
                            }
                        }
                    }
                case .own:
                    Text(live.pcOnline ? t("watching_pc") : t("waiting_pc")).font(.subheadline).foregroundColor(live.pcOnline ? Theme.good : Theme.muted).padding(.vertical, 4)
                    Button(t("disconnect")) { live.watch(.idle) }.foregroundColor(Theme.bad).font(.subheadline.bold())
                    if live.pcOnline && live.ask == "ok" {
                        Divider().padding(.vertical, 6)
                        Text(t("your_code")).font(.subheadline.bold())
                        if !live.myCode.isEmpty {
                            Text(live.myCode).font(.system(size: 22, weight: .black, design: .monospaced)).foregroundColor(Theme.accent).padding(.vertical, 2)
                            Text(t("your_code_sub")).font(.caption).foregroundColor(Theme.muted)
                            HStack(spacing: 14) {
                                Button(t("new_code")) { live.share(on: true, new: true) }.foregroundColor(Theme.accent)
                                Button(t("stop_sharing")) { live.share(on: false) }.foregroundColor(Theme.bad)
                            }.font(.caption.bold()).padding(.top, 4)
                        } else {
                            Text(t("get_code_sub")).font(.caption).foregroundColor(Theme.muted)
                            Button(t("get_code")) { live.share(on: true) }.foregroundColor(Theme.accent).font(.caption.bold()).padding(.top, 4)
                        }
                    }
                case .code:
                    Text(t("watching_other_back")).font(.subheadline).foregroundColor(Theme.muted).padding(.vertical, 4)
                    Button(t("connect")) { live.watch(.own) }.buttonStyle(.borderedProminent).tint(Theme.accent)
                case .idle:
                    Text(live.pcOnline ? t("pc_is_online") : t("open_pc")).font(.subheadline).foregroundColor(live.pcOnline ? Theme.good : Theme.muted).padding(.vertical, 4)
                    Button(t("connect")) { live.watch(.own) }.buttonStyle(.borderedProminent).tint(Theme.accent).disabled(!live.pcOnline)
                    if Date().timeIntervalSince(live.declinedAt) < 60 { Text(t("declined_on_pc")).font(.caption).foregroundColor(Theme.bad).padding(.top, 4) }
                }
            }
            // the PC app, right under it: it is what sends the telemetry
            Link(destination: URL(string: "https://pitlanehq.app/dl/PitlaneHQ-Setup.exe")!) { ActionRow(title: t("download_pc").uppercased(), sub: t("download_pc_sub"), icon: "desktopcomputer") }
            // someone else's telemetry with the code they give you
            Panel {
                Text(t("watch_other")).font(.headline.weight(.black))
                if live.mode == .code {
                    Text((live.pcOnline ? t("watching") : t("waiting_their_pc")) + " · " + stride(from: 0, to: live.code.count, by: 4).map { i in String(Array(live.code)[i..<min(i + 4, live.code.count)]) }.joined(separator: "-"))
                        .font(.subheadline).foregroundColor(live.pcOnline ? Theme.good : Theme.muted).padding(.vertical, 4)
                    Button(t("disconnect")) { live.watch(.idle) }.foregroundColor(Theme.bad).font(.subheadline.bold())
                } else {
                    Text(t("watch_other_sub")).font(.caption).foregroundColor(Theme.muted).padding(.vertical, 4)
                    TextField("ABCD-EFGH-JK", text: $codeIn).textInputAutocapitalization(.characters).autocorrectionDisabled().padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 6))
                    if codeErr { Text(t("code_bad")).font(.caption).foregroundColor(Theme.bad) }
                    Button(t("watch")) {
                        let c = PLCrypto.codeNorm(codeIn)
                        if PLCrypto.codeOk(c) { codeErr = false; live.watch(.code, code: c) } else { codeErr = true }
                    }.buttonStyle(.borderedProminent).tint(Theme.accent).padding(.top, 4)
                }
            }
            if live.mode == .code || (live.mode == .own && live.ask == "ok") {
            StatusPill(text: status.0, color: status.1)
            if let m = live.message { Text(t(m)).font(.caption).foregroundColor(Theme.bad) }
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
            }
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

// the admin profile: every shared lap and race analysis with the name it shows and who really
// uploaded it (anonymous items and DRINKS drivers too), and the accounts
/// An account whose "in Pitlane HQ since" is being changed.
struct SinceEdit: Identifiable {
    let id: String
    var date: Date
    let custom: Bool
}

struct AdminView: View {
    @EnvironmentObject var account: Account
    @State private var kind = "status"
    @State private var since: SinceEdit?
    @State private var renaming: (String, String)?
    @State private var newName = ""
    @State private var rows: [[String: Any]]?
    @State private var error: String?
    @State private var confirmDelete: (String, String)?

    var body: some View {
        Screen(title: t("admin_profile"), sub: t("admin_profile_sub")) {
            ScrollView(.horizontal, showsIndicators: false) { HStack(spacing: 8) {
                ForEach([("status", t("admin_server")), ("users", t("admin_accounts")), ("sessions", t("admin_activity_t")), ("uploads", t("admin_shared")), ("blocked", t("admin_blocked"))], id: \.0) { p in
                    let k = p.0
                    Button { kind = k; Task { await load() } } label: {
                        Text(p.1).font(.subheadline).padding(.horizontal, 12).padding(.vertical, 6)
                            .background(kind == k ? Theme.accent : Theme.surface2).foregroundColor(kind == k ? Theme.ink : Theme.fg).clipShape(Capsule())
                    }
                }
            } }
            if let e = error {
                EmptyNote(text: t(e))
            } else if let rows = rows {
                if rows.isEmpty { EmptyNote(text: t("admin_nothing")) }
                ForEach(rows.indices, id: \.self) { i in row(rows[i]) }
            } else {
                EmptyNote(text: t("loading"))
            }
        }
        .task { await load() }
        .sheet(item: $since) { e in
            NavigationStack {
                Form {
                    DatePicker(t("admin_set_since"), selection: Binding(get: { since?.date ?? e.date }, set: { since?.date = $0 }), in: ...Date(), displayedComponents: .date).datePickerStyle(.graphical)
                    if e.custom { Button(t("admin_account_date")) { let id = e.id; since = nil; Task { try? await account.adminSince(id, nil); await load() } } }
                }
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button(t("cancel")) { since = nil } }
                    ToolbarItem(placement: .confirmationAction) {
                        Button(t("save")) {
                            let id = e.id, d = since?.date ?? e.date
                            let c = Calendar.current.dateComponents([.year, .month, .day], from: d)
                            var u = DateComponents(); u.year = c.year; u.month = c.month; u.day = c.day; u.hour = 12
                            var cal = Calendar(identifier: .gregorian); cal.timeZone = TimeZone(identifier: "UTC")!
                            let ms = (cal.date(from: u) ?? d).timeIntervalSince1970 * 1000
                            since = nil
                            Task { try? await account.adminSince(id, ms); await load() }
                        }
                    }
                }
            }
        }
        .alert(t("admin_rename"), isPresented: Binding(get: { renaming != nil }, set: { if !$0 { renaming = nil } })) {
            TextField(t("admin_rename"), text: $newName)
            Button(t("save")) {
                let id = renaming?.0 ?? "", n = newName.trimmingCharacters(in: .whitespaces)
                renaming = nil
                if !n.isEmpty { Task { try? await account.adminAccount(id, "rename", name: n); await load() } }
            }
            Button(t("cancel"), role: .cancel) { renaming = nil }
        }
        .alert(t("admin_delete_ask", confirmDelete?.1 ?? ""), isPresented: Binding(get: { confirmDelete != nil }, set: { if !$0 { confirmDelete = nil } })) {
            Button(t("delete"), role: .destructive) {
                let id = confirmDelete?.0 ?? ""
                Task { try? await account.adminDeleteUser(id); await load() }
            }
            Button(t("cancel"), role: .cancel) {}
        }
    }

    // one function per kind of row: one big builder was too much for the type checker
    @ViewBuilder private func row(_ x: [String: Any]) -> some View {
        Panel {
            switch kind {
            case "status": statusRow(x)
            case "sessions": sessionRow(x)
            case "blocked": blockedRow(x)
            case "users": userRow(x)
            default: itemRow(x)
            }
        }
    }

    @ViewBuilder private func statusRow(_ x: [String: Any]) -> some View {
        let m = x["mail"] as? [String: Any] ?? [:], c = x["counts"] as? [String: Any] ?? [:]
        let ready = m["ready"] as? Bool ?? false
        let n = { (k: String) in "\(c[k] as? Int ?? 0)" }
        Text(t("admin_mail") + ": " + (ready ? t("admin_mail_ok") + " (" + (m["via"] as? String ?? "") + ")" : t("admin_mail_off"))).font(.subheadline.bold()).foregroundColor(ready ? Theme.good : Theme.bad)
        Text(m["from"] as? String ?? "").font(.caption).foregroundColor(Theme.muted)
        if let e = m["lastError"] as? [String: Any] {
            Text(t("admin_mail_err") + ": " + ["message", "reply", "status", "body"].compactMap { e[$0].map { "\($0)" } }.filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption).foregroundColor(Theme.bad)
        }
        let g = x["config"] as? [String: Any] ?? [:]
        let pat = g["patreon"] as? Bool ?? false
        Group {
            Text(t("admin_counts", n("accounts"), n("verified"), n("sessions"), n("shared"))).font(.caption).foregroundColor(Theme.fg)
            Text(t("admin_activity", n("new7"), n("drivers7"), n("sessions1"), n("sessions7"))).font(.caption).foregroundColor(Theme.muted)
            Text(t("admin_community", n("boards"), n("leagues"), (g["leaguesOpen"] as? Bool ?? false) ? t("admin_open") : t("admin_only"))).font(.caption).foregroundColor(Theme.muted)
            Text(t("admin_sup", n("supporters"), n("supportersPatreon"), n("patrons"))).font(.caption).foregroundColor(Theme.muted)
            Text(t("admin_model", n("learnt"), n("models")) + " · " + t("admin_dirty", n("modelsDirty"))).font(.caption).foregroundColor(Theme.muted)
            Text(pat ? t("admin_patreon_ok") : t("admin_patreon_off")).font(.caption).foregroundColor(pat ? Theme.good : Theme.bad)
        }
        // the tools: the coach models learn again, the blocked sign-ins open again
        Group {
            Divider().padding(.vertical, 4)
            Text(t("admin_tools_t")).font(.subheadline.bold())
            Button(t("admin_rebuild")) { Task { try? await account.adminTool("models"); await load() } }.foregroundColor(Theme.accent).font(.caption.bold())
            Button(t("admin_unlock", n("authFails"))) { Task { try? await account.adminTool("unlock"); await load() } }.foregroundColor(Theme.accent).font(.caption.bold())
        }
    }

    @ViewBuilder private func sessionRow(_ x: [String: Any]) -> some View {
        let str = { (k: String) in x[k] as? String ?? "" }
        let int = { (k: String) in (x[k] as? NSNumber)?.intValue ?? 0 }
        Text(str("track") + (str("trackConfig").isEmpty ? "" : " · " + str("trackConfig"))).font(.subheadline.bold()).foregroundColor(Theme.fg)
        Text(str("car") + " · " + kindText(str("kind")) + " · " + t("laps_n", int("laps"))).font(.caption).foregroundColor(Theme.muted)
        Text(day((x["started"] as? NSNumber)?.doubleValue ?? 0) + " · " + str("who")).font(.caption).foregroundColor(Theme.muted)
    }

    @ViewBuilder private func blockedRow(_ x: [String: Any]) -> some View {
        let str = { (k: String) in x[k] as? String ?? "" }
        let int = { (k: String) in (x[k] as? NSNumber)?.intValue ?? 0 }
        let a = x["account"] as? [String: Any], bl = x["blocked"] as? Bool ?? false
        let who: String = (a?["display"] as? String) ?? (x["net"] as? String) ?? "—"
        let title: String = str("kind") + " · " + who + (bl ? " · " + t("admin_is_blocked") : "")
        Text(title).font(.subheadline.bold()).foregroundColor(bl ? Theme.bad : Theme.fg)
        Text(t("admin_tries", "\(int("n"))")).font(.caption).foregroundColor(Theme.muted)
        Button(t("admin_unblock")) { Task { try? await account.adminUnlock("k", str("k")); await load() } }.foregroundColor(Theme.accent).font(.caption.bold())
    }

    @ViewBuilder private func userRow(_ x: [String: Any]) -> some View {
        let str = { (k: String) in x[k] as? String ?? "" }
        let int = { (k: String) in (x[k] as? NSNumber)?.intValue ?? 0 }
        let ms0 = (x["memberSince"] as? NSNumber)?.doubleValue ?? 0, ms = ms0 > 0 ? ms0 : ((x["created"] as? NSNumber)?.doubleValue ?? 0)
        let name: String = str("display") + ((x["admin"] as? Bool ?? false) ? " · Admin" : "")
        Text(name).font(.subheadline.bold()).foregroundColor(Theme.fg)
        Text(str("id")).font(.caption2.monospaced()).foregroundColor(Theme.muted).textSelection(.enabled)
        Text(t("admin_since", day(ms)) + (ms0 > 0 ? " ✎" : "")).font(.caption).foregroundColor(Theme.muted)
        Text(t("admin_user_line", "\(int("sessions"))", "\(int("laps"))", "\(int("guests"))")).font(.caption).foregroundColor(Theme.muted)
        // help with an account: confirm its email, turn off its two-step sign-in, sign it out, rename it, unblock it
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 14) {
                if int("verified") != 1 { Button(t("admin_verify")) { Task { try? await account.adminAccount(str("id"), "verify"); await load() } } }
                if int("twoFactor") == 1 { Button(t("admin_2fa_off")) { Task { try? await account.adminAccount(str("id"), "2fa-off"); await load() } } }
                Button(t("admin_signout")) { Task { try? await account.adminAccount(str("id"), "signout"); await load() } }
                Button(t("admin_rename")) { newName = str("display"); renaming = (str("id"), str("display")) }
                Button(t("admin_unblock")) { Task { try? await account.adminUnlock("account", str("id")); await load() } }
            }
            .foregroundColor(Theme.accent).font(.caption.bold())
        }
        HStack(spacing: 14) {
            NavigationLink(value: Route.profile("acct:" + str("id"))) { Text(t("profile")) }.foregroundColor(Theme.accent).font(.caption.bold())
            Button(t("admin_set_since")) { since = SinceEdit(id: str("id"), date: Date(timeIntervalSince1970: ms / 1000), custom: ms0 > 0) }.foregroundColor(Theme.accent).font(.caption.bold())
        }
        let sup = int("supporter") == 1
        let supLabel: String = (sup ? "♥ " + t("supporter_remove") : t("supporter_give")) + (sup && int("supporterHidden") == 1 ? " · " + t("supporter_hidden_by") : "")
        Button(supLabel) {
            Task { try? await account.adminSupporter(str("id"), !sup); await load() }
        }.foregroundColor(sup ? Color(red: 1, green: 0.56, blue: 0.75) : Theme.accent).font(.caption.bold())
        if !(x["admin"] as? Bool ?? false) {
            Button(t("admin_delete_account")) { confirmDelete = (str("id"), str("display")) }.foregroundColor(Theme.bad).font(.caption.bold())
        }
    }

    @ViewBuilder private func itemRow(_ x: [String: Any]) -> some View {
        let str = { (k: String) in x[k] as? String ?? "" }
        let int = { (k: String) in (x[k] as? NSNumber)?.intValue ?? 0 }
        Text(str("track") + " · " + str("car")).font(.subheadline.bold()).foregroundColor(Theme.fg)
        Text((str("kind") == "laps" ? t("lap") : t("race")) + " · " + lapTime(x["time"] as? Double)).font(.caption).foregroundColor(Theme.muted)
        Text(t("admin_shown_as", str("shownAs")) + (int("anon") == 1 ? " 🔒" : "")).font(.caption).foregroundColor(Theme.fg)
        Text(t("admin_real", str("realUploader"))).font(.caption.bold()).foregroundColor(Theme.fg)
        Button(t("delete")) {
            Task { try? await account.adminDelete(str("kind"), str("id")); await load() }
        }.foregroundColor(Theme.bad).font(.caption.bold())
    }

    private func load() async {
        rows = nil
        error = nil
        do { rows = try await account.adminList(kind) } catch { self.error = (error as? AppError)?.key ?? "server_down" }
    }
}

struct DrinksView: View {
    @EnvironmentObject var account: Account
    @StateObject private var live = Live()
    @State private var name = ""
    @State private var taken: String?

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
                    if let n = taken { Text(t("name_taken_drinks", n)).font(.caption).foregroundColor(Theme.bad) }
                    TextField(t("drinks_name"), text: $name).padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 6))
                    HStack(spacing: 8) {
                        Button(t("drinks_set")) {
                            let g = String(name.trimmingCharacters(in: .whitespaces).prefix(32))
                            name = ""
                            taken = nil
                            // a new name is checked first: names used by other people on Pitlane HQ are refused
                            if d.guests.contains(where: { $0.caseInsensitiveCompare(g) == .orderedSame }) {
                                live.setDrinks(on: true, guest: g, guestAuto: false)
                            } else {
                                Task { if await account.nameFree(g) { live.setDrinks(on: true, guest: g, guestAuto: false) } else { taken = g } }
                            }
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
    @State private var showMail = false
    @StateObject private var devices = Loader<[Device]>()

    private var version: String { (Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "") + " beta" }

    var body: some View {
        Screen(title: t("account_tab")) {
            SectionLabel(text: t("account"))
            Panel {
                HStack(spacing: 6) { Text(account.display.isEmpty ? t("driver") : account.display).font(.title3.weight(.black)).lineLimit(1); MyBadges() }
                HStack {
                    Text(showMail ? account.email : String(repeating: "•", count: min(14, max(8, account.email.count)))).font(.caption).foregroundColor(Theme.muted)
                    Button(t(showMail ? "hide" : "show")) { showMail.toggle() }.font(.caption.bold()).foregroundColor(Theme.accent)
                }
                Text(account.verified ? t("verified") : t("not_verified")).font(.caption2).foregroundColor(account.verified ? Theme.good : Theme.accent)
            }
            TwoFactorPanel()
            NavigationLink(value: Route.profile(nil)) { ActionRow(title: t("my_profile").uppercased(), sub: t("my_profile_sub"), icon: "person.crop.circle") }
                .buttonStyle(.plain)
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
            // admins: the app as everyone sees it, to test it (everything for admins and in development hides)
            if account.realAdmin {
                Panel {
                    Toggle(t("as_user"), isOn: $account.asUser).font(.subheadline.bold())
                    Text(t("as_user_sub")).font(.caption).foregroundColor(Theme.muted)
                }
            }
            if account.admin {
                SectionLabel(text: t("admin_tools"))
                NavigationLink { AdminView() } label: { ActionRow(title: t("admin_profile").uppercased(), sub: t("admin_profile_sub"), icon: "person.badge.key") }
                    .buttonStyle(.plain)
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
