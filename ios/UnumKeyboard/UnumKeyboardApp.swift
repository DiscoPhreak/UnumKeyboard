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
        ZStack {
            Color.black.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    Spacer().frame(height: 40)

                    Text("Unum Keyboard")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.white)

                    Text("Set up your keyboard")
                        .font(.system(size: 16))
                        .foregroundColor(.gray)

                    SetupStepView(
                        number: "1",
                        title: "Open Settings",
                        description: "Go to Settings → General → Keyboard → Keyboards"
                    )

                    SetupStepView(
                        number: "2",
                        title: "Add New Keyboard",
                        description: "Tap \"Add New Keyboard...\" and select Unum Keyboard"
                    )

                    SetupStepView(
                        number: "3",
                        title: "Allow Full Access",
                        description: "Tap Unum Keyboard → Enable \"Allow Full Access\" for haptic feedback"
                    )

                    SetupStepView(
                        number: "4",
                        title: "Switch Keyboard",
                        description: "In any text field, tap the globe 🌐 icon to switch to Unum Keyboard"
                    )

                    Button(action: {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }) {
                        Text("Open Settings")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.white)
                            .cornerRadius(12)
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 8)

                    Spacer()
                }
                .padding(.horizontal, 24)
            }
        }
        .preferredColorScheme(.dark)
    }
}

struct SetupStepView: View {
    let number: String
    let title: String
    let description: String

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            Text(number)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(.black)
                .frame(width: 32, height: 32)
                .background(Color.white)
                .cornerRadius(16)

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.white)

                Text(description)
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
            }

            Spacer()
        }
        .padding(16)
        .background(Color(white: 0.10))
        .cornerRadius(12)
    }
}
