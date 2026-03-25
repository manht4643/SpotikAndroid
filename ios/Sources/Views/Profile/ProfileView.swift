import SwiftUI
import Kingfisher

// MARK: - ProfileView

struct ProfileView: View {
    @Environment(\.spotikPalette) var p
    @StateObject private var vm = ProfileViewModel()
    var onLogout: () -> Void = {}

    // Scroll / avatar state
    @State private var scrollOffset: CGFloat = 0
    @State private var avatarExpansion: CGFloat = 0   // 0 = normal, 1 = fullscreen
    @GestureState private var dragDelta: CGFloat = 0
    @State private var isDraggingDown = false
    @State private var lastHapticThreshold: Int = 0

    // Dropdown menu
    @State private var showMenu = false

    // Edit sheet
    @State private var showEdit = false

    private let expandedAvatarSize: CGFloat   = 120
    private let collapsedAvatarSize: CGFloat  = 30
    private let snapThreshold: CGFloat        = 0.35

    var body: some View {
        ZStack(alignment: .top) {
            p.deepBg.ignoresSafeArea()

            // Profile glow behind avatar — accent glow mesh
            ProfileGlowView(progress: avatarExpansion, palette: p)
                .frame(height: 360)
                .offset(y: 80 + avatarExpansion * (-80))

            // Main scroll content
            GeometryReader { outer in
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        ProfileHeader(
                            vm: vm,
                            avatarExpansion: avatarExpansion,
                            scrollOffset: scrollOffset,
                            expandedSize: expandedAvatarSize,
                            palette: p
                        )

                        // Bento grid — fades in fullscreen
                        if let profile = vm.profile {
                            BentoGrid(vm: vm, profile: profile, palette: p)
                                .opacity(1 - avatarExpansion * 1.5)
                                .padding(.bottom, 32)
                        }
                    }
                    .background(
                        GeometryReader { inner in
                            Color.clear.preference(
                                key: ScrollOffsetKey.self,
                                value: inner.frame(in: .named("scroll")).minY
                            )
                        }
                    )
                }
                .coordinateSpace(name: "scroll")
                .onPreferenceChange(ScrollOffsetKey.self) { val in
                    scrollOffset = val
                    // Update scroll collapse - negative value means scrolled up
                    let sf = max(0, -val / 80.0)
                }
                // Pull-down gesture for avatar expansion
                .simultaneousGesture(
                    DragGesture(minimumDistance: 10)
                        .onChanged { g in
                            guard scrollOffset >= -2 else { return }
                            let dy = g.translation.height
                            if dy > 0 {
                                let friction: CGFloat = dy < 80 ? 0.85 : 0.4
                                let newExp = min(1, avatarExpansion + (dy / 350.0) * friction)
                                withAnimation(.interactiveSpring()) {
                                    avatarExpansion = newExp
                                }
                                // Haptic at threshold
                                let thresholdHit = Int(newExp / snapThreshold)
                                if thresholdHit > lastHapticThreshold {
                                    UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                                    lastHapticThreshold = thresholdHit
                                }
                            }
                        }
                        .onEnded { g in
                            lastHapticThreshold = 0
                            let vel = g.predictedEndTranslation.height
                            let shouldExpand = avatarExpansion > snapThreshold || vel > 200
                            withAnimation(.spring(response: 0.45, dampingFraction: 0.72)) {
                                avatarExpansion = shouldExpand ? 1.0 : 0.0
                            }
                            if shouldExpand {
                                UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
                            }
                        }
                )
            }

            // Sticky toolbar
            ProfileToolbar(
                vm: vm,
                showMenu: $showMenu,
                showEdit: $showEdit,
                scrollOffset: scrollOffset,
                avatarExpansion: avatarExpansion,
                palette: p
            )
        }
        .confirmationDialog("Вы уверены?", isPresented: $vm.showLogoutDialog) {
            Button("Выйти из аккаунта", role: .destructive) {
                vm.logout { onLogout() }
            }
        }
        .sheet(isPresented: $showEdit) {
            EditProfileSheet(vm: vm, palette: p)
        }
        .onAppear { vm.loadProfile() }
    }
}

// MARK: - Profile Glow (animated mesh behind avatar)

private struct ProfileGlowView: View {
    let progress: CGFloat
    let palette: SpotikPalette

