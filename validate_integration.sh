#!/bin/bash

# Akahiyaj Integration Validation Script
# Validates that all enhanced components are properly integrated

echo "🚀 Starting Akahiyaj Integration Validation..."
echo "================================================"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Validation functions
validate_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} Found: $1"
        return 0
    else
        echo -e "${RED}✗${NC} Missing: $1"
        return 1
    fi
}

validate_directory() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} Directory exists: $1"
        return 0
    else
        echo -e "${RED}✗${NC} Directory missing: $1"
        return 1
    fi
}

validate_string_in_file() {
    if grep -q "$2" "$1" 2>/dev/null; then
        echo -e "${GREEN}✓${NC} Found '$2' in $1"
        return 0
    else
        echo -e "${RED}✗${NC} Missing '$2' in $1"
        return 1
    fi
}

# Initialize counters
total_checks=0
passed_checks=0

# Validation 1: Check enhanced component files
echo -e "\n${BLUE}1. Validating Enhanced Component Files${NC}"
echo "-------------------------------------"

enhanced_components=(
    "app/src/main/java/com/dawitf/akahidegn/viewmodel/UserProfileViewModel.kt"
    "app/src/main/java/com/dawitf/akahidegn/domain/repository/UserProfileRepository.kt"
    "app/src/main/java/com/dawitf/akahidegn/data/repository/UserProfileRepositoryImpl.kt"
    "app/src/main/java/com/dawitf/akahidegn/analytics/AnalyticsManager.kt"
    "app/src/main/java/com/dawitf/akahidegn/localization/LocalizationManager.kt"
    "app/src/main/java/com/dawitf/akahidegn/offline/OfflineManager.kt"
    "app/src/main/java/com/dawitf/akahidegn/accessibility/AccessibilityManager.kt"
)

for component in "${enhanced_components[@]}"; do
    total_checks=$((total_checks + 1))
    if validate_file "$component"; then
        passed_checks=$((passed_checks + 1))
    fi
done

# Validation 2: Check performance optimization files
echo -e "\n${BLUE}2. Validating Performance Optimization Files${NC}"
echo "--------------------------------------------"

performance_files=(
    "app/src/main/java/com/dawitf/akahidegn/performance/ImageCacheManager.kt"
    "app/src/main/java/com/dawitf/akahidegn/performance/PerformanceManager.kt"
    "app/src/main/java/com/dawitf/akahidegn/performance/NetworkOptimizationManager.kt"
    "app/src/main/java/com/dawitf/akahidegn/di/PerformanceModule.kt"
)

for file in "${performance_files[@]}"; do
    total_checks=$((total_checks + 1))
    if validate_file "$file"; then
        passed_checks=$((passed_checks + 1))
    fi
done

# Validation 3: Check dependency injection setup
echo -e "\n${BLUE}3. Validating Dependency Injection Setup${NC}"
echo "----------------------------------------"

di_file="app/src/main/java/com/dawitf/akahidegn/di/RepositoryModule.kt"
total_checks=$((total_checks + 1))
if validate_file "$di_file"; then
    passed_checks=$((passed_checks + 1))
    
    # Check if enhanced components are properly bound
    total_checks=$((total_checks + 4))
    if validate_string_in_file "$di_file" "bindUserProfileRepository"; then
        passed_checks=$((passed_checks + 1))
    fi
    if validate_string_in_file "$di_file" "bindAnalyticsManager"; then
        passed_checks=$((passed_checks + 1))
    fi
    if validate_string_in_file "$di_file" "bindLocalizationManager"; then
        passed_checks=$((passed_checks + 1))
    fi
    if validate_string_in_file "$di_file" "bindAccessibilityManager"; then
        passed_checks=$((passed_checks + 1))
    fi
fi

# Validation 4: Check MainActivity integration
echo -e "\n${BLUE}4. Validating MainActivity Integration${NC}"
echo "-------------------------------------"

