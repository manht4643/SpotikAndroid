import SwiftUI

// MARK: - Theme Types

enum AppThemeType: String, CaseIterable, Identifiable {
    case dark         = "dark"
    case light        = "light"
    case sunsetGlow   = "sunset_glow"
    case aurora       = "aurora"
    case neonRose     = "neon_rose"
    case midnightGold = "midnight_gold"
    case cyberFrost   = "cyber_frost"
    case cherryBlossom = "cherry_blossom"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .dark:          return "Тёмная"
        case .light:         return "Светлая"
        case .sunsetGlow:    return "Закат"
        case .aurora:        return "Аврора"
        case .neonRose:      return "Роза"
        case .midnightGold:  return "Золото"
        case .cyberFrost:    return "Лёд"
        case .cherryBlossom: return "Сакура"
        }
    }
}

// MARK: - Palette

struct SpotikPalette {
    let surfaceDark: Color
    let cardDark: Color
    let borderLight: Color
    let borderDark: Color
    let textPrimary: Color
    let textSecondary: Color
    let textMuted: Color
    let accent: Color
    let accentLike: Color
    let premium: Color
    let navGlow: Color
    let deepBg: Color
    let techMid: Color
    let calmMid: Color
    let skyLight: Color
    let paleLight: Color
    let meshColors: [Color]
}

// MARK: - Palettes

extension SpotikPalette {
    static let dark = SpotikPalette(
        surfaceDark:   Color(hex: "000000"),
        cardDark:      Color(hex: "1C1C1E"),
        borderLight:   Color(hex: "FFFFFF").opacity(0.2),
        borderDark:    Color(hex: "FFFFFF").opacity(0.1),
        textPrimary:   Color(hex: "FFFFFF"),
        textSecondary: Color(hex: "FFFFFF").opacity(0.7),
        textMuted:     Color(hex: "FFFFFF").opacity(0.4),
        accent:        Color(hex: "0A84FF"),
        accentLike:    Color(hex: "FF453A"),
        premium:       Color(hex: "FFD60A"),
        navGlow:       Color(hex: "0A84FF"),
        deepBg:        Color(hex: "000000"),
        techMid:       Color(hex: "1C1C1E"),
        calmMid:       Color(hex: "3A3A3C"),
        skyLight:      Color(hex: "636366"),
        paleLight:     Color(hex: "8E8E93"),
        meshColors: [
            Color(hex: "000000"), Color(hex: "000000"), Color(hex: "000000"), Color(hex: "000000"),
            Color(hex: "000000"), Color(hex: "1C1C1E"), Color(hex: "1C1C1E"), Color(hex: "000000"),
            Color(hex: "000000"), Color(hex: "1C1C1E"), Color(hex: "0A84FF").opacity(0.15), Color(hex: "000000"),
            Color(hex: "000000"), Color(hex: "000000"), Color(hex: "000000"), Color(hex: "000000"),
        ]
    )

    static let light = SpotikPalette(
        surfaceDark:   Color(hex: "F2F2F7"),
        cardDark:      Color(hex: "FFFFFF"),
        borderLight:   Color(hex: "000000").opacity(0.1),
        borderDark:    Color(hex: "000000").opacity(0.05),
        textPrimary:   Color(hex: "000000"),
        textSecondary: Color(hex: "000000").opacity(0.7),
        textMuted:     Color(hex: "000000").opacity(0.4),
        accent:        Color(hex: "007AFF"),
        accentLike:    Color(hex: "FF3B30"),
        premium:       Color(hex: "FF9F0A"),
        navGlow:       Color(hex: "007AFF"),
        deepBg:        Color(hex: "FFFFFF"),
        techMid:       Color(hex: "E5E5EA"),
        calmMid:       Color(hex: "C7C7CC"),
        skyLight:      Color(hex: "AEAEB2"),
        paleLight:     Color(hex: "8E8E93"),
        meshColors: Array(repeating: Color(hex: "F2F2F7"), count: 16)
    )

    static let sunsetGlow = SpotikPalette(
        surfaceDark:   Color(hex: "1A0505"),
        cardDark:      Color(hex: "2A0A0A"),
        borderLight:   Color(hex: "F97316").opacity(0.25),
        borderDark:    Color(hex: "F97316").opacity(0.12),
        textPrimary:   Color(hex: "FFFFFF"),
        textSecondary: Color(hex: "FFFFFF").opacity(0.7),
        textMuted:     Color(hex: "FFFFFF").opacity(0.4),
        accent:        Color(hex: "F97316"),
        accentLike:    Color(hex: "FB7185"),
        premium:       Color(hex: "FBBF24"),
        navGlow:       Color(hex: "FBBF24"),
        deepBg:        Color(hex: "1A0505"),
        techMid:       Color(hex: "2A0D00"),
        calmMid:       Color(hex: "7C2D12"),
        skyLight:      Color(hex: "C2410C"),
        paleLight:     Color(hex: "EA580C"),
        meshColors: [
            Color(hex: "1A0505"), Color(hex: "2D0A0A"), Color(hex: "4A1000"), Color(hex: "1A0505"),
            Color(hex: "2D0A0A"), Color(hex: "7C2D12"), Color(hex: "F97316").opacity(0.5), Color(hex: "4A1000"),
            Color(hex: "4A1000"), Color(hex: "F97316").opacity(0.3), Color(hex: "FBBF24").opacity(0.4), Color(hex: "7C2D12"),
            Color(hex: "1A0505"), Color(hex: "4A1000"), Color(hex: "7C2D12"), Color(hex: "1A0505"),
        ]
    )

