import CommonCrypto
import CryptoKit
import Foundation

/// The same keys and sealing as the PC (placcount.go, liverelay.go), the web app and Android:
/// PBKDF2-SHA256 600 000 rounds -> HKDF ("pitlanehq auth" / "pitlanehq wrap") -> AES-256-GCM
/// with the 12-byte nonce in front and an associated-data label.
enum PLCrypto {
    static let accountAAD = Data("pitlanehq-v1".utf8)
    static let liveAAD = Data("pitlanehq-live-v1".utf8)

    struct Keys {
        let auth: String
        let wrap: Data
    }

    enum Failure: LocalizedError {
        case derive, damaged
        var errorDescription: String? { self == .derive ? "Could not derive the account key" : "Damaged data" }
    }

    /// A live share code ("ABCD-EFGH-JK"): its room on the server (one hash) and its key (PBKDF2), like the PC.
    static func codeNorm(_ v: String) -> String { String(v.uppercased().filter { ($0 >= "A" && $0 <= "Z") || ($0 >= "0" && $0 <= "9") }) }
    static func codeOk(_ c: String) -> Bool { c.count == 10 && c.allSatisfy { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".contains($0) } }
    static func codeRoom(_ c: String) -> String { Data(SHA256.hash(data: Data(("pitlanehq-share-room|" + c).utf8))).prefix(16).map { String(format: "%02x", $0) }.joined() }
    static func codeKey(_ c: String) -> Data? {
        let pw = Array(c.utf8).map { Int8(bitPattern: $0) }, salt = Array("pitlanehq-share-key-v1".utf8)
        var out = [UInt8](repeating: 0, count: 32)
        let rc = CCKeyDerivationPBKDF(CCPBKDFAlgorithm(kCCPBKDF2), pw, pw.count, salt, salt.count, CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256), 100_000, &out, out.count)
        return rc == Int32(kCCSuccess) ? Data(out) : nil
    }

    static func derive(email: String, password: String) throws -> Keys {
        let salt = Data(SHA256.hash(data: Data(("pitlanehq-account-v1:" + email.trimmingCharacters(in: .whitespaces).lowercased()).utf8)))
        let pw = Array(password.utf8).map { Int8(bitPattern: $0) }
        var master = [UInt8](repeating: 0, count: 32)
        let rc = salt.withUnsafeBytes { s in
            CCKeyDerivationPBKDF(
                CCPBKDFAlgorithm(kCCPBKDF2), pw, pw.count,
                s.bindMemory(to: UInt8.self).baseAddress, salt.count,
                CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256), 600_000, &master, master.count
            )
        }
        guard rc == Int32(kCCSuccess) else { throw Failure.derive }
        let ikm = SymmetricKey(data: Data(master))
        func hkdf(_ info: String) -> Data {
            HKDF<SHA256>.deriveKey(inputKeyMaterial: ikm, salt: Data(), info: Data(info.utf8), outputByteCount: 32)
                .withUnsafeBytes { Data($0) }
        }
        return Keys(auth: hkdf("pitlanehq auth").map { String(format: "%02x", $0) }.joined(), wrap: hkdf("pitlanehq wrap"))
    }

    static func open(key: Data, sealed: String, aad: Data) throws -> Data {
        guard let b = Data(base64Encoded: sealed, options: .ignoreUnknownCharacters), b.count > 28 else { throw Failure.damaged }
        return try AES.GCM.open(AES.GCM.SealedBox(combined: b), using: SymmetricKey(data: key), authenticating: aad)
    }

    static func seal(key: Data, plain: Data, aad: Data) throws -> String {
        let box = try AES.GCM.seal(plain, using: SymmetricKey(data: key), authenticating: aad)
        guard let c = box.combined else { throw Failure.damaged }
        return c.base64EncodedString()
    }

    // ---------- gzip (what Go's compress/gzip reads and writes) ----------

    static func gunzip(_ d: Data) throws -> Data {
        let b = [UInt8](d)
        guard b.count >= 18, b[0] == 0x1f, b[1] == 0x8b, b[2] == 8 else { throw Failure.damaged }
        let flags = b[3]
        var i = 10
        if flags & 4 != 0 { guard i + 2 <= b.count else { throw Failure.damaged }; i += 2 + Int(b[i]) + Int(b[i + 1]) << 8 }
        if flags & 8 != 0 { while i < b.count && b[i] != 0 { i += 1 }; i += 1 }
        if flags & 16 != 0 { while i < b.count && b[i] != 0 { i += 1 }; i += 1 }
        if flags & 2 != 0 { i += 2 }
        guard i < b.count - 8 else { throw Failure.damaged }
        // NSData's .zlib is raw DEFLATE, the body of a gzip member
        return try (Data(b[i..<(b.count - 8)]) as NSData).decompressed(using: .zlib) as Data
    }

    static func gzip(_ d: Data) throws -> Data {
        let body = try (d as NSData).compressed(using: .zlib) as Data
        var out = Data([0x1f, 0x8b, 8, 0, 0, 0, 0, 0, 0, 255])
        out.append(body)
        var crc = crc32(d).littleEndian
        var size = UInt32(truncatingIfNeeded: d.count).littleEndian
        withUnsafeBytes(of: &crc) { out.append(contentsOf: $0) }
        withUnsafeBytes(of: &size) { out.append(contentsOf: $0) }
        return out
    }

    private static let crcTable: [UInt32] = (0..<256).map { n -> UInt32 in
        var c = UInt32(n)
        for _ in 0..<8 { c = c & 1 != 0 ? 0xEDB8_8320 ^ (c >> 1) : c >> 1 }
        return c
    }

    static func crc32(_ d: Data) -> UInt32 {
        var c: UInt32 = 0xFFFF_FFFF
        for x in d { c = crcTable[Int((c ^ UInt32(x)) & 0xFF)] ^ (c >> 8) }
        return c ^ 0xFFFF_FFFF
    }
}
