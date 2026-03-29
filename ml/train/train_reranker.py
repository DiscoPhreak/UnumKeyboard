#!/usr/bin/env python3
"""
Train the Unum Keyboard neural reranker model.

Architecture:
  - Small encoder-only transformer (similar to tiny BERT)
  - ~20-30M parameters
  - Input: [CLS] context_tokens [SEP] candidate_word [SEP]
  - Output: single score (0-1) per candidate
  - Vocab: 30K WordPiece tokens

Training:
  - Objective: binary classification (is this the correct next word?)
  - Start from pre-trained small BERT checkpoint
  - Fine-tune on candidate reranking task
  - Target: >80% top-1 accuracy on held-out test set

Usage:
  python train_reranker.py --data train_data.jsonl --output model/ --epochs 5
"""

import argparse
import json
from pathlib import Path

# These imports will work once dependencies are installed:
# pip install torch transformers datasets

def main():
    parser = argparse.ArgumentParser(description="Train neural reranker")
    parser.add_argument("--data", type=str, required=True, help="Training data JSONL")
    parser.add_argument("--val-data", type=str, help="Validation data JSONL")
    parser.add_argument("--output", type=str, default="model/", help="Output directory")
    parser.add_argument("--base-model", type=str, default="google/bert_uncased_L-4_H-256_A-4",
                       help="Pre-trained model to fine-tune (small BERT)")
    parser.add_argument("--epochs", type=int, default=5)
    parser.add_argument("--batch-size", type=int, default=64)
    parser.add_argument("--lr", type=float, default=2e-5)
    parser.add_argument("--max-length", type=int, default=64,
                       help="Max token sequence length (context + candidate)")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)

    print("=" * 60)
    print("Unum Keyboard Neural Reranker Training")
    print("=" * 60)
    print(f"Base model: {args.base_model}")
    print(f"Data: {args.data}")
    print(f"Epochs: {args.epochs}")
    print(f"Batch size: {args.batch_size}")
    print(f"Learning rate: {args.lr}")
    print(f"Max length: {args.max_length}")
    print(f"Output: {args.output}")
    print()

    try:
        import torch
        from transformers import AutoTokenizer, AutoModelForSequenceClassification, TrainingArguments, Trainer
        from datasets import Dataset
    except ImportError:
        print("ERROR: Required packages not installed.")
        print("Run: pip install torch transformers datasets")
        print()
        print("Training scaffold is ready. Install dependencies and re-run.")
        return

    # Load data
    print("Loading training data...")
    examples = []
    with open(args.data) as f:
        for line in f:
            examples.append(json.loads(line.strip()))
    print(f"Loaded {len(examples)} examples")

    # Tokenizer
    print(f"Loading tokenizer from {args.base_model}...")
    tokenizer = AutoTokenizer.from_pretrained(args.base_model)

    # Prepare dataset
    def tokenize_example(example):
        # Format: [CLS] context [SEP] candidate [SEP]
        return tokenizer(
            example["context"],
            example["candidate"],
            truncation=True,
            max_length=args.max_length,
            padding="max_length"
        )

    dataset = Dataset.from_list(examples)
    dataset = dataset.map(tokenize_example, batched=False)
    dataset = dataset.rename_column("label", "labels")
    dataset = dataset.train_test_split(test_size=0.1, seed=args.seed)

    # Model
    print(f"Loading model from {args.base_model}...")
    model = AutoModelForSequenceClassification.from_pretrained(
        args.base_model,
        num_labels=2  # binary: correct / incorrect candidate
    )
    param_count = sum(p.numel() for p in model.parameters())
    print(f"Model parameters: {param_count:,}")

    # Training
    training_args = TrainingArguments(
        output_dir=str(output_dir / "checkpoints"),
        num_train_epochs=args.epochs,
        per_device_train_batch_size=args.batch_size,
        per_device_eval_batch_size=args.batch_size,
        learning_rate=args.lr,
        weight_decay=0.01,
        eval_strategy="epoch",
        save_strategy="epoch",
        load_best_model_at_end=True,
        seed=args.seed,
        logging_steps=100,
        fp16=torch.cuda.is_available(),
    )

    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=dataset["train"],
        eval_dataset=dataset["test"],
    )

    print("Starting training...")
    trainer.train()

    # Save
    print(f"Saving model to {output_dir}...")
    model.save_pretrained(str(output_dir))
    tokenizer.save_pretrained(str(output_dir))

    # Evaluate
    results = trainer.evaluate()
    print(f"\nEvaluation results: {results}")
    print(f"\nModel saved to {output_dir}")
    print("Next step: python ml/export/to_onnx.py --model model/ --output model.onnx")


if __name__ == "__main__":
    main()
