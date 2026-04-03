import UIKit

protocol KeyboardViewDelegate: AnyObject {
    func keyboardView(_ view: KeyboardView, didTapText text: String)
    func keyboardViewDidTapDelete(_ view: KeyboardView)
    func keyboardViewDidTapEnter(_ view: KeyboardView)
    func keyboardViewDidTapShift(_ view: KeyboardView)
    func keyboardViewDidTapSymbolToggle(_ view: KeyboardView, keyId: String)
    /// Called when trackpad cursor movement occurs (M10)
    func keyboardView(_ view: KeyboardView, didMoveCursor offset: Int)
    /// Called when trackpad selection extends (M10)
    func keyboardView(_ view: KeyboardView, didExtendSelection offset: Int)
}

class KeyboardView: UIView {

    weak var delegate: KeyboardViewDelegate?

    // Theme colors (M12 — configurable via applyTheme)
    private var bgColor = UIColor.black
    private var keyBgColor = UIColor(white: 0.10, alpha: 1.0)
    private var keyPressedColor = UIColor(white: 0.20, alpha: 1.0)
    private var specialKeyBgColor = UIColor(white: 0.16, alpha: 1.0)
    private var textColor = UIColor.white
    private var specialTextColor = UIColor(white: 0.73, alpha: 1.0)
    private var flickHintColor = UIColor(white: 0.40, alpha: 1.0)

    // Config dimensions (M12 — configurable via applyConfig)
    private var keyCornerRadius: CGFloat = 6
    private var keySpacing: CGFloat = 4
    private var horizontalPadding: CGFloat = 3
    private var verticalPadding: CGFloat = 6

    /// Apply a theme using ARGB Long values from shared KeyboardTheme.
    func applyTheme(
        backgroundColor: UInt,
        keyBackground: UInt,
        keyPressed: UInt,
        specialKeyBackground: UInt,
        keyText: UInt,
        specialKeyText: UInt,
        flickHint: UInt
    ) {
        bgColor = UIColor(argb: backgroundColor)
        keyBgColor = UIColor(argb: keyBackground)
        keyPressedColor = UIColor(argb: keyPressed)
        specialKeyBgColor = UIColor(argb: specialKeyBackground)
        textColor = UIColor(argb: keyText)
        specialTextColor = UIColor(argb: specialKeyText)
        flickHintColor = UIColor(argb: flickHint)
        self.backgroundColor = bgColor
        setNeedsLayout()
        setNeedsDisplay()
    }

    /// Apply config dimensions.
    func applyConfig(
        cornerRadius: CGFloat = 6,
        spacing: CGFloat = 4,
        hPadding: CGFloat = 3,
        vPadding: CGFloat = 6
    ) {
        keyCornerRadius = cornerRadius
        keySpacing = spacing
        horizontalPadding = hPadding
        verticalPadding = vPadding
        setNeedsLayout()
    }

    // Flick detection constants
    private let minFlickDistance: CGFloat = 20
    private let maxFlickDistance: CGFloat = 150
    private let maxFlickDuration: TimeInterval = 0.3
    private let directionalityThreshold: CGFloat = 1.5

    // Layout state
    enum ShiftState { case off, on, capsLock }
    enum LayoutMode { case letters, symbols, symbols2 }

    var shiftState: ShiftState = .off { didSet { setNeedsLayout(); setNeedsDisplay() } }
    var layoutMode: LayoutMode = .letters { didSet { setNeedsLayout(); setNeedsDisplay() } }

    private var keyButtons: [FlickKeyButton] = []

    // Backspace repeat
    private var backspaceTimer: Timer?
    private var backspaceDelayTimer: Timer?

    // Flick tracking
    private var flickStartPoint: CGPoint = .zero
    private var flickStartTime: TimeInterval = 0
    private var flickOriginButton: FlickKeyButton?
    private var flickFired: Bool = false

    // Spacebar trackpad (M10)
    private var spacebarTrackpadActive: Bool = false
    private var spacebarTouchStart: CGPoint = .zero
    private var spacebarTouchTime: TimeInterval = 0
    private var lastTrackpadCursorOffset: Int = 0
    private let trackpadActivationDelay: TimeInterval = 0.3
    private let trackpadSensitivity: CGFloat = 15
    private let trackpadSelectionThreshold: CGFloat = 30
    private var trackpadSelecting: Bool = false

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

    // MARK: - Flick Character Mappings

