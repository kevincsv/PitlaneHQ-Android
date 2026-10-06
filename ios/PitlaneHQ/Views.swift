import SwiftUI

// ---------- pieces ----------

struct Panel<Content: View>: View {
    @ViewBuilder var content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 4) { content }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
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
    var body: some View {
        Panel {
            SectionLabel(text: label)
            Text(value).font(.system(size: 20, weight: .black, design: .monospaced)).foregroundColor(color).lineLimit(1).minimumScaleFactor(0.6)
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

struct Screen<Content: View>: View {
    let title: String
    var sub = ""
    @ViewBuilder var content: Content
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title.uppercased()).font(.system(size: 24, weight: .black)).foregroundColor(Theme.fg).lineLimit(2)
                    if !sub.isEmpty { Text(sub).font(.caption).foregroundColor(Theme.muted) }
                }
                .padding(.vertical, 8)
                content
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(Theme.ink.ignoresSafeArea())
        .toolbarBackground(Theme.ink, for: .navigationBar)
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// A list the screen loads from the server, with refresh and errors.
@MainActor
final class Loader<T>: ObservableObject {
    @Published var data: T?
    @Published var loading = false
    @Published var error: String?

    func load(_ f: @escaping () async throws -> T) async {
        loading = true
        error = nil
        do { data = try await f() } catch { self.error = error.localizedDescription }
        loading = false
    }
}

struct LoadState: View {
    let loading: Bool
    let error: String?
    var body: some View {
        if loading { ProgressView().tint(Theme.accent).frame(maxWidth: .infinity) }
        if let error { Text(error).font(.caption).foregroundColor(Theme.bad) }
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
                Text("COMPANION").font(.system(size: 12, weight: .bold, design: .monospaced)).foregroundColor(Theme.accent)
                Panel {
                    Text("SIGN IN").font(.system(size: 22, weight: .black))
                    Text("Use the same Pitlane HQ account as on your PC.").font(.caption).foregroundColor(Theme.muted)
                    TextField("Email", text: $email)
                        .textContentType(.username).keyboardType(.emailAddress).textInputAutocapitalization(.never).autocorrectionDisabled()
                        .padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 6)).padding(.top, 10)
                    SecureField("Password", text: $password)
                        .textContentType(.password)
                        .padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 6))
                    if let e = account.error { Text(e).font(.caption).foregroundColor(Theme.bad) }
                    Button {
                        Task { await account.login(email: email, password: password) }
                    } label: {
                        Text(account.busy ? "SIGNING IN…" : "SIGN IN").font(.headline.weight(.black)).frame(maxWidth: .infinity).padding(12)
                            .background(Theme.accent).foregroundColor(Theme.ink).clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .disabled(account.busy || email.isEmpty || password.isEmpty)
                    .padding(.top, 6)
                    HStack {
                        Link("Create an account", destination: webApp).font(.caption)
                        Spacer()
                        Link("Forgot password?", destination: URL(string: server.absoluteString + "/account/forgot")!).font(.caption).foregroundColor(Theme.muted)
                    }
                    .padding(.top, 6)
                }
                Text("Your password only unlocks your keys on this phone. It never leaves it.").font(.caption2).foregroundColor(Theme.muted)
            }
            .padding(24)
        }
        .background(Theme.ink.ignoresSafeArea())
    }
}

// ---------- tabs ----------

struct MainView: View {
    var body: some View {
        TabView {
            NavigationStack { HomeView() }.tabItem { Label("Home", systemImage: "house") }
            NavigationStack { AnalysisView() }.tabItem { Label("Analysis", systemImage: "chart.xyaxis.line") }
            NavigationStack { CommunityView() }.tabItem { Label("Community", systemImage: "person.3") }
            NavigationStack { LiveView() }.tabItem { Label("Live", systemImage: "antenna.radiowaves.left.and.right") }
            NavigationStack { ProfileView() }.tabItem { Label("Profile", systemImage: "person.crop.circle") }
        }
    }
}

