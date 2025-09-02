# analyse_logs_final_robust.py
import pandas as pd
import argparse
from io import StringIO

def analyze_performance_logs(start_log_file, end_log_file):
    """
    Parses start and end log files from scratch, cleans the data, 
    standardizes timestamps, calculates performance metrics, and 
    prints a summary report.
    """
    try:
        # --- Step 1: Ingest and Parse Start Logs (postman_logs.txt) ---
        
        # Manually read and clean each line to robustly handle surrounding quotes
        cleaned_start_lines = []
        with open(start_log_file, 'r') as f:
            for line in f:
                # Strip whitespace and the surrounding single quotes from each line
                clean_line = line.strip().strip("'")
                if clean_line:
                    cleaned_start_lines.append(clean_line)
        
        # Use StringIO to treat the list of cleaned lines as a file for pandas
        start_io = StringIO("\n".join(cleaned_start_lines))
        start_df = pd.read_csv(start_io, names=['status', 'timestamp', 'reference'])

        # --- Step 2: Ingest and Parse End Logs (webhook_logs.txt) ---
        
        # The webhook log format is clean, so we can read it directly
        end_df = pd.read_csv(end_log_file, names=['status', 'timestamp', 'reference'])

        # --- Step 3: Clean and Standardize Data ---

        # Explicitly set the key column to string type to be safe
        start_df['reference'] = start_df['reference'].astype(str).str.strip()
        end_df['reference'] = end_df['reference'].astype(str).str.strip()

        # Convert timestamps and standardize to UTC to allow for calculations
        start_df['timestamp'] = pd.to_datetime(start_df['timestamp'], utc=True)
        end_df['timestamp'] = pd.to_datetime(end_df['timestamp'], utc=True)

        # Handle duplicates: keep the FIRST start time and the LAST end time
        start_df.drop_duplicates(subset=['reference'], keep='first', inplace=True)
        end_df.drop_duplicates(subset=['reference'], keep='last', inplace=True)

        # --- Step 4: Correlate Data and Calculate Latency ---
        
        merged_df = pd.merge(start_df, end_df, on='reference', suffixes=('_start', '_end'))

        if merged_df.empty:
            print("No matching requests found between log files. Please check for format discrepancies.")
            return

        merged_df['latency_ms'] = (merged_df['timestamp_end'] - merged_df['timestamp_start']).dt.total_seconds() * 1000

        # --- Step 5: Calculate and Report Metrics ---
        
        total_requests = len(merged_df)
        avg_latency = merged_df['latency_ms'].mean()
        median_latency = merged_df['latency_ms'].median()
        p95_latency = merged_df['latency_ms'].quantile(0.95)
        min_latency = merged_df['latency_ms'].min()
        max_latency = merged_df['latency_ms'].max()

        total_duration_seconds = (merged_df['timestamp_end'].max() - merged_df['timestamp_start'].min()).total_seconds()
        throughput = total_requests / total_duration_seconds if total_duration_seconds > 0 else 0

        print("\n--- Async Performance Analysis ---")
        print(f"Total Correlated Requests: {total_requests}")
        print("-" * 32)
        print(f"Average Latency: {avg_latency:.2f} ms")
        print(f"Median (p50) Latency: {median_latency:.2f} ms")
        print(f"95th Percentile (p95) Latency: {p95_latency:.2f} ms")
        print(f"Min Latency: {min_latency:.2f} ms")
        print(f"Max Latency: {max_latency:.2f} ms")
        print("-" * 32)
        print(f"Total Test Duration: {total_duration_seconds:.2f} seconds")
        print(f"Overall Throughput: {throughput:.2f} req/sec")
        print("----------------------------------\n")

    except FileNotFoundError as e:
        print(f"Error: File not found - {e.filename}")
    except Exception as e:
        print(f"An unexpected error occurred: {e}")

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Analyze async performance logs.')
    parser.add_argument('start_log', help='Path to the start log file (e.g., postman_logs.txt)')
    parser.add_argument('end_log', help='Path to the end log file (e.g., webhook_logs.txt)')
    args = parser.parse_args()
    
    analyze_performance_logs(args.start_log, args.end_log)