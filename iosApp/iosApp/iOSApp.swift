import SwiftUI
import GoogleSignIn

@main
struct iOSApp: App {
    private let authBridge = GoogleAuthBridge()

    init() {
        // Restore the Google session so silent token refresh works after relaunch.
        GIDSignIn.sharedInstance.restorePreviousSignIn { _, _ in }
    }

    var body: some Scene {
        WindowGroup {
            ContentView(authBridge: authBridge)
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
