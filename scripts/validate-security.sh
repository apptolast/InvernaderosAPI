#!/bin/bash

# ===========================================
# Security Validation Script
# ===========================================
# This script helps validate that no credentials are exposed
# Run this before committing changes
#
# Usage:
#   chmod +x scripts/validate-security.sh  # Make executable first time
#   ./scripts/validate-security.sh
# ===========================================

set -e

echo "🔒 Running security validation checks..."
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ISSUES_FOUND=0

# Check 1: Look for hardcoded passwords
echo "📋 Check 1: Scanning for hardcoded passwords..."
if grep -rn "password.*=.*['\"][^$]" --include="*.kt" --include="*.java" --include="*.yaml" --include="*.yml" --include="*.properties" --exclude-dir=".git" --exclude-dir="build" --exclude-dir=".gradle" --exclude="SECURITY*.md" 2>/dev/null | grep -v "^[[:space:]]*#" | grep -v ".example" | grep -q .; then
    echo -e "${RED}❌ FAIL: Found hardcoded passwords${NC}"
    ISSUES_FOUND=$((ISSUES_FOUND + 1))
else
    echo -e "${GREEN}✅ PASS: No hardcoded passwords found${NC}"
fi
echo ""

# Check 2: Look for API keys
echo "📋 Check 2: Scanning for API keys and tokens..."
if grep -rEi "(api[_-]?key|api[_-]?secret|access[_-]?token|bearer[_-]?token)\s*[:=]\s*['\"][^$]" --include="*.kt" --include="*.java" --include="*.yaml" --include="*.yml" --include="*.properties" --exclude-dir=".git" --exclude="SECURITY*" 2>/dev/null | grep -v ".example" | grep -q .; then
    echo -e "${RED}❌ FAIL: Found API keys or tokens${NC}"
    ISSUES_FOUND=$((ISSUES_FOUND + 1))
else
    echo -e "${GREEN}✅ PASS: No API keys or tokens found${NC}"
fi
echo ""

# Check 3: Verify .env is in .gitignore
echo "📋 Check 3: Verifying .env is in .gitignore..."
if grep -q "^\.env$" .gitignore; then
    echo -e "${GREEN}✅ PASS: .env is in .gitignore${NC}"
else
    echo -e "${RED}❌ FAIL: .env is not in .gitignore${NC}"
    ISSUES_FOUND=$((ISSUES_FOUND + 1))
fi
echo ""

# Check 4: Verify .env.example exists
echo "📋 Check 4: Verifying .env.example exists..."
if [ -f ".env.example" ]; then
    echo -e "${GREEN}✅ PASS: .env.example exists${NC}"
else
    echo -e "${RED}❌ FAIL: .env.example does not exist${NC}"
    ISSUES_FOUND=$((ISSUES_FOUND + 1))
fi
echo ""

# Check 5: Verify .env is not tracked
echo "📋 Check 5: Verifying .env is not tracked by git..."
if git ls-files | grep -q "^\.env$"; then
    echo -e "${RED}❌ FAIL: .env is tracked by git${NC}"
    ISSUES_FOUND=$((ISSUES_FOUND + 1))
else
    echo -e "${GREEN}✅ PASS: .env is not tracked by git${NC}"
fi
echo ""

# Check 6: Look for common credential patterns
echo "📋 Check 6: Scanning for common credential patterns..."
# Note: These are previously exposed credentials that should no longer appear in the codebase
# The IP 138.199.157.58 was a production Redis server that was exposed in documentation
PATTERNS=(
    "AppToLast2023%"
    "greenhouse2024"
    "api_spring_boot"
    "138.199.157.58"
)

PATTERN_FOUND=0
for pattern in "${PATTERNS[@]}"; do
    if grep -r "$pattern" --include="*.kt" --include="*.java" --include="*.yaml" --include="*.yml" --include="*.md" --exclude-dir=".git" --exclude="SECURITY*" 2>/dev/null | grep -v ".example" | grep -q .; then
        echo -e "${RED}❌ Found pattern: $pattern${NC}"
        PATTERN_FOUND=1
    fi
done

if [ $PATTERN_FOUND -eq 0 ]; then
    echo -e "${GREEN}✅ PASS: No known credential patterns found${NC}"
else
    ISSUES_FOUND=$((ISSUES_FOUND + 1))
fi
echo ""

# Check 7: Verify docker-compose uses environment variables
echo "📋 Check 7: Verifying docker-compose.yaml uses environment variables..."
if [ -f "docker-compose.yaml" ]; then
    if grep -E "PASSWORD.*=.*\$\{" docker-compose.yaml | grep -q .; then
        echo -e "${GREEN}✅ PASS: docker-compose.yaml uses environment variables${NC}"
    else
        echo -e "${YELLOW}⚠️  WARNING: docker-compose.yaml might not be using environment variables${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  WARNING: docker-compose.yaml not found${NC}"
fi
echo ""

# Summary
echo "================================="
if [ $ISSUES_FOUND -eq 0 ]; then
    echo -e "${GREEN}✅ All security checks passed!${NC}"
    echo "Your changes look safe to commit."
    exit 0
else
    echo -e "${RED}❌ Found $ISSUES_FOUND security issue(s)${NC}"
    echo "Please fix the issues before committing."
    echo ""
    echo "For help, see SECURITY.md"
    exit 1
fi
