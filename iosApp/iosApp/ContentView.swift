import SwiftUI
import shared

struct ContentView: View {

    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        // Create the Compose UI controller from shared module
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Updates handled by Compose
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
