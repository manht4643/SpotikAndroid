import Foundation
import Alamofire

// MARK: - Models

struct RegisterRequest: Encodable {
    let email: String
    let name: String
    let age: Int
    let city: String
    let avatarUrl: String?
    let bio: String?
}

struct LoginRequest: Encodable {
    let email: String
    let code: String
}

struct UpdateProfileRequest: Encodable {
    let name: String?
    let age: Int?
    let bio: String?
    let city: String?
    let avatarUrl: String?
}

struct AuthResponse: Decodable {
    let success: Bool
    let token: String?
    let userId: String?
    let message: String?
}

struct UserProfile: Decodable {
    let id: String
    let name: String
    let age: Int
    let city: String
    let bio: String?
    let avatarUrl: String?
    let email: String?
}

struct ApiResponse: Decodable {
    let success: Bool
    let message: String?
}

struct SendCodeRequest: Encodable { let email: String }
struct VerifyCodeRequest: Encodable { let email: String; let code: String }

struct TelegramInitResponse: Decodable {
    let success: Bool
    let botUsername: String?
    let nonce: String?
    let deepLink: String?
    let message: String?
}

struct TelegramPollResponse: Decodable {
    let success: Bool
    let confirmed: Bool
    let token: String?
    let userId: String?
    let message: String?
}

// MARK: - API Errors

enum APIError: LocalizedError {
    case serverError(String)
    case httpError(Int)
    case noNetwork
    case unknown

    var errorDescription: String? {
        switch self {
        case .serverError(let m): return m
        case .httpError(let code): return "HTTP \(code)"
        case .noNetwork: return "Сервер недоступен"
        case .unknown: return "Неизвестная ошибка"
        }
    }
}

// MARK: - APIService

final class APIService {
    static let shared = APIService()

    private let baseURL = "https://avacorebot.online"

    private lazy var session: Session = {
        let interceptor = AuthInterceptor()
        return Session(interceptor: interceptor)
    }()

    private init() {}

    // MARK: Auth

    func sendEmailCode(email: String) async throws -> ApiResponse {
        try await request(.post, "/api/email/send-code",
                          body: SendCodeRequest(email: email))
    }

    func verifyEmailCode(email: String, code: String) async throws -> ApiResponse {
        try await request(.post, "/api/email/verify-code",
                          body: VerifyCodeRequest(email: email, code: code))
    }

    func login(email: String, code: String) async throws -> AuthResponse {
        try await request(.post, "/api/login",
                          body: LoginRequest(email: email, code: code))
    }

    func register(_ body: RegisterRequest) async throws -> AuthResponse {
        try await request(.post, "/api/register", body: body)
    }

    func logout() async throws -> ApiResponse {
        try await request(.post, "/api/logout", body: Empty())
    }

    // MARK: Profile

    func getProfile() async throws -> UserProfile {
        try await request(.get, "/api/me", body: Empty?.none)
    }

    func updateProfile(_ body: UpdateProfileRequest) async throws -> ApiResponse {
        try await request(.put, "/api/me", body: body)
    }

    // MARK: Telegram

    func telegramInit() async throws -> TelegramInitResponse {
        try await request(.post, "/api/auth/telegram/init", body: Empty())
    }

    func telegramPoll(nonce: String) async throws -> TelegramPollResponse {
        try await rawRequest(.get, "/api/auth/telegram/poll?nonce=\(nonce)")
    }

    // MARK: Internal

    private func request<Req: Encodable, Res: Decodable>(
        _ method: HTTPMethod, _ path: String, body: Req?
    ) async throws -> Res {
        let url = baseURL + path
        let req: DataRequest
        if let b = body {
            req = session.request(url, method: method,
                                  parameters: b,
                                  encoder: JSONParameterEncoder.default)
        } else {
            req = session.request(url, method: method)
        }

        let response = await req
            .validate()
            .serializingDecodable(Res.self)
            .response

        switch response.result {
        case .success(let value):
            return value
        case .failure(let error):
            if let statusCode = response.response?.statusCode {
                throw APIError.httpError(statusCode)
            }
            if error.isSessionTaskError {
                throw APIError.noNetwork
            }
            throw APIError.unknown
        }
    }

    private func rawRequest<Res: Decodable>(_ method: HTTPMethod, _ path: String) async throws -> Res {
        return try await request(method, path, body: Empty?.none)
    }
}

// MARK: - Auth Interceptor (adds JWT header)

private struct AuthInterceptor: RequestInterceptor {
    func adapt(_ urlRequest: URLRequest, for session: Session,
               completion: @escaping (Result<URLRequest, Error>) -> Void) {
        var req = urlRequest
        if let token = AuthManager.shared.token, !token.isEmpty,
           !token.hasPrefix("offline_") {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        completion(.success(req))
    }
}

private struct Empty: Codable {}
