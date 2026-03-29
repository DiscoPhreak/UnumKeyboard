import UIKit

protocol SuggestionBarDelegate: AnyObject {
    func suggestionBar(_ bar: SuggestionBar, didSelectSuggestion word: String)
}

class SuggestionBar: UIView {

    weak var delegate: SuggestionBarDelegate?

    private let bgColor = UIColor(white: 0.04, alpha: 1.0)
    private let textColor = UIColor(white: 0.87, alpha: 1.0)
    private let dividerColor = UIColor(white: 0.20, alpha: 1.0)
    private let maxSuggestions = 3

    private var buttons: [UIButton] = []

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setup()
    }

    private func setup() {
        backgroundColor = bgColor

        let stack = UIStackView()
        stack.axis = .horizontal
        stack.distribution = .fillEqually
        stack.alignment = .fill
        stack.translatesAutoresizingMaskIntoConstraints = false
        addSubview(stack)

        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: trailingAnchor),
            stack.topAnchor.constraint(equalTo: topAnchor),
            stack.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])

        for i in 0..<maxSuggestions {
            let button = UIButton(type: .system)
            button.setTitle("", for: .normal)
            button.setTitleColor(textColor, for: .normal)
            button.titleLabel?.font = .systemFont(ofSize: 16)
            button.tag = i
            button.addTarget(self, action: #selector(suggestionTapped(_:)), for: .touchUpInside)
            buttons.append(button)
            stack.addArrangedSubview(button)

            if i < maxSuggestions - 1 {
                let divider = UIView()
                divider.backgroundColor = dividerColor
                divider.translatesAutoresizingMaskIntoConstraints = false
                stack.addArrangedSubview(divider)
                divider.widthAnchor.constraint(equalToConstant: 1).isActive = true
            }
        }
    }

    func updateSuggestions(_ suggestions: [String]) {
        for i in 0..<maxSuggestions {
            buttons[i].setTitle(suggestions.indices.contains(i) ? suggestions[i] : "", for: .normal)
        }
    }

    func clear() {
        for button in buttons {
            button.setTitle("", for: .normal)
        }
    }

    @objc private func suggestionTapped(_ sender: UIButton) {
        guard let word = sender.title(for: .normal), !word.isEmpty else { return }
        delegate?.suggestionBar(self, didSelectSuggestion: word)
    }
}