main_activity="app/src/main/java/com/dawitf/akahidegn/MainActivity.kt"
total_checks=$((total_checks + 1))
if validate_file "$main_activity"; then
    passed_checks=$((passed_checks + 1))
    
    # Check for new AppScreen enum values
    total_checks=$((total_checks + 5))
    if validate_string_in_file "$main_activity" "USER_PROFILE"; then
        passed_checks=$((passed_checks + 1))
    fi
    if validate_string_in_file "$main_activity" "SOCIAL"; then
        passed_checks=$((passed_checks + 1))
    fi
    if validate_string_in_file "$main_activity" "ENHANCED_SEARCH"; then
        passed_checks=$((passed_checks + 1))
    fi
    if validate_string_in_file "$main_activity" "ACTIVITY_HISTORY"; then
        passed_checks=$((passed_checks + 1))
    fi
    if validate_string_in_file "$main_activity" "ACCESSIBILITY_SETTINGS"; then
        passed_checks=$((passed_checks + 1))
    fi
    
    # Check for injected enhanced managers
    total_checks=$((total_checks + 4))
    if validate_string_in_file "$main_activity" "lateinit var analyticsManager"; then
        passed_checks=$((passed_checks + 1))
    fi
    if validate_string_in_file "$main_activity" "lateinit var performanceManager"; then
        passed_checks=$((passed_checks + 1))
    fi
    if validate_string_in_file "$main_activity" "lateinit var imageCacheManager"; then
        passed_checks=$((passed_checks + 1))
    fi
    if validate_string_in_file "$main_activity" "performanceManager.startMonitoring"; then
        passed_checks=$((passed_checks + 1))
    fi
fi

# Validation 5: Check test files
echo -e "\n${BLUE}5. Validating Test Files${NC}"
echo "------------------------"

test_files=(
    "app/src/test/java/com/dawitf/akahidegn/integration/NavigationIntegrationTest.kt"
    "app/src/test/java/com/dawitf/akahidegn/e2e/CriticalUserFlowsE2ETest.kt"
    "app/src/test/java/com/dawitf/akahidegn/performance/testing/PerformanceBenchmarkTest.kt"
)

for file in "${test_files[@]}"; do
    total_checks=$((total_checks + 1))
    if validate_file "$file"; then
        passed_checks=$((passed_checks + 1))
    fi
done

# Validation 6: Check UI components integration
echo -e "\n${BLUE}6. Validating UI Component Integration${NC}"
echo "-------------------------------------"

ui_components=(
    "app/src/main/java/com/dawitf/akahidegn/ui/profile"
    "app/src/main/java/com/dawitf/akahidegn/ui/social"
    "app/src/main/java/com/dawitf/akahidegn/ui/search"
    "app/src/main/java/com/dawitf/akahidegn/ui/activity"
    "app/src/main/java/com/dawitf/akahidegn/ui/accessibility"
)

for component in "${ui_components[@]}"; do
    total_checks=$((total_checks + 1))
    if validate_directory "$component"; then
        passed_checks=$((passed_checks + 1))
    fi
done

# Validation 7: Check for build compatibility
echo -e "\n${BLUE}7. Validating Build Configuration${NC}"
echo "---------------------------------"

build_file="app/build.gradle.kts"
total_checks=$((total_checks + 1))
if validate_file "$build_file"; then
    passed_checks=$((passed_checks + 1))
    
    # Check for required dependencies
    total_checks=$((total_checks + 2))
    if validate_string_in_file "$build_file" "hilt"; then
        passed_checks=$((passed_checks + 1))
    fi
    if validate_string_in_file "$build_file" "compose"; then
        passed_checks=$((passed_checks + 1))
    fi
fi

# Calculate final score
percentage=$((passed_checks * 100 / total_checks))

# Display results
echo -e "\n${YELLOW}================================================${NC}"
echo -e "${YELLOW}Integration Validation Results${NC}"
echo -e "${YELLOW}================================================${NC}"
echo -e "Total Checks: $total_checks"
echo -e "Passed Checks: $passed_checks"
echo -e "Failed Checks: $((total_checks - passed_checks))"
echo -e "Success Rate: $percentage%"

if [ $percentage -ge 90 ]; then
    echo -e "\n${GREEN}🎉 Excellent! Integration is highly complete.${NC}"
    exit_code=0
elif [ $percentage -ge 75 ]; then
    echo -e "\n${YELLOW}⚠️  Good! Most integrations are complete, but some items need attention.${NC}"
    exit_code=0
elif [ $percentage -ge 50 ]; then
    echo -e "\n${YELLOW}⚠️  Partial integration. Several items need to be completed.${NC}"
    exit_code=1
else
    echo -e "\n${RED}❌ Integration is incomplete. Many items need attention.${NC}"
    exit_code=1
fi

# Additional recommendations
echo -e "\n${BLUE}Next Steps:${NC}"
echo "1. Run integration tests: ./gradlew test"
echo "2. Run UI tests: ./gradlew connectedAndroidTest"
echo "3. Check for compilation errors: ./gradlew assembleDebug"
echo "4. Validate performance: ./gradlew benchmark"

exit $exit_code
