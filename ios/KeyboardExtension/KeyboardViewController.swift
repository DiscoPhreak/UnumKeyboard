import UIKit

class KeyboardViewController: UIInputViewController, KeyboardViewDelegate, SuggestionBarDelegate {

    private var keyboardView: KeyboardView!
    private var suggestionBar: SuggestionBar!
    private var currentWord = ""

    // Prediction will be powered by the shared KMP framework once the Xcode
    // project links the XCFramework. For now the suggestion bar UI is ready.

    override func viewDidLoad() {
        super.viewDidLoad()

        let container = UIStackView()
        container.axis = .vertical
        container.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(container)

        NSLayoutConstraint.activate([
            container.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            container.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            container.topAnchor.constraint(equalTo: view.topAnchor),
            container.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])

        suggestionBar = SuggestionBar(frame: .zero)
        suggestionBar.delegate = self
        suggestionBar.translatesAutoresizingMaskIntoConstraints = false
        container.addArrangedSubview(suggestionBar)
        suggestionBar.heightAnchor.constraint(equalToConstant: 44).isActive = true

        keyboardView = KeyboardView(frame: .zero)
        keyboardView.delegate = self
        keyboardView.translatesAutoresizingMaskIntoConstraints = false
        container.addArrangedSubview(keyboardView)
        keyboardView.heightAnchor.constraint(equalToConstant: 260).isActive = true
    }

    // MARK: - KeyboardViewDelegate

    func keyboardView(_ view: KeyboardView, didTapText text: String) {
        textDocumentProxy.insertText(text)

        if text == " " {
            currentWord = ""
        } else if text == "." || text == "!" || text == "?" {
            currentWord = ""
        } else {
            currentWord += text
        }
    }

    func keyboardViewDidTapDelete(_ view: KeyboardView) {
        textDocumentProxy.deleteBackward()
        if !currentWord.isEmpty {
            currentWord.removeLast()
        }
    }

    func keyboardViewDidTapEnter(_ view: KeyboardView) {
        textDocumentProxy.insertText("\n")
        currentWord = ""
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

    func keyboardView(_ view: KeyboardView, didGestureWord word: String, alternatives: [String]) {
        // Insert gesture word with trailing space
        textDocumentProxy.insertText(word + " ")
        currentWord = ""
        // Show alternatives in suggestion bar if available
        if !alternatives.isEmpty {
            suggestionBar.updateSuggestions(alternatives)
        }
    }

    // MARK: - SuggestionBarDelegate

    func suggestionBar(_ bar: SuggestionBar, didSelectSuggestion word: String) {
        // Delete current partial word and replace with selection
        for _ in 0..<currentWord.count {
            textDocumentProxy.deleteBackward()
        }
        textDocumentProxy.insertText(word + " ")
        currentWord = ""
    }
}