    static let aurora = SpotikPalette(
        surfaceDark:   Color(hex: "021A0E"),
        cardDark:      Color(hex: "051F12"),
        borderLight:   Color(hex: "10B981").opacity(0.25),
        borderDark:    Color(hex: "10B981").opacity(0.12),
        textPrimary:   Color(hex: "FFFFFF"),
        textSecondary: Color(hex: "FFFFFF").opacity(0.7),
        textMuted:     Color(hex: "FFFFFF").opacity(0.4),
        accent:        Color(hex: "10B981"),
        accentLike:    Color(hex: "F472B6"),
        premium:       Color(hex: "34D399"),
        navGlow:       Color(hex: "34D399"),
        deepBg:        Color(hex: "021A0E"),
        techMid:       Color(hex: "064E3B"),
        calmMid:       Color(hex: "065F46"),
        skyLight:      Color(hex: "047857"),
        paleLight:     Color(hex: "059669"),
        meshColors: [
            Color(hex: "021A0E"), Color(hex: "031F10"), Color(hex: "064E3B"), Color(hex: "021A0E"),
            Color(hex: "031F10"), Color(hex: "065F46"), Color(hex: "10B981").opacity(0.4), Color(hex: "064E3B"),
            Color(hex: "064E3B"), Color(hex: "10B981").opacity(0.3), Color(hex: "34D399").opacity(0.3), Color(hex: "065F46"),
            Color(hex: "021A0E"), Color(hex: "064E3B"), Color(hex: "065F46"), Color(hex: "021A0E"),
        ]
    )

    static let neonRose = SpotikPalette(
        surfaceDark:   Color(hex: "1A0515"),
        cardDark:      Color(hex: "280A1E"),
        borderLight:   Color(hex: "EC4899").opacity(0.25),
        borderDark:    Color(hex: "EC4899").opacity(0.12),
        textPrimary:   Color(hex: "FFFFFF"),
        textSecondary: Color(hex: "FFFFFF").opacity(0.7),
        textMuted:     Color(hex: "FFFFFF").opacity(0.4),
        accent:        Color(hex: "EC4899"),
        accentLike:    Color(hex: "F43F5E"),
        premium:       Color(hex: "F9A8D4"),
        navGlow:       Color(hex: "EC4899"),
        deepBg:        Color(hex: "1A0515"),
        techMid:       Color(hex: "500724"),
        calmMid:       Color(hex: "831843"),
        skyLight:      Color(hex: "9D174D"),
        paleLight:     Color(hex: "BE185D"),
        meshColors: [
            Color(hex: "1A0515"), Color(hex: "280A1E"), Color(hex: "500724"), Color(hex: "1A0515"),
            Color(hex: "280A1E"), Color(hex: "831843"), Color(hex: "EC4899").opacity(0.4), Color(hex: "500724"),
            Color(hex: "500724"), Color(hex: "EC4899").opacity(0.3), Color(hex: "F9A8D4").opacity(0.25), Color(hex: "831843"),
            Color(hex: "1A0515"), Color(hex: "500724"), Color(hex: "831843"), Color(hex: "1A0515"),
        ]
    )

    static let midnightGold = SpotikPalette(
        surfaceDark:   Color(hex: "0F0C02"),
        cardDark:      Color(hex: "1A1503"),
        borderLight:   Color(hex: "D4A017").opacity(0.25),
        borderDark:    Color(hex: "D4A017").opacity(0.12),
        textPrimary:   Color(hex: "FFFFFF"),
        textSecondary: Color(hex: "FFFFFF").opacity(0.7),
        textMuted:     Color(hex: "FFFFFF").opacity(0.4),
        accent:        Color(hex: "D4A017"),
        accentLike:    Color(hex: "FF6B35"),
        premium:       Color(hex: "FFD700"),
        navGlow:       Color(hex: "FFD700"),
        deepBg:        Color(hex: "0F0C02"),
        techMid:       Color(hex: "432E00"),
        calmMid:       Color(hex: "78520A"),
        skyLight:      Color(hex: "92400E"),
        paleLight:     Color(hex: "B45309"),
        meshColors: [
            Color(hex: "0F0C02"), Color(hex: "1A1503"), Color(hex: "432E00"), Color(hex: "0F0C02"),
            Color(hex: "1A1503"), Color(hex: "78520A"), Color(hex: "D4A017").opacity(0.4), Color(hex: "432E00"),
            Color(hex: "432E00"), Color(hex: "D4A017").opacity(0.3), Color(hex: "FFD700").opacity(0.25), Color(hex: "78520A"),
            Color(hex: "0F0C02"), Color(hex: "432E00"), Color(hex: "78520A"), Color(hex: "0F0C02"),
        ]
    )

    static let cyberFrost = SpotikPalette(
        surfaceDark:   Color(hex: "03090F"),
        cardDark:      Color(hex: "060E18"),
        borderLight:   Color(hex: "00BFFF").opacity(0.25),
        borderDark:    Color(hex: "00BFFF").opacity(0.12),
        textPrimary:   Color(hex: "FFFFFF"),
        textSecondary: Color(hex: "FFFFFF").opacity(0.7),
        textMuted:     Color(hex: "FFFFFF").opacity(0.4),
        accent:        Color(hex: "00BFFF"),
        accentLike:    Color(hex: "FF6B9D"),
        premium:       Color(hex: "7DF9FF"),
        navGlow:       Color(hex: "7DF9FF"),
        deepBg:        Color(hex: "03090F"),
        techMid:       Color(hex: "0C1B33"),
        calmMid:       Color(hex: "1E3A5F"),
        skyLight:      Color(hex: "1D4ED8"),
        paleLight:     Color(hex: "2563EB"),
        meshColors: [
            Color(hex: "03090F"), Color(hex: "060E18"), Color(hex: "0C1B33"), Color(hex: "03090F"),
            Color(hex: "060E18"), Color(hex: "1E3A5F"), Color(hex: "00BFFF").opacity(0.4), Color(hex: "0C1B33"),
            Color(hex: "0C1B33"), Color(hex: "00BFFF").opacity(0.2), Color(hex: "7DF9FF").opacity(0.2), Color(hex: "1E3A5F"),
            Color(hex: "03090F"), Color(hex: "0C1B33"), Color(hex: "1E3A5F"), Color(hex: "03090F"),
        ]
    )

    static let cherryBlossom = SpotikPalette(
        surfaceDark:   Color(hex: "140508"),
        cardDark:      Color(hex: "1E0810"),
        borderLight:   Color(hex: "FF69B4").opacity(0.25),
        borderDark:    Color(hex: "FF69B4").opacity(0.12),
        textPrimary:   Color(hex: "FFFFFF"),
        textSecondary: Color(hex: "FFFFFF").opacity(0.7),
        textMuted:     Color(hex: "FFFFFF").opacity(0.4),
        accent:        Color(hex: "FF69B4"),
        accentLike:    Color(hex: "FF1493"),
        premium:       Color(hex: "FFB7C5"),
        navGlow:       Color(hex: "FFB7C5"),
        deepBg:        Color(hex: "140508"),
        techMid:       Color(hex: "4A0E2A"),
        calmMid:       Color(hex: "831843"),
        skyLight:      Color(hex: "9D174D"),
        paleLight:     Color(hex: "BE185D"),
        meshColors: [
            Color(hex: "140508"), Color(hex: "1E0810"), Color(hex: "4A0E2A"), Color(hex: "140508"),
            Color(hex: "1E0810"), Color(hex: "831843"), Color(hex: "FF69B4").opacity(0.35), Color(hex: "4A0E2A"),
            Color(hex: "4A0E2A"), Color(hex: "FF69B4").opacity(0.25), Color(hex: "FFB7C5").opacity(0.2), Color(hex: "831843"),
            Color(hex: "140508"), Color(hex: "4A0E2A"), Color(hex: "831843"), Color(hex: "140508"),
        ]
    )

    static func palette(for theme: AppThemeType) -> SpotikPalette {
        switch theme {
        case .dark:          return .dark
        case .light:         return .light
        case .sunsetGlow:    return .sunsetGlow
        case .aurora:        return .aurora
        case .neonRose:      return .neonRose
        case .midnightGold:  return .midnightGold
        case .cyberFrost:    return .cyberFrost
        case .cherryBlossom: return .cherryBlossom
        }
    }
}

// MARK: - Environment Key

struct SpotikPaletteKey: EnvironmentKey {
    static let defaultValue: SpotikPalette = .dark
}

extension EnvironmentValues {
    var spotikPalette: SpotikPalette {
        get { self[SpotikPaletteKey.self] }
        set { self[SpotikPaletteKey.self] = newValue }
    }
}

// MARK: - Color Hex Extension

extension Color {
    init(hex: String, opacity: Double = 1.0) {
        var h = hex.trimmingCharacters(in: .alphanumerics.inverted)
        if h.count == 6 { h = "FF" + h }
        var int: UInt64 = 0
        Scanner(string: h).scanHexInt64(&int)
        let a = Double((int >> 24) & 0xFF) / 255
        let r = Double((int >> 16) & 0xFF) / 255
        let g = Double((int >> 8)  & 0xFF) / 255
        let b = Double(int         & 0xFF) / 255
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a * opacity)
    }
}
