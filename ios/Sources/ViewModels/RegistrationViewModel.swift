import SwiftUI
import PhotosUI
import Combine

// MARK: - RegistrationViewModel

@MainActor
final class RegistrationViewModel: ObservableObject {
    // Step 0 — Email
    @Published var email: String = ""

    // Step 1 — OTP
    @Published var otp: String = ""
    @Published var isEmailVerified = false
    @Published var isVerifying = false
    @Published var resendCooldown: Int = 0

    // Step 2 — City
    @Published var selectedCity: String? = nil

    // Step 3 — Nickname
    @Published var name: String = ""

    // Step 4 — Age
    @Published var age: Int = 20

    // Step 5 — Photo
    @Published var avatarImage: UIImage? = nil
    @Published var selectedPhotoItem: PhotosPickerItem? = nil

    // General
    @Published var currentStep: Int = 0
    @Published var isSubmitting = false
    @Published var errorMessage: String? = nil

    private var cooldownTimer: AnyCancellable?

    let cities = [
        "Альметьевск", "Казань", "Набережные Челны", "Нижнекамск",
        "Елабуга", "Лениногорск", "Бугульма", "Азнакаево",
        "Заинск", "Чистополь", "Бавлы", "Сарманово"
    ]

    var isEmailValid: Bool {
        let regex = #"^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"#
        return email.range(of: regex, options: .regularExpression) != nil
    }

    var canProceed: Bool {
        switch currentStep {
        case 0: return isEmailValid
        case 1: return isEmailVerified
        case 2: return selectedCity != nil
        case 3: return !name.trimmingCharacters(in: .whitespaces).isEmpty
        case 4: return true
        case 5: return true
        default: return false
        }
    }

    // MARK: OTP

    func sendCode() {
        Task {
            do {
                _ = try await APIService.shared.sendEmailCode(email: email)
                startResendCooldown()
            } catch { showError("Не удалось отправить код") }
        }
    }

    func verifyCode() {
        guard otp.count == 6 else { return }
        isVerifying = true
        Task {
            do {
                let res = try await APIService.shared.verifyEmailCode(email: email, code: otp)
                if res.success {
                    isEmailVerified = true
                    advance()
                } else {
                    showError(res.message ?? "Неверный код")
                }
            } catch {
                showError("Ошибка верификации")
            }
            isVerifying = false
        }
    }

    // MARK: Navigation

    func advance() {
        guard currentStep < 5 else { return }
        withAnimation(.easeInOut(duration: 0.3)) { currentStep += 1 }
    }

    func back() {
        guard currentStep > 0 else { return }
        withAnimation(.easeInOut(duration: 0.25)) { currentStep -= 1 }
    }

    // MARK: Submit

    func submit(onSuccess: @escaping () -> Void) {
        isSubmitting = true
        Task {
            let avatarUrl: String? = nil // TODO: upload image, get URL

            let body = RegisterRequest(
                email: email,
                name: name,
                age: age,
                city: selectedCity ?? "",
                avatarUrl: avatarUrl,
                bio: nil
            )

            do {
                let res = try await APIService.shared.register(body)
                if res.success, let token = res.token {
                    AuthManager.shared.saveSession(token: token, userId: res.userId ?? "", email: email)
                    AuthManager.shared.saveProfile(name: name, age: age, bio: nil,
                                                   city: selectedCity, avatarUrl: avatarUrl)
                    onSuccess()
                } else {
                    showError(res.message ?? "Ошибка регистрации")
                }
            } catch APIError.httpError(409) {
                showError("Аккаунт с этим email уже существует")
            } catch APIError.httpError(400) {
                showError("Проверьте введённые данные")
            } catch {
                // Offline mode
                AuthManager.shared.saveOfflineRegistration(
                    email: email, name: name, age: age,
                    bio: nil, city: selectedCity, avatarUrl: avatarUrl
                )
                onSuccess()
            }
            isSubmitting = false
        }
    }

    // MARK: Helpers

    private func startResendCooldown() {
        resendCooldown = 60
        cooldownTimer?.cancel()
        cooldownTimer = Timer.publish(every: 1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self else { return }
                if resendCooldown > 0 { resendCooldown -= 1 }
                else { cooldownTimer?.cancel() }
            }
    }

    func showError(_ msg: String) {
        errorMessage = msg
    }

    func clearError() { errorMessage = nil }
}
