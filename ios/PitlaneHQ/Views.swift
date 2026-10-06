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
    var body: some View {
        VStack(spacing: 0) {
            if !net.online { banner(t("showing_saved"), Theme.bad) }
            if account.demo { banner(t("demo_banner"), Theme.accent) }
        }
    }

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
                        Text(title.uppercased()).font(.system(size: 22, weight: .black)).foregroundColor(Theme.fg).lineLimit(2)
                        if !sub.isEmpty { Text(sub).font(.caption).foregroundColor(Theme.muted).lineLimit(2) }
                    }
                    .padding(.vertical, 8)
                    content
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
        }
        .background(Theme.ink.ignoresSafeArea())
        .toolbarBackground(Theme.ink, for: .navigationBar)
        .navigationBarTitleDisplayMode(.inline)
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
    }
}

// ---------- tabs ----------

struct MainView: View {
    var body: some View {
        TabView {
            NavigationStack { HomeView().routes() }.tabItem { Label(t("home"), systemImage: "house") }
            NavigationStack { AnalysisView().routes() }.tabItem { Label(t("analysis"), systemImage: "chart.xyaxis.line") }
            NavigationStack { CommunityView().routes() }.tabItem { Label(t("community"), systemImage: "person.3") }
            NavigationStack { LiveView() }.tabItem { Label(t("live"), systemImage: "antenna.radiowaves.left.and.right") }
            NavigationStack { SettingsView() }.tabItem { Label(t("settings"), systemImage: "gearshape") }
        }
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
        Screen(title: account.display.isEmpty ? t("driver") : account.display, sub: t("racing_companion")) {
            SectionLabel(text: t("race_summary"))
            HStack(spacing: 8) {
                Metric(label: t("current_ir"), value: last.map { $0.ir > 0 ? "\($0.ir + $0.irChange)" : "—" } ?? "—")
                Metric(label: t("ir_change"), value: recent.isEmpty ? "—" : signed(change), color: change > 0 ? Theme.good : change < 0 ? Theme.bad : Theme.fg, sub: t("last_races", recent.count))
            }
            HStack(spacing: 8) {
                Metric(label: t("races"), value: "\(races.count)")
                Metric(label: t("wins"), value: "\(races.filter { $0.finish == 1 }.count)")
                Metric(label: t("top5"), value: "\(races.filter { (1...5).contains($0.finish) }.count)")
                Metric(label: t("avg_inc"), value: recent.isEmpty ? "—" : String(format: "%.1f", Double(recent.reduce(0) { $0 + $1.inc }) / Double(recent.count)))
            }
            Panel {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(t("safety").uppercased()).font(.system(size: 13, weight: .black))
                        Text(t("safety_wip")).font(.caption2).foregroundColor(Theme.muted)
                    }
                    Spacer()
                    StatusPill(text: "WIP", color: Theme.accent)
                }
            }
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
                    VStack(alignment: .leading, spacing: 2) {
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
        let best = x.laps.filter { $0.time > 0 }.map(\.time).min()
        Screen(title: x.track, sub: x.car + " · " + dayTime(x.when)) {
            HStack(spacing: 8) {
                Metric(label: t("start"), value: "P\(x.start)")
                Metric(label: t("finish"), value: x.dnf ? t("dnf") : "P\(x.finish)", color: x.finish == 1 ? Theme.purple : Theme.fg)
                Metric(label: "iRating", value: signed(x.irChange), color: x.irChange > 0 ? Theme.good : x.irChange < 0 ? Theme.bad : Theme.fg, sub: x.ir > 0 ? "\(x.ir) → \(x.ir + x.irChange)" : nil)
            }
            HStack(spacing: 8) {
                Metric(label: t("incidents"), value: "\(x.inc)x", color: x.inc >= 8 ? Theme.bad : Theme.fg)
                Metric(label: t("pos_gain"), value: signed(x.start - x.finish), color: x.start > x.finish ? Theme.good : x.start < x.finish ? Theme.bad : Theme.fg)
                Metric(label: t("sof"), value: x.sof > 0 ? "\(x.sof)" : "—")
            }
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
                            Text(lapTime(l.time)).foregroundColor(l.time == best ? Theme.purple : Theme.fg)
                            Spacer()
                            Text("P\(l.pos)").foregroundColor(Theme.muted).frame(width: 44, alignment: .leading)
                            Text(l.pit ? t("pit") : l.inc > 0 ? "\(l.inc)x" : "").foregroundColor(l.pit ? Theme.blue : Theme.bad).frame(width: 40, alignment: .leading)
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
                                Text([x.kind, day(x.started), t("laps_n", x.laps)].filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption).foregroundColor(Theme.muted)
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
                                .foregroundColor(!lap.valid ? Theme.bad : lap.time == best ? Theme.purple : Theme.fg)
                            Spacer()
                            if let best, lap.valid, lap.time > best { Text(String(format: "+%.3f", lap.time - best)).font(.system(size: 11, design: .monospaced)).foregroundColor(Theme.muted) }
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

    var body: some View {
        let d = refTime.map { lap.time - $0 }
        Screen(title: t("lap_analysis") + " · L\(lap.n)", sub: session.track + " · " + session.car) {
            HStack(spacing: 8) {
                Text(t("compare_with")).font(.caption).foregroundColor(Theme.muted)
                Tabs(labels: [t("my_best"), t("community_fastest")], selected: Binding(get: { community ? 1 : 0 }, set: { community = $0 == 1 }))
            }
            LoadState(loading: loading, error: error) { Task { await load() } }
            HStack(spacing: 8) {
                Metric(label: t("you"), value: lapTime(lap.time), color: Theme.accent)
                Metric(label: refLabel.isEmpty ? t("ref") : refLabel, value: lapTime(refTime), color: Theme.blue)
                Metric(label: t("delta"), value: d.map { String(format: "%+.3f", $0) } ?? "—", color: d == nil ? Theme.fg : d! <= 0 ? Theme.good : Theme.bad)
            }
            if !loading {
                if let trace {
                    let c = compare(trace, ref)
                    if ref == nil { Text(t("no_reference")).font(.caption).foregroundColor(Theme.muted) }
                    Chart(title: t("speed") + " (km/h)", series: [(c.speedA, Theme.accent)] + (c.speedB.map { [($0, Theme.blue)] } ?? []))
                    if let delta = c.delta { Chart(title: t("delta") + " (s)", series: [(delta, Theme.purple)], zero: true) }
                    Chart(title: t("inputs"), series: [(c.thrA, Theme.good), (c.brkA, Theme.bad)], fixedMax: 1)
                    if ref != nil {
                        SectionLabel(text: t("where_time"))
                        let ls = losses(c)
                        if ls.isEmpty { EmptyNote(text: t("all_clean")) }
                        ForEach(ls, id: \.self) { lo in
                            Panel {
                                HStack {
                                    Text(t("at_m", lo.fromM)).font(.headline.weight(.black))
                                    Spacer()
                                    Text(t("lost", String(format: "%.3f", lo.lost))).font(.system(.subheadline, design: .monospaced).bold()).foregroundColor(Theme.bad)
                                }
                                if let b = lo.brakeDiffM, abs(b) >= 5 { Text(b < 0 ? t("brake_earlier", -b) : t("brake_later", b)).font(.caption).foregroundColor(Theme.muted) }
                                Text(t("min_speed", String(format: "%.0f", lo.minA), String(format: "%.0f", lo.minB))).font(.caption).foregroundColor(Theme.muted)
                                if let th = lo.throttleDiffM, th >= 10 { Text(t("throttle_later", th)).font(.caption).foregroundColor(Theme.muted) }
                            }
                        }
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

/// A line chart over the lap distance.
struct Chart: View {
    let title: String
    let series: [([Double], Color)]
    var zero = false
    var fixedMax: Double? = nil

    var body: some View {
        let all = series.flatMap { $0.0 }
        let lo = fixedMax != nil ? 0 : zero ? min(all.min() ?? 0, 0) : (all.min() ?? 0)
        let hi = fixedMax ?? (zero ? max(all.max() ?? 0, 0) : (all.max() ?? 1))
        let span = hi - lo > 1e-9 ? hi - lo : 1
        Panel {
            SectionLabel(text: title.uppercased())
            GeometryReader { g in
                ZStack {
                    if zero {
                        Path { p in
                            let y = g.size.height * (1 - (0 - lo) / span)
                            p.move(to: CGPoint(x: 0, y: y))
                            p.addLine(to: CGPoint(x: g.size.width, y: y))
                        }
                        .stroke(Theme.line, lineWidth: 1)
                    }
                    ForEach(Array(series.enumerated()), id: \.offset) { _, s in
                        Path { p in
                            let v = s.0
                            guard v.count > 1 else { return }
                            for (i, x) in v.enumerated() {
                                let pt = CGPoint(x: g.size.width * Double(i) / Double(v.count - 1), y: g.size.height * (1 - (x - lo) / span))
                                if i == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
                            }
                        }
                        .stroke(s.1, lineWidth: 1.5)
                    }
                }
            }
            .frame(height: 120)
            HStack {
                Text(fixedMax == 1 ? "0%" : String(format: "%.1f", lo))
                Spacer()
                Text(fixedMax == 1 ? "100%" : String(format: "%.1f", hi))
            }
            .font(.system(size: 10)).foregroundColor(Theme.muted)
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
            Tabs(labels: [t("leaderboards"), t("reports"), t("setups")], selected: $tab)
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
            if let m = live.message { Text(t(m)).font(.caption).foregroundColor(Theme.bad) }
            if live.link == .open && !live.pcOnline { EmptyNote(text: t("open_pc")) }
            HStack(spacing: 8) {
                Metric(label: t("speed"), value: live.num("Speed").map { String(format: "%.0f", $0 * 3.6) } ?? "—")
                Metric(label: t("gear"), value: gear.map { $0 < 0 ? "R" : $0 == 0 ? "N" : "\($0)" } ?? "—")
                Metric(label: "RPM", value: live.num("RPM").map { String(format: "%.0f", $0) } ?? "—")
            }
            HStack(spacing: 8) {
                Metric(label: t("lap"), value: live.num("Lap").map { "\(Int($0))" } ?? "—")
                Metric(label: t("pos"), value: live.num("PlayerCarPosition").flatMap { $0 > 0 ? "P\(Int($0))" : nil } ?? "—")
                Metric(label: t("fuel"), value: live.num("FuelLevel").map { String(format: "%.1f L", $0) } ?? "—")
            }
            HStack(spacing: 8) {
                Metric(label: t("current"), value: lapTime(live.num("LapCurrentLapTime")))
                Metric(label: t("last"), value: lapTime(live.num("LapLastLapTime")))
                Metric(label: t("best"), value: lapTime(live.num("LapBestLapTime")), color: Theme.purple)
            }
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

// ---------- Settings ----------

struct SettingsView: View {
    @EnvironmentObject var account: Account
    @EnvironmentObject var i18n: I18n
    @StateObject private var devices = Loader<[Device]>()

    private var version: String { Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "" }

    var body: some View {
        Screen(title: t("settings")) {
            SectionLabel(text: t("account"))
            Panel {
                Text(account.display.isEmpty ? t("driver") : account.display).font(.title3.weight(.black))
                Text(account.email).font(.caption).foregroundColor(Theme.muted)
                Text(account.verified ? t("verified") : t("not_verified")).font(.caption2).foregroundColor(account.verified ? Theme.good : Theme.accent)
            }
            Link(destination: webApp) { ActionRow(title: t("web").uppercased(), sub: t("web_sub"), icon: "safari") }
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
            SectionLabel(text: t("testing"))
            Panel {
                Toggle(t("demo_mode"), isOn: $account.demo).font(.subheadline.bold())
                Text(t("demo_note")).font(.caption).foregroundColor(Theme.muted)
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
