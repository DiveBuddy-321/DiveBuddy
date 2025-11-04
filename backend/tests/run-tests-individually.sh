#!/bin/sh

# Script to run each test file individually
# This helps identify which test file might be causing issues

set -e  # Exit on first error (remove this line if you want to continue on errors)

echo "=========================================="
echo "Running tests individually..."
echo "=========================================="
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
FAILED_TESTS=""

# List of test files (space-separated, works in /bin/sh)
TEST_FILES="
mocked/authM.test.ts
mocked/eventM.test.ts
mocked/userM.test.ts
mocked/chatM.test.ts
mocked/socketM.test.ts
unmocked/eventNM.test.ts
unmocked/userNM.test.ts
unmocked/chatNM.test.ts
unmocked/socketNM.test.ts
"

# Run each test file
for test_file in $TEST_FILES; do
  echo "=========================================="
  printf "${YELLOW}Running: %s${NC}\n" "$test_file"
  echo "=========================================="

  if npx jest "$test_file" --runInBand --coverage; then
    printf "${GREEN}✓ PASSED: %s${NC}\n" "$test_file"
    PASSED=$((PASSED + 1))
  else
    printf "${RED}✗ FAILED: %s${NC}\n" "$test_file"
    FAILED=$((FAILED + 1))
    FAILED_TESTS="$FAILED_TESTS\n  ${RED}✗ $test_file${NC}"
  fi

  echo ""
done

# Summary
echo "=========================================="
echo "TEST SUMMARY"
echo "=========================================="
printf "${GREEN}Passed: %s${NC}\n" "$PASSED"
printf "${RED}Failed: %s${NC}\n" "$FAILED"

if [ "$FAILED" -gt 0 ]; then
  echo ""
  echo "Failed tests:"
  printf "$FAILED_TESTS\n"
  exit 1
else
  printf "${GREEN}All tests passed!${NC}\n"
  exit 0
fi