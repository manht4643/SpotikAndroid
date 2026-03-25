import SwiftUI

struct SearchView: View {
    @Environment(\.spotikPalette) var p
    @State private var appear = false

    var body: some View {
        ZStack {
            p.deepBg.ignoresSafeArea()

            VStack(spacing: 20) {
                Spacer()

                Image(systemName: "magnifyingglass")
                    .font(.system(size: 72, weight: .ultraLight))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [p.accent, p.accent.opacity(0.4)],
                            startPoint: .top, endPoint: .bottom
                        )
                    )
                    .scaleEffect(appear ? 1.0 : 0.7)
                    .opacity(appear ? 1.0 : 0)

                Text("Поиск")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundColor(p.textPrimary)
                    .opacity(appear ? 1.0 : 0)

                Text("В разработке")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(p.accent)
                    .padding(.horizontal, 12).padding(.vertical, 6)
                    .background(Capsule().fill(p.accent.opacity(0.15)))
                    .opacity(appear ? 1.0 : 0)

                Text("Скоро здесь можно будет искать\nлюдей по интересам и фильтрам")
                    .font(.system(size: 15))
                    .foregroundColor(p.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
                    .opacity(appear ? 1.0 : 0)
                    .offset(y: appear ? 0 : 10)

                Spacer()
            }
            .animation(.spring(response: 0.6, dampingFraction: 0.8), value: appear)
        }
        .onAppear { withAnimation { appear = true } }
    }
}
