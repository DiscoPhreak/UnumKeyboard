import UIKit

class KeyboardViewController: UIInputViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .black

        let label = UILabel()
        label.text = "Unum Keyboard"
        label.textColor = .white
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(label)

        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            view.heightAnchor.constraint(equalToConstant: 260)
        ])
    }
}
