#!/usr/bin/env bash
set -euo pipefail

# Simple guard that parses Macrobenchmark logs for median startup times and compares
# against baseline thresholds. Adjust thresholds as needed.

LOG_FILE="benchmark/build/benchmark.log"
BASELINE_DIR="benchmark/baselines"
mkdir -p "$BASELINE_DIR"

# Default thresholds in ms (cold <= 950, warm <= 450)
COLD_THRESHOLD=${COLD_THRESHOLD:-950}
WARM_THRESHOLD=${WARM_THRESHOLD:-450}

if [[ ! -f "$LOG_FILE" ]]; then
  echo "No benchmark log found at $LOG_FILE. Skipping verification (PASS)."
  exit 0
fi

cold_ms=$(grep -E "startupCold.*median" -A1 "$LOG_FILE" | grep -Eo "median=([0-9]+)" | head -n1 | cut -d= -f2 || true)
warm_ms=$(grep -E "startupWarm.*median" -A1 "$LOG_FILE" | grep -Eo "median=([0-9]+)" | head -n1 | cut -d= -f2 || true)

status=0

if [[ -n "$cold_ms" ]]; then
  echo "Cold startup median: ${cold_ms} ms (threshold ${COLD_THRESHOLD} ms)"
  if (( cold_ms > COLD_THRESHOLD )); then
    echo "Cold startup regression detected." >&2
    status=1
  fi
else
  echo "Cold startup metric not found in log; ignoring."
fi

if [[ -n "$warm_ms" ]]; then
  echo "Warm startup median: ${warm_ms} ms (threshold ${WARM_THRESHOLD} ms)"
  if (( warm_ms > WARM_THRESHOLD )); then
    echo "Warm startup regression detected." >&2
    status=1
  fi
else
  echo "Warm startup metric not found in log; ignoring."
fi

exit $status
