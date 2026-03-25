import SwiftUI
import MapKit

// MARK: - Spot Model

struct Spot: Identifiable {
    let id = UUID()
    let name: String
    let description: String
    let details: String
    let emoji: String
    let coordinate: CLLocationCoordinate2D
    let stripColors: [Color]
}

// MARK: - MapView

struct MapView: View {
    @Environment(\.spotikPalette) var p
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 54.9024, longitude: 52.2978), // Almetyevsk
        span: MKCoordinateSpan(latitudeDelta: 0.08, longitudeDelta: 0.08)
    )
    @State private var selectedSpot: Spot? = nil
    @State private var mapAppeared = false
    @State private var position: MapCameraPosition = .region(
        MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 54.9024, longitude: 52.2978),
            span: MKCoordinateSpan(latitudeDelta: 0.08, longitudeDelta: 0.08)
        )
    )

    private let spots: [Spot] = [
        Spot(name: "Центральный парк",     description: "Просторный парк для скейтборда", details: "8 рампы + площадка", emoji: "🛹",
             coordinate: .init(latitude: 54.9045, longitude: 52.2991), stripColors: [.cyan, .blue]),
        Spot(name: "ул. Ленина, набережная", description: "Гладкие перила и ступени",    details: "Идеально для фрагдропа", emoji: "🏄",
             coordinate: .init(latitude: 54.8998, longitude: 52.2965), stripColors: [.orange, .red]),
        Spot(name: "Школа №12",            description: "Закрытый двор, мрамор",        details: "Только вечером",        emoji: "🏫",
             coordinate: .init(latitude: 54.9060, longitude: 52.3010), stripColors: [.purple, .pink]),
        Spot(name: "Торговый центр Парк",  description: "Подземная парковка",            details: "Круглосуточно",         emoji: "🏢",
             coordinate: .init(latitude: 54.9012, longitude: 52.2940), stripColors: [.green, .teal]),
        Spot(name: "Стадион «Нефтяник»",  description: "Беговая дорожка и трибуны",    details: "Пт-Вс открыт",         emoji: "🏟️",
             coordinate: .init(latitude: 54.9080, longitude: 52.2950), stripColors: [.yellow, .orange]),
        Spot(name: "Брусчатка Авиастроителей", description: "Длинный прогон",          details: "3 блока без трафика",   emoji: "🛤️",
             coordinate: .init(latitude: 54.8970, longitude: 52.3005), stripColors: [.red, .pink]),
        Spot(name: "Площадь Нефтяников",  description: "Открытая plaza с фонтанами",   details: "Летом ограничен",       emoji: "⛲",
             coordinate: .init(latitude: 54.9035, longitude: 52.2975), stripColors: [.blue, .indigo]),
    ]

    var body: some View {
        ZStack {
            // Map with custom dark style
            Map(position: $position) {
                ForEach(spots) { spot in
                    Annotation(spot.name, coordinate: spot.coordinate) {
                        SpotMarker(spot: spot, isSelected: selectedSpot?.id == spot.id)
                            .onTapGesture {
                                withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
                                    selectedSpot = (selectedSpot?.id == spot.id) ? nil : spot
                                }
                                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                            }
                    }
                }
            }
            .mapStyle(.standard(elevation: .realistic, emphasis: .muted, pointsOfInterest: .excludingAll))
            .ignoresSafeArea()
            .opacity(mapAppeared ? 1 : 0)
            .animation(.easeIn(duration: 0.8), value: mapAppeared)

            // Bottom spot card
            if let spot = selectedSpot {
                VStack {
                    Spacer()
                    SpotCard(spot: spot) {
                        withAnimation { selectedSpot = nil }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 100) // above tab bar
                    .transition(.asymmetric(
                        insertion: .move(edge: .bottom).combined(with: .opacity),
                        removal: .move(edge: .bottom).combined(with: .opacity)
                    ))
                }
            }
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                mapAppeared = true
            }
        }
        .animation(.spring(response: 0.45), value: selectedSpot?.id)
    }
}

// MARK: - Spot Marker

private struct SpotMarker: View {
    let spot: Spot
    let isSelected: Bool

    var body: some View {
        ZStack {
            Circle()
                .fill(
                    LinearGradient(
                        colors: spot.stripColors,
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    )
                )
                .frame(width: isSelected ? 44 : 36, height: isSelected ? 44 : 36)
                .shadow(color: spot.stripColors.first?.opacity(0.6) ?? .clear, radius: isSelected ? 12 : 6)

            Text(spot.emoji)
                .font(.system(size: isSelected ? 20 : 16))
        }
        .animation(.spring(response: 0.3, dampingFraction: 0.65), value: isSelected)
    }
}

// MARK: - Spot Card

private struct SpotCard: View {
    @Environment(\.spotikPalette) var p
    let spot: Spot
    let onClose: () -> Void

    var body: some View {
        HStack(spacing: 16) {
            // Stripe animation
            RoundedRectangle(cornerRadius: 4)
                .fill(LinearGradient(
                    colors: spot.stripColors,
                    startPoint: .top, endPoint: .bottom
                ))
                .frame(width: 5)
                .frame(height: 70)

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(spot.emoji + " " + spot.name)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(p.textPrimary)
                    Spacer()
                    Button(action: onClose) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(p.textMuted)
                            .font(.system(size: 22))
                    }
                }
                Text(spot.description)
                    .font(.system(size: 13))
                    .foregroundColor(p.textSecondary)
                Text(spot.details)
                    .font(.system(size: 12))
                    .foregroundColor(p.accent)
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(p.cardDark)
                .shadow(color: .black.opacity(0.3), radius: 16, y: 6)
        )
    }
}
