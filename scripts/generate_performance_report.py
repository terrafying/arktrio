#!/usr/bin/env python3

import argparse
import json
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path
from typing import Dict, Any

def load_locust_results(results_dir: str) -> pd.DataFrame:
    """Load and process Locust results."""
    stats = pd.read_csv(f"{results_dir}/stats.csv")
    return stats

def load_benchmark_results(results_file: str) -> Dict[str, Any]:
    """Load benchmark results."""
    with open(results_file) as f:
        return json.load(f)

def generate_summary(locust_stats: pd.DataFrame, benchmark_results: Dict[str, Any]) -> str:
    """Generate a markdown summary of performance results."""
    summary = ["# Performance Test Results\n"]
    
    # Locust Results
    summary.append("## Load Test Results\n")
    summary.append("### Response Times (ms)\n")
    summary.append("| Endpoint | Average | 50% | 90% | 95% | 99% |\n")
    summary.append("|----------|---------|-----|-----|-----|-----|\n")
    
    for _, row in locust_stats.iterrows():
        summary.append(
            f"| {row['Name']} | {row['Average Response Time']:.2f} | "
            f"{row['50%']:.2f} | {row['90%']:.2f} | {row['95%']:.2f} | "
            f"{row['99%']:.2f} |\n"
        )
    
    # Benchmark Results
    summary.append("\n## Benchmark Results\n")
    summary.append("### Operation Times (ms)\n")
    summary.append("| Operation | Mean | Std Dev | Min | Max |\n")
    summary.append("|-----------|------|---------|-----|-----|\n")
    
    for test_name, results in benchmark_results.items():
        stats = results['stats']
        summary.append(
            f"| {test_name} | {stats['mean']*1000:.2f} | "
            f"{stats['stddev']*1000:.2f} | {stats['min']*1000:.2f} | "
            f"{stats['max']*1000:.2f} |\n"
        )
    
    return "".join(summary)

def main():
    parser = argparse.ArgumentParser(description="Generate performance test report")
    parser.add_argument("--locust-results", required=True, help="Directory containing Locust results")
    parser.add_argument("--benchmark-results", required=True, help="JSON file containing benchmark results")
    parser.add_argument("--output", required=True, help="Output markdown file")
    args = parser.parse_args()
    
    # Load results
    locust_stats = load_locust_results(args.locust_results)
    benchmark_results = load_benchmark_results(args.benchmark_results)
    
    # Generate summary
    summary = generate_summary(locust_stats, benchmark_results)
    
    # Write output
    with open(args.output, "w") as f:
        f.write(summary)

if __name__ == "__main__":
    main() 