import SwiftUI

// Root navigation view wrapping auth/registration flow
struct RootNavigationView: View {
    @EnvironmentObject var authManager: AuthManager

    var body: some View {
        NavigationView {
            AuthView()
                .navigationBarHidden(true)
        }
        .navigationViewStyle(.stack)
        .tint(Color(hex: "0A84FF")) // global accent for NavigationLink chevrons, back buttons
    }
}
