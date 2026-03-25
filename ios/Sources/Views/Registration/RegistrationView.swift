import SwiftUI
import PhotosUI

struct RegistrationView: View {
    @Environment(\.spotikPalette) var p
    @StateObject private var vm = RegistrationViewModel()
    @Environment(\.presentationMode) var presentationMode

    var body: some View {
        ZStack(alignment: .bottom) {
            MeshGradientBackground()

            // Step content
            ZStack {
                ForEach(0..<6) { step in
                    if vm.currentStep == step {
                        stepView(step)
                            .transition(.asymmetric(
                                insertion: .move(edge: .trailing).combined(with: .opacity),
                                removal: .move(edge: .leading).combined(with: .opacity)
                            ))
                    }
                }
            }
            .animation(.easeInOut(duration: 0.35), value: vm.currentStep)

            // Bottom navigation bar
            RegistrationBottomBar(vm: vm) {
                presentationMode.wrappedValue.dismiss()
            }
        }
        .navigationBarHidden(true)
        .ignoresSafeArea(.keyboard)
        // Error toast
        .overlay(alignment: .top) {
            if let err = vm.errorMessage {
                ErrorToast(message: err)
                    .padding(.top, 60)
                    .padding(.horizontal, 24)
                    .onTapGesture { vm.clearError() }
            }
        }
        .onAppear {
            if vm.currentStep == 1 { vm.sendCode() }
        }
    }

    @ViewBuilder
    private func stepView(_ step: Int) -> some View {
        switch step {
        case 0: Step0EmailView(vm: vm)
        case 1: Step1OTPView(vm: vm)
        case 2: Step2CityView(vm: vm)
        case 3: Step3NicknameView(vm: vm)
        case 4: Step4AgeView(vm: vm)
        case 5: Step5PhotoView(vm: vm)
        default: EmptyView()
        }
    }
}

// MARK: - Bottom Bar

private struct RegistrationBottomBar: View {
    @Environment(\.spotikPalette) var p
    @ObservedObject var vm: RegistrationViewModel
    let onBack: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Progress track
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(p.textMuted.opacity(0.12))
                    Capsule()
                        .fill(LinearGradient(
                            colors: [p.accent, p.accent.opacity(0.7)],
                            startPoint: .leading, endPoint: .trailing
                        ))
                        .frame(width: geo.size.width * CGFloat(vm.currentStep + 1) / 6.0)
                        .animation(.spring(response: 0.5, dampingFraction: 0.8), value: vm.currentStep)
                }
            }
            .frame(height: 4)

            HStack {
                // Step count
                Text("\(vm.currentStep + 1) / 6")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(p.textSecondary)

                Spacer()

                // Back button
                if vm.currentStep > 0 {
                    Button {
                        vm.back()
                        UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    } label: {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(p.textPrimary)
                            .padding(12)
                            .background(Circle().fill(Color(hex: "CC16161E")))
                    }
                    .buttonStyle(PeekPop())
                    .transition(.scale.combined(with: .opacity))
                }

                Spacer()

                // Next / Done button
                Button {
                    if vm.currentStep == 5 {
                        vm.submit { /* onSuccess closes nav */ }
                    } else if vm.currentStep == 1 && !vm.isEmailVerified {
                        vm.verifyCode()
                    } else {
                        vm.advance()
                        UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    }
                } label: {
                    ZStack {
                        Circle()
                            .fill(vm.canProceed ? p.accent : Color.gray.opacity(0.4))
                            .frame(width: 56, height: 56)
                            .shadow(color: vm.canProceed ? p.accent.opacity(0.5) : .clear, radius: 12)

                        if vm.isSubmitting || vm.isVerifying {
                            ProgressView().tint(.white).scaleEffect(0.85)
                        } else {
                            Image(systemName: vm.currentStep == 5 ? "checkmark" : "arrow.right")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }
                }
                .buttonStyle(PeekPop(maxScale: 1.06))
                .disabled(!vm.canProceed || vm.isSubmitting)
                .animation(.spring(response: 0.3), value: vm.canProceed)
            }
            .padding(.top, 12)
            .padding(.bottom, 8)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 12)
        .background(.ultraThinMaterial)
        .overlay(alignment: .top) {
            Divider().opacity(0.3)
        }
        .animation(.spring(response: 0.35), value: vm.currentStep)
    }
}

// MARK: - Step 0 — Email

