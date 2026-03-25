import SwiftUI

struct AuthView: View {
    @Environment(\.spotikPalette) var p
    @StateObject private var vm = AuthViewModel()
    @State private var appear = false

    // Callback to push to registration
    var onCreateAccount: (() -> Void)? = nil

    var body: some View {
        ZStack {
            MeshGradientBackground()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    Spacer().frame(height: 80)

                    // Logo
                    VStack(spacing: 8) {
                        ZStack {
                            Circle()
                                .fill(LinearGradient(
                                    colors: [p.accent, p.accent.opacity(0.6)],
                                    startPoint: .topLeading, endPoint: .bottomTrailing))
                                .frame(width: 72, height: 72)
                                .shadow(color: p.accent.opacity(0.5), radius: 20)

                            Image(systemName: "location.fill")
                                .font(.system(size: 30, weight: .bold))
                                .foregroundColor(.white)
                        }

                        Text("Spotik")
                            .font(.system(size: 32, weight: .bold, design: .rounded))
                            .foregroundColor(p.textPrimary)
                    }
                    .opacity(appear ? 1 : 0)
                    .offset(y: appear ? 0 : -20)
                    .animation(.spring(response: 0.6, dampingFraction: 0.8).delay(0.1), value: appear)

                    Spacer().frame(height: 40)

                    // Glass card form
                    GlassCard {
                        VStack(spacing: 16) {
                            // Email field
                            HStack(spacing: 12) {
                                Image(systemName: "envelope.fill")
                                    .foregroundColor(p.textMuted)
                                    .frame(width: 20)
                                TextField("Email", text: $vm.email)
                                    .keyboardType(.emailAddress)
                                    .autocapitalization(.none)
                                    .foregroundColor(p.textPrimary)
                                    .tint(p.accent)
                            }
                            .padding(14)
                            .background(Color(hex: "1A1A2E"))
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .stroke(vm.isEmailValid ? p.accent.opacity(0.6) : p.borderDark, lineWidth: 1)
                            )

                            // OTP field
                            if vm.isCodeSent {
                                HStack(spacing: 12) {
                                    Image(systemName: "lock.fill")
                                        .foregroundColor(p.textMuted)
                                        .frame(width: 20)
                                    TextField("6-значный код", text: $vm.code)
                                        .keyboardType(.numberPad)
                                        .onChange(of: vm.code) { _, v in
                                            if v.count > 6 { vm.code = String(v.prefix(6)) }
                                        }
                                        .foregroundColor(p.textPrimary)
                                        .tint(p.accent)
                                }
                                .padding(14)
                                .background(Color(hex: "1A1A2E"))
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                                        .stroke(p.borderDark, lineWidth: 1)
                                )
                                .transition(.asymmetric(
                                    insertion: .move(edge: .top).combined(with: .opacity),
                                    removal: .opacity
                                ))
                            }
                        }
                    }
                    .padding(.horizontal, 24)
                    .opacity(appear ? 1 : 0)
                    .offset(y: appear ? 0 : 20)
                    .animation(.spring(response: 0.6, dampingFraction: 0.8).delay(0.2), value: appear)

                    Spacer().frame(height: 12)

                    // Error
                    if let err = vm.errorMessage {
                        ErrorToast(message: err)
                            .padding(.horizontal, 24)
                    }

                    Spacer().frame(height: 16)

                    // Action button
                    AccentGradientButton(
                        title: vm.isCodeSent ? "ВОЙТИ" : "ПОЛУЧИТЬ КОД",
                        isLoading: vm.isLoading,
                        isEnabled: vm.isCodeSent ? vm.canLogin : vm.isEmailValid
                    ) {
                        if vm.isCodeSent {
                            vm.login {
                                // Auth state change handled by AuthManager @Published
                            }
                        } else {
                            vm.sendCode()
                        }
                    }
                    .padding(.horizontal, 24)
                    .opacity(appear ? 1 : 0)
                    .animation(.spring(response: 0.6, dampingFraction: 0.8).delay(0.3), value: appear)

                    // Resend
                    if vm.isCodeSent {
                        Button {
                            if vm.resendCooldown == 0 { vm.sendCode() }
                        } label: {
                            Text(vm.resendCooldown > 0
                                 ? "Отправить повторно (\(vm.resendCooldown)с)"
                                 : "Отправить повторно")
                                .font(.system(size: 14))
                                .foregroundColor(vm.resendCooldown > 0 ? p.textMuted : p.accent)
                        }
                        .padding(.top, 12)
                        .transition(.opacity)
                    }

                    Spacer().frame(height: 24)

                    // Divider
                    HStack {
                        Rectangle().fill(p.borderDark).frame(height: 1)
                        Text("или").font(.system(size: 13)).foregroundColor(p.textMuted)
                            .padding(.horizontal, 12)
                        Rectangle().fill(p.borderDark).frame(height: 1)
                    }
                    .padding(.horizontal, 24)

                    Spacer().frame(height: 16)

                    // Telegram button
                    Button {
                        vm.telegramInit { /* handled by AuthManager */ }
                    } label: {
                        HStack(spacing: 10) {
                            Image(systemName: "paperplane.fill")
                                .foregroundColor(.white)
                            Text(vm.isTelegramWaiting ? "Ожидание..." : "Войти через Telegram")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(.white)
                            if vm.isTelegramWaiting {
                                ProgressView().tint(.white).scaleEffect(0.8)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color(hex: "2AABEE"))
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(PeekPop())
                    .padding(.horizontal, 24)

                    Spacer().frame(height: 24)

                    // Register link
                    HStack(spacing: 4) {
                        Text("Нет аккаунта?")
                            .foregroundColor(p.textSecondary)
                            .font(.system(size: 14))
                        NavigationLink(destination: RegistrationView()) {
                            Text("Создать")
                                .foregroundColor(p.accent)
                                .font(.system(size: 14, weight: .semibold))
                        }
                    }

                    Spacer().frame(height: 40)
                }
            }
        }
        .navigationBarHidden(true)
        .onAppear { withAnimation { appear = true } }
        .animation(.default, value: vm.isCodeSent)
    }
}
