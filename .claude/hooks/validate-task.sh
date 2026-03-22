#!/bin/bash
set -e

# Hook: Runs when a teammate completes a task
# Exit code 2 = send feedback and keep teammate working
# Exit code 0 = task completion approved

INPUT=$(cat)
CWD=$(echo "$INPUT" | jq -r '.cwd // "."')
cd "$CWD"

echo "Running quality gate..." >&2

# Step 1: Verify Kotlin compilation
if [ -f "build.gradle.kts" ] || [ -f "build.gradle" ]; then
  if ! ./gradlew compileKotlin --quiet 2>/dev/null; then
    echo "BUILD FAILED: Kotlin compilation errors. Fix them before completing the task." >&2
    exit 2
  fi
  echo "Compilation: OK" >&2
fi

# Step 2: Verify tests pass
if ! ./gradlew test --quiet 2>/dev/null; then
  echo "TESTS FAILED: Some tests are failing. Fix them before completing the task." >&2
  exit 2
fi
echo "Tests: OK" >&2

echo "Quality gate passed." >&2
exit 0