private struct Step0EmailView: View {
    @Environment(\.spotikPalette) var p
    @ObservedObject var vm: RegistrationViewModel

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 28) {
                Spacer().frame(height: 60)
                Image(systemName: "bell.fill")
                    .font(.system(size: 80))
                    .foregroundStyle(LinearGradient(
                        colors: [p.accent, p.navGlow],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ))
                    .frame(height: 120)

                VStack(spacing: 8) {
                    Text("Добро пожаловать")
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundColor(p.textPrimary)
                    Text("Укажи email, чтобы начать")
                        .font(.system(size: 15))
                        .foregroundColor(p.textSecondary)
                }
                .multilineTextAlignment(.center)

                GlassCard {
                    HStack(spacing: 12) {
                        Image(systemName: "envelope.fill")
                            .foregroundColor(p.textMuted)
                        TextField("Email", text: $vm.email)
                            .keyboardType(.emailAddress)
                            .autocapitalization(.none)
                            .foregroundColor(p.textPrimary)
                            .tint(p.accent)
                            .submitLabel(.done)
                    }
                    .padding(14)
                    .background(Color(hex: "1A1A2E"))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(vm.isEmailValid ? p.accent.opacity(0.6) : p.borderDark, lineWidth: 1)
                    )
                }
                .padding(.horizontal, 24)

                Spacer()

                // "Already have account" link
                NavigationLink(destination: AuthView()) {
                    HStack(spacing: 4) {
                        Text("Уже есть аккаунт?")
                            .foregroundColor(p.textSecondary).font(.system(size: 14))
                        Text("Войти").foregroundColor(p.accent)
                            .font(.system(size: 14, weight: .semibold))
                    }
                }
                Spacer().frame(height: 120)
            }
        }
    }
}

// MARK: - Step 1 — OTP

private struct Step1OTPView: View {
    @Environment(\.spotikPalette) var p
    @ObservedObject var vm: RegistrationViewModel

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 28) {
                Spacer().frame(height: 60)
                Image(systemName: "envelope.badge.fill")
                    .font(.system(size: 80))
                    .foregroundStyle(LinearGradient(
                        colors: [p.accent, p.navGlow],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ))
                    .frame(height: 120)
                    .symbolEffect(.bounce, options: .repeating)

                VStack(spacing: 8) {
                    Text("Подтверди email")
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundColor(p.textPrimary)
                    Text("Код отправлен на \(vm.email)")
                        .font(.system(size: 14))
                        .foregroundColor(p.textSecondary)
                        .lineLimit(2)
                        .multilineTextAlignment(.center)
                }

                GlassCard {
                    HStack(spacing: 12) {
                        Image(systemName: "lock.fill").foregroundColor(p.textMuted)
                        TextField("6-значный код", text: $vm.otp)
                            .keyboardType(.numberPad)
                            .onChange(of: vm.otp) { _, v in
                                if v.count > 6 { vm.otp = String(v.prefix(6)) }
                            }
                            .foregroundColor(p.textPrimary)
                            .tint(p.accent)
                    }
                    .padding(14)
                    .background(Color(hex: "1A1A2E"))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .padding(.horizontal, 24)

                // Resend
                Button {
                    if vm.resendCooldown == 0 { vm.sendCode() }
                } label: {
                    Text(vm.resendCooldown > 0
                         ? "Отправить повторно (\(vm.resendCooldown)с)"
                         : "Отправить повторно")
                        .font(.system(size: 14))
                        .foregroundColor(vm.resendCooldown > 0 ? p.textMuted : p.accent)
                }

                Spacer().frame(height: 120)
            }
        }
    }
}

// MARK: - Step 2 — City

private struct Step2CityView: View {
    @Environment(\.spotikPalette) var p
    @ObservedObject var vm: RegistrationViewModel

    var body: some View {
        VStack(spacing: 0) {
            Spacer().frame(height: 60)
            Image(systemName: "compass.drawing")
                .font(.system(size: 72))
                .foregroundStyle(LinearGradient(
                    colors: [p.accent, p.navGlow],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                ))
                .padding(.bottom, 16)

            VStack(spacing: 6) {
                Text("Выбери город")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundColor(p.textPrimary)
                Text("Мы покажем споты рядом с тобой")
                    .font(.system(size: 14)).foregroundColor(p.textSecondary)
            }
            .multilineTextAlignment(.center)
            .padding(.bottom, 20)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 8) {
                    ForEach(vm.cities, id: \.self) { city in
                        CityCardRow(
                            city: city,
                            isSelected: vm.selectedCity == city,
                            palette: p
                        ) {
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                                vm.selectedCity = city
                            }
                            UIImpactFeedbackGenerator(style: .light).impactOccurred()
                        }
                    }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 120)
            }
        }
    }
}

private struct CityCardRow: View {
    let city: String
    let isSelected: Bool
    let palette: SpotikPalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: "mappin.and.ellipse")
                    .foregroundColor(isSelected ? palette.accent : palette.textMuted)
                    .font(.system(size: 20))
                Text(city)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(palette.textPrimary)
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(palette.accent)
                        .font(.system(size: 22))
                        .transition(.scale.combined(with: .opacity))
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(isSelected ? palette.accent.opacity(0.18) : Color(hex: "14142A"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(isSelected ? palette.accent.opacity(0.5) : Color(hex: "1E1E3A"), lineWidth: 1)
                    )
            )
        }
        .buttonStyle(PeekPop())
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: isSelected)
    }
}

// MARK: - Step 3 — Nickname

