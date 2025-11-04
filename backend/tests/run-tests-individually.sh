#!/bin/bash

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
FAILED_TESTS=()

# Array of test files
TEST_FILES=(
  "mocked/authM.test.ts"
  "mocked/eventM.test.ts"
  "mocked/userM.test.ts"
  "mocked/chatM.test.ts"
  "mocked/socketM.test.ts"
  "unmocked/eventNM.test.ts"
  "unmocked/userNM.test.ts"
  "unmocked/chatNM.test.ts"
  "unmocked/socketNM.test.ts"
)

# Run each test file
for test_file in "${TEST_FILES[@]}"; do
  echo "=========================================="
  echo -e "${YELLOW}Running: $test_file${NC}"
  echo "=========================================="
  
  if npx jest "$test_file" --runInBand --coverage; then
    echo -e "${GREEN}✓ PASSED: $test_file${NC}"
    ((PASSED++))
  else
    echo -e "${RED}✗ FAILED: $test_file${NC}"
    ((FAILED++))
    FAILED_TESTS+=("$test_file")
  fi
  
  echo ""
  echo ""
done

# Summary
echo "=========================================="
echo "TEST SUMMARY"
echo "=========================================="
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"

if [ ${#FAILED_TESTS[@]} -gt 0 ]; then
  echo ""
  echo "Failed tests:"
  for failed_test in "${FAILED_TESTS[@]}"; do
    echo -e "  ${RED}✗ $failed_test${NC}"
  done
  exit 1
else
  echo -e "${GREEN}All tests passed!${NC}"
  exit 0
fi
