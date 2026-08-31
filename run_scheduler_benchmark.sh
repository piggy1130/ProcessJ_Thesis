#!/usr/bin/env bash

set -euo pipefail

benchmark_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
mode="${1:-all}"
runs="${2:-3}"

case "$mode" in
    all|channel|blocked|timer) ;;
    *)
        echo "Usage: $0 [all|channel|blocked|timer] [positive-run-count]" >&2
        exit 2
        ;;
esac

if [[ ! "$runs" =~ ^[1-9][0-9]*$ ]]; then
    echo "Run count must be a positive integer." >&2
    exit 2
fi

cd "$benchmark_dir"

for runtime_source in "$benchmark_dir"/src/processj/runtime/*.java; do
    runtime_name="${runtime_source##*/}"
    runtime_class="$benchmark_dir/bin/processj/runtime/${runtime_name%.java}.class"

    if [[ ! -f "$runtime_class" || "$runtime_source" -nt "$runtime_class" ]]; then
        echo "ProcessJ runtime is not built from the latest source; run 'ant compile' first." >&2
        exit 2
    fi
done

./pjc scheduler_benchmark.pj

benchmark_classpath="$benchmark_dir/bin:$benchmark_dir/lib/JVM:$benchmark_dir/scheduler_benchmark.jar"
TIMEFORMAT='[host] wall=%3R s user=%3U s sys=%3S s'

for ((run = 1; run <= runs; ++run)); do
    printf '\n=== scheduler benchmark: mode=%s run=%d/%d ===\n' \
        "$mode" "$run" "$runs"

    # The repository's pj wrapper cannot forward program arguments safely, so
    # invoke the generated main class directly when selecting a benchmark mode.
    time timeout --foreground 60s java \
        -Dorg.slf4j.simpleLogger.defaultLogLevel=off \
        -cp "$benchmark_classpath" \
        scheduler_benchmark "$mode"
done