struct HomeView: View {
    @EnvironmentObject var account: Account

    var body: some View {
        Screen(title: "Pitlane HQ", sub: "Your racing companion") {
            SectionLabel(text: "ACCOUNT")
            Panel {
                Text(account.display.isEmpty ? "Pitlane driver" : account.display).font(.title3.weight(.black))
                Text(account.email).font(.caption).foregroundColor(Theme.muted)
            }
            SectionLabel(text: "PC SETTINGS IN YOUR ACCOUNT")
            Panel {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(account.syncVersion > 0 ? "\(account.syncedFiles) files · version \(account.syncVersion)" : "Nothing synced yet").font(.subheadline.bold())
                        Text(account.syncUpdated > 0 ? "Updated " + Date(timeIntervalSince1970: account.syncUpdated / 1000).formatted(date: .abbreviated, time: .shortened) : "Turn on sync in PitlaneHQ.exe → Account")
                            .font(.caption).foregroundColor(Theme.muted)
                        if let e = account.error { Text(e).font(.caption).foregroundColor(Theme.bad) }
                    }
                    Spacer()
                    Button(account.busy ? "…" : "SYNC") { Task { await account.sync() } }
                        .font(.subheadline.bold()).buttonStyle(.borderedProminent).foregroundColor(Theme.ink).disabled(account.busy)
                }
            }
            SectionLabel(text: "MORE")
            Link(destination: webApp) { ActionRow(title: "FULL PITLANE HQ", sub: "Coach, comparisons and everything else on the web", icon: "safari") }
        }
        .refreshable { await account.sync() }
    }
}

struct AnalysisView: View {
    @EnvironmentObject var account: Account
    @StateObject private var list = Loader<[CloudSession]>()

    var body: some View {
        Screen(title: "My laps", sub: "Sessions uploaded by PitlaneHQ.exe") {
            LoadState(loading: list.loading, error: list.error)
            if let s = list.data, s.isEmpty {
                Panel { Text("No sessions yet. Drive with PitlaneHQ.exe running and signed in: your laps upload by themselves.").font(.subheadline).foregroundColor(Theme.muted) }
            }
            ForEach(list.data ?? []) { x in
                NavigationLink(value: x) {
                    Panel {
                        Text(x.track + (x.trackConfig.isEmpty ? "" : " · " + x.trackConfig)).font(.headline.weight(.black)).foregroundColor(Theme.fg).lineLimit(1)
                        Text(x.car).font(.caption).foregroundColor(Theme.muted).lineLimit(1)
                        HStack {
                            Text([x.kind, Date(timeIntervalSince1970: x.started / 1000).formatted(date: .abbreviated, time: .omitted), "\(x.laps) laps"].filter { !$0.isEmpty }.joined(separator: " · "))
                                .font(.caption).foregroundColor(Theme.muted)
                            Spacer()
                            Text(lapTime(x.best)).font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(Theme.purple)
                        }
                        .padding(.top, 4)
                    }
                }
                .buttonStyle(.plain)
            }
        }
        .navigationDestination(for: CloudSession.self) { SessionView(session: $0) }
        .refreshable { await list.load { try await account.sessions() } }
        .task { if list.data == nil { await list.load { try await account.sessions() } } }
    }
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
            LoadState(loading: laps.loading, error: laps.error)
            HStack(spacing: 8) {
                Metric(label: "BEST", value: lapTime(best), color: Theme.purple)
                Metric(label: "BEST SECTORS", value: lapTime(ideal))
            }
            SectionLabel(text: "LAPS")
            ForEach(all) { lap in
                Panel {
                    HStack {
                        Text("L\(lap.n)").font(.system(.subheadline, design: .monospaced)).foregroundColor(Theme.muted).frame(width: 44, alignment: .leading)
                        Text(lapTime(lap.time)).font(.system(.subheadline, design: .monospaced).bold())
                            .foregroundColor(!lap.valid ? Theme.bad : lap.time == best ? Theme.purple : Theme.fg)
                        Spacer()
                        if !lap.valid { Text("INVALID").font(.system(size: 10, design: .monospaced)).foregroundColor(Theme.bad) }
                    }
                    if !lap.sectors.isEmpty {
                        HStack(spacing: 10) {
                            ForEach(Array(lap.sectors.enumerated()), id: \.offset) { i, t in
                                Text("S\(i + 1) " + String(format: "%.3f", t)).font(.system(size: 11, design: .monospaced))
                                    .foregroundColor(lap.valid && i < bestSec.count && bestSec[i] == t ? Theme.purple : Theme.muted)
                            }
                        }
                    }
                }
            }
        }
        .task { if laps.data == nil { await laps.load { try await account.laps(session.id) } } }
    }
}

