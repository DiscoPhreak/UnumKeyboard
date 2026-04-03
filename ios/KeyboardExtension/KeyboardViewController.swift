import UIKit

class KeyboardViewController: UIInputViewController, KeyboardViewDelegate, SuggestionBarDelegate {

    private var keyboardView: KeyboardView!
    private var suggestionBar: SuggestionBar!
    private var currentWord = ""
    private var previousWord = ""

    // Autocorrect undo state — stores the original and corrected word
    // so backspace immediately after auto-correction reverts to the original.
    private var lastAutoCorrection: (original: String, corrected: String)?

    // Prediction and autocorrect will be powered by the shared KMP framework
    // once the Xcode project links the XCFramework. Learning data and block
    // list will be persisted via UserDefaults once the shared KMP framework is linked.

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
        if text == " " {
            if !currentWord.isEmpty {
                // TODO: Once KMP framework is linked, call:
                //   let correction = predictionService.getAutoCorrection(currentWord)
                //   if correction.shouldAutoApply {
                //       for _ in 0..<currentWord.count { textDocumentProxy.deleteBackward() }
                //       textDocumentProxy.insertText(correction.corrected + " ")
                //       lastAutoCorrection = (original: currentWord, corrected: correction.corrected)
                //   } else {
                //       textDocumentProxy.insertText(" ")
                //       lastAutoCorrection = nil
                //   }
                textDocumentProxy.insertText(" ")
                lastAutoCorrection = nil
                previousWord = currentWord
            } else {
                textDocumentProxy.insertText(" ")
                lastAutoCorrection = nil
            }
            currentWord = ""
        } else if text == "." || text == "!" || text == "?" {
            // TODO: Same auto-correction pattern as space branch once KMP linked
            textDocumentProxy.insertText(text)
            if !currentWord.isEmpty {
                previousWord = currentWord
            }
            currentWord = ""
            previousWord = "" // sentence boundary
            lastAutoCorrection = nil
        } else {
            textDocumentProxy.insertText(text)
            lastAutoCorrection = nil
            currentWord += text
        }
    }

    func keyboardViewDidTapDelete(_ view: KeyboardView) {
        // Undo auto-correction: if backspace immediately follows an auto-correction,
        // revert to the original word
        if let undo = lastAutoCorrection {
            // Delete "corrected " (corrected word + space)
            for _ in 0..<(undo.corrected.count + 1) {
                textDocumentProxy.deleteBackward()
            }
            // Re-insert "original "
            textDocumentProxy.insertText(undo.original + " ")
            lastAutoCorrection = nil
            currentWord = ""
            // TODO: Once KMP linked, call predictionService.addToBlockList(undo.original)
            return
        }

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

    func keyboardView(_ view: KeyboardView, didMoveCursor offset: Int) {
        textDocumentProxy.adjustTextPosition(byCharacterOffset: offset)
    }

    func keyboardView(_ view: KeyboardView, didExtendSelection offset: Int) {
        // UITextDocumentProxy doesn't have direct selection extension API.
        // We use adjustTextPosition which moves the cursor — for selection,
        // apps would need the full UITextInput protocol. This is the best
        // approximation available in a keyboard extension.
        textDocumentProxy.adjustTextPosition(byCharacterOffset: offset)
    }

    // MARK: - SuggestionBarDelegate

    func suggestionBar(_ bar: SuggestionBar, didSelectSuggestion word: String) {
        // Delete current partial word and replace with selection
        for _ in 0..<currentWord.count {
            textDocumentProxy.deleteBackward()
        }
        textDocumentProxy.insertText(word + " ")
        previousWord = word
        currentWord = ""
        // Learning: suggestion accepted (will call shared KMP once linked)
    }
}
