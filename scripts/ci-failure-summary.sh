#!/usr/bin/env bash
# Prints the failing tests and the lint/detekt errors in one place at the end
# of a failed CI log, so nobody has to scroll through or download reports.
set -uo pipefail

echo "=== Failing tests ==="
find . -path '*/build/test-results/*' -name 'TEST-*.xml' -print0 2>/dev/null |
  xargs -0 -r grep -l -E '<(failure|error)' |
  while read -r file; do
    echo "--- ${file}"
    grep -A 6 -E '<testcase|<(failure|error)' "$file" |
      grep -B 1 -A 5 -E '<(failure|error)' | head -n 40
  done

echo "=== Android Lint errors ==="
find . -path '*/build/reports/*' -name 'lint-results*.txt' -print0 2>/dev/null |
  xargs -0 -r grep -h -E ': (Error|Warning):' | head -n 60

echo "=== detekt findings ==="
find . -path '*/build/reports/detekt/*.txt' -print0 2>/dev/null |
  xargs -0 -r cat | head -n 60

exit 0
