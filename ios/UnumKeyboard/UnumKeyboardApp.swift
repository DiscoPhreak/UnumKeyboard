import SwiftUI

@main
struct UnumKeyboardApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    var body: some View {
        VStack(spacing: 20) {
            Text("Unum Keyboard")
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("To get started, enable Unum Keyboard in Settings:")
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            VStack(alignment: .leading, spacing: 8) {
                Text("1. Open Settings")
                Text("2. Go to General > Keyboard > Keyboards")
                Text("3. Tap Add New Keyboard...")
                Text("4. Select Unum Keyboard")
                Text("5. Tap Unum Keyboard > Allow Full Access")
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(12)
        }
        .padding()
    }
}