private struct Step3NicknameView: View {
    @Environment(\.spotikPalette) var p
    @ObservedObject var vm: RegistrationViewModel

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 28) {
                Spacer().frame(height: 60)
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 80))
                    .foregroundStyle(LinearGradient(
                        colors: [p.accent, p.navGlow],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ))
                    .frame(height: 120)

                Text("Напиши никнейм")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundColor(p.textPrimary)

                GlassCard {
                    HStack(spacing: 12) {
                        Image(systemName: "person.fill").foregroundColor(p.textMuted)
                        TextField("Никнейм", text: $vm.name)
                            .autocapitalization(.words)
                            .foregroundColor(p.textPrimary)
                            .tint(p.accent)
                            .submitLabel(.done)
                    }
                    .padding(14)
                    .background(Color(hex: "1A1A2E"))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 120)
            }
        }
    }
}

// MARK: - Step 4 — Age

private struct Step4AgeView: View {
    @Environment(\.spotikPalette) var p
    @ObservedObject var vm: RegistrationViewModel

    var body: some View {
        VStack(spacing: 28) {
            Spacer().frame(height: 60)
            Image(systemName: "gift.fill")
                .font(.system(size: 80))
                .foregroundStyle(LinearGradient(
                    colors: [p.accent, p.navGlow],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                ))

            VStack(spacing: 6) {
                Text("Сколько тебе лет?")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundColor(p.textPrimary)
                Text("Мы подберём контент по возрасту")
                    .font(.system(size: 14)).foregroundColor(p.textSecondary)
            }

            AgeWheelPicker(selection: $vm.age, palette: p)
                .frame(height: 160)
                .padding(.horizontal, 40)

            Spacer()
        }
        .multilineTextAlignment(.center)
    }
}

// MARK: - Age Wheel Picker

private struct AgeWheelPicker: View {
    @Binding var selection: Int
    let palette: SpotikPalette
    let ages = Array(14...99)

    var body: some View {
        ZStack {
            // Accent lines
            VStack(spacing: 0) {
                Spacer()
                Rectangle().fill(palette.accent.opacity(0.4)).frame(height: 1)
                Spacer().frame(height: 44)
                Rectangle().fill(palette.accent.opacity(0.4)).frame(height: 1)
                Spacer()
            }

            Picker("", selection: $selection) {
                ForEach(ages, id: \.self) { age in
                    Text("\(age)")
                        .font(.system(size: 28, weight: .bold))
                        .tag(age)
                }
            }
            .pickerStyle(.wheel)
            .colorScheme(.dark)
        }
    }
}

// MARK: - Step 5 — Photo

private struct Step5PhotoView: View {
    @Environment(\.spotikPalette) var p
    @ObservedObject var vm: RegistrationViewModel
    @State private var showPicker = false

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 28) {
                Spacer().frame(height: 60)
                Image(systemName: "camera.fill")
                    .font(.system(size: 72))
                    .foregroundStyle(LinearGradient(
                        colors: [p.accent, p.navGlow],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ))

                Text("Добавь фото")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundColor(p.textPrimary)

                // Avatar circle
                Button { showPicker = true } label: {
                    ZStack {
                        Circle()
                            .fill(Color(hex: "1A1A2E"))
                            .frame(width: 170, height: 170)
                            .overlay(
                                Circle().stroke(
                                    LinearGradient(
                                        colors: [p.accent, p.calmMid],
                                        startPoint: .top, endPoint: .bottom
                                    ),
                                    lineWidth: 2
                                )
                            )

                        if let img = vm.avatarImage {
                            Image(uiImage: img)
                                .resizable()
                                .scaledToFill()
                                .frame(width: 170, height: 170)
                                .clipShape(Circle())
                        } else {
                            VStack(spacing: 8) {
                                Image(systemName: "photo.badge.plus")
                                    .font(.system(size: 40))
                                    .foregroundColor(p.textMuted)
                                Text("Нажми")
                                    .font(.system(size: 13))
                                    .foregroundColor(p.textMuted)
                            }
                        }
                    }
                }
                .buttonStyle(PeekPop(maxScale: 1.04))
                .photosPicker(isPresented: $showPicker,
                              selection: $vm.selectedPhotoItem,
                              matching: .images)
                .onChange(of: vm.selectedPhotoItem) { _, item in
                    Task {
                        if let data = try? await item?.loadTransferable(type: Data.self),
                           let ui = UIImage(data: data) {
                            vm.avatarImage = ui
                        }
                    }
                }

                // Change photo button
                if vm.avatarImage != nil {
                    Button { showPicker = true } label: {
                        Text("Изменить фото")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(p.textSecondary)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                            .background(
                                Capsule().fill(p.cardDark)
                                    .overlay(Capsule().stroke(p.borderLight, lineWidth: 0.5))
                            )
                    }
                    .buttonStyle(PeekPop())
                    .transition(.scale.combined(with: .opacity))
                }

                Spacer().frame(height: 120)
            }
        }
        .animation(.spring(response: 0.4, dampingFraction: 0.75), value: vm.avatarImage != nil)
    }
}
