import Foundation

// ---------- the data the server and the account sync give ----------

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

struct CloudLap: Identifiable, Hashable {
    let id: String
    let n: Int
    let time: Double
    let valid: Bool
    let sectors: [Double]
    var inc: Int = 0 // incident points on the lap (they do not make it invalid)
}

/// One row every `bin` metres: speed m/s, throttle 0-1, brake 0-1, gear, steering rad, lap time s.
struct Trace {
    let bin: Double
    let rows: [[Double]]
    /// The shape of the track: where the car was at every row (Pitlane HQ 0.5 and later record it).
    var x: [Double]? = nil
    var y: [Double]? = nil
    /// The incidents of the lap as [d, pts, d, pts…] (lap distance in m, incident points).
    var inc: [Double] = []
    var hasShape: Bool { x != nil && y != nil && x!.count == rows.count && x!.count > 10 }
    /// What each incident was: "off", "loss", "light" (light contact) or "contact" (older laps: by the points).
    var incK: [String] = []
    var incidents: [(d: Double, pts: Int, kind: String)] {
        (0..<(inc.count / 2)).map { i in
            let pts = Int(inc[2 * i + 1])
            let k: String = i < incK.count ? incK[i] : (pts >= 4 ? "contact" : pts == 2 ? "loss" : "off")
            return (d: inc[2 * i], pts: pts, kind: k)
        }
    }
}