    var body: some View {
        TimelineView(.animation) { tl in
            let t = tl.date.timeIntervalSinceReferenceDate
            Canvas { ctx, size in
                let cx = size.width / 2
                let cy = size.height * 0.4
                let pulse = sin(t * 0.7) * 0.5 + 0.5
                let alpha = 0.24 + pulse * 0.18

                let grd = Gradient(colors: [
                    palette.accent.opacity(alpha),
                    palette.accent.opacity(alpha * 0.4),
                    .clear
                ])
                ctx.fill(
                    Path(ellipseIn: CGRect(x: cx-140, y: cy-140, width: 280, height: 280)),
                    with: .radialGradient(grd,
                        center: .init(x: cx, y: cy),
                        startRadius: 0, endRadius: 140)
                )
            }
        }
        .opacity(Double(1 - progress * 0.5))
    }
}

// MARK: - Profile Header

private struct ProfileHeader: View {
    @ObservedObject var vm: ProfileViewModel
    let avatarExpansion: CGFloat
    let scrollOffset: CGFloat
    let expandedSize: CGFloat
    let palette: SpotikPalette

    private var screenWidth: CGFloat { UIScreen.main.bounds.width }
    private var avatarSize: CGFloat {
        let expanded = screenWidth
        return expandedSize + (expanded - expandedSize) * avatarExpansion
    }
    private var cornerRadius: CGFloat { expandedSize / 2 * (1 - avatarExpansion) }
    private var sf: CGFloat {
        // scroll fraction: 0 = expanded, 1 = collapsed
        guard scrollOffset < 0 else { return 0 }
        return min(1.0, -scrollOffset / 80.0)
    }