    private func flickMappings(for keyId: String) -> (up: String?, down: String?, left: String?, right: String?) {
        let isUpper = shiftState != .off

        // Number row flick-up for letter keys
        let flickMap: [String: (up: String?, down: String?, left: String?, right: String?)] = [
            "q": ("1", nil, nil, nil), "w": ("2", nil, nil, nil),
            "e": ("3", nil, nil, isUpper ? "É" : "é"), "r": ("4", nil, nil, nil),
            "t": ("5", nil, nil, nil), "y": ("6", nil, nil, nil),
            "u": ("7", nil, nil, isUpper ? "Ü" : "ü"), "i": ("8", nil, nil, isUpper ? "Í" : "í"),
            "o": ("9", nil, nil, isUpper ? "Ó" : "ó"), "p": ("0", nil, nil, nil),
            "a": ("@", nil, nil, isUpper ? "Á" : "á"), "s": ("#", nil, nil, "$"),
            "d": ("&", nil, nil, nil), "f": ("*", nil, nil, nil),
            "g": ("-", nil, nil, nil), "h": ("+", nil, nil, nil),
            "j": ("(", nil, nil, nil), "k": (")", nil, nil, nil),
            "l": ("'", nil, nil, nil),
            "z": ("!", nil, nil, nil), "x": ("\"", nil, nil, nil),
            "c": (":", nil, nil, isUpper ? "Ç" : "ç"), "v": (";", nil, nil, nil),
            "b": ("/", nil, nil, nil), "n": ("?", nil, nil, isUpper ? "Ñ" : "ñ"),
            "m": (",", nil, nil, nil)
        ]

        return flickMap[keyId.lowercased()] ?? (nil, nil, nil, nil)
    }

    private func flickChar(for button: FlickKeyButton, direction: FlickDirection) -> String? {
        let mapping = flickMappings(for: button.keyId)
        switch direction {
        case .up: return mapping.up
        case .down: return mapping.down
        case .left: return mapping.left
        case .right: return mapping.right
        case .none: return nil
        }
    }

    // MARK: - Layout

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

                let button = FlickKeyButton(frame: frame)
                button.keyId = key.id
                button.label = key.label
                button.kind = key.type
                button.isSpecial = key.type != .character
                button.normalBg = key.type == .character ? keyBgColor : specialKeyBgColor
                button.pressedBg = keyPressedColor
                button.labelColor = key.type == .character ? textColor : specialTextColor

                // Set flick hint for letter keys
                if layoutMode == .letters && key.type == .character {
                    let mapping = flickMappings(for: key.id)
                    button.flickUpHint = mapping.up
                }

                button.flickHintColor = flickHintColor
                button.layer.cornerRadius = keyCornerRadius
                button.clipsToBounds = true
                button.configure()

