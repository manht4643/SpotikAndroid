import SwiftUI

struct SplashView: View {
    @Environment(\.spotikPalette) var p
    @State private var logoScale: CGFloat = 0.7
    @State private var logoOpacity: CGFloat = 0
    @State private var glowOpacity: CGFloat = 0

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            // Glow behind logo
            Circle()
                .fill(
                    RadialGradient(
                        colors: [Color(hex: "0A84FF").opacity(0.4), .clear],
                        center: .center,
                        startRadius: 0,
                        endRadius: 120
                    )
                )
                .frame(width: 240, height: 240)
                .scaleEffect(logoScale * 1.4)
                .opacity(glowOpacity)

            VStack(spacing: 12) {
                // Logo mark
                ZStack {
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: "0A84FF"), Color(hex: "0060D0")],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 90, height: 90)
                        .shadow(color: Color(hex: "0A84FF").opacity(0.6), radius: 24)

                    Image(systemName: "location.fill")
                        .font(.system(size: 38, weight: .bold))
                        .foregroundColor(.white)
                }

                Text("Spotik")
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
            }
            .scaleEffect(logoScale)
            .opacity(logoOpacity)
        }
        .onAppear {
            withAnimation(.spring(response: 0.7, dampingFraction: 0.6).delay(0.1)) {
                logoScale   = 1.0
                logoOpacity = 1.0
            }
            withAnimation(.easeIn(duration: 0.6).delay(0.3)) {
                glowOpacity = 0.8
            }
        }
    }
}