    var body: some View {
        VStack(spacing: 0) {
            Spacer().frame(height: safeAreaTopHeight() + 56)

            // Avatar
            ZStack {
                if let url = vm.profile?.avatarUrl.flatMap({ URL(string: $0) }) {
                    KFImage(url)
                        .resizable()
                        .scaledToFill()
                        .frame(width: avatarSize, height: avatarSize)
                        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                } else {
                    RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                        .fill(LinearGradient(
                            colors: [palette.accent.opacity(0.3), palette.calmMid],
                            startPoint: .topLeading, endPoint: .bottomTrailing
                        ))
                        .frame(width: avatarSize, height: avatarSize)
                        .overlay(
                            Image(systemName: "person.fill")
                                .font(.system(size: avatarSize * 0.4))
                                .foregroundColor(palette.textMuted)
                        )
                }
            }
            .animation(.spring(response: 0.45, dampingFraction: 0.72), value: avatarExpansion)
            .shadow(color: palette.accent.opacity(0.2 * (1 - avatarExpansion)), radius: 20)

            Spacer().frame(height: 16)

            // Name + info — fades out in fullscreen
            if let profile = vm.profile {
                VStack(spacing: 4) {
                    HStack(spacing: 8) {
                        Text(profile.name)
                            .font(.system(size: 26, weight: .bold, design: .rounded))
                            .foregroundColor(palette.textPrimary)
                        Text("\(profile.age)")
                            .font(.system(size: 26, weight: .bold, design: .rounded))
                            .foregroundColor(palette.textPrimary)
                    }

                    HStack(spacing: 4) {
                        Image(systemName: "mappin.fill")
                            .font(.system(size: 12))
                            .foregroundColor(palette.accent)
                        Text(profile.city)
                            .font(.system(size: 13))
                            .foregroundColor(palette.textSecondary)
                    }
                }
                .opacity(Double(max(0, 1 - avatarExpansion * 2)))
                .padding(.bottom, 24)
            } else {
                // Skeleton
                VStack(spacing: 8) {
                    RoundedRectangle(cornerRadius: 8).fill(palette.calmMid)
                        .frame(width: 160, height: 26)
                    RoundedRectangle(cornerRadius: 6).fill(palette.calmMid.opacity(0.6))
                        .frame(width: 100, height: 16)
                }
                .opacity(Double(max(0, 1 - avatarExpansion * 2)))
                .padding(.bottom, 24)
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func safeAreaTopHeight() -> CGFloat {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?.windows.first?.safeAreaInsets.top ?? 0
    }
}

// MARK: - Toolbar

private struct ProfileToolbar: View {
    @ObservedObject var vm: ProfileViewModel
    @Binding var showMenu: Bool
    @Binding var showEdit: Bool
    let scrollOffset: CGFloat
    let avatarExpansion: CGFloat
    let palette: SpotikPalette

    private var sf: CGFloat { min(1.0, max(0, -scrollOffset / 80.0)) }

    var body: some View {
        HStack {
            Spacer()
            // Name in toolbar (appears on scroll collapse)
            Text(vm.profile?.name ?? "")
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(palette.textPrimary)
                .opacity(Double(sf))

            Spacer()

            // Menu button
            Menu {
                Button { showEdit = true } label: {
                    Label("Редактировать профиль", systemImage: "pencil")
                }
                Button(role: .destructive) {
                    vm.showLogoutDialog = true
                } label: {
                    Label("Выйти из аккаунта", systemImage: "rectangle.portrait.and.arrow.right")
                }
            } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(palette.textPrimary)
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(palette.cardDark.opacity(0.8)))
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, safeAreaTop() + 8)
        .padding(.bottom, 8)
    }

    private func safeAreaTop() -> CGFloat {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?.windows.first?.safeAreaInsets.top ?? 0
    }
}

// MARK: - Bento Grid

private struct BentoGrid: View {
    @ObservedObject var vm: ProfileViewModel
    let profile: UserProfile
    let palette: SpotikPalette

    var body: some View {
        VStack(spacing: 12) {
            // Stats row
            HStack(spacing: 12) {
                StatTile(icon: "heart.fill",    value: vm.likesCount,    label: "Лайков",     palette: palette)
                StatTile(icon: "eye.fill",       value: vm.viewsCount,    label: "Просмотров", palette: palette)
                StatTile(icon: "person.2.fill",  value: vm.referralsCount, label: "Рефералов",  palette: palette)
            }

            // Activity chart
            ActivityChartCard(data: vm.activityData, palette: palette)

            // Bio card
            if let bio = profile.bio, !bio.isEmpty {
                BioCard(bio: bio, palette: palette)
            }

            // Logout
            Button {
                vm.showLogoutDialog = true
            } label: {
                Text("Выйти из аккаунта")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(Color(hex: "FF453A"))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
            }
            .buttonStyle(PeekPop())
        }
        .padding(.horizontal, 16)
    }
}

private struct StatTile: View {
    let icon: String
    let value: Int
    let label: String
    let palette: SpotikPalette

    var body: some View {
        BentoTile(cornerRadius: 24) {
            VStack(spacing: 10) {
                ZStack {
                    Circle().fill(palette.accent.opacity(0.2)).frame(width: 44, height: 44)
                    Image(systemName: icon)
                        .font(.system(size: 18))
                        .foregroundColor(palette.accent)
                }
                Text(formatNumber(value))
                    .font(.system(size: 24, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                Text(label)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(.white.opacity(0.8))
            }
            .padding(14)
            .frame(maxWidth: .infinity)
        }
        .aspectRatio(1, contentMode: .fit)
        .environment(\.spotikPalette, palette)
    }

    private func formatNumber(_ n: Int) -> String {
        if n >= 1000 { return String(format: "%.1fK", Double(n) / 1000) }
        return "\(n)"
    }
}

private struct ActivityChartCard: View {
    let data: [Double]   // 0..1 for Mon-Sun
    let palette: SpotikPalette
    let days = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"]

    var body: some View {
        BentoTile(cornerRadius: 28) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("Активность")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.white)
                    Spacer()
                    Text("Неделя")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(palette.accent)
                        .padding(.horizontal, 8).padding(.vertical, 4)
                        .background(Capsule().fill(palette.accent.opacity(0.15)))
                }

                ActivityCurve(data: data, color: palette.accent)
                    .frame(height: 80)

                HStack {
                    ForEach(Array(days.enumerated()), id: \.offset) { _, d in
                        Text(d)
                            .font(.system(size: 10))
                            .foregroundColor(palette.textMuted)
                            .frame(maxWidth: .infinity)
                    }
                }
            }
            .padding(20)
        }
        .frame(height: 180)
        .environment(\.spotikPalette, palette)
    }
}

private struct ActivityCurve: View {
    let data: [Double]
    let color: Color