                button.addTarget(self, action: #selector(keyTouchDown(_:event:)), for: .touchDown)
                button.addTarget(self, action: #selector(keyTouchDrag(_:event:)), for: [.touchDragInside, .touchDragOutside])
                button.addTarget(self, action: #selector(keyTouchUp(_:event:)), for: [.touchUpInside, .touchUpOutside])
                button.addTarget(self, action: #selector(keyTouchCancel(_:)), for: .touchCancel)

                addSubview(button)
                keyButtons.append(button)

                keyLeft += keyWidth + keySpacing
            }
        }
    }

    // MARK: - Touch Handling with Flick Detection

    @objc private func keyTouchDown(_ sender: FlickKeyButton, event: UIEvent) {
        sender.backgroundColor = keyPressedColor
        flickOriginButton = sender
        flickFired = false

        if let touch = event.touches(for: sender)?.first {
            let point = touch.location(in: self)
            flickStartPoint = point
            flickStartTime = touch.timestamp
        }

        // Start spacebar trackpad tracking (M10)
        if sender.kind == .space, let touch = event.touches(for: sender)?.first {
            spacebarTouchStart = touch.location(in: self)
            spacebarTouchTime = touch.timestamp
            spacebarTrackpadActive = false
            lastTrackpadCursorOffset = 0
            trackpadSelecting = false
        }

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

    @objc private func keyTouchDrag(_ sender: FlickKeyButton, event: UIEvent) {
        guard !flickFired, let touch = event.touches(for: sender)?.first else { return }

        let point = touch.location(in: self)
        let elapsed = touch.timestamp - flickStartTime

        // Spacebar trackpad (M10)
        if sender.kind == .space {
            let trackpadElapsed = touch.timestamp - spacebarTouchTime
            if trackpadElapsed >= trackpadActivationDelay {
                if !spacebarTrackpadActive {
                    spacebarTrackpadActive = true
                    spacebarTouchStart = point
                    lastTrackpadCursorOffset = 0
                }
                let dx = point.x - spacebarTouchStart.x
                let dy = point.y - spacebarTouchStart.y
                let cursorOffset = Int(round(dx / trackpadSensitivity))
                let delta = cursorOffset - lastTrackpadCursorOffset

                if abs(dy) > trackpadSelectionThreshold && !trackpadSelecting {
                    trackpadSelecting = true
                }

                if delta != 0 {
                    if trackpadSelecting {
                        delegate?.keyboardView(self, didExtendSelection: delta)
                    } else {
                        delegate?.keyboardView(self, didMoveCursor: delta)
                    }
                    lastTrackpadCursorOffset = cursorOffset
                }
                return
            }
        }

        if elapsed > maxFlickDuration { return }

        let dx = point.x - flickStartPoint.x
        let dy = point.y - flickStartPoint.y
        let distance = sqrt(dx * dx + dy * dy)

        if distance > maxFlickDistance { return }

        if distance >= minFlickDistance {
            let direction = resolveDirection(dx: dx, dy: dy)
            if direction != .none, let originButton = flickOriginButton {
                if let text = flickChar(for: originButton, direction: direction) {
                    flickFired = true
                    stopBackspaceRepeat()
                    delegate?.keyboardView(self, didTapText: text)
                    if shiftState == .on { shiftState = .off }
                    sender.backgroundColor = sender.normalBg
                }
            }
        }
    }

    @objc private func keyTouchUp(_ sender: FlickKeyButton, event: UIEvent) {
        sender.backgroundColor = sender.normalBg
        stopBackspaceRepeat()

        // Spacebar trackpad: suppress space input if trackpad was active (M10)
        if sender.kind == .space && spacebarTrackpadActive {
            spacebarTrackpadActive = false
            trackpadSelecting = false
            lastTrackpadCursorOffset = 0
            flickOriginButton = nil
            return
        }
        spacebarTrackpadActive = false
        trackpadSelecting = false

        if flickFired {
            flickOriginButton = nil
            return
        }

        // Check for flick on release
        if let touch = event.touches(for: sender)?.first, let originButton = flickOriginButton {
            let point = touch.location(in: self)
            let elapsed = touch.timestamp - flickStartTime
            let dx = point.x - flickStartPoint.x
            let dy = point.y - flickStartPoint.y
            let distance = sqrt(dx * dx + dy * dy)

            if distance >= minFlickDistance && distance <= maxFlickDistance && elapsed <= maxFlickDuration {
                let direction = resolveDirection(dx: dx, dy: dy)
                if direction != .none {
                    if let text = flickChar(for: originButton, direction: direction) {
                        delegate?.keyboardView(self, didTapText: text)
                        if shiftState == .on { shiftState = .off }
                        flickOriginButton = nil
                        return
                    }
                }
            }
        }

        // Regular tap
        flickOriginButton = nil

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

    @objc private func keyTouchCancel(_ sender: FlickKeyButton) {
        sender.backgroundColor = sender.normalBg
        stopBackspaceRepeat()
        flickOriginButton = nil
        flickFired = false
    }

    private func resolveDirection(dx: CGFloat, dy: CGFloat) -> FlickDirection {
        let absDx = abs(dx)
        let absDy = abs(dy)

        if absDx > absDy * directionalityThreshold {
            return dx > 0 ? .right : .left
        } else if absDy > absDx * directionalityThreshold {
            return dy > 0 ? .down : .up
        }
        return .none
    }

    private func stopBackspaceRepeat() {
        backspaceDelayTimer?.invalidate()
        backspaceDelayTimer = nil
        backspaceTimer?.invalidate()
        backspaceTimer = nil
    }
}

// MARK: - Types

enum FlickDirection {
    case none, up, down, left, right
}

enum KeyKind {
    case character, shift, backspace, space, enter, symbolToggle
}

class FlickKeyButton: UIControl {
    var keyId: String = ""
    var label: String = ""
    var kind: KeyKind = .character
    var isSpecial: Bool = false
    var normalBg: UIColor = .darkGray
    var pressedBg: UIColor = .gray
    var labelColor: UIColor = .white
    var flickUpHint: String? = nil
    var flickHintColor: UIColor = .gray

    private let titleLabel = UILabel()
    private let hintLabel = UILabel()

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

        // Flick-up hint in top-right corner
        if let hint = flickUpHint {
            hintLabel.text = hint
            hintLabel.textColor = flickHintColor
            hintLabel.font = .systemFont(ofSize: 10)
            hintLabel.textAlignment = .right
            hintLabel.translatesAutoresizingMaskIntoConstraints = false
            addSubview(hintLabel)
            NSLayoutConstraint.activate([
                hintLabel.topAnchor.constraint(equalTo: topAnchor, constant: 2),
                hintLabel.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -4)
            ])
        }
    }
}

// MARK: - UIColor ARGB convenience

extension UIColor {
    convenience init(argb: UInt) {
        let a = CGFloat((argb >> 24) & 0xFF) / 255.0
        let r = CGFloat((argb >> 16) & 0xFF) / 255.0
        let g = CGFloat((argb >> 8) & 0xFF) / 255.0
        let b = CGFloat(argb & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b, alpha: a)
    }
}
