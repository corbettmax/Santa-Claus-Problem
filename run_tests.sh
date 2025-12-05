#!/bin/bash

################################################################################
# Santa Claus Problem - Automated Test Runner
#
# This script runs all three implementations (Python, C, Java) with various
# test configurations and validates their behavior.
#
# Usage: ./run_tests.sh [options]
# Options:
#   -a, --all       Run all test cases (default)
#   -q, --quick     Run only quick tests (10s duration)
#   -l, --language  Run specific language (python|c|java)
#   -h, --help      Show this help message
################################################################################

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
RUN_PYTHON=true
RUN_C=true
RUN_JAVA=true
RUN_GO=true
TEST_MODE="all"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -q|--quick)
            TEST_MODE="quick"
            shift
            ;;
        -l|--language)
            case $2 in
                python)
                    RUN_C=false
                    RUN_JAVA=false
                    RUN_GO=false
                    ;;
                c)
                    RUN_PYTHON=false
                    RUN_JAVA=false
                    RUN_GO=false
                    ;;
                java)
                    RUN_PYTHON=false
                    RUN_C=false
                    RUN_GO=false
                    ;;
                go)
                    RUN_PYTHON=false
                    RUN_C=false
                    RUN_JAVA=false
                    ;;
                *)
                    echo "Unknown language: $2"
                    exit 1
                    ;;
            esac
            shift 2
            ;;
        -h|--help)
            grep "^#" "$0" | grep -v "^#!/" | sed 's/^# *//'
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Create results directory
RESULTS_DIR="test_results"
mkdir -p "$RESULTS_DIR"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Log file
LOG_FILE="$RESULTS_DIR/test_run_${TIMESTAMP}.log"

################################################################################
# Helper Functions
################################################################################

print_header() {
    echo -e "${BLUE}=================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}=================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

log_message() {
    echo "[$(date +"%Y-%m-%d %H:%M:%S")] $1" >> "$LOG_FILE"
}

run_test() {
    local lang=$1
    local test_name=$2
    local duration=$3
    local output_file=$4
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    print_info "Running $lang - $test_name (${duration}s)"
    log_message "Starting test: $lang - $test_name"
    
    local start_time=$(date +%s)
    
    case $lang in
        Python)
            timeout $((duration + 5)) python3 sc-python.py > "$output_file" 2>&1
            local exit_code=$?
            ;;
        C)
            timeout $((duration + 5)) ./sc-c > "$output_file" 2>&1
            local exit_code=$?
            ;;
        Java)
            timeout $((duration + 5)) java SantaClaus > "$output_file" 2>&1
            local exit_code=$?
            ;;
        Go)
            timeout $((duration + 5)) ./santaclause > "$output_file" 2>&1
            local exit_code=$?
            ;;
    esac
    
    local end_time=$(date +%s)
    local actual_duration=$((end_time - start_time))
    
    # Validate output
    if [ $exit_code -eq 0 ] || [ $exit_code -eq 124 ]; then
        # Check for key indicators in output
        local has_santa=$(grep -c "SANTA:" "$output_file")
        local has_reindeer=$(grep -c "Reindeer" "$output_file")
        local has_elves=$(grep -c "Elf" "$output_file")
        
        if [ $has_santa -gt 0 ] && [ $has_reindeer -gt 0 ] && [ $has_elves -gt 0 ]; then
            print_success "$lang - $test_name (${actual_duration}s)"
            log_message "Test PASSED: $lang - $test_name"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            return 0
        else
            print_error "$lang - $test_name - Missing expected output"
            log_message "Test FAILED: $lang - $test_name - Incomplete output"
            FAILED_TESTS=$((FAILED_TESTS + 1))
            return 1
        fi
    else
        print_error "$lang - $test_name - Exit code: $exit_code"
        log_message "Test FAILED: $lang - $test_name - Exit code: $exit_code"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

analyze_output() {
    local output_file=$1
    local lang=$2
    
    echo ""
    echo "Analysis for $lang:"
    echo "  Deliveries: $(grep -c "Delivery #" "$output_file")"
    echo "  Consultations: $(grep -c "Session #" "$output_file")"
    echo "  Reindeer returns: $(grep -c "Returning from vacation" "$output_file" | head -1)"
    echo "  Elf waiting events: $(grep -c "Waiting for help" "$output_file")"
    echo ""
}

################################################################################
# Main Test Execution
################################################################################

print_header "SANTA CLAUS PROBLEM - TEST SUITE"
echo "Test mode: $TEST_MODE"
echo "Timestamp: $(date)"
echo "Log file: $LOG_FILE"
echo ""

log_message "Test suite started"
log_message "Mode: $TEST_MODE"

# Check for test_cases.json
if [ ! -f "test_cases.json" ]; then
    print_error "test_cases.json not found!"
    exit 1
fi

################################################################################
# Compilation Phase
################################################################################

print_header "COMPILATION PHASE"

# Compile C program
if [ "$RUN_C" = true ]; then
    print_info "Compiling C implementation..."
    if gcc -pthread -o sc-c sc-c.c 2> "$RESULTS_DIR/c_compile.log"; then
        print_success "C compilation successful"
        log_message "C compilation: SUCCESS"
    else
        print_error "C compilation failed (see $RESULTS_DIR/c_compile.log)"
        log_message "C compilation: FAILED"
        RUN_C=false
    fi
fi

# Compile Java program
if [ "$RUN_JAVA" = true ]; then
    print_info "Compiling Java implementation..."
    if javac SantaClaus.java 2> "$RESULTS_DIR/java_compile.log"; then
        print_success "Java compilation successful"
        log_message "Java compilation: SUCCESS"
    else
        print_error "Java compilation failed (see $RESULTS_DIR/java_compile.log)"
        log_message "Java compilation: FAILED"
        RUN_JAVA=false
    fi
fi

# Compile Go program
if [ "$RUN_GO" = true ]; then
    print_info "Compiling Go implementation..."
    if go build -o santaclause santaclause.go 2> "$RESULTS_DIR/go_compile.log"; then
        print_success "Go compilation successful"
        log_message "Go compilation: SUCCESS"
    else
        print_error "Go compilation failed (see $RESULTS_DIR/go_compile.log)"
        log_message "Go compilation: FAILED"
        RUN_GO=false
    fi
fi

# Check Python
if [ "$RUN_PYTHON" = true ]; then
    print_info "Checking Python availability..."
    if python3 --version > /dev/null 2>&1; then
        print_success "Python3 available"
        log_message "Python check: SUCCESS"
    else
        print_error "Python3 not found"
        log_message "Python check: FAILED"
        RUN_PYTHON=false
    fi
fi

echo ""

################################################################################
# Test Execution Phase
################################################################################

print_header "TEST EXECUTION PHASE"

# Test Case 1: Short Duration Test (Quick Test)
if [ "$RUN_PYTHON" = true ]; then
    run_test "Python" "Short Duration Test" 10 "$RESULTS_DIR/python_short_${TIMESTAMP}.txt"
    [ $? -eq 0 ] && analyze_output "$RESULTS_DIR/python_short_${TIMESTAMP}.txt" "Python"
fi

if [ "$RUN_C" = true ]; then
    run_test "C" "Short Duration Test" 10 "$RESULTS_DIR/c_short_${TIMESTAMP}.txt"
    [ $? -eq 0 ] && analyze_output "$RESULTS_DIR/c_short_${TIMESTAMP}.txt" "C"
fi

if [ "$RUN_JAVA" = true ]; then
    run_test "Java" "Short Duration Test" 10 "$RESULTS_DIR/java_short_${TIMESTAMP}.txt"
    [ $? -eq 0 ] && analyze_output "$RESULTS_DIR/java_short_${TIMESTAMP}.txt" "Java"
fi

if [ "$RUN_GO" = true ]; then
    run_test "Go" "Short Duration Test" 10 "$RESULTS_DIR/go_short_${TIMESTAMP}.txt"
    [ $? -eq 0 ] && analyze_output "$RESULTS_DIR/go_short_${TIMESTAMP}.txt" "Go"
fi

# Only run full tests if not in quick mode
if [ "$TEST_MODE" != "quick" ]; then
    echo ""
    print_header "FULL TEST SUITE"
    
    # Test Case 2: Basic Functionality Test (30s)
    if [ "$RUN_PYTHON" = true ]; then
        run_test "Python" "Basic Functionality" 30 "$RESULTS_DIR/python_basic_${TIMESTAMP}.txt"
        [ $? -eq 0 ] && analyze_output "$RESULTS_DIR/python_basic_${TIMESTAMP}.txt" "Python"
    fi
    
    if [ "$RUN_C" = true ]; then
        run_test "C" "Basic Functionality" 30 "$RESULTS_DIR/c_basic_${TIMESTAMP}.txt"
        [ $? -eq 0 ] && analyze_output "$RESULTS_DIR/c_basic_${TIMESTAMP}.txt" "C"
    fi
    
    if [ "$RUN_JAVA" = true ]; then
        run_test "Java" "Basic Functionality" 30 "$RESULTS_DIR/java_basic_${TIMESTAMP}.txt"
        [ $? -eq 0 ] && analyze_output "$RESULTS_DIR/java_basic_${TIMESTAMP}.txt" "Java"
    fi
    
    if [ "$RUN_GO" = true ]; then
        run_test "Go" "Basic Functionality" 30 "$RESULTS_DIR/go_basic_${TIMESTAMP}.txt"
        [ $? -eq 0 ] && analyze_output "$RESULTS_DIR/go_basic_${TIMESTAMP}.txt" "Go"
    fi
    
    # Test Case 3: Extended Duration Test (60s)
    # echo ""
    # print_info "Extended tests available but skipped (uncomment in script to run)"
fi

################################################################################
# Results Summary
################################################################################

echo ""
print_header "TEST RESULTS SUMMARY"

echo "Total tests run: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    print_success "All tests passed!"
    log_message "Test suite completed: ALL PASSED"
    exit 0
else
    print_error "Some tests failed. Check log file: $LOG_FILE"
    log_message "Test suite completed: $FAILED_TESTS FAILED"
    exit 1
fi

################################################################################
# Cleanup compiled files
################################################################################

rm -f sc-c SantaClaus.class santaclause
