import SwiftUI
import Combine

// MARK: - AuthViewModel

@MainActor
final class AuthViewModel: ObservableObject {
    @Published var email: String = ""
    @Published var code: String = ""
    @Published var isCodeSent = false
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    @Published var resendCooldown: Int = 0

    private var cooldownTimer: AnyCancellable?

    var isEmailValid: Bool {
        let regex = #"^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"#
        return email.range(of: regex, options: .regularExpression) != nil
    }

    var canLogin: Bool { code.count == 6 && !isLoading }

    // MARK: Actions

    func sendCode() {
        guard isEmailValid else { return }
        isLoading = true
        clearError()
        Task {
            do {
                _ = try await APIService.shared.sendEmailCode(email: email)
                isCodeSent = true
                startResendCooldown(seconds: 30)
            } catch {
                showError(error.localizedDescription)
            }
            isLoading = false
        }
    }

    func login(onSuccess: @escaping () -> Void) {
        guard canLogin else { return }
        isLoading = true
        clearError()
        Task {
            do {
                let res = try await APIService.shared.login(email: email, code: code)
                if res.success, let token = res.token {
                    AuthManager.shared.saveSession(
                        token: token,
                        userId: res.userId ?? "",
                        email: email
                    )
                    onSuccess()
                } else {
                    showError(res.message ?? "Неверный код")
                }
            } catch let err as APIError {
                showError(err.localizedDescription)
            } catch {
                showError("Сервер недоступен")
            }
            isLoading = false
        }
    }

    // MARK: Telegram

    @Published var telegramNonce: String? = nil
    @Published var isTelegramWaiting = false
    private var telegramPollTask: Task<Void, Never>?

    func telegramInit(onSuccess: @escaping () -> Void) {
        isLoading = true
        Task {
            do {
                let res = try await APIService.shared.telegramInit()
                if let link = res.deepLink {
                    await UIApplication.shared.open(URL(string: link)!)
                    telegramNonce = res.nonce
                    isTelegramWaiting = true
                    startTelegramPoll(onSuccess: onSuccess)
                }
            } catch {
                showError("Не удалось запустить Telegram-вход")
            }
            isLoading = false
        }
    }

    private func startTelegramPoll(onSuccess: @escaping () -> Void) {
        guard let nonce = telegramNonce else { return }
        telegramPollTask?.cancel()
        telegramPollTask = Task {
            for _ in 0..<60 {
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                guard !Task.isCancelled else { return }
                if let res = try? await APIService.shared.telegramPoll(nonce: nonce),
                   res.confirmed, let token = res.token {
                    AuthManager.shared.saveSession(token: token, userId: res.userId ?? "", email: nil)
                    await MainActor.run { onSuccess() }
                    return
                }
            }
            await MainActor.run {
                isTelegramWaiting = false
                showError("Время ожидания вышло")
            }
        }
    }

    // MARK: Helpers

    private func startResendCooldown(seconds: Int) {
        resendCooldown = seconds
        cooldownTimer = Timer.publish(every: 1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self else { return }
                if resendCooldown > 0 {
                    resendCooldown -= 1
                } else {
                    cooldownTimer?.cancel()
                }
            }
    }

    private func showError(_ msg: String) {
        errorMessage = msg
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) { [weak self] in
            if self?.errorMessage == msg { self?.errorMessage = nil }
        }
    }

    private func clearError() { errorMessage = nil }
}
