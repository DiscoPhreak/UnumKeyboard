import Foundation
import shared

/// Swift-friendly wrapper around the KMP PredictionService and TwoStagePipeline.
/// Handles initialization, prediction, autocorrect, and learning persistence.
final class PredictionBridge {

    private let service = SharedPredictionService()
    private var pipeline: SharedTwoStagePipeline?
    private let defaults: UserDefaults?

    private static let learningDataKey = "unum_learning_data"
    private static let blockListKey = "unum_autocorrect_blocklist"

    var isLoaded: Bool { service.isLoaded }

    /// Callback invoked when the pipeline updates predictions (Stage 1 or Stage 2).
    var onPredictionsUpdated: (([String]) -> Void)?

    init() {
        defaults = UserDefaults(suiteName: "group.com.unum.keyboard") ?? .standard
    }

    // MARK: - Initialization

    func loadDictionary(bundle: Bundle = .main) {
        guard let unigramsURL = bundle.url(forResource: "unigrams", withExtension: "txt", subdirectory: "en-US"),
              let bigramsURL = bundle.url(forResource: "bigrams", withExtension: "txt", subdirectory: "en-US"),
              let trigramsURL = bundle.url(forResource: "trigrams", withExtension: "txt", subdirectory: "en-US") else {
            guard let unigramsURL = bundle.url(forResource: "unigrams", withExtension: "txt"),
                  let bigramsURL = bundle.url(forResource: "bigrams", withExtension: "txt"),
                  let trigramsURL = bundle.url(forResource: "trigrams", withExtension: "txt") else {
                NSLog("[PredictionBridge] Dictionary files not found in bundle")
                return
            }
            loadFromURLs(unigrams: unigramsURL, bigrams: bigramsURL, trigrams: trigramsURL)
            return
        }
        loadFromURLs(unigrams: unigramsURL, bigrams: bigramsURL, trigrams: trigramsURL)
    }

    private func loadFromURLs(unigrams: URL, bigrams: URL, trigrams: URL) {
        do {
            let unigramsText = try String(contentsOf: unigrams, encoding: .utf8)
            let bigramsText = try String(contentsOf: bigrams, encoding: .utf8)
            let trigramsText = try String(contentsOf: trigrams, encoding: .utf8)
            service.initialize(unigramsText: unigramsText, bigramsText: bigramsText,
                               trigramsText: trigramsText, adjacencyMap: [:])
            loadPersistedData()

            // Create pipeline with ContextReranker
            let reranker = service.createReranker()
            let pipe = SharedTwoStagePipeline(
                predictionService: service,
                reranker: reranker,
                enabled: true
            )
            pipe.onPredictionsUpdated = { [weak self] predictions in
                guard let predictions = predictions as? [SharedPrediction] else { return }
                let words = predictions.map { $0.word }
                DispatchQueue.main.async {
                    self?.onPredictionsUpdated?(words)
                }
            }
            pipe.loadModel()
            pipeline = pipe

            NSLog("[PredictionBridge] Dictionary and pipeline loaded successfully")
        } catch {
            NSLog("[PredictionBridge] Failed to load dictionary: \(error)")
        }
    }

    // MARK: - Pipeline keystroke events

    func onKeystroke(prefix: String, context: [String]) {
        pipeline?.onKeystroke(prefix: prefix, context: context)
    }

    func onWordCommitted(_ word: String) {
        pipeline?.onWordCommitted(word: word)
        let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        service.learnWord(word: word, timestamp: timestamp)
    }

    func onSentenceBoundary() {
        pipeline?.onSentenceBoundary()
    }

    // MARK: - Direct predictions (fallback when pipeline not available)

    func predict(maxResults: Int32 = 3) -> [String] {
        let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        let predictions = service.predict(maxResults: maxResults, currentTime: timestamp)
        return predictions.map { ($0 as! SharedPrediction).word }
    }

    func updatePrefix(_ prefix: String) {
        service.updatePrefix(prefix: prefix)
    }

    func commitWord(_ word: String) {
        service.commitWord(word: word)
        let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        service.learnWord(word: word, timestamp: timestamp)
    }

    func resetContext() {
        service.resetContext()
    }

    // MARK: - Autocorrect

    func getAutoCorrection(_ word: String) -> AutoCorrectionInfo? {
        guard let result = service.getAutoCorrection(word: word) else { return nil }
        return AutoCorrectionInfo(
            original: result.original,
            corrected: result.corrected,
            confidence: result.confidence,
            shouldAutoApply: result.shouldAutoApply
        )
    }

    func addToBlockList(_ word: String) {
        service.addToBlockList(word: word)
        persistData()
    }

    // MARK: - Persistence

    private func loadPersistedData() {
        if let learningData = defaults?.string(forKey: Self.learningDataKey), !learningData.isEmpty {
            service.loadLearningData(data: learningData)
        }
        if let blockListData = defaults?.string(forKey: Self.blockListKey), !blockListData.isEmpty {
            service.loadBlockList(data: blockListData)
        }
    }

    func persistData() {
        defaults?.set(service.saveLearningData(), forKey: Self.learningDataKey)
        defaults?.set(service.serializeBlockList(), forKey: Self.blockListKey)
    }

    func destroy() {
        pipeline?.destroy()
        pipeline = nil
    }
}

struct AutoCorrectionInfo {
    let original: String
    let corrected: String
    let confidence: Float
    let shouldAutoApply: Bool
}
