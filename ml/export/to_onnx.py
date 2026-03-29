#!/usr/bin/env python3
"""
Export the trained reranker model to ONNX format with INT8 quantization.

Pipeline:
  PyTorch model (float32, ~100MB)
    → ONNX export
      → INT8 dynamic quantization (via ONNX Runtime tools)
        → Final model: ~25-30MB on disk

Usage:
  python to_onnx.py --model model/ --output reranker.onnx --quantize
"""

import argparse
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description="Export reranker to ONNX")
    parser.add_argument("--model", type=str, required=True, help="Trained model directory")
    parser.add_argument("--output", type=str, default="reranker.onnx", help="Output ONNX file")
    parser.add_argument("--quantize", action="store_true", help="Apply INT8 dynamic quantization")
    parser.add_argument("--max-length", type=int, default=64, help="Max sequence length")
    args = parser.parse_args()

    try:
        import torch
        from transformers import AutoTokenizer, AutoModelForSequenceClassification
    except ImportError:
        print("ERROR: Required packages not installed.")
        print("Run: pip install torch transformers onnx onnxruntime")
        return

    print(f"Loading model from {args.model}...")
    tokenizer = AutoTokenizer.from_pretrained(args.model)
    model = AutoModelForSequenceClassification.from_pretrained(args.model)
    model.eval()

    # Create dummy input
    dummy_input = tokenizer(
        "I want to",  # context
        "go",          # candidate
        return_tensors="pt",
        max_length=args.max_length,
        padding="max_length",
        truncation=True
    )

    print(f"Exporting to ONNX: {args.output}...")
    torch.onnx.export(
        model,
        (dummy_input["input_ids"], dummy_input["attention_mask"], dummy_input["token_type_ids"]),
        args.output,
        input_names=["input_ids", "attention_mask", "token_type_ids"],
        output_names=["logits"],
        dynamic_axes={
            "input_ids": {0: "batch_size"},
            "attention_mask": {0: "batch_size"},
            "token_type_ids": {0: "batch_size"},
            "logits": {0: "batch_size"}
        },
        opset_version=14
    )

    output_path = Path(args.output)
    size_mb = output_path.stat().st_size / (1024 * 1024)
    print(f"ONNX model size: {size_mb:.1f} MB")

    if args.quantize:
        try:
            from onnxruntime.quantization import quantize_dynamic, QuantType

            quantized_output = str(output_path.with_suffix(".int8.onnx"))
            print(f"Quantizing to INT8: {quantized_output}...")

            quantize_dynamic(
                args.output,
                quantized_output,
                weight_type=QuantType.QInt8
            )

            quant_size_mb = Path(quantized_output).stat().st_size / (1024 * 1024)
            print(f"Quantized model size: {quant_size_mb:.1f} MB")
            print(f"Size reduction: {(1 - quant_size_mb / size_mb) * 100:.0f}%")

        except ImportError:
            print("WARNING: onnxruntime not installed, skipping quantization")
            print("Run: pip install onnxruntime")

    # Verify
    try:
        import onnxruntime as ort

        print("\nVerifying ONNX model...")
        final_model = str(output_path.with_suffix(".int8.onnx")) if args.quantize else args.output
        session = ort.InferenceSession(final_model)

        import numpy as np
        inputs = {
            "input_ids": dummy_input["input_ids"].numpy(),
            "attention_mask": dummy_input["attention_mask"].numpy(),
            "token_type_ids": dummy_input["token_type_ids"].numpy()
        }
        outputs = session.run(None, inputs)
        print(f"Inference OK. Output shape: {outputs[0].shape}")
        print(f"\nDone! Model ready for deployment: {final_model}")

    except ImportError:
        print("\nSkipping verification (onnxruntime not installed)")
        print(f"Done! ONNX model saved to: {args.output}")


if __name__ == "__main__":
    main()
