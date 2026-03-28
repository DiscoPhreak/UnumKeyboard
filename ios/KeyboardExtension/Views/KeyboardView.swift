import UIKit

protocol KeyboardViewDelegate: AnyObject {
    func keyboardView(_ view: KeyboardView, didTapText text: String)
    func keyboardViewDidTapDelete(_ view: KeyboardView)
    func keyboardViewDidTapEnter(_ view: KeyboardView)
    func keyboardViewDidTapShift(_ view: KeyboardView)
    func keyboardViewDidTapSymbolToggle(_ view: KeyboardView, keyId: String)
}

class KeyboardView: UIView {

    weak var delegate: KeyboardViewDelegate?

    // AMOLED dark theme
    private let bgColor = UIColor.black
    private let keyBgColor = UIColor(white: 0.10, alpha: 1.0)
    private let keyPressedColor = UIColor(white: 0.20, alpha: 1.0)
    private let specialKeyBgColor = UIColor(white: 0.16, alpha: 1.0)
    private let textColor = UIColor.white
    private let specialTextColor = UIColor(white: 0.73, alpha: 1.0)

    private let keyCornerRadius: CGFloat = 6
    private let keySpacing: CGFloat = 4
    private let horizontalPadding: CGFloat = 3
    private let verticalPadding: CGFloat = 6

    // Layout state
    enum ShiftState { case off, on, capsLock }
    enum LayoutMode { case letters, symbols, symbols2 }

    var shiftState: ShiftState = .off { didSet { setNeedsLayout(); setNeedsDisplay() } }
    var layoutMode: LayoutMode = .letters { didSet { setNeedsLayout(); setNeedsDisplay() } }

    private var keyButtons: [KeyButton] = []

    // Backspace repeat
    private var backspaceTimer: Timer?
    private var backspaceDelayTimer: Timer?

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = bgColor
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        backgroundColor = bgColor
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        rebuildKeys()
    }

    private func currentLayout() -> [[(id: String, label: String, width: CGFloat, type: KeyKind)]] {
        switch layoutMode {
        case .letters:
            let isUpper = shiftState != .off
            let row1 = "qwertyuiop".map { c -> (String, String, CGFloat, KeyKind) in
                let ch = isUpper ? String(c).uppercased() : String(c)
                return (ch, ch, 1.0, .character)
            }
            let row2 = "asdfghjkl".map { c -> (String, String, CGFloat, KeyKind) in
                let ch = isUpper ? String(c).uppercased() : String(c)
                return (ch, ch, 1.0, .character)
            }
            let shiftLabel: String = {
                switch shiftState {
                case .off: return "⇧"
                case .on: return "⬆"
                case .capsLock: return "⇪"
                }
            }()
            var row3: [(String, String, CGFloat, KeyKind)] = [("shift", shiftLabel, 1.5, .shift)]
            row3 += "zxcvbnm".map { c -> (String, String, CGFloat, KeyKind) in
                let ch = isUpper ? String(c).uppercased() : String(c)
                return (ch, ch, 1.0, .character)
            }
            row3.append(("backspace", "⌫", 1.5, .backspace))
            let row4: [(String, String, CGFloat, KeyKind)] = [
                ("symbol_toggle", "?123", 1.5, .symbolToggle),
                ("comma", ",", 1.0, .character),
                ("space", " ", 5.0, .space),
                ("period", ".", 1.0, .character),
                ("enter", "↵", 1.5, .enter)
            ]
            return [row1, row2, row3, row4]

        case .symbols:
            let row1: [(String, String, CGFloat, KeyKind)] = "1234567890".map { ($0.description, $0.description, 1.0, .character) }
            let row2: [(String, String, CGFloat, KeyKind)] = [
                ("@","@",1,.character), ("#","#",1,.character), ("$","$",1,.character),
                ("_","_",1,.character), ("&","&",1,.character), ("-","-",1,.character),
                ("+","+",1,.character), ("(","(",1,.character), (")",")",1,.character)
            ]
            let row3: [(String, String, CGFloat, KeyKind)] = [
                ("symbol_toggle_2", "#+=", 1.5, .symbolToggle),
                ("*","*",1,.character), ("\"","\"",1,.character), ("'","'",1,.character),
                (":",":",1,.character), (";",";",1,.character), ("!","!",1,.character),
                ("?","?",1,.character),
                ("backspace", "⌫", 1.5, .backspace)
            ]
            let row4: [(String, String, CGFloat, KeyKind)] = [
                ("abc_toggle", "ABC", 1.5, .symbolToggle),
                ("comma", ",", 1.0, .character),
                ("space", " ", 5.0, .space),
                ("period", ".", 1.0, .character),
                ("enter", "↵", 1.5, .enter)
            ]
            return [row1, row2, row3, row4]

        case .symbols2:
            let row1: [(String, String, CGFloat, KeyKind)] = [
                ("~","~",1,.character), ("`","`",1,.character), ("|","|",1,.character),
                ("•","•",1,.character), ("√","√",1,.character), ("π","π",1,.character),
                ("÷","÷",1,.character), ("×","×",1,.character), ("¶","¶",1,.character),
                ("∆","∆",1,.character)
            ]
            let row2: [(String, String, CGFloat, KeyKind)] = [
                ("£","£",1,.character), ("¢","¢",1,.character), ("€","€",1,.character),
                ("¥","¥",1,.character), ("^","^",1,.character), ("[","[",1,.character),
                ("]","]",1,.character), ("{","{",1,.character), ("}","}",1,.character)
            ]
            let row3: [(String, String, CGFloat, KeyKind)] = [
                ("symbol_toggle_1", "123", 1.5, .symbolToggle),
                ("%","%",1,.character), ("©","©",1,.character), ("®","®",1,.character),
                ("™","™",1,.character), ("✓","✓",1,.character), ("\\","\\",1,.character),
                ("<","<",1,.character), (">",">",1,.character),
                ("backspace", "⌫", 1.5, .backspace)
            ]
            let row4: [(String, String, CGFloat, KeyKind)] = [
                ("abc_toggle", "ABC", 1.5, .symbolToggle),
                ("comma", ",", 1.0, .character),
                ("space", " ", 5.0, .space),
                ("period", ".", 1.0, .character),
                ("enter", "↵", 1.5, .enter)
            ]
            return [row1, row2, row3, row4]
        }
    }

    private func rebuildKeys() {
        keyButtons.forEach { $0.removeFromSuperview() }
        keyButtons.removeAll()

        let rows = currentLayout()
        let rowCount = CGFloat(rows.count)
        let availableHeight = bounds.height - verticalPadding * 2 - keySpacing * (rowCount - 1)
        let rowHeight = availableHeight / rowCount

        for (rowIndex, row) in rows.enumerated() {
            let totalWeight = row.reduce(0) { $0 + $1.width }
            let availableWidth = bounds.width - horizontalPadding * 2 - keySpacing * CGFloat(row.count - 1)
            let unitWidth = availableWidth / totalWeight

            let rowTop = verticalPadding + CGFloat(rowIndex) * (rowHeight + keySpacing)
            let rowTotalWidth = row.reduce(0) { $0 + $1.width * unitWidth } + keySpacing * CGFloat(row.count - 1)
            var keyLeft = max(horizontalPadding, (bounds.width - rowTotalWidth) / 2)

            for key in row {
                let keyWidth = key.width * unitWidth
                let frame = CGRect(x: keyLeft, y: rowTop, width: keyWidth, height: rowHeight)

                let button = KeyButton(frame: frame)
                button.keyId = key.id
                button.label = key.label
                button.kind = key.type
                button.isSpecial = key.type != .character
                button.normalBg = key.type == .character ? keyBgColor : specialKeyBgColor
                button.pressedBg = keyPressedColor
                button.labelColor = key.type == .character ? textColor : specialTextColor
                button.layer.cornerRadius = keyCornerRadius
                button.clipsToBounds = true
                button.configure()

                button.addTarget(self, action: #selector(keyTouchDown(_:)), for: .touchDown)
                button.addTarget(self, action: #selector(keyTouchUp(_:)), for: [.touchUpInside])
                button.addTarget(self, action: #selector(keyTouchCancel(_:)), for: [.touchUpOutside, .touchCancel])

                addSubview(button)
                keyButtons.append(button)

                keyLeft += keyWidth + keySpacing
            }
        }
    }

    @objc private func keyTouchDown(_ sender: KeyButton) {
        sender.backgroundColor = keyPressedColor

        if sender.kind == .backspace {
            delegate?.keyboardViewDidTapDelete(self)
            backspaceDelayTimer = Timer.scheduledTimer(withTimeInterval: 0.4, repeats: false) { [weak self] _ in
                self?.backspaceTimer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] _ in
                    guard let self = self else { return }
                    self.delegate?.keyboardViewDidTapDelete(self)
                }
            }
        }
    }

    @objc private func keyTouchUp(_ sender: KeyButton) {
        sender.backgroundColor = sender.normalBg
        stopBackspaceRepeat()

        switch sender.kind {
        case .character:
            delegate?.keyboardView(self, didTapText: sender.label)
            if shiftState == .on { shiftState = .off }
        case .space:
            delegate?.keyboardView(self, didTapText: " ")
        case .enter:
            delegate?.keyboardViewDidTapEnter(self)
        case .backspace:
            break // handled in touchDown
        case .shift:
            delegate?.keyboardViewDidTapShift(self)
        case .symbolToggle:
            delegate?.keyboardViewDidTapSymbolToggle(self, keyId: sender.keyId)
        }
    }

    @objc private func keyTouchCancel(_ sender: KeyButton) {
        sender.backgroundColor = sender.normalBg
        stopBackspaceRepeat()
    }

    private func stopBackspaceRepeat() {
        backspaceDelayTimer?.invalidate()
        backspaceDelayTimer = nil
        backspaceTimer?.invalidate()
        backspaceTimer = nil
    }
}

enum KeyKind {
    case character, shift, backspace, space, enter, symbolToggle
}

class KeyButton: UIControl {
    var keyId: String = ""
    var label: String = ""
    var kind: KeyKind = .character
    var isSpecial: Bool = false
    var normalBg: UIColor = .darkGray
    var pressedBg: UIColor = .gray
    var labelColor: UIColor = .white

    private let titleLabel = UILabel()

    func configure() {
        backgroundColor = normalBg
        titleLabel.text = label
        titleLabel.textColor = labelColor
        titleLabel.font = isSpecial ? .systemFont(ofSize: 14) : .systemFont(ofSize: 22)
        titleLabel.textAlignment = .center
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(titleLabel)
        NSLayoutConstraint.activate([
            titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: centerYAnchor)
        ])
    }
}
