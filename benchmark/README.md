# Macrobenchmark Module

This module contains Macrobenchmark tests for the app (cold/warm startup).

- Test class: `com.dawitf.akahidegn.benchmark.StartupBenchmark`
- Build: `./gradlew :benchmark:connectedDebugAndroidTest`
- Artifacts: `benchmark/build/benchmark.log` and `benchmark/build/outputs/`

CI checks compare medians to thresholds via `scripts/verify_macrobenchmark.sh`.
