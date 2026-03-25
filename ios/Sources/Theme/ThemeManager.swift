import SwiftUI
import Combine

// MARK: - ThemeManager

final class ThemeManager: ObservableObject {
    static let shared = ThemeManager()

    @Published var currentTheme: AppThemeType {
        didSet { UserDefaults.standard.set(currentTheme.rawValue, forKey: "spotik_theme") }
    }

    var palette: SpotikPalette { SpotikPalette.palette(for: currentTheme) }

    private init() {
        let saved = UserDefaults.standard.string(forKey: "spotik_theme") ?? ""
        currentTheme = AppThemeType(rawValue: saved) ?? .dark
    }

    func setTheme(_ theme: AppThemeType) {
        withAnimation(.spring(response: 0.6, dampingFraction: 0.8)) {
            currentTheme = theme
        }
    }
}

// MARK: - Animated palette view modifier

struct ThemedView<Content: View>: View {
    @EnvironmentObject var themeManager: ThemeManager
    @ViewBuilder let content: (SpotikPalette) -> Content

    var body: some View {
        content(themeManager.palette)
            .environment(\.spotikPalette, themeManager.palette)
    }
}

// MARK: - Convenience modifiers

extension View {
    func spotikThemed() -> some View {
        self.modifier(SpotikThemeModifier())
    }
}

struct SpotikThemeModifier: ViewModifier {
    @EnvironmentObject var themeManager: ThemeManager

    func body(content: Content) -> some View {
        content.environment(\.spotikPalette, themeManager.palette)
    }
}
