import Foundation
import Combine

// MARK: - AuthManager

final class AuthManager: ObservableObject {
    static let shared = AuthManager()

    private let defaults = UserDefaults.standard
    private enum Key {
        static let token     = "spotik_token"
        static let userId    = "spotik_userId"
        static let email     = "spotik_email"
        static let name      = "spotik_name"
        static let age       = "spotik_age"
        static let bio       = "spotik_bio"
        static let city      = "spotik_city"
        static let avatar    = "spotik_avatar"
        static let hasProfile = "spotik_hasProfile"
        static let pendingSync = "spotik_pendingSync"
    }

    @Published private(set) var isAuthenticated: Bool = false

    var token: String? {
        get { defaults.string(forKey: Key.token) }
        set {
            defaults.set(newValue, forKey: Key.token)
            isAuthenticated = newValue != nil && !(newValue?.isEmpty ?? true)
        }
    }

    var userId: String? {
        get { defaults.string(forKey: Key.userId) }
        set { defaults.set(newValue, forKey: Key.userId) }
    }

    var email: String? {
        get { defaults.string(forKey: Key.email) }
        set { defaults.set(newValue, forKey: Key.email) }
    }

    var userName: String? {
        get { defaults.string(forKey: Key.name) }
        set { defaults.set(newValue, forKey: Key.name) }
    }

    var userAge: Int {
        get { defaults.integer(forKey: Key.age) == 0 ? 18 : defaults.integer(forKey: Key.age) }
        set { defaults.set(newValue, forKey: Key.age) }
    }

    var userBio: String? {
        get { defaults.string(forKey: Key.bio) }
        set { defaults.set(newValue, forKey: Key.bio) }
    }

    var userCity: String? {
        get { defaults.string(forKey: Key.city) }
        set { defaults.set(newValue, forKey: Key.city) }
    }

    var userAvatar: String? {
        get { defaults.string(forKey: Key.avatar) }
        set { defaults.set(newValue, forKey: Key.avatar) }
    }

    var hasProfile: Bool {
        get { defaults.bool(forKey: Key.hasProfile) }
        set { defaults.set(newValue, forKey: Key.hasProfile) }
    }

    var hasPendingSync: Bool {
        defaults.bool(forKey: Key.pendingSync)
    }

    private init() {
        isAuthenticated = !(token?.isEmpty ?? true) && token != nil
    }

    func saveSession(token: String, userId: String, email: String?) {
        self.token  = token
        self.userId = userId
        self.email  = email
    }

    func saveProfile(name: String, age: Int, bio: String?, city: String?, avatarUrl: String?) {
        userName   = name
        userAge    = age
        userBio    = bio
        userCity   = city
        userAvatar = avatarUrl
        hasProfile = true
    }

    func saveOfflineRegistration(email: String, name: String, age: Int,
                                  bio: String?, city: String?, avatarUrl: String?) {
        let offlineToken = "offline_\(Date().timeIntervalSince1970)"
        saveSession(token: offlineToken, userId: "local_user", email: email)
        saveProfile(name: name, age: age, bio: bio, city: city, avatarUrl: avatarUrl)
        defaults.set(true, forKey: Key.pendingSync)
    }

    func clearPendingSync() {
        defaults.removeObject(forKey: Key.pendingSync)
    }

    func logout() {
        [Key.token, Key.userId, Key.email, Key.name, Key.bio,
         Key.city, Key.avatar, Key.hasProfile, Key.pendingSync].forEach {
            defaults.removeObject(forKey: $0)
        }
        defaults.set(0, forKey: Key.age)
        isAuthenticated = false
    }
}
