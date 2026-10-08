import Foundation
import UIKit

/// The variables the phone asks the PC for.
let liveVars = [
    "Speed", "RPM", "Gear", "FuelLevel", "Lap", "LapCurrentLapTime", "LapLastLapTime", "LapBestLapTime",
    "LapDeltaToBestLap", "Throttle", "Brake", "PlayerCarPosition",
]

/// Live telemetry from your PC through your Pitlane HQ account's live room (/live), the same
/// link the web app and Android use: any network, no PC address. Everything is sealed with the
/// account's data key (AES-256-GCM): the server only passes it along. The PC only streams
/// while a screen of yours is watching, so this runs only while the Live screen is open.
@MainActor
final class Live: ObservableObject {
    enum Link { case off, connecting, open }
    /// idle: only whether your PC is online (it sends nothing); own: you watch your PC; code: you watch someone's share code
    enum Mode { case idle, own, code }

    @Published var link = Link.off
    @Published var pcOnline = false
    @Published var simConnected = false
    @Published var values: [String: Double] = [:]
    @Published var message: String?
    @Published var drinks: Drinks?
    @Published var mode: Mode = UserDefaults.standard.bool(forKey: "liveOwn") ? .own : .idle
    @Published var code = ""
    @Published var myCode = ""
    /// Connect asks your PC first: "" (not asked), "wait" (your PC shows Accept / Decline) or "ok"
    @Published var ask = ""
    @Published var askAt = Date.distantPast
    @Published var declinedAt = Date.distantPast
    private var codeKey: Data?
    /// this phone for your PC's question: an id that stays
    private let devId: String = {
        if let v = UserDefaults.standard.string(forKey: "devId"), v.count == 16 { return v }
        let v = String(UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased().prefix(16))
        UserDefaults.standard.set(v, forKey: "devId")
        return v
    }()
    private var account: (String?, Data?, Bool) = (nil, nil, false)

    private var task: URLSessionWebSocketTask?
    private var key: Data?
    private var loop: Task<Void, Never>?
    private var fields: [String] = []

    func num(_ k: String) -> Double? { values[k] }

    /// Connect to your PC (own), watch a code (code), or Disconnect (idle): the link opens again the new way.
    func watch(_ m: Mode, code c: String = "") {
        let running = loop != nil
        stop()
        mode = m
        code = m == .code ? c : ""
        codeKey = m == .code ? PLCrypto.codeKey(c) : nil
        myCode = ""
        ask = ""
        UserDefaults.standard.set(m == .own, forKey: "liveOwn")
        if running { start(token: account.0, key: account.1, demo: account.2) }
    }

    /// Your share code, made or stopped by your PC: on, new (a new code stops the old one).
    func share(on: Bool, new: Bool = false) {
        guard mode == .own, let ws = task, let key = account.1 else { return }
        guard let raw = try? JSONSerialization.data(withJSONObject: ["share", ["on": on, "new": new]] as [Any]),
              let z = try? PLCrypto.gzip(raw), let sealed = try? PLCrypto.seal(key: key, plain: z, aad: PLCrypto.liveAAD) else { return }
        ws.send(.string("e:" + sealed)) { _ in }
    }

    func start(token: String?, key: Data?, demo: Bool = false) {
        account = (token, key, demo)
        guard loop == nil else { return }
        if demo {
            // Settings → Demo data: an invented lap, nothing from the server
            loop = Task { [weak self] in
                var tick = 0
                while !Task.isCancelled {
                    guard let self else { return }
                    self.link = .open
                    self.pcOnline = true
                    self.simConnected = true
                    self.values = Demo.live(tick)
                    tick += 1
                    try? await Task.sleep(nanoseconds: 100_000_000)
                }
            }
            return
        }
        let useKey: Data? = mode == .code ? codeKey : key
        guard let useKey, token != nil || mode == .code else { return }
        loop = Task { [weak self] in
            var wait: UInt64 = 2
            while !Task.isCancelled {
                guard let self else { return }
                let opened = await self.run(token: token, key: useKey)
                if Task.isCancelled || self.message == "signed_out" { break }
                if opened { wait = 2 }
                try? await Task.sleep(nanoseconds: wait * 1_000_000_000)
                wait = min(wait * 2, 30)
            }
        }
    }

    func stop() {
        loop?.cancel()
        loop = nil
        task?.cancel(with: .normalClosure, reason: nil)
        task = nil
        link = .off
        pcOnline = false
        simConnected = false
        values = [:]
        drinks = nil
        message = nil
    }