    var body: some View {
        Canvas { ctx, size in
            guard data.count > 1 else { return }
            let pt: (Int) -> CGPoint = { i in
                CGPoint(
                    x: size.width * CGFloat(i) / CGFloat(data.count - 1),
                    y: size.height * (1 - data[i])
                )
            }
            // Fill under curve
            var fillPath = Path()
            fillPath.move(to: CGPoint(x: 0, y: size.height))
            fillPath.addLine(to: pt(0))
            for i in 1..<data.count {
                let prev = pt(i-1), curr = pt(i)
                let cp1 = CGPoint(x: (prev.x + curr.x) / 2, y: prev.y)
                let cp2 = CGPoint(x: (prev.x + curr.x) / 2, y: curr.y)
                fillPath.addCurve(to: curr, control1: cp1, control2: cp2)
            }
            fillPath.addLine(to: CGPoint(x: size.width, y: size.height))
            fillPath.closeSubpath()
            ctx.fill(fillPath, with: .linearGradient(
                Gradient(colors: [color.opacity(0.35), color.opacity(0)]),
                startPoint: .init(x: 0, y: 0), endPoint: .init(x: 0, y: size.height)
            ))

            // Stroke
            var linePath = Path()
            linePath.move(to: pt(0))
            for i in 1..<data.count {
                let prev = pt(i-1), curr = pt(i)
                let cp1 = CGPoint(x: (prev.x + curr.x) / 2, y: prev.y)
                let cp2 = CGPoint(x: (prev.x + curr.x) / 2, y: curr.y)
                linePath.addCurve(to: curr, control1: cp1, control2: cp2)
            }
            ctx.stroke(linePath, with: .color(color), style: StrokeStyle(lineWidth: 2.5, lineCap: .round, lineJoin: .round))
        }
    }
}

private struct BioCard: View {
    let bio: String
    let palette: SpotikPalette

    var body: some View {
        BentoTile(cornerRadius: 24) {
            VStack(alignment: .leading, spacing: 8) {
                Text("О себе").font(.system(size: 14, weight: .semibold))
                    .foregroundColor(palette.textSecondary)
                Text(bio).font(.system(size: 15))
                    .foregroundColor(palette.textPrimary)
            }
            .padding(18).frame(maxWidth: .infinity, alignment: .leading)
        }
        .environment(\.spotikPalette, palette)
    }
}

// MARK: - Edit Profile Sheet

struct EditProfileSheet: View {
    @ObservedObject var vm: ProfileViewModel
    let palette: SpotikPalette
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            ZStack {
                palette.deepBg.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 20) {
                        // Avatar
                        ZStack(alignment: .bottomTrailing) {
                            Circle()
                                .fill(palette.cardDark)
                                .frame(width: 100, height: 100)
                                .overlay(
                                    Image(systemName: "person.fill")
                                        .font(.system(size: 40))
                                        .foregroundColor(palette.textMuted)
                                )
                            Circle()
                                .fill(palette.accent)
                                .frame(width: 32, height: 32)
                                .overlay(
                                    Image(systemName: "camera.fill")
                                        .font(.system(size: 14))
                                        .foregroundColor(.white)
                                )
                        }
                        .padding(.top, 20)

                        // Fields
                        VStack(spacing: 12) {
                            EditField("Имя", text: $vm.editName, palette: palette)
                            EditField("Город", text: $vm.editCity, palette: palette)
                            EditField("О себе", text: $vm.editBio, palette: palette, isMultiline: true)
                        }
                        .padding(.horizontal, 20)

                        // Age picker
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Возраст").font(.system(size: 14)).foregroundColor(palette.textSecondary)
                            Picker("", selection: $vm.editAge) {
                                ForEach(14...99, id: \.self) { a in
                                    Text("\(a)").tag(a)
                                }
                            }
                            .pickerStyle(.wheel)
                            .frame(height: 100)
                            .colorScheme(.dark)
                        }
                        .padding(.horizontal, 20)

                        // Logout button
                        Button { vm.showLogoutDialog = true } label: {
                            Text("Выйти из аккаунта")
                                .font(.system(size: 16))
                                .foregroundColor(Color(hex: "FF453A"))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                        }

                        Spacer().frame(height: 40)
                    }
                }
            }
            .navigationTitle("Редактирование")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Отмена") { dismiss() }
                        .foregroundColor(palette.accent)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Сохранить") {
                        vm.saveEdits()
                        dismiss()
                    }
                    .foregroundColor(palette.accent)
                    .fontWeight(.semibold)
                }
            }
        }
    }
}

private struct EditField: View {
    let label: String
    @Binding var text: String
    let palette: SpotikPalette
    var isMultiline: Bool = false

    init(_ label: String, text: Binding<String>, palette: SpotikPalette, isMultiline: Bool = false) {
        self.label = label
        self._text = text
        self.palette = palette
        self.isMultiline = isMultiline
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.system(size: 13)).foregroundColor(palette.textSecondary)
            Group {
                if isMultiline {
                    TextEditor(text: $text)
                        .frame(minHeight: 80)
                        .scrollContentBackground(.hidden)
                } else {
                    TextField(label, text: $text)
                }
            }
            .foregroundColor(palette.textPrimary)
            .tint(palette.accent)
            .padding(12)
            .background(palette.cardDark)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(palette.borderDark, lineWidth: 0.5)
            )
        }
    }
}

// MARK: - Preference Key

private struct ScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}
