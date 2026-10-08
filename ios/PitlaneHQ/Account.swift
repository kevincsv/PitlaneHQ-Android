import Foundation
import Security

let server = URL(string: "https://pitlanehq.app")!
let webApp = URL(string: "https://pitlanehq.app/")!
/// Support Pitlane HQ (Settings); empty: not shown.
let patreonURL = "https://www.patreon.com/c/PitlaneHQ/membership"
let supportEmail = "support@pitlanehq.app"
let feedbackURL = URL(string: "mailto:support@pitlanehq.app?subject=Pitlane%20HQ%20feedback")!
let changelogURL = "https://pitlanehq.app/changelog/phones"

/// Errors the screens show in the user's language: `key` is a key of I18n (or a server message).
struct AppError: LocalizedError {
    let key: String
    var errorDescription: String? { key }
    static let signedOut = AppError(key: "signed_out")
    static let offline = AppError(key: "no_internet")
    static let serverDown = AppError(key: "server_down")
}

/// Data from the server, or the copy saved on this phone when the server cannot be reached (`stale`).
struct Got<T> {
    let data: T
    var stale = false
}

// ---------- Keychain ----------

enum Vault {
    private static let service = "com.pitlanehq.account"

    static func set(_ key: String, _ value: Data?) {
        let q: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service, kSecAttrAccount as String: key]
        SecItemDelete(q as CFDictionary)
        guard let value else { return }
        var add = q
        add[kSecValueData as String] = value
        add[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        SecItemAdd(add as CFDictionary, nil)
    }

    static func get(_ key: String) -> Data? {
        let q: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service, kSecAttrAccount as String: key,
            kSecReturnData as String: true, kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var out: AnyObject?
        return SecItemCopyMatching(q as CFDictionary, &out) == errSecSuccess ? out as? Data : nil
    }
}

private func num(_ v: Any?) -> Double? { (v as? NSNumber)?.doubleValue }
private func int(_ v: Any?) -> Int { (v as? NSNumber)?.intValue ?? 0 }
private func str(_ v: Any?) -> String { v as? String ?? "" }
private func orNull(_ v: Any?) -> Any { v ?? NSNull() }
private func pos(_ v: Any?) -> Double? { num(v).flatMap { $0.isFinite && $0 > 0 ? $0 : nil } }
private func doubles(_ v: Any?) -> [Double] { (v as? [NSNumber] ?? []).map(\.doubleValue) }

/// The Pitlane HQ account, exactly like the PC, the web app and Android: the password only
/// derives the keys on this phone; the session token and the data key stay in the Keychain,
/// and the copies saved for offline use are sealed with the data key.
@MainActor
final class Account: ObservableObject {
    @Published var signedIn = false
    @Published var email = ""
    @Published var display = ""
    @Published var verified = true
    /// one of the server's admins (ADMINS): only they can turn on demo data
    @Published var realAdmin = false {
        didSet { if !admin && demo { demo = false } }
    }
    /// an admin can see the app as everyone else does (to test it), on this phone
    @Published var asUser = UserDefaults.standard.bool(forKey: "asUser") {
        didSet { UserDefaults.standard.set(asUser, forKey: "asUser"); if !admin && demo { demo = false } }
    }
    /// what the screens use: false while an admin looks as a normal user
    var admin: Bool { realAdmin && !asUser }
    @Published var busy = false
    @Published var error: String?
    @Published var needCode = false        // the password was right: the authenticator code comes next
    @Published var twoFactor = UserDefaults.standard.bool(forKey: "twoFactor")
    @Published var recoveryLeft = UserDefaults.standard.integer(forKey: "recoveryLeft")
    /// the supporter badge (donates), and whether its owner hid it
    @Published var supporter = UserDefaults.standard.bool(forKey: "supporter")
    @Published var supporterHidden = UserDefaults.standard.bool(forKey: "supporterHidden")
    private var pending: (token: String, email: String, wrap: Data)?
    @Published var syncedFiles = 0
    @Published var syncVersion = 0
    @Published var syncUpdated: Double = 0
    @Published var races: [Race] = []
    @Published var demo = UserDefaults.standard.bool(forKey: "demo") {
        didSet { UserDefaults.standard.set(demo, forKey: "demo"); loadRaces() }
    }