    /// One connection, until it closes. Returns whether it got through.
    private func run(token: String?, key: Data) async -> Bool {
        let q = (mode == .idle ? "idle" : "view") + (mode == .code ? "&share=" + PLCrypto.codeRoom(code) : "")
        var r = URLRequest(url: URL(string: server.absoluteString.replacingOccurrences(of: "https://", with: "wss://") + "/live?role=" + q)!)
        if let token { r.setValue("Bearer " + token, forHTTPHeaderField: "authorization") }
        let ws = URLSession.shared.webSocketTask(with: r)
        self.key = key
        task = ws
        fields = []
        link = .connecting
        ws.resume()
        if mode == .own { hello(ws, key, fresh: true) } else { want(ws, key) }
        var opened = false
        // hello again every 20 s while you watch your PC, so it knows you still do
        let hi = Task { [weak self, weak ws] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 20_000_000_000)
                guard let self, let ws, self.task === ws, self.mode == .own else { continue }
                self.hello(ws, key, fresh: false)
            }
        }
        // keepalive: answered by the server without waking the room
        let ping = Task { [weak ws] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 30_000_000_000)
                try? await ws?.send(.string("ping"))
            }
        }
        // the PC sends the list of variables once: if it got lost, ask again
        let again = Task { [weak self, weak ws] in
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            guard let self, let ws, self.task === ws, self.pcOnline, self.fields.isEmpty else { return }
            self.want(ws, key)
        }
        defer {
            ping.cancel()
            again.cancel()
            hi.cancel()
        }
        while !Task.isCancelled {
            do {
                let m = try await ws.receive()
                if !opened {
                    opened = true
                    link = .open
                    message = nil
                }
                if case let .string(s) = m { handle(ws, key, s) }
            } catch {
                if task === ws {
                    let code = (ws.response as? HTTPURLResponse)?.statusCode
                    link = .off
                    pcOnline = false
                    message = code == 401 ? "signed_out" : code == 429 ? "too_many" : "conn_lost"
                }
                break
            }
        }
        return opened
    }

    /// DRINKS mode on the PC: sealed like everything else, the PC checks the account is an admin.
    func setDrinks(on: Bool, guest: String, guestAuto: Bool) {
        guard let ws = task, let key else { return }
        let payload: [String: Any] = ["on": on, "guest": guest, "guestAuto": guestAuto]
        guard let raw = try? JSONSerialization.data(withJSONObject: ["drinks", payload] as [Any]),
              let z = try? PLCrypto.gzip(raw), let sealed = try? PLCrypto.seal(key: key, plain: z, aad: PLCrypto.liveAAD) else { return }
        ws.send(.string("e:" + sealed)) { _ in }
    }

    /// Connect asks your PC: it shows Accept / Decline there and sends nothing before you accept.
    private func hello(_ ws: URLSessionWebSocketTask, _ key: Data, fresh: Bool) {
        guard mode == .own else { return }
        if fresh {
            ask = "wait"
            askAt = Date()
        }
        let name = "iOS · " + UIDeviceName.current
        guard let raw = try? JSONSerialization.data(withJSONObject: ["hello", ["id": devId, "name": name]] as [Any]),
              let z = try? PLCrypto.gzip(raw), let sealed = try? PLCrypto.seal(key: key, plain: z, aad: PLCrypto.liveAAD) else { return }
        ws.send(.string("e:" + sealed)) { _ in }
    }

    /// Your PC did not answer: ask it again.
    func askAgain() {
        guard let ws = task, let key else { return }
        hello(ws, key, fresh: true)
    }

    private func want(_ ws: URLSessionWebSocketTask, _ key: Data) {
        if mode == .own && ask != "ok" { return }
        guard let raw = try? JSONSerialization.data(withJSONObject: ["want", ["vars": liveVars, "all": false]] as [Any]),
              let z = try? PLCrypto.gzip(raw), let sealed = try? PLCrypto.seal(key: key, plain: z, aad: PLCrypto.liveAAD) else { return }
        ws.send(.string("e:" + sealed)) { _ in }
    }

    private func handle(_ ws: URLSessionWebSocketTask, _ key: Data, _ text: String) {
        if text == "pong" { return }
        if text.hasPrefix("{") {
            guard let o = try? JSONSerialization.jsonObject(with: Data(text.utf8)) as? [String: Any], o["ctl"] as? String == "pc" else { return }
            let on = o["on"] as? Bool ?? false
            pcOnline = on
            if mode == .idle { return }
            if on {
                if mode == .own { hello(ws, key, fresh: true) } else { want(ws, key) } // a PC that just started is asked again
            } else {
                simConnected = false
                values = [:]
                ask = ""
            }
            return
        }
        guard text.hasPrefix("e:"),
              let z = try? PLCrypto.open(key: key, sealed: String(text.dropFirst(2)), aad: PLCrypto.liveAAD),
              let raw = try? PLCrypto.gunzip(z),
              let a = try? JSONSerialization.jsonObject(with: raw) as? [Any], a.count == 2, let ev = a[0] as? String else { return }
        // your PC's answer, to this phone (your devices share the room)
        if ev == "ok" || ev == "no" {
            guard mode == .own, (a[1] as? [String: Any])?["id"] as? String == devId else { return }
            if ev == "no" {
                watch(.idle)
                declinedAt = Date()
            } else if ask != "ok" {
                ask = "ok"
                fields = []
                want(ws, key)
            }
            return
        }
        if mode == .own && ask != "ok" { return } // what your PC sends another device you accepted
        switch ev {
        case "share":
            myCode = (a[1] as? [String: Any])?["code"] as? String ?? ""
        case "status":
            pcOnline = true
            simConnected = (a[1] as? [String: Any])?["connected"] as? Bool ?? false
        case "drinks":
            if let d = a[1] as? [String: Any] {
                pcOnline = true
                drinks = Drinks(admin: d["admin"] as? Bool ?? false, on: d["on"] as? Bool ?? false, guest: d["guest"] as? String ?? "",
                                guestAuto: d["guestAuto"] as? Bool ?? false, guests: d["guests"] as? [String] ?? [], driver: d["driver"] as? String ?? "")
            }
        case "fields":
            fields = a[1] as? [String] ?? []
        case "t":
            guard let v = (a[1] as? [String: Any])?["v"] as? [Any] else { return }
            var m: [String: Double] = [:]
            for (i, name) in fields.enumerated() where i < v.count {
                if let n = v[i] as? NSNumber { m[name] = n.doubleValue }
            }
            pcOnline = true
            values = m
        default:
            break
        }
    }
}

/// The phone's model for your PC's question ("iPhone", "iPad").
enum UIDeviceName {
    @MainActor static var current: String { UIDevice.current.model }
}
