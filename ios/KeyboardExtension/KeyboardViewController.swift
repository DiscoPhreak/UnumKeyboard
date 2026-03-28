import UIKit

class KeyboardViewController: UIInputViewController, KeyboardViewDelegate {

    private var keyboardView: KeyboardView!

    override func viewDidLoad() {
        super.viewDidLoad()

        keyboardView = KeyboardView(frame: .zero)
        keyboardView.delegate = self
        keyboardView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(keyboardView)

        NSLayoutConstraint.activate([
            keyboardView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            keyboardView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            keyboardView.topAnchor.constraint(equalTo: view.topAnchor),
            keyboardView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            keyboardView.heightAnchor.constraint(equalToConstant: 260)
        ])
    }

    // MARK: - KeyboardViewDelegate

    func keyboardView(_ view: KeyboardView, didTapText text: String) {
        textDocumentProxy.insertText(text)
    }

    func keyboardViewDidTapDelete(_ view: KeyboardView) {
        textDocumentProxy.deleteBackward()
    }

    func keyboardViewDidTapEnter(_ view: KeyboardView) {
        textDocumentProxy.insertText("\n")
    }

    func keyboardViewDidTapShift(_ view: KeyboardView) {
        switch keyboardView.shiftState {
        case .off:
            keyboardView.shiftState = .on
        case .on:
            keyboardView.shiftState = .capsLock
        case .capsLock:
            keyboardView.shiftState = .off
        }
    }

    func keyboardViewDidTapSymbolToggle(_ view: KeyboardView, keyId: String) {
        switch keyId {
        case "symbol_toggle":
            keyboardView.layoutMode = .symbols
        case "abc_toggle":
            keyboardView.layoutMode = .letters
        case "symbol_toggle_2":
            keyboardView.layoutMode = .symbols2
        case "symbol_toggle_1":
            keyboardView.layoutMode = .symbols
        default:
            break
        }
    }
}
