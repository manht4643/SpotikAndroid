import SwiftUI

@MainActor
final class ProfileViewModel: ObservableObject {
    @Published var profile: UserProfile? = nil
    @Published var isLoading = false
    @Published var isEditing = false
    @Published var showLogoutDialog = false

    // Edit fields
    @Published var editName: String = ""
    @Published var editAge: Int = 18
    @Published var editBio: String = ""
    @Published var editCity: String = ""

    // Stats (mock for demo)
    @Published var likesCount: Int = 0
    @Published var viewsCount: Int = 0
    @Published var referralsCount: Int = 0

    // Activity
    @Published var activityData: [Double] = [0.3, 0.5, 0.8, 0.4, 0.9, 0.6, 0.7]

    func loadProfile() {
        // Use cached data first for instant display
        if let name = AuthManager.shared.userName {
            profile = UserProfile(
                id: AuthManager.shared.userId ?? "",
                name: name,
                age: AuthManager.shared.userAge,
                city: AuthManager.shared.userCity ?? "",
                bio: AuthManager.shared.userBio,
                avatarUrl: AuthManager.shared.userAvatar,
                email: AuthManager.shared.email
            )
            editName = name
            editAge = AuthManager.shared.userAge
            editBio = AuthManager.shared.userBio ?? ""
            editCity = AuthManager.shared.userCity ?? ""
        }

        // Then refresh from server
        Task {
            do {
                let p = try await APIService.shared.getProfile()
                profile = p
                editName = p.name
                editAge = p.age
                editBio = p.bio ?? ""
                editCity = p.city
                AuthManager.shared.saveProfile(
                    name: p.name, age: p.age, bio: p.bio,
                    city: p.city, avatarUrl: p.avatarUrl
                )
                // Mock stats — replace with real API when available
                likesCount   = Int.random(in: 120...980)
                viewsCount   = Int.random(in: 2000...12000)
                referralsCount = Int.random(in: 3...42)
            } catch {
                // Keep cached version
            }
        }
    }

    func saveEdits() {
        Task {
            let body = UpdateProfileRequest(
                name: editName.isEmpty ? nil : editName,
                age: editAge,
                bio: editBio.isEmpty ? nil : editBio,
                city: editCity.isEmpty ? nil : editCity,
                avatarUrl: nil
            )
            do {
                _ = try await APIService.shared.updateProfile(body)
                AuthManager.shared.saveProfile(
                    name: editName, age: editAge, bio: editBio,
                    city: editCity, avatarUrl: profile?.avatarUrl
                )
                profile = UserProfile(
                    id: profile?.id ?? "",
                    name: editName, age: editAge,
                    city: editCity, bio: editBio,
                    avatarUrl: profile?.avatarUrl,
                    email: profile?.email
                )
                isEditing = false
            } catch {
                // silent fail — local update already done
                isEditing = false
            }
        }
    }

    func logout(onLogout: @escaping () -> Void) {
        Task {
            _ = try? await APIService.shared.logout()
            AuthManager.shared.logout()
            onLogout()
        }
    }
}