    private let defaults = UserDefaults.standard
    private let dir: URL = {
        let d = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0].appendingPathComponent("saved", isDirectory: true)
        try? FileManager.default.createDirectory(at: d, withIntermediateDirectories: true)
        return d
    }()

    init() {
        // a reinstall keeps the Keychain but not the settings: start signed out then
        if defaults.string(forKey: "email") == nil { Vault.set("token", nil); Vault.set("dataKey", nil) }
        signedIn = Vault.get("token") != nil && Vault.get("dataKey") != nil
        email = defaults.string(forKey: "email") ?? ""
        display = defaults.string(forKey: "display") ?? ""
        verified = defaults.object(forKey: "verified") as? Bool ?? true
        realAdmin = defaults.bool(forKey: "admin")
        if !admin && demo { demo = false }
        syncedFiles = defaults.integer(forKey: "syncedFiles")
        syncVersion = defaults.integer(forKey: "syncVersion")
        syncUpdated = defaults.double(forKey: "syncUpdated")
        loadRaces()
    }

    var token: String? { Vault.get("token").flatMap { String(data: $0, encoding: .utf8) } }
    var dataKey: Data? { Vault.get("dataKey") }

    // ---------- saved copies (sealed with the account's data key) ----------

    private func file(_ name: String) -> URL { dir.appendingPathComponent(String(name.map { $0.isLetter || $0.isNumber || "_.-".contains($0) ? $0 : "_" }.prefix(120))) }

    private func save(_ name: String, _ data: Data) {
        guard let key = dataKey, let s = try? PLCrypto.seal(key: key, plain: data, aad: PLCrypto.accountAAD) else { return }
        try? Data(s.utf8).write(to: file(name), options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
    }

    private func saved(_ name: String) -> Data? {
        guard let key = dataKey, let d = try? Data(contentsOf: file(name)), let s = String(data: d, encoding: .utf8) else { return nil }
        return try? PLCrypto.open(key: key, sealed: s, aad: PLCrypto.accountAAD)
    }

    private func clear() {
        Vault.set("token", nil)
        Vault.set("dataKey", nil)
        for k in ["email", "display", "verified", "admin", "syncedFiles", "syncVersion", "syncUpdated", "twoFactor", "recoveryLeft"] { defaults.removeObject(forKey: k) }
        twoFactor = false
        recoveryLeft = 0
        try? FileManager.default.removeItem(at: dir)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        signedIn = false
        display = ""
        syncedFiles = 0
        syncVersion = 0
        syncUpdated = 0
        realAdmin = false
        races = []
    }

    // ---------- server ----------

    func call(_ method: String, _ path: String, body: [String: Any]? = nil, auth: Bool = true) async throws -> Data {
        guard let url = URL(string: server.absoluteString + path) else { throw AppError.serverDown }
        var r = URLRequest(url: url, timeoutInterval: 20)
        r.httpMethod = method
        if let body {
            r.httpBody = try JSONSerialization.data(withJSONObject: body)
            r.setValue("application/json", forHTTPHeaderField: "content-type")
        }
        if auth {
            guard let t = token else { throw AppError.signedOut }
            r.setValue("Bearer " + t, forHTTPHeaderField: "authorization")
        }
        let pair: (Data, URLResponse)
        do {
            pair = try await URLSession.shared.data(for: r)
        } catch {
            throw AppError.offline
        }
        let (data, resp) = pair
        let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
        if code == 401 && auth {
            clear()
            error = AppError.signedOut.key
            throw AppError.signedOut
        }
        guard (200..<300).contains(code) else {
            if code >= 500 { throw AppError.serverDown }
            let msg = (try? JSONSerialization.jsonObject(with: data) as? [String: Any])?["error"] as? String
            throw AppError(key: msg ?? "server_down")
        }
        return data
    }

    /// A GET whose answer is saved, so the screen still has it without a connection.
    private func cachedGet(_ path: String) async throws -> Got<Any> {
        do {
            let d = try await call("GET", path)
            save(path, d)
            return Got(data: try JSONSerialization.jsonObject(with: d))
        } catch let e as AppError where e.key == AppError.offline.key || e.key == AppError.serverDown.key {
            guard let d = saved(path) else { throw e }
            return Got(data: try JSONSerialization.jsonObject(with: d), stale: true)
        }
    }

    func login(email e: String, password: String) async {
        let em = e.trimmingCharacters(in: .whitespaces).lowercased()
        guard em.range(of: #"^[^@\s]+@[^@\s]+\.[^@\s]+$"#, options: .regularExpression) != nil else { error = "bad_email"; return }
        guard !password.isEmpty else { error = "enter_pw"; return }
        busy = true
        error = nil
        defer { busy = false }
        do {
            let keys = try await Task.detached { try PLCrypto.derive(email: em, password: password) }.value
            let data = try await call("POST", "/account/login", body: ["email": em, "auth": keys.auth, "device": "iPhone"], auth: false)
            guard let j = try JSONSerialization.jsonObject(with: data) as? [String: Any] else { throw AppError.serverDown }
            if j["twoFactor"] as? Bool == true, let p = j["pending"] as? String {
                pending = (p, em, keys.wrap)
                needCode = true
                return
            }
            try finishLogin(j, email: em, wrap: keys.wrap)
            await sync()
        } catch {
            self.error = (error as? AppError)?.key ?? error.localizedDescription
        }
    }

    /// Second step of the sign-in: the 6-digit code of the authenticator app, or a recovery code.
    func loginCode(_ code: String) async {
        guard let p = pending else { needCode = false; error = "sign_in_again"; return }
        busy = true
        error = nil
        defer { busy = false }
        do {
            let data = try await call("POST", "/account/login/2fa", body: ["pending": p.token, "code": code.trimmingCharacters(in: .whitespaces)], auth: false)
            guard let j = try JSONSerialization.jsonObject(with: data) as? [String: Any] else { throw AppError.serverDown }
            pending = nil
            needCode = false
            try finishLogin(j, email: p.email, wrap: p.wrap)
            await sync()
        } catch {
            self.error = (error as? AppError)?.key ?? error.localizedDescription
        }
    }

    func cancelCode() { pending = nil; needCode = false; error = nil }

    private func finishLogin(_ j: [String: Any], email em: String, wrap: Data) throws {
        guard let token = j["token"] as? String, let wrapped = j["wrappedKey"] as? String else { throw AppError.serverDown }
        let key = try PLCrypto.open(key: wrap, sealed: wrapped, aad: PLCrypto.accountAAD)
        guard key.count == 32 else { throw AppError.serverDown }
        Vault.set("token", Data(token.utf8))
        Vault.set("dataKey", key)
        email = em
        display = str(j["display"])
        verified = j["verified"] as? Bool ?? true
        realAdmin = j["admin"] as? Bool ?? false
        twoFactor = j["twoFactor"] as? Bool ?? false
        defaults.set(realAdmin, forKey: "admin")
        defaults.set(em, forKey: "email")
        defaults.set(display, forKey: "display")
        defaults.set(verified, forKey: "verified")
        defaults.set(twoFactor, forKey: "twoFactor")
        signedIn = true
    }

    // ---------- two-step sign-in (optional, recommended) ----------
    struct TwoFactorSetup { let secret: String; let url: URL }

    func setup2fa(password: String) async throws -> TwoFactorSetup {
        let keys = try await Task.detached { [email] in try PLCrypto.derive(email: email, password: password) }.value
        let j = try JSONSerialization.jsonObject(with: await call("POST", "/account/2fa/setup", body: ["auth": keys.auth])) as? [String: Any] ?? [:]
        guard let secret = j["secret"] as? String, let u = (j["url"] as? String).flatMap(URL.init) else { throw AppError.serverDown }
        return TwoFactorSetup(secret: secret, url: u)
    }

    /// Confirms the first code; returns the recovery codes (shown once).
    func enable2fa(code: String) async throws -> [String] {
        let j = try JSONSerialization.jsonObject(with: await call("POST", "/account/2fa/enable", body: ["code": code.trimmingCharacters(in: .whitespaces)])) as? [String: Any] ?? [:]
        let codes = j["codes"] as? [String] ?? []
        twoFactor = true
        recoveryLeft = codes.count
        defaults.set(true, forKey: "twoFactor")
        defaults.set(codes.count, forKey: "recoveryLeft")
        return codes
    }

    func disable2fa(password: String, code: String) async throws {
        let keys = try await Task.detached { [email] in try PLCrypto.derive(email: email, password: password) }.value
        _ = try await call("POST", "/account/2fa/disable", body: ["auth": keys.auth, "code": code.trimmingCharacters(in: .whitespaces)])
        twoFactor = false
        recoveryLeft = 0
        defaults.set(false, forKey: "twoFactor")
        defaults.set(0, forKey: "recoveryLeft")
    }

    /// Pulls the encrypted settings bundle the PC keeps in the account and opens it here: your races come from it.
    func sync(quiet: Bool = false) async {
        if !quiet {
            busy = true
            error = nil
        }
        defer { if !quiet { busy = false } }
        do {
            let data = try await call("GET", "/account/sync")
            let j = try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
            var files = 0
            if let blob = j["blob"] as? String, !blob.isEmpty, let key = dataKey {
                let raw = try PLCrypto.gunzip(PLCrypto.open(key: key, sealed: blob, aad: PLCrypto.accountAAD))
                let all = try JSONSerialization.jsonObject(with: raw) as? [String: Any] ?? [:]
                files = all.count
                // Go writes the files as base64 strings
                if let b64 = all["races.json"] as? String, let racesData = Data(base64Encoded: b64), let a = try? JSONSerialization.jsonObject(with: racesData) as? [[String: Any]] {
                    let slim = trimRaces(a)
                    if let d = try? JSONSerialization.data(withJSONObject: slim) { save("races", d) }
                }
            }
            syncedFiles = files
            syncVersion = int(j["version"])
            syncUpdated = num(j["updated"]) ?? 0
            defaults.set(syncedFiles, forKey: "syncedFiles")
            defaults.set(syncVersion, forKey: "syncVersion")
            defaults.set(syncUpdated, forKey: "syncUpdated")
            if let me = try? JSONSerialization.jsonObject(with: await call("GET", "/account/me")) as? [String: Any] {
                display = str(me["display"])
                verified = me["verified"] as? Bool ?? true
                realAdmin = me["admin"] as? Bool ?? false
                twoFactor = me["twoFactor"] as? Bool ?? false
                recoveryLeft = me["recoveryLeft"] as? Int ?? 0
                supporter = me["supporter"] as? Bool ?? false
                supporterHidden = me["supporterHidden"] as? Bool ?? false
                defaults.set(supporter, forKey: "supporter")
                defaults.set(supporterHidden, forKey: "supporterHidden")
                defaults.set(realAdmin, forKey: "admin")
                defaults.set(display, forKey: "display")
                defaults.set(verified, forKey: "verified")
                defaults.set(twoFactor, forKey: "twoFactor")
                defaults.set(recoveryLeft, forKey: "recoveryLeft")
            }
            loadRaces()
            await publishProfileRaces()
        } catch {
            if !quiet || (error as? AppError)?.key == AppError.signedOut.key { self.error = (error as? AppError)?.key ?? error.localizedDescription }
        }
    }

    private var lastAuto = Date.distantPast

    /// The app came to the screen (it opened, or came back from the background): your account, public
    /// name and races are read again by themselves, at most every 30 seconds, without a spinner.
    func syncIfDue() async {
        guard signedIn, !busy, Date().timeIntervalSince(lastAuto) > 30 else { return }
        lastAuto = Date()
        await sync(quiet: true)
    }

    // only what the phone shows, newest first: races.json also has braking points and more
    /// Text saved with the wrong encoding by an older PC ("AutÃ³dromo"): only the damaged pieces are repaired.
    private func fixTxt(_ s: String) -> String {
        guard s.unicodeScalars.contains(where: { $0.value >= 0xC2 && $0.value <= 0xEF }) else { return s }
        var out = ""
        var buf: [UInt8] = []
        func flush() {
            if !buf.isEmpty {
                if let d = String(bytes: buf, encoding: .utf8) { out += d } else { out += String(String.UnicodeScalarView(buf.map { Unicode.Scalar($0) })) }
                buf = []
            }
        }
        let scalars = Array(s.unicodeScalars)
        var i = 0
        while i < scalars.count {
            let v = scalars[i].value
            let need = v >= 0xC2 && v <= 0xDF ? 1 : v >= 0xE0 && v <= 0xEF ? 2 : 0
            if need > 0 && i + need < scalars.count && (1...need).allSatisfy({ scalars[i + $0].value >= 0x80 && scalars[i + $0].value <= 0xBF }) {
                buf = (0...need).map { UInt8(scalars[i + $0].value) }
                flush()
                i += need + 1
            } else {
                out.unicodeScalars.append(scalars[i])
                i += 1
            }
        }
        return out
    }

    private func trimRaces(_ a: [[String: Any]]) -> [[String: Any]] {
        a.filter { str($0["game"]).isEmpty || str($0["game"]) == "iracing" }
            .sorted { (num($0["when"]) ?? 0) > (num($1["when"]) ?? 0) }
            .prefix(60)
            .map { r0 -> [String: Any] in
                var r = r0
                // the race's incident total, like the web: the largest of the report's total, the laps' and the events'
                let laps0 = r["laps"] as? [[String: Any]] ?? []
                let fromLaps: Int = laps0.reduce(0) { $0 + int($1["i"]) }
                let fromEv: Int = (r["incidents"] as? [[String: Any]] ?? []).reduce(0) { $0 + max(int($1["pts"]), int($1["p"])) }
                r["inc"] = max(int(r["inc"]), fromLaps, fromEv)
                r["brakes"] = nil
                r["incidents"] = nil
                r["laps"] = laps0.map { l -> [String: Any] in
                    ["n": l["n"] ?? 0, "t": l["t"] ?? 0, "p": l["p"] ?? 0, "i": l["i"] ?? 0, "pit": l["pit"] ?? false, "cut": l["cut"] ?? false]
                }
                return r
            }
    }

    func loadRaces() {
        if demo { races = Demo.races(); return }
        guard let d = saved("races"), let a = try? JSONSerialization.jsonObject(with: d) as? [[String: Any]] else { races = []; return }
        races = a.map { r in
            Race(
                id: str(r["id"]), when: num(r["when"]) ?? 0, track: fixTxt(str(r["track"])), car: fixTxt(str(r["car"])), official: r["official"] as? Bool ?? false,
                start: int(r["start"]), finish: int(r["finish"]), field: int(r["field"]),
                inc: max(int(r["inc"]), (r["laps"] as? [[String: Any]] ?? []).reduce(0) { $0 + int($1["i"]) }), best: pos(r["best"]),
                fieldBest: pos(r["fieldBest"]), avg: pos(r["avg"]), consistency: pos(r["consistency"]), pits: int(r["pits"]), fuelUsed: pos(r["fuelUsed"]),
                ir: int(r["ir"]), irChange: int(r["irChange"]), sof: int(r["sof"]), dnf: r["dnf"] as? Bool ?? false,
                laps: (r["laps"] as? [[String: Any]] ?? []).map { RaceLap(n: int($0["n"]), time: num($0["t"]) ?? 0, pos: int($0["p"]), inc: int($0["i"]), pit: $0["pit"] as? Bool ?? false, cut: $0["cut"] as? Bool ?? false) },
                results: (r["results"] as? [[String: Any]] ?? []).map {
                    RaceResult(pos: int($0["cpos"]) > 0 ? int($0["cpos"]) : int($0["pos"]), name: fixTxt(str($0["name"])), ir: int($0["ir"]), best: pos($0["best"]), inc: int($0["inc"]), laps: int($0["laps"]))
                }.sorted { $0.pos < $1.pos }
            )
        }
    }

    func logout() async {
        _ = try? await call("POST", "/account/logout")
        clear()
        error = nil
    }

    func devices() async throws -> Got<[Device]> {
        let j = try JSONSerialization.jsonObject(with: await call("GET", "/account/sessions")) as? [String: Any] ?? [:]
        return Got(data: (j["sessions"] as? [[String: Any]] ?? []).map { Device(id: str($0["id"]), device: str($0["device"]), lastSeen: num($0["lastSeen"]) ?? 0, current: $0["current"] as? Bool ?? false) })
    }

    func revoke(_ id: String) async throws {
        _ = try await call("POST", "/account/sessions/revoke", body: ["id": id])
    }

    // ---------- your laps (uploaded by PitlaneHQ.exe) ----------

    // session and lap ids look like acct_<id>:<…>; the ':' stays as it is in the path
    private func idPath(_ id: String) -> String {
        var allowed = CharacterSet.alphanumerics
        allowed.insert(charactersIn: "-._~:")
        return id.addingPercentEncoding(withAllowedCharacters: allowed) ?? id
    }

    func sessions() async throws -> Got<[CloudSession]> {
        if demo { return Got(data: Demo.sessions()) }
        let g = try await cachedGet("/api/sessions?limit=100")
        return Got(data: (g.data as? [[String: Any]] ?? []).compactMap { s in
            guard let id = s["id"] as? String else { return nil }
            return CloudSession(id: id, started: num(s["started"]) ?? 0, track: str(s["track"]), trackConfig: str(s["track_config"]), car: str(s["car"]), kind: str(s["kind"]), laps: int(s["laps"]), best: pos(s["best"]),
                                cat: s["cat"] as? String, lic: s["lic"] as? String)
        }, stale: g.stale)
    }

    func laps(_ sessionId: String) async throws -> Got<[CloudLap]> {
        if demo { return Got(data: Demo.laps(sessionId)) }
        let g = try await cachedGet("/api/sessions/" + idPath(sessionId))
        let a = (g.data as? [String: Any])?["laps"] as? [[String: Any]] ?? []
        return Got(data: a.map { l in
            CloudLap(id: str(l["id"]), n: int(l["n"]), time: num(l["time"]) ?? 0, valid: ((l["valid"] as? NSNumber)?.intValue ?? 1) == 1, sectors: doubles(l["sectors"]), inc: int(l["inc"]))
        }, stale: g.stale)
    }

    private func parseTrace(_ j: Any?) -> Trace? {
        guard let t = j as? [String: Any], let d = t["d"] as? [[NSNumber]], !d.isEmpty else { return nil }
        var tr = Trace(bin: num(t["bin"]) ?? 10, rows: d.map { $0.map(\.doubleValue) })
        if let x = t["x"] as? [NSNumber], let y = t["y"] as? [NSNumber], x.count == d.count, y.count == d.count {
            tr.x = x.map(\.doubleValue)
            tr.y = y.map(\.doubleValue)
        }
        if let inc = t["inc"] as? [NSNumber] { tr.inc = inc.map(\.doubleValue) }
        if let k = t["incK"] as? [String] { tr.incK = k }
        return tr
    }

    func lapTrace(_ lapId: String) async throws -> Trace? {
        if demo { return Demo.trace(lapId) }
        return parseTrace((try await cachedGet("/api/laps/" + idPath(lapId)).data as? [String: Any])?["trace"])
    }

    func bests() async throws -> Got<[PersonalBest]> {
        if demo { return Got(data: Demo.bests()) }
        let g = try await cachedGet("/api/bests")
        return Got(data: (g.data as? [[String: Any]] ?? []).compactMap { b in
            let game = str(b["game"])
            guard game.isEmpty || game == "iracing" else { return nil }
            let lap = str(b["bestLapId"]), ses = str(b["bestSessionId"])
            return PersonalBest(track: str(b["track"]), trackConfig: str(b["track_config"]), car: str(b["car"]), best: num(b["best"]) ?? 0, laps: int(b["laps"]),
                                last: num(b["last"]) ?? 0, bestLapId: lap.isEmpty ? nil : lap, bestSessionId: ses.isEmpty ? nil : ses)
        }.sorted { $0.last > $1.last }, stale: g.stale)
    }

    // ---------- community ----------

    func combos() async throws -> Got<[Combo]> {
        if demo { return Got(data: Demo.combos()) }
        let g = try await cachedGet("/community/combos?game=iracing")
        let a = (g.data as? [String: Any])?["combos"] as? [[String: Any]] ?? []
        return Got(data: a.map { c in
            Combo(trackId: (c["trackId"] as? NSNumber)?.int64Value ?? 0, track: str(c["track"]), carId: (c["carId"] as? NSNumber)?.int64Value ?? 0, car: str(c["car"]), laps: int(c["laps"]), best: pos(c["best"]), cat: c["cat"] as? String)
        }, stale: g.stale)
    }

    /// One line per driver: their best lap.
    func leaderboard(trackId: Int64, carId: Int64) async throws -> Got<[CommunityLap]> {
        if demo { return Got(data: Demo.board(trackId: trackId, carId: carId)) }
        let g = try await cachedGet("/community/laps?game=iracing&trackId=\(trackId)&carId=\(carId)")
        var seen = Set<String>()
        var out: [CommunityLap] = []
        for l in (g.data as? [String: Any])?["laps"] as? [[String: Any]] ?? [] {
            let alias = (l["alias"] as? String) ?? "Driver"
            guard let t = num(l["time"]) else { continue }
            if alias != "Anonymous" {
                if seen.contains(alias) { continue }
                seen.insert(alias)
            }
            out.append(CommunityLap(id: str(l["id"]), alias: alias, time: t, created: num(l["created"]) ?? 0, hasTrace: (l["hasTrace"] as? Bool) ?? false, sectors: doubles(l["sectors"]), mine: (l["mine"] as? Bool) ?? false,
                                    lic: l["lic"] as? String, prof: (l["prof"] as? Bool) ?? false, sup: (l["sup"] as? Bool) ?? false, field: (l["field"] as? Bool) ?? false))
        }
        return Got(data: out.sorted { $0.time < $1.time }, stale: g.stale)
    }

    // the admin profile: accounts, and shared items with who really uploaded them
    func adminList(_ kind: String) async throws -> [[String: Any]] {
        if kind == "status" { return [try await adminStatus()] }
        let j = try JSONSerialization.jsonObject(with: await call("GET", "/community/admin/" + kind)) as? [String: Any]
        let key = ["users": "users", "sessions": "sessions", "blocked": "blocked"][kind] ?? "items"
        return j?[key] as? [[String: Any]] ?? []
    }
    func adminDeleteUser(_ id: String) async throws { _ = try await call("DELETE", "/community/admin/users/" + idPath(id)) }
    /// The owner of Pitlane HQ gives or takes away the supporter badge.
    func adminSupporter(_ id: String, _ on: Bool) async throws { _ = try await call("POST", "/community/admin/supporter", body: ["id": id, "on": on]) }
    /// "In Pitlane HQ since" by hand (nil: the day the account was created)
    func adminSince(_ id: String, _ since: Double?) async throws { _ = try await call("POST", "/community/admin/since", body: ["id": id, "since": since.map { $0 as Any } ?? NSNull()]) }
    /// the admin tools: "models" (rebuild the coach models) or "unlock" (unblock the sign-ins)
    func adminTool(_ tool: String) async throws { _ = try await call("POST", "/community/admin/" + tool, body: tool == "unlock" ? (["all": true] as [String: Any]) : [:]) }
    /// help with an account: "verify", "2fa-off", "signout" or "rename" (with the new public name)
    func adminAccount(_ id: String, _ act: String, name: String? = nil) async throws { _ = try await call("POST", "/community/admin/users/" + idPath(id) + "/" + act, body: name.map { ["name": $0] as [String: Any] } ?? [:]) }
    /// unblock one entry ("k") or one account's sign-in ("account")
    func adminUnlock(_ key: String, _ value: String) async throws { _ = try await call("POST", "/community/admin/unlock", body: [key: value]) }
    /// A supporter hides (or shows again) their own badge.
    func setBadgeHidden(_ hidden: Bool) async throws { _ = try await call("POST", "/community/profile/badge", body: ["hidden": hidden]) }

    /// A driver's profile, from one of their laps on a leaderboard, or yours (lapId nil).
    func profile(_ lapId: String?) async throws -> DriverProfile {
        if demo { return Demo.profile(lapId) }
        // "acct:<id>": an account's profile, from the admin profile
        let q = (lapId.map { $0.hasPrefix("acct:") ? "id=" + String($0.dropFirst(5)) : "lap=" + ($0.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? $0) } ?? "me=1") + "&tz=\(-TimeZone.current.secondsFromGMT() / 60)"
        let j = (try JSONSerialization.jsonObject(with: await call("GET", "/community/profile?" + q, auth: token != nil)) as? [String: Any]) ?? [:]
        let races = (j["races"] as? [[String: Any]] ?? []).map { r in
            ProfileRace(when: num(r["when"]) ?? 0, track: fixTxt(str(r["track"])), car: fixTxt(str(r["car"])), cat: r["cat"] as? String, lic: r["lic"] as? String, official: r["official"] as? Bool ?? false,
                        start: int(r["start"]), finish: int(r["finish"]), field: int(r["field"]), inc: int(r["inc"]), best: pos(r["best"]), irChange: int(r["irChange"]), dnf: r["dnf"] as? Bool ?? false)
        }
        let laps = (j["laps"] as? [[String: Any]] ?? []).map { x in
            ProfileLap(track: fixTxt(str(x["track"])), car: fixTxt(str(x["car"])), time: num(x["time"]) ?? 0, created: num(x["created"]) ?? 0, cat: x["cat"] as? String, lic: x["lic"] as? String, anon: x["anon"] as? Bool ?? false,
                       trackId: (x["trackId"] as? NSNumber)?.int64Value ?? 0, carId: (x["carId"] as? NSNumber)?.int64Value ?? 0, pos: int(x["pos"]), of: int(x["of"]))
        }
        return DriverProfile(name: (j["name"] as? String) ?? "Driver", since: num(j["since"]) ?? 0, mine: j["mine"] as? Bool ?? false, admin: j["admin"] as? Bool ?? false, anonymous: j["anonymous"] as? Bool ?? false,
                             supporter: j["supporter"] as? Bool ?? false, supporterHidden: j["supporterHidden"] as? Bool ?? false, lics: j["lics"] as? [String: String] ?? [:], races: races, laps: laps,
                             days: (j["days"] as? [String: Any] ?? [:]).mapValues { int($0) })
    }

    /// Leagues (in development: admins only for now): post one with a Discord invite, edit or remove yours.
    func leagues() async throws -> [League] {
        let j = (try JSONSerialization.jsonObject(with: await call("GET", "/community/leagues")) as? [String: Any]) ?? [:]
        return (j["leagues"] as? [[String: Any]] ?? []).map { x in
            League(id: str(x["id"]), name: fixTxt(str(x["name"])), about: fixTxt(str(x["about"])), cat: x["cat"] as? String, discord: str(x["discord"]), web: str(x["web"]), schedule: str(x["schedule"]),
                   cars: fixTxt(str(x["cars"])), lang: str(x["lang"]), mine: x["mine"] as? Bool ?? false, by: str(x["by"]))
        }
    }
    func saveLeague(_ id: String?, _ l: League) async throws {
        let path = "/community/leagues" + (id.map { "/" + ($0.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? $0) } ?? "")
        _ = try await call("POST", path, body: ["name": l.name, "about": l.about, "cat": l.cat ?? NSNull(), "discord": l.discord, "web": l.web, "schedule": l.schedule, "cars": l.cars, "lang": l.lang])
    }
    func deleteLeague(_ id: String) async throws {
        _ = try await call("POST", "/community/leagues/" + (id.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? id), body: ["delete": true])
    }

    /// Your profile's recent races: a summary of your own result in each (the race history itself stays encrypted).
    func publishProfileRaces() async {
        guard !demo, signedIn, let d = saved("races"), let a = try? JSONSerialization.jsonObject(with: d) as? [[String: Any]] else { return }
        let cats = ["oval": "oval", "dirtoval": "dirt_oval", "dirtroad": "dirt_road", "formulacar": "formula_car", "sportscar": "sports_car"]
        let list: [[String: Any]] = a.filter { int($0["finish"]) > 0 && (num($0["when"]) ?? 0) > 0 && !($0["partial"] as? Bool ?? false) }
            .sorted { (num($0["when"]) ?? 0) > (num($1["when"]) ?? 0) }.prefix(20).map { r -> [String: Any] in
                let me = (r["results"] as? [[String: Any]] ?? []).first { $0["me"] as? Bool ?? false }
                let rawCat = str(r["cat"]).lowercased().filter { $0.isLetter }
                let id0 = String(str(r["id"]).filter { ($0.isASCII && ($0.isLetter || $0.isNumber)) || "_.:-".contains($0) }.prefix(80))
                return ["id": id0.isEmpty ? "r\(Int(num(r["when"]) ?? 0))" : id0, "when": num(r["when"]) ?? 0, "game": str(r["game"]).isEmpty ? "iracing" : str(r["game"]),
                        "track": str(r["track"]), "car": str(r["car"]), "cat": orNull(cats[rawCat]), "lic": orNull(me?["lic"] as? String),
                        "official": r["official"] as? Bool ?? false, "start": int(r["start"]), "finish": int(r["finish"]), "field": int(r["field"]), "inc": int(r["inc"]),
                        "best": orNull(pos(r["best"])), "laps": (r["laps"] as? [Any])?.count ?? 0, "ir": int(r["ir"]), "irChange": int(r["irChange"]), "sof": int(r["sof"]), "dnf": r["dnf"] as? Bool ?? false]
            }
        guard let body = try? JSONSerialization.data(withJSONObject: list, options: [.sortedKeys]) else { return }
        let last = UserDefaults.standard.string(forKey: "profRaces") ?? ""
        let sig = String(body.count) + ":" + String(body.reduce(UInt32(2166136261)) { ($0 ^ UInt32($1)) &* 16777619 })
        if last == sig { return }
        if (try? await call("POST", "/community/profile/races", body: ["races": list])) != nil { UserDefaults.standard.set(sig, forKey: "profRaces") }
    }
    func adminStatus() async throws -> [String: Any] {
        (try JSONSerialization.jsonObject(with: await call("GET", "/community/admin/status")) as? [String: Any]) ?? [:]
    }
    func adminDelete(_ kind: String, _ id: String) async throws { _ = try await call("DELETE", "/community/admin/\(kind)/" + idPath(id)) }
    // a DRINKS driver name: taken by someone else on the platform? (your own names never clash)
    func nameFree(_ name: String) async -> Bool {
        guard let d = try? await call("POST", "/community/name-check", body: ["name": name]),
              let j = try? JSONSerialization.jsonObject(with: d) as? [String: Any] else { return true }
        return j["free"] as? Bool ?? true
    }

    func communityTrace(_ id: String) async throws -> Trace? {
        if demo { return Demo.trace(id) }
        return parseTrace((try await cachedGet("/community/laps/" + idPath(id)).data as? [String: Any])?["trace"])
    }

    func reports() async throws -> Got<[SharedReport]> {
        if demo { return Got(data: Demo.reports()) }
        let g = try await cachedGet("/community/reports?game=iracing")
        let a = (g.data as? [String: Any])?["reports"] as? [[String: Any]] ?? []
        return Got(data: a.map { r in
            SharedReport(id: str(r["id"]), alias: str(r["alias"]), track: str(r["track"]), car: str(r["car"]), created: num(r["created"]) ?? 0, finish: int(r["finish"]), field: int(r["field"]), best: pos(r["best"]), mine: (r["mine"] as? Bool) ?? false)
        }, stale: g.stale)
    }

    func setups() async throws -> Got<[SharedSetup]> {
        if demo { return Got(data: Demo.setups()) }
        let g = try await cachedGet("/community/setups?game=iracing")
        let a = (g.data as? [String: Any])?["setups"] as? [[String: Any]] ?? []
        return Got(data: a.map { s in
            SharedSetup(id: str(s["id"]), alias: str(s["alias"]), name: str(s["name"]), car: str(s["car"]), track: str(s["track"]), notes: str(s["notes"]), downloads: int(s["downloads"]), created: num(s["created"]) ?? 0)
        }, stale: g.stale)
    }
}
