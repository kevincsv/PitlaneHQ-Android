import Foundation
import Security

let server = URL(string: "https://pitlanehq.app")!
let webApp = URL(string: "https://pitlanehq.app/app/?companion=1")!

struct APIError: LocalizedError {
    let message: String
    var signedOut = false
    var errorDescription: String? { message }
    static let signedOutError = APIError(message: "Signed out: sign in again", signedOut: true)
}

// ---------- the data the server sends ----------

struct CloudSession: Identifiable, Hashable {
    let id: String
    let started: Double
    let track: String
    let trackConfig: String
    let car: String
    let kind: String
    let laps: Int
    let best: Double?
}

struct CloudLap: Identifiable {
    let id = UUID()
    let n: Int
    let time: Double
    let valid: Bool
    let sectors: [Double]
}

struct Combo: Identifiable, Hashable {
    var id: String { "\(trackId)-\(carId)" }
    let trackId: Int64
    let track: String
    let carId: Int64
    let car: String
    let laps: Int
    let best: Double?
}

struct CommunityLap: Identifiable {
    let id = UUID()
    let alias: String
    let time: Double
}

func lapTime(_ s: Double?) -> String {
    guard let s, s.isFinite, s > 0 else { return "—" }
    let m = Int(s / 60)
    let r = s - Double(m) * 60
    return m > 0 ? String(format: "%d:%06.3f", m, r) : String(format: "%.3f", r)
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

/// The Pitlane HQ account, exactly like the PC, the web app and Android: the password only
/// derives the keys on this phone; the session token and the data key stay in the Keychain.
@MainActor
final class Account: ObservableObject {
    @Published var signedIn = false
    @Published var email = ""
    @Published var display = ""
    @Published var busy = false
    @Published var error: String?
    @Published var syncedFiles = 0
    @Published var syncVersion = 0
    @Published var syncUpdated: Double = 0

    private let defaults = UserDefaults.standard

    init() {
        // a reinstall keeps the Keychain but not the settings: start signed out then
        if defaults.string(forKey: "email") == nil { Vault.set("token", nil); Vault.set("dataKey", nil) }
        signedIn = Vault.get("token") != nil && Vault.get("dataKey") != nil
        email = defaults.string(forKey: "email") ?? ""
        display = defaults.string(forKey: "display") ?? ""
        syncedFiles = defaults.integer(forKey: "syncedFiles")
        syncVersion = defaults.integer(forKey: "syncVersion")
        syncUpdated = defaults.double(forKey: "syncUpdated")
    }

    var token: String? { Vault.get("token").flatMap { String(data: $0, encoding: .utf8) } }
    var dataKey: Data? { Vault.get("dataKey") }

    private func clear() {
        Vault.set("token", nil)
        Vault.set("dataKey", nil)
        for k in ["email", "display", "syncedFiles", "syncVersion", "syncUpdated"] { defaults.removeObject(forKey: k) }
        signedIn = false
        display = ""
        syncedFiles = 0
        syncVersion = 0
        syncUpdated = 0
    }

    func call(_ method: String, _ path: String, body: [String: Any]? = nil, auth: Bool = true) async throws -> Data {
        guard let url = URL(string: server.absoluteString + path) else { throw APIError(message: "Bad address") }
        var r = URLRequest(url: url, timeoutInterval: 30)
        r.httpMethod = method
        if let body {
            r.httpBody = try JSONSerialization.data(withJSONObject: body)
            r.setValue("application/json", forHTTPHeaderField: "content-type")
        }
        if auth {
            guard let t = token else { throw APIError.signedOutError }
            r.setValue("Bearer " + t, forHTTPHeaderField: "authorization")
        }
        let (data, resp) = try await URLSession.shared.data(for: r)
        let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
        if code == 401 && auth {
            clear()
            error = APIError.signedOutError.message
            throw APIError.signedOutError
        }
        guard (200..<300).contains(code) else {
            let msg = (try? JSONSerialization.jsonObject(with: data) as? [String: Any])?["error"] as? String
            throw APIError(message: msg ?? "Server error \(code)")
        }
        return data
    }

    func login(email e: String, password: String) async {
        let em = e.trimmingCharacters(in: .whitespaces).lowercased()
        guard em.range(of: #"^[^@\s]+@[^@\s]+\.[^@\s]+$"#, options: .regularExpression) != nil else { error = "Enter a valid email"; return }
        guard !password.isEmpty else { error = "Enter your password"; return }
        busy = true
        error = nil
        defer { busy = false }
        do {
            let keys = try await Task.detached { try PLCrypto.derive(email: em, password: password) }.value
            let data = try await call("POST", "/account/login", body: ["email": em, "auth": keys.auth, "device": "iPhone"], auth: false)
            guard let j = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let token = j["token"] as? String, let wrapped = j["wrappedKey"] as? String else { throw APIError(message: "Unexpected answer from the server") }
            let key = try PLCrypto.open(key: keys.wrap, sealed: wrapped, aad: PLCrypto.accountAAD)
            guard key.count == 32 else { throw APIError(message: "The account key is damaged") }
            Vault.set("token", Data(token.utf8))
            Vault.set("dataKey", key)
            email = em
            display = j["display"] as? String ?? ""
            defaults.set(em, forKey: "email")
            defaults.set(display, forKey: "display")
            signedIn = true
            await sync()
        } catch {
            self.error = error.localizedDescription
        }
    }

    /// Pulls the encrypted settings bundle the PC keeps in the account and opens it here.
    func sync() async {
        busy = true
        error = nil
        defer { busy = false }
        do {
            let data = try await call("GET", "/account/sync")
            let j = try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
            var files = 0
            if let blob = j["blob"] as? String, !blob.isEmpty, let key = dataKey {
                let raw = try PLCrypto.gunzip(PLCrypto.open(key: key, sealed: blob, aad: PLCrypto.accountAAD))
                files = (try JSONSerialization.jsonObject(with: raw) as? [String: Any])?.count ?? 0
            }
            syncedFiles = files
            syncVersion = (j["version"] as? NSNumber)?.intValue ?? 0
            syncUpdated = (j["updated"] as? NSNumber)?.doubleValue ?? 0
            defaults.set(syncedFiles, forKey: "syncedFiles")
            defaults.set(syncVersion, forKey: "syncVersion")
            defaults.set(syncUpdated, forKey: "syncUpdated")
            if let me = try? JSONSerialization.jsonObject(with: await call("GET", "/account/me")) as? [String: Any], let d = me["display"] as? String {
                display = d
                defaults.set(d, forKey: "display")
            }
        } catch {
            self.error = error.localizedDescription
        }
    }

    func logout() async {
        _ = try? await call("POST", "/account/logout")
        clear()
        error = nil
    }

    // ---------- your laps (uploaded by PitlaneHQ.exe) ----------

    func sessions() async throws -> [CloudSession] {
        let a = try JSONSerialization.jsonObject(with: await call("GET", "/api/sessions?limit=100")) as? [[String: Any]] ?? []
        return a.compactMap { s in
            guard let id = s["id"] as? String else { return nil }
            return CloudSession(
                id: id, started: (s["started"] as? NSNumber)?.doubleValue ?? 0, track: s["track"] as? String ?? "",
                trackConfig: s["track_config"] as? String ?? "", car: s["car"] as? String ?? "", kind: s["kind"] as? String ?? "",
                laps: (s["laps"] as? NSNumber)?.intValue ?? 0, best: (s["best"] as? NSNumber)?.doubleValue
            )
        }
    }

    // session ids look like acct_<id>:<…>; the ':' stays as it is in the path
    func laps(_ sessionId: String) async throws -> [CloudLap] {
        var allowed = CharacterSet.alphanumerics
        allowed.insert(charactersIn: "-._~:")
        let id = sessionId.addingPercentEncoding(withAllowedCharacters: allowed) ?? sessionId
        let j = try JSONSerialization.jsonObject(with: await call("GET", "/api/sessions/" + id)) as? [String: Any] ?? [:]
        return (j["laps"] as? [[String: Any]] ?? []).map { l in
            CloudLap(
                n: (l["n"] as? NSNumber)?.intValue ?? 0, time: (l["time"] as? NSNumber)?.doubleValue ?? 0,
                valid: ((l["valid"] as? NSNumber)?.intValue ?? 1) == 1,
                sectors: (l["sectors"] as? [NSNumber] ?? []).map(\.doubleValue)
            )
        }
    }

    // ---------- community ----------

    func combos() async throws -> [Combo] {
        let j = try JSONSerialization.jsonObject(with: await call("GET", "/community/combos?game=iracing")) as? [String: Any] ?? [:]
        return (j["combos"] as? [[String: Any]] ?? []).map { c in
            Combo(
                trackId: (c["trackId"] as? NSNumber)?.int64Value ?? 0, track: c["track"] as? String ?? "",
                carId: (c["carId"] as? NSNumber)?.int64Value ?? 0, car: c["car"] as? String ?? "",
                laps: (c["laps"] as? NSNumber)?.intValue ?? 0, best: (c["best"] as? NSNumber)?.doubleValue
            )
        }
    }

    /// One line per driver: their best lap.
    func leaderboard(_ c: Combo) async throws -> [CommunityLap] {
        let j = try JSONSerialization.jsonObject(with: await call("GET", "/community/laps?game=iracing&trackId=\(c.trackId)&carId=\(c.carId)")) as? [String: Any] ?? [:]
        var seen = Set<String>()
        var out: [CommunityLap] = []
        for l in j["laps"] as? [[String: Any]] ?? [] {
            let alias = l["alias"] as? String ?? "Driver"
            guard let t = (l["time"] as? NSNumber)?.doubleValue else { continue }
            if alias != "Anonymous" {
                if seen.contains(alias) { continue }
                seen.insert(alias)
            }
            out.append(CommunityLap(alias: alias, time: t))
        }
        return out.sorted { $0.time < $1.time }
    }
}
