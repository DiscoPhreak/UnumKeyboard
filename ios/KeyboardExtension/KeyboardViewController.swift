import UIKit

class KeyboardViewController: UIInputViewController, KeyboardViewDelegate, SuggestionBarDelegate {

    private var keyboardView: KeyboardView!
    private var suggestionBar: SuggestionBar!
    private var currentWord = ""
    private var previousWord = ""

    private let predictionBridge = PredictionBridge()

    // Autocorrect undo state
    private var lastAutoCorrection: (original: String, corrected: String)?

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

        predictionBridge.loadDictionary(bundle: Bundle(for: type(of: self)))
    }

    // MARK: - Prediction updates

    private func updatePredictions() {
        guard predictionBridge.isLoaded else { return }
        let suggestions = predictionBridge.predict()
        suggestionBar.updateSuggestions(suggestions)
    }

    // MARK: - KeyboardViewDelegate

    func keyboardView(_ view: KeyboardView, didTapText text: String) {
        if text == " " {
            if !currentWord.isEmpty {
                let correction = predictionBridge.getAutoCorrection(currentWord)
                if let correction = correction, correction.shouldAutoApply {
                    for _ in 0..<currentWord.count { textDocumentProxy.deleteBackward() }
                    textDocumentProxy.insertText(correction.corrected + " ")
                    lastAutoCorrection = (original: currentWord, corrected: correction.corrected)
                    predictionBridge.commitWord(correction.corrected)
                } else {
                    textDocumentProxy.insertText(" ")
                    lastAutoCorrection = nil
                    predictionBridge.commitWord(currentWord)
                }
                previousWord = currentWord
            } else {
                textDocumentProxy.insertText(" ")
                lastAutoCorrection = nil
            }
            currentWord = ""
            predictionBridge.updatePrefix("")
            updatePredictions()
        } else if text == "." || text == "!" || text == "?" {
            if !currentWord.isEmpty {
                let correction = predictionBridge.getAutoCorrection(currentWord)
                if let correction = correction, correction.shouldAutoApply {
                    for _ in 0..<currentWord.count { textDocumentProxy.deleteBackward() }
                    textDocumentProxy.insertText(correction.corrected + text)
                    predictionBridge.commitWord(correction.corrected)
                } else {
                    textDocumentProxy.insertText(text)
                    predictionBridge.commitWord(currentWord)
                }
                previousWord = currentWord
            } else {
                textDocumentProxy.insertText(text)
            }
            currentWord = ""
            previousWord = ""
            lastAutoCorrection = nil
            predictionBridge.resetContext()
            predictionBridge.updatePrefix("")
            suggestionBar.clear()
        } else {
            textDocumentProxy.insertText(text)
            lastAutoCorrection = nil
            currentWord += text
            predictionBridge.updatePrefix(currentWord)
            updatePredictions()
        }
    }

    func keyboardViewDidTapDelete(_ view: KeyboardView) {
        // Undo auto-correction on immediate backspace
        if let undo = lastAutoCorrection {
            for _ in 0..<(undo.corrected.count + 1) {
                textDocumentProxy.deleteBackward()
            }
            textDocumentProxy.insertText(undo.original + " ")
            lastAutoCorrection = nil
            currentWord = ""
            predictionBridge.addToBlockList(undo.original)
            predictionBridge.updatePrefix("")
            updatePredictions()
            return
        }

        textDocumentProxy.deleteBackward()
        if !currentWord.isEmpty {
            currentWord.removeLast()
        }
        predictionBridge.updatePrefix(currentWord)
        updatePredictions()
    }

    func keyboardViewDidTapEnter(_ view: KeyboardView) {
        if !currentWord.isEmpty {
            predictionBridge.commitWord(currentWord)
        }
        textDocumentProxy.insertText("\n")
        currentWord = ""
        previousWord = ""
        predictionBridge.resetContext()
        predictionBridge.updatePrefix("")
        suggestionBar.clear()
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
        textDocumentProxy.adjustTextPosition(byCharacterOffset: offset)
    }

    // MARK: - SuggestionBarDelegate

    func suggestionBar(_ bar: SuggestionBar, didSelectSuggestion word: String) {
        for _ in 0..<currentWord.count {
            textDocumentProxy.deleteBackward()
        }
        textDocumentProxy.insertText(word + " ")
        predictionBridge.commitWord(word)
        previousWord = word
        currentWord = ""
        predictionBridge.updatePrefix("")
        updatePredictions()
    }

    // MARK: - Lifecycle

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        predictionBridge.persistData()
    }
}
