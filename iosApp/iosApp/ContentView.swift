import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    let authBridge: GoogleAuthBridge

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(authBridge: authBridge)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    let authBridge: GoogleAuthBridge

    var body: some View {
        ComposeView(authBridge: authBridge)
            .ignoresSafeArea()
    }
}
