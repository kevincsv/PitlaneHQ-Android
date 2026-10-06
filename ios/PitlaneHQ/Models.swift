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
}

/// One row every `bin` metres: speed m/s, throttle 0-1, brake 0-1, gear, steering rad, lap time s.
struct Trace {
    let bin: Double
    let rows: [[Double]]
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
    let x = d / tr.bin
    let i = max(0, min(tr.rows.count - 1, Int(x)))
    let j = min(i + 1, tr.rows.count - 1)
    let f = max(0, min(1, x - Double(i)))
    let a = k < tr.rows[i].count ? tr.rows[i][k] : 0
    let b = k < tr.rows[j].count ? tr.rows[j][k] : 0
    return a * (1 - f) + b * f
}

func compare(_ a: Trace, _ b: Trace?, step: Double = 10) -> Compared {
    let len = Double(a.rows.count - 1) * a.bin
    let lenB = b.map { Double($0.rows.count - 1) * $0.bin } ?? len
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
        tA: tA, tB: tB
    )
}

/// The three stretches of about 250 m where the lap loses the most time against the reference.
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
