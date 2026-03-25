import SwiftUI
import UIKit

// MARK: - Tab enum

enum AppTab: CaseIterable, Identifiable {
    case search, map, profile
    var id: Self { self }

    var icon: String {
        switch self {
        case .search:  return "magnifyingglass"
        case .map:     return "map.fill"
        case .profile: return "person.fill"
        }
    }

    var label: String {
        switch self {
        case .search:  return "Поиск"
        case .map:     return "Карта"
        case .profile: return "Профиль"
        }
    }
}

// MARK: - MainTabView

struct MainTabView: View {
    @EnvironmentObject var authManager: AuthManager
    @Environment(\.spotikPalette) var p
    @State private var selectedTab: AppTab = .map

    var body: some View {
        ZStack(alignment: .bottom) {
            // Screen content
            ZStack {
                ForEach(AppTab.allCases) { tab in
                    tabContent(tab)
                        .opacity(selectedTab == tab ? 1 : 0)
                        .animation(.easeInOut(duration: 0.28), value: selectedTab)
                        .allowsHitTesting(selectedTab == tab)
                }
            }
            .ignoresSafeArea()

            // Glass tab bar
            GlassTabBar(selected: $selectedTab)
                .padding(.horizontal, 28)
                .padding(.bottom, safeAreaBottom() + 4)
        }
        .ignoresSafeArea(edges: .bottom)
        .environment(\.spotikPalette, p)
    }

    @ViewBuilder
    private func tabContent(_ tab: AppTab) -> some View {
        switch tab {
        case .search:  SearchView()
        case .map:     MapView()
        case .profile:
            NavigationView {
                ProfileView(onLogout: { /* Handled by AuthManager */ })
                    .navigationBarHidden(true)
            }
            .navigationViewStyle(.stack)
        }
    }

    private func safeAreaBottom() -> CGFloat {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?.windows.first?.safeAreaInsets.bottom ?? 0
    }
}

// MARK: - Glass Tab Bar (Liquid Glass)

struct GlassTabBar: View {
    @Binding var selected: AppTab
    @Environment(\.spotikPalette) var p
    @Namespace private var pillNamespace

    var body: some View {
        HStack(spacing: 0) {
            ForEach(AppTab.allCases) { tab in
                GlassTabItem(
                    tab: tab,
                    isSelected: selected == tab,
                    namespace: pillNamespace,
                    palette: p
                ) {
                    if selected != tab {
                        withAnimation(.spring(response: 0.38, dampingFraction: 0.72)) {
                            selected = tab
                        }
                        UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    }
                }
            }
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 8)
        .background {
            GlassBarBackground(accentColor: p.navGlow, selectedTab: selected)
        }
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .stroke(Color.white.opacity(0.14), lineWidth: 0.6)
        )
        .shadow(color: .black.opacity(0.35), radius: 24, y: 8)
    }
}

// MARK: - Glass Bar Background (blur + glow)

private struct GlassBarBackground: UIViewRepresentable {
    let accentColor: Color
    let selectedTab: AppTab

    func makeUIView(context: Context) -> UIVisualEffectView {
        let blur = UIBlurEffect(style: .systemUltraThinMaterialDark)
        let v = UIVisualEffectView(effect: blur)
        v.layer.cornerRadius = 28
        v.clipsToBounds = true
        return v
    }

    func updateUIView(_ uiView: UIVisualEffectView, context: Context) {}
}

// MARK: - Individual Tab Item

private struct GlassTabItem: View {
    let tab: AppTab
    let isSelected: Bool
    let namespace: Namespace.ID
    let palette: SpotikPalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                // Active pill background with matchedGeometryEffect for liquid slide
                if isSelected {
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .fill(palette.accent.opacity(0.22))
                        .matchedGeometryEffect(id: "PILL", in: namespace)
                        .shadow(color: palette.accent.opacity(0.45), radius: 10)
                }

                VStack(spacing: 4) {
                    Image(systemName: tab.icon)
                        .font(.system(size: isSelected ? 22 : 20, weight: isSelected ? .semibold : .regular))
                        .foregroundColor(isSelected ? palette.accent : palette.textMuted)
                        .scaleEffect(isSelected ? 1.08 : 1.0)

                    Text(tab.label)
                        .font(.system(size: 10, weight: isSelected ? .semibold : .regular))
                        .foregroundColor(isSelected ? palette.accent : palette.textMuted)
                }
                .padding(.horizontal, 18)
                .padding(.vertical, 8)
            }
        }
        .buttonStyle(PeekPop(maxScale: 1.07, minScale: 0.95))
        .animation(.spring(response: 0.38, dampingFraction: 0.72), value: isSelected)
    }
}