struct CommunityView: View {
    @EnvironmentObject var account: Account
    @StateObject private var list = Loader<[Combo]>()
    @State private var q = ""

    var body: some View {
        let shown = (list.data ?? []).filter { q.isEmpty || $0.track.localizedCaseInsensitiveContains(q) || $0.car.localizedCaseInsensitiveContains(q) }
        return Screen(title: "Community", sub: "Laps shared by Pitlane HQ drivers · iRacing") {
            TextField("Search track or car", text: $q).padding(10).background(Theme.surface2).clipShape(RoundedRectangle(cornerRadius: 6))
            LoadState(loading: list.loading, error: list.error)
            if list.data != nil && shown.isEmpty {
                Panel { Text("No shared laps found.").font(.subheadline).foregroundColor(Theme.muted) }
            }
            ForEach(shown) { x in
                NavigationLink(value: x) {
                    Panel {
                        Text(x.track).font(.headline.weight(.black)).foregroundColor(Theme.fg).lineLimit(1)
                        Text(x.car).font(.caption).foregroundColor(Theme.muted).lineLimit(1)
                        HStack {
                            Text("\(x.laps) laps").font(.caption).foregroundColor(Theme.muted)
                            Spacer()
                            Text(lapTime(x.best)).font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(Theme.purple)
                        }
                        .padding(.top, 4)
                    }
                }
                .buttonStyle(.plain)
            }
        }
        .navigationDestination(for: Combo.self) { ComboView(combo: $0) }
        .refreshable { await list.load { try await account.combos() } }
        .task { if list.data == nil { await list.load { try await account.combos() } } }
    }
}

struct ComboView: View {
    @EnvironmentObject var account: Account
    let combo: Combo
    @StateObject private var board = Loader<[CommunityLap]>()

    var body: some View {
        let laps = board.data ?? []
        let top = laps.first?.time
        return Screen(title: combo.track, sub: combo.car) {
            LoadState(loading: board.loading, error: board.error)
            SectionLabel(text: "FASTEST DRIVERS")
            ForEach(Array(laps.enumerated()), id: \.element.id) { i, lap in
                Panel {
                    HStack {
                        Text("\(i + 1)").font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(i == 0 ? Theme.purple : Theme.muted).frame(width: 32, alignment: .leading)
                        Text(lap.alias).font(.subheadline.bold()).lineLimit(1)
                        Spacer()
                        VStack(alignment: .trailing, spacing: 0) {
                            Text(lapTime(lap.time)).font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(i == 0 ? Theme.purple : Theme.fg)
                            if let top, i > 0 { Text(String(format: "+%.3f", lap.time - top)).font(.system(size: 11, design: .monospaced)).foregroundColor(Theme.muted) }
                        }
                    }
                }
            }
        }
        .task { if board.data == nil { await board.load { try await account.leaderboard(combo) } } }
    }
}

struct LiveView: View {
    @EnvironmentObject var account: Account
    @StateObject private var live = Live()
    @Environment(\.scenePhase) private var phase

    private var status: (String, Color) {
        switch live.link {
        case .off: return ("OFFLINE", Theme.muted)
        case .connecting: return ("CONNECTING", Theme.muted)
        case .open:
            if !live.pcOnline { return ("PC OFFLINE", Theme.muted) }
            if !live.simConnected { return ("PC ONLINE · SIM NOT RUNNING", Theme.accent) }
            return ("LIVE", Theme.good)
        }
    }

