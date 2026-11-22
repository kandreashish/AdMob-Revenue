import Foundation
import UIKit
import GoogleSignIn
import ComposeApp

/// Bridges GoogleSignIn-iOS to the shared Kotlin `IosAuthBridge` interface.
final class GoogleAuthBridge: NSObject, IosAuthBridge {

    static let admobScopes = [
        "https://www.googleapis.com/auth/admob.readonly",
        "https://www.googleapis.com/auth/admob.report"
    ]

    // MARK: Sign in

    func signIn(
        onSuccess: @escaping (String, String, String, String?) -> Void,
        onError: @escaping (String) -> Void
    ) {
        DispatchQueue.main.async {
            guard let presenter = Self.topViewController() else {
                onError("Unable to present Google Sign-In.")
                return
            }
            GIDSignIn.sharedInstance.signIn(
                withPresenting: presenter,
                hint: nil,
                additionalScopes: Self.admobScopes
            ) { result, error in
                if let error = error as NSError? {
                    if error.code == GIDSignInError.canceled.rawValue {
                        onError("Google Sign-In was cancelled.")
                    } else {
                        onError("Google Sign-In failed: \(error.localizedDescription)")
                    }
                    return
                }
                guard let user = result?.user else {
                    onError("Google Sign-In returned no user.")
                    return
                }
                let granted = user.grantedScopes ?? []
                guard Self.admobScopes.allSatisfy(granted.contains) else {
                    onError("AdMob access was not granted. Please allow AdMob permissions.")
                    return
                }
                onSuccess(
                    user.profile?.name ?? "AdMob Publisher",
                    user.profile?.email ?? "",
                    user.accessToken.tokenString,
                    user.profile?.imageURL(withDimension: 200)?.absoluteString
                )
            }
        }
    }

    // MARK: Silent refresh (called by the Kotlin repository on 401 / expiry)

    func refreshToken(onResult: @escaping (String?) -> Void) {
        DispatchQueue.main.async {
            let refresh: (GIDGoogleUser) -> Void = { user in
                user.refreshTokensIfNeeded { refreshed, _ in
                    onResult(refreshed?.accessToken.tokenString)
                }
            }
            if let user = GIDSignIn.sharedInstance.currentUser {
                refresh(user)
            } else {
                GIDSignIn.sharedInstance.restorePreviousSignIn { user, _ in
                    if let user { refresh(user) } else { onResult(nil) }
                }
            }
        }
    }

    // MARK: Sign out

    func signOut() {
        DispatchQueue.main.async {
            GIDSignIn.sharedInstance.signOut()
        }
    }

    // MARK: Helpers

    private static func topViewController() -> UIViewController? {
        let root = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?
            .rootViewController
        var top = root
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}
