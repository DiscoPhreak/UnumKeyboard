import UIKit

class KeyboardViewController: UIInputViewController, KeyboardViewDelegate, SuggestionBarDelegate {

    private var keyboardView: KeyboardView!
    private var suggestionBar: SuggestionBar!
    private var currentWord = ""
    private var previousWord = ""
    private var contextWords: [String] = []

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

        // Pipeline-driven prediction updates (Stage 1 + Stage 2 reranking)
        predictionBridge.onPredictionsUpdated = { [weak self] words in
            self?.suggestionBar.updateSuggestions(words)
        }
        predictionBridge.loadDictionary(bundle: Bundle(for: type(of: self)))
    }

    // MARK: - KeyboardViewDelegate

    func keyboardView(_ view: KeyboardView, didTapText text: String) {
        if text == " " {
            if !currentWord.isEmpty {
                let correction = predictionBridge.getAutoCorrection(currentWord)
                let committedWord: String

                if let correction = correction, correction.shouldAutoApply {
                    for _ in 0..<currentWord.count { textDocumentProxy.deleteBackward() }
                    textDocumentProxy.insertText(correction.corrected + " ")
                    lastAutoCorrection = (original: currentWord, corrected: correction.corrected)
                    committedWord = correction.corrected
                } else {
                    textDocumentProxy.insertText(" ")
                    lastAutoCorrection = nil
                    committedWord = currentWord
                }

                contextWords.append(committedWord)
                if contextWords.count > 5 { contextWords.removeFirst() }
                predictionBridge.onWordCommitted(committedWord)
                previousWord = currentWord
            } else {
                textDocumentProxy.insertText(" ")
                lastAutoCorrection = nil
            }
            currentWord = ""
        } else if text == "." || text == "!" || text == "?" {
            if !currentWord.isEmpty {
                let correction = predictionBridge.getAutoCorrection(currentWord)
                let committedWord: String

                if let correction = correction, correction.shouldAutoApply {
                    for _ in 0..<currentWord.count { textDocumentProxy.deleteBackward() }
                    textDocumentProxy.insertText(correction.corrected + text)
                    committedWord = correction.corrected
                } else {
                    textDocumentProxy.insertText(text)
                    committedWord = currentWord
                }

                predictionBridge.onWordCommitted(committedWord)
                previousWord = currentWord
            } else {
                textDocumentProxy.insertText(text)
            }
            currentWord = ""
            previousWord = ""
            contextWords = []
            lastAutoCorrection = nil
            predictionBridge.onSentenceBoundary()
        } else {
            textDocumentProxy.insertText(text)
            lastAutoCorrection = nil
            currentWord += text
            predictionBridge.onKeystroke(prefix: currentWord, context: contextWords)
        }
    }

    func keyboardViewDidTapDelete(_ view: KeyboardView) {
        if let undo = lastAutoCorrection {
            for _ in 0..<(undo.corrected.count + 1) {
                textDocumentProxy.deleteBackward()
            }
            textDocumentProxy.insertText(undo.original + " ")
            lastAutoCorrection = nil
            currentWord = ""
            predictionBridge.addToBlockList(undo.original)
            predictionBridge.onKeystroke(prefix: "", context: contextWords)
            return
        }

        textDocumentProxy.deleteBackward()
        if !currentWord.isEmpty {
            currentWord.removeLast()
        }
        predictionBridge.onKeystroke(prefix: currentWord, context: contextWords)
    }

    func keyboardViewDidTapEnter(_ view: KeyboardView) {
        if !currentWord.isEmpty {
            predictionBridge.onWordCommitted(currentWord)
        }
        textDocumentProxy.insertText("\n")
        currentWord = ""
        previousWord = ""
        contextWords = []
        predictionBridge.onSentenceBoundary()
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
        contextWords.append(word)
        if contextWords.count > 5 { contextWords.removeFirst() }
        predictionBridge.onWordCommitted(word)
        previousWord = word
        currentWord = ""
    }

    // MARK: - Lifecycle

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        predictionBridge.persistData()
        predictionBridge.destroy()
    }
}