    var body: some View {
        let gear = live.num("Gear").map { Int($0) }
        let delta = live.num("LapDeltaToBestLap")
        return Screen(title: "Live telemetry", sub: "From PitlaneHQ.exe through your account") {
            StatusPill(text: status.0, color: status.1)
            if let m = live.message { Text(m).font(.caption).foregroundColor(Theme.bad) }
            if live.link == .open && !live.pcOnline {
                Panel { Text("Open PitlaneHQ.exe on your PC and sign in with this account. Live telemetry starts by itself.").font(.subheadline).foregroundColor(Theme.muted) }
            }
            HStack(spacing: 8) {
                Metric(label: "SPEED", value: live.num("Speed").map { String(format: "%.0f", $0 * 3.6) } ?? "—")
                Metric(label: "GEAR", value: gear.map { $0 < 0 ? "R" : $0 == 0 ? "N" : "\($0)" } ?? "—")
                Metric(label: "RPM", value: live.num("RPM").map { String(format: "%.0f", $0) } ?? "—")
            }
            HStack(spacing: 8) {
                Metric(label: "LAP", value: live.num("Lap").map { "\(Int($0))" } ?? "—")
                Metric(label: "POS", value: live.num("PlayerCarPosition").flatMap { $0 > 0 ? "P\(Int($0))" : nil } ?? "—")
                Metric(label: "FUEL", value: live.num("FuelLevel").map { String(format: "%.1f L", $0) } ?? "—")
            }
            HStack(spacing: 8) {
                Metric(label: "CURRENT", value: lapTime(live.num("LapCurrentLapTime")))
                Metric(label: "LAST", value: lapTime(live.num("LapLastLapTime")))
                Metric(label: "BEST", value: lapTime(live.num("LapBestLapTime")), color: Theme.purple)
            }
            Metric(label: "DELTA TO BEST", value: delta.map { String(format: "%+.3f", $0) } ?? "—", color: delta == nil ? Theme.fg : delta! <= 0 ? Theme.good : Theme.bad)
            Panel {
                SectionLabel(text: "INPUTS")
                bar("THROTTLE", live.num("Throttle"), Theme.good)
                bar("BRAKE", live.num("Brake"), Theme.bad)
            }
            Text("Encrypted with your account key: the server only passes it along.").font(.caption2).foregroundColor(Theme.muted)
        }
        .onAppear { live.start(token: account.token, key: account.dataKey) }
        .onDisappear { live.stop() }
        .onChange(of: phase) { p in
            if p == .active { live.start(token: account.token, key: account.dataKey) } else { live.stop() }
        }
    }

    private func bar(_ label: String, _ v: Double?, _ c: Color) -> some View {
        HStack {
            Text(label).font(.system(size: 11, design: .monospaced)).foregroundColor(Theme.muted).frame(width: 80, alignment: .leading)
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

struct ProfileView: View {
    @EnvironmentObject var account: Account

    var body: some View {
        Screen(title: "Profile", sub: "Account") {
            Panel {
                Text(account.display.isEmpty ? "Pitlane driver" : account.display).font(.title3.weight(.black))
                Text(account.email).font(.caption).foregroundColor(Theme.muted)
            }
            Link(destination: webApp) { ActionRow(title: "ACCOUNT SETTINGS", sub: "Name, password and devices on the web", icon: "person.badge.key") }
            Panel {
                Text("YOUR DATA").font(.headline.weight(.black))
                Text("Your password never leaves this phone. Your synced settings and live telemetry are encrypted with your account key before they reach the server.")
                    .font(.caption).foregroundColor(Theme.muted)
            }
            Button {
                Task { await account.logout() }
            } label: {
                Text("SIGN OUT").font(.headline.bold()).frame(maxWidth: .infinity).padding(12)
                    .background(Theme.surface2).foregroundColor(Theme.bad).clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
    }
}
