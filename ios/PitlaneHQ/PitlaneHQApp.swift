import SwiftUI

@main
struct PitlaneHQApp: App {
    @StateObject private var account = Account()

    var body: some Scene {
        WindowGroup {
            Group {
                if account.signedIn { MainView() } else { LoginView() }
            }
            .environmentObject(account)
            .preferredColorScheme(.dark)
            .tint(Theme.accent)
        }
    }
}

enum Theme {
    static let ink = Color(red: 0x11 / 255, green: 0x15 / 255, blue: 0x1B / 255)
    static let surface = Color(red: 0x19 / 255, green: 0x20 / 255, blue: 0x2A / 255)
    static let surface2 = Color(red: 0x1E / 255, green: 0x26 / 255, blue: 0x31 / 255)
    static let line = Color(red: 0x2B / 255, green: 0x35 / 255, blue: 0x42 / 255)
    static let fg = Color(red: 0xE7 / 255, green: 0xEB / 255, blue: 0xF1 / 255)
    static let muted = Color(red: 0x8A / 255, green: 0x97 / 255, blue: 0xA9 / 255)
    static let accent = Color(red: 1, green: 0xB0 / 255, blue: 0x2E / 255)
    static let good = Color(red: 0x38 / 255, green: 0xC9 / 255, blue: 0x7C / 255)
    static let bad = Color(red: 1, green: 0x63 / 255, blue: 0x63 / 255)
    static let purple = Color(red: 0xB9 / 255, green: 0x8C / 255, blue: 1)
}
