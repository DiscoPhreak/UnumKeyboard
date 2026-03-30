#!/usr/bin/env python3
"""
Prepare training data for the Unum Keyboard neural reranker.

Data format:
  Each training example is a (context, candidate, label) triple where:
  - context: last 32 tokens before the target word
  - candidate: a word from the trie's top-10 suggestions
  - label: 1 if candidate is the actual next word, 0 otherwise

Data sources:
  - Conversational: Reddit comments, OpenSubtitles
  - Formal: Wikipedia, news articles
  - Combined for balanced coverage of typing styles

Usage:
  python prepare_data.py --input corpus.txt --output train_data.jsonl --vocab vocab.txt
"""

import argparse
import json
import random
from pathlib import Path
from collections import Counter


def extract_examples(text: str, vocab: set, window_size: int = 32, negatives: int = 9):
    """
    Extract training examples from a text corpus.

    For each word in the text:
    1. Context = previous `window_size` words
    2. Positive = the actual word
    3. Negatives = `negatives` random words from vocab that the trie would suggest
    """
    words = text.lower().split()
    examples = []

    for i in range(window_size, len(words)):
        target = words[i]
        if target not in vocab:
            continue

        context = words[max(0, i - window_size):i]

        # Positive example
        examples.append({
            "context": " ".join(context),
            "candidate": target,
            "label": 1
        })

        # Hard negatives: words that start with the same letter (trie would suggest them)
        same_prefix = [w for w in vocab if w[0] == target[0] and w != target]
        neg_candidates = random.sample(same_prefix, min(negatives, len(same_prefix)))

        for neg in neg_candidates:
            examples.append({
                "context": " ".join(context),
                "candidate": neg,
                "label": 0
            })

    return examples


def build_vocab(text: str, min_freq: int = 5, max_vocab: int = 30000):
    """Build vocabulary from corpus."""
    words = text.lower().split()
    counter = Counter(words)
    vocab = {word for word, count in counter.most_common(max_vocab) if count >= min_freq}
    return vocab


def main():
    parser = argparse.ArgumentParser(description="Prepare reranker training data")
    parser.add_argument("--input", type=str, required=True, help="Input corpus file")
    parser.add_argument("--output", type=str, required=True, help="Output JSONL file")
    parser.add_argument("--vocab", type=str, help="Optional vocab file (one word per line)")
    parser.add_argument("--max-examples", type=int, default=1_000_000)
    parser.add_argument("--window-size", type=int, default=32)
    parser.add_argument("--negatives", type=int, default=9)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    random.seed(args.seed)

    print(f"Reading corpus from {args.input}...")
    text = Path(args.input).read_text(encoding="utf-8")

    if args.vocab:
        vocab = set(Path(args.vocab).read_text().strip().split("\n"))
        print(f"Loaded vocab: {len(vocab)} words")
    else:
        print("Building vocab from corpus...")
        vocab = build_vocab(text)
        print(f"Built vocab: {len(vocab)} words")

    print("Extracting training examples...")
    examples = extract_examples(
        text, vocab,
        window_size=args.window_size,
        negatives=args.negatives
    )

    if len(examples) > args.max_examples:
        random.shuffle(examples)
        examples = examples[:args.max_examples]

    print(f"Writing {len(examples)} examples to {args.output}...")
    with open(args.output, "w") as f:
        for ex in examples:
            f.write(json.dumps(ex) + "\n")

    # Stats
    positives = sum(1 for ex in examples if ex["label"] == 1)
    negatives = len(examples) - positives
    print(f"Done. Positives: {positives}, Negatives: {negatives}")


if __name__ == "__main__":
    main()