struct PersonalBest: Identifiable, Hashable {
    var id: String { track + "|" + trackConfig + "|" + car }
    let track: String
    let trackConfig: String
    let car: String
    let best: Double
    let laps: Int
    let last: Double
    let bestLapId: String?
    let bestSessionId: String?
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

struct CommunityLap: Identifiable, Hashable {
    let id: String
    let alias: String
    let time: Double
    let created: Double
    let hasTrace: Bool
    let sectors: [Double]
    /// the signed-in driver's own lap, also when it was shared anonymously (only they see that)
    var mine: Bool = false
}

struct SharedReport: Identifiable, Hashable {
    let id: String
    let alias: String
    let track: String
    let car: String
    let created: Double
    let finish: Int
    let field: Int
    let best: Double?
    var mine: Bool = false
}

struct SharedSetup: Identifiable, Hashable {
    let id: String
    let alias: String
    let name: String
    let car: String
    let track: String
    let notes: String
    let downloads: Int
    let created: Double
}

struct Device: Identifiable, Hashable {
    let id: String
    let device: String
    let lastSeen: Double
    let current: Bool
}

/// DRINKS mode on the PC (admins only): friends drive and their laps go to the community under their name.
struct Drinks: Equatable {
    let admin: Bool
    let on: Bool
    let guest: String
    let guestAuto: Bool
    let guests: [String]
    let driver: String
}

/// A race the PC recorded (races.json in the account sync).
struct Race: Identifiable, Hashable {
    let id: String
    let when: Double
    let track: String
    let car: String
    let official: Bool
    let start: Int
    let finish: Int
    let field: Int
    let inc: Int
    let best: Double?
    let fieldBest: Double?
    let avg: Double?
    let consistency: Double?
    let pits: Int
    let fuelUsed: Double?
    let ir: Int
    let irChange: Int
    let sof: Int
    let dnf: Bool
    let laps: [RaceLap]
    let results: [RaceResult]
}

struct RaceLap: Hashable {
    let n: Int
    let time: Double
    let pos: Int
    let inc: Int
    let pit: Bool
    var cut: Bool = false // the car left the track: not a valid lap
}

struct RaceResult: Hashable {
    let pos: Int
    let name: String
    let ir: Int
    let best: Double?
    let inc: Int
    let laps: Int
}

func lapTime(_ s: Double?) -> String {
    guard let s, s.isFinite, s > 0 else { return "—" }
    let m = Int(s / 60)
    let r = s - Double(m) * 60
    return m > 0 ? String(format: "%d:%06.3f", m, r) : String(format: "%.3f", r)
}

func signed(_ n: Int) -> String { n > 0 ? "+\(n)" : "\(n)" }

func day(_ ms: Double) -> String {
    ms > 0 ? Date(timeIntervalSince1970: ms / 1000).formatted(date: .abbreviated, time: .omitted) : ""
}

func dayTime(_ ms: Double) -> String {
    ms > 0 ? Date(timeIntervalSince1970: ms / 1000).formatted(date: .abbreviated, time: .shortened) : ""
}

// ---------- comparing two laps (the same as Android's LapMath.kt) ----------

/// Two lap traces on the same distance grid (every `step` metres), ready to draw and compare.
struct Compared {
    let step: Double
    let speedA: [Double]
    let speedB: [Double]?
    let thrA: [Double]
    let brkA: [Double]
    let delta: [Double]?
    let tA: [Double]
    let tB: [Double]?
    var thrB: [Double]? = nil
    var brkB: [Double]? = nil
    var gearA: [Double]? = nil
    var gearB: [Double]? = nil
}

/// The gear at a distance: the nearest row, not a blend of two gears.
private func gearAt(_ tr: Trace, _ d: Double) -> Double {
    if tr.rows.isEmpty || tr.bin <= 0 { return 0 }
    let i = max(0, min(tr.rows.count - 1, Int((d / tr.bin).rounded())))
    return tr.rows[i].count > 3 ? tr.rows[i][3] : 0
}

/// One place on the lap where time goes, with what is different there.
struct Loss: Hashable {
    let fromM: Int
    let lost: Double
    let brakeDiffM: Int?
    let minA: Double
    let minB: Double
    let throttleDiffM: Int?
}

private func at(_ tr: Trace, _ d: Double, _ k: Int) -> Double {
    if tr.rows.isEmpty || tr.bin <= 0 { return 0 }
    let x = d / tr.bin
    let i = max(0, min(tr.rows.count - 1, Int(x)))
    let j = min(i + 1, tr.rows.count - 1)
    let f = max(0, min(1, x - Double(i)))
    let a = k < tr.rows[i].count ? tr.rows[i][k] : 0
    let b = k < tr.rows[j].count ? tr.rows[j][k] : 0
    let v = a * (1 - f) + b * f
    return v.isFinite ? v : 0
}

func compare(_ a: Trace, _ b: Trace?, step step0: Double = 10) -> Compared {
    let len = Double(a.rows.count - 1) * a.bin
    let lenB = b.map { Double($0.rows.count - 1) * $0.bin } ?? len
    // at most 400 points per line: enough for a phone screen, light to draw and to touch
    let step = max(step0, min(len, lenB) / 400)
    let n = max(2, Int(min(len, lenB) / step))
    let d = (0..<n).map { Double($0) * step }
    let tA = d.map { at(a, $0, 5) }
    let tB = b.map { tr in d.map { at(tr, $0, 5) } }
    return Compared(
        step: step,
        speedA: d.map { at(a, $0, 0) * 3.6 },
        speedB: b.map { tr in d.map { at(tr, $0, 0) * 3.6 } },
        thrA: d.map { at(a, $0, 1) },
        brkA: d.map { at(a, $0, 2) },
        delta: tB.map { tb in tA.indices.map { tA[$0] - tb[$0] } },
        tA: tA, tB: tB,
        thrB: b.map { tr in d.map { at(tr, $0, 1) } },
        brkB: b.map { tr in d.map { at(tr, $0, 2) } },
        gearA: d.map { gearAt(a, $0) },
        gearB: b.map { tr in d.map { gearAt(tr, $0) } }
    )
}

/// One corner in four phases, as driver coaches read data: braking (brake point, how hard), entry
/// (releasing the brake into the turn, trail braking, coasting), apex (minimum speed) and exit (when
/// the throttle comes back). `tip` is an I18n key with `args`, about the phase that loses the most.
/// The same analysis as the web's braking coach and Android's LapMath.kt.
struct Corner: Hashable {
    let n: Int
    let atM: Int
    let lost: Double
    let phases: [String: Double]
    let phase: String?
    let coastA: Double
    let coastB: Double
    let tip: String?
    let args: [String]
}

let phaseKeys = ["brake", "entry", "apex", "exit"]

func corners(_ c: Compared) -> [Corner] {
    guard let tB = c.tB, let sB = c.speedB, let bB = c.brkB, let hB = c.thrB else { return [] }
    let n = [c.tA.count, tB.count, sB.count, bB.count, hB.count, c.brkA.count, c.thrA.count, c.speedA.count].min() ?? 0
    if n < 10 { return [] }
    func bins(_ m: Double) -> Int { max(1, Int(m / c.step)) }
    func seg(_ x: Int, _ y: Int) -> Double { x < y ? (c.tA[y] - c.tA[x]) - (tB[y] - tB[x]) : 0 }
    // braking zones on the reference: from the brake to the slowest point before the throttle is back
    var zones: [(Int, Int)] = []
    var i = 1
    while i < n {
        if bB[i] > 0.12 && bB[i - 1] <= 0.12 {
            var k = i
            var imin = i
            while k < n && k < i + bins(600) && !(hB[k] > 0.6 && bB[k] < 0.05) {
                if sB[k] < sB[imin] { imin = k }
                k += 1
            }
            if sB[i] - sB[imin] > 15 { zones.append((i, imin)) }
            i = max(k, i + 1)
        } else {
            i += 1
        }
    }
    var out: [Corner] = []
    for (idx, z) in zones.enumerated() {
        let zi = z.0, zmin = z.1
        let i0 = max(0, zi - bins(50)), i1 = min(n - 1, zmin + bins(150))
        let lost = seg(i0, i1)
        // the same corner on this lap: its brake point within 100 m
        let w = bins(100)
        var ja: Int? = nil
        for j in max(1, zi - w)...min(n - 1, zi + w) where c.brkA[j] > 0.12 && c.brkA[j - 1] <= 0.12 {
            if ja == nil || abs(j - zi) < abs(ja! - zi) { ja = j }
        }
        var aMin: Int? = nil
        if let j = ja { aMin = (j...i1).min { c.speedA[$0] < c.speedA[$1] } }
        var ipb = zi
        for k in zi...zmin where bB[k] > bB[ipb] { ipb = k }
        let rel = (ipb...zmin).first { bB[$0] < 0.05 } ?? zmin
        let a0 = max(rel, zmin - bins(20)), a1 = min(i1, zmin + bins(20))
        let ph: [String: Double] = ["brake": seg(i0, rel), "entry": seg(rel, a0), "apex": seg(a0, a1), "exit": seg(a1, i1)]
        var coastA = 0.0, coastB = 0.0
        for k in i0...i1 {
            if c.thrA[k] < 0.05 && c.brkA[k] < 0.05 { coastA += c.step }
            if hB[k] < 0.05 && bB[k] < 0.05 { coastB += c.step }
        }
        var tip: String? = nil
        var args: [String] = []
        var phase: String? = nil
        if lost > 0.03 {
            phase = ph.max { $0.value < $1.value }?.key
            let dd: Int? = ja.map { Int(Double($0 - zi) * c.step) }
            var ipa = ja ?? 0
            if let j = ja, let m = aMin { for k in j...max(j, m) where c.brkA[k] > c.brkA[ipa] { ipa = k } }
            let pa = ja != nil ? c.brkA[ipa] : 0
            var trailA: Int? = nil
            if let m = aMin, ja != nil { trailA = Int(Double(((ipa...max(ipa, m)).first { c.brkA[$0] < 0.05 } ?? m) - ipa) * c.step) }
            let trailB = Int(Double(rel - ipb) * c.step)
            let dmin: Double? = aMin.map { c.speedA[$0] - sB[zmin] }
            var late: Int? = nil
            if let m = aMin, let pA = (m...max(m, i1)).first(where: { c.thrA[$0] > 0.5 }), let pB = (zmin...i1).first(where: { hB[$0] > 0.5 }) { late = Int(Double(pA - pB) * c.step) }
            switch phase ?? "" {
            case "brake":
                if let d = dd, d < -6 { tip = "tip_brake_later"; args = ["\(-d)"] }
                else if ja != nil && pa < bB[ipb] - 0.1 { tip = "tip_brake_harder"; args = ["\(Int(pa * 100))", "\(Int(bB[ipb] * 100))"] }
                else if let d = dd, d > 6 { tip = "tip_brake_earlier"; args = ["\(d)"] }
                else { tip = "tip_brake_generic" }
            case "entry":
                if let ta = trailA, trailB - ta >= 15 { tip = "tip_trail"; args = ["\(ta)", "\(trailB)"] }
                else if coastA - coastB >= 10 { tip = "tip_coast"; args = ["\(Int(coastA))", "\(Int(coastB))"] }
                else { tip = "tip_entry_speed" }
            case "apex":
                if let dm = dmin, dm < -2 { tip = "tip_apex_speed"; args = ["\(Int(-dm))"] } else { tip = "tip_apex_line" }
            default:
                if let l = late, l >= 8 { tip = "tip_throttle"; args = ["\(l)"] }
                else if coastA - coastB >= 10 { tip = "tip_no_wait" }
                else { tip = "tip_full_throttle" }
            }
        }
        out.append(Corner(n: idx + 1, atM: Int(Double(zi) * c.step), lost: lost, phases: ph, phase: phase, coastA: coastA, coastB: coastB, tip: tip, args: args))
    }
    return out
}

/// The three stretches of about 250 m where the lap loses the most time against the reference.
/// Where a lap starts braking (brake over 12 % after being below), in metres, from a series on the `step` grid.
func brakePoints(_ brk: [Double]?, _ step: Double) -> [Double] {
    guard let brk, brk.count >= 3 else { return [] }
    var out: [Double] = []
    var i = 1
    while i < brk.count {
        if brk[i] > 0.12 && brk[i - 1] <= 0.12 {
            out.append(Double(i) * step)
            var k = i
            while k < brk.count && k < i + 60 && brk[k] > 0.05 { k += 1 }
            i = k + 1
        } else { i += 1 }
    }
    return out
}

func losses(_ c: Compared, segM: Double = 250) -> [Loss] {
    guard let tB = c.tB, let speedB = c.speedB, c.tA.count > 1 else { return [] }
    let per = max(1, Int(segM / c.step))
    var out: [Loss] = []
    var s = 0
    while s < c.tA.count - 1 {
        let e = min(c.tA.count - 1, s + per)
        let lost = (c.tA[e] - c.tA[s]) - (tB[e] - tB[s])
        let r = s...e
        let minA = r.map { c.speedA[$0] }.min() ?? 0
        let minB = r.map { speedB[$0] }.min() ?? 0
        // braking: the reference has no brake channel here, so use where its speed starts to drop
        let bA = r.first { c.brkA[$0] > 0.3 }
        let bB = (s..<e).first { $0 + 2 <= e && speedB[$0 + 2] < speedB[$0] - 3 }
        let brakeDiff = (bA != nil && bB != nil) ? Int(Double(bA! - bB!) * c.step) : nil
        let fullA = r.first { c.thrA[$0] > 0.95 && c.speedA[$0] > minA + 2 }
        let minIdxB = r.min { speedB[$0] < speedB[$1] } ?? s
        let fullB = r.first { speedB[$0] > minB + 2 && $0 > minIdxB }
        let thrDiff = (fullA != nil && fullB != nil) ? Int(Double(fullA! - fullB!) * c.step) : nil
        out.append(Loss(fromM: Int(Double(s) * c.step), lost: lost, brakeDiffM: brakeDiff, minA: minA, minB: minB, throttleDiffM: thrDiff))
        s = e
    }
    return Array(out.filter { $0.lost > 0.02 }.sorted { $0.lost > $1.lost }.prefix(3))
}
