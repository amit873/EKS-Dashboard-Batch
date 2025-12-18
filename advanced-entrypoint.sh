#!/bin/sh

# Advanced entrypoint with config-based JAR discovery
# Usage: advanced-entrypoint.sh [batch_name]

BATCH_TYPE=${1:-dashboard}
CONFIG_FILE="/app/batch-config.properties"

# Function to get JAR file from config
get_jar_file() {
    local batch_name=$1
    if [ -f "$CONFIG_FILE" ]; then
        grep "^batch\.$batch_name\.jar=" "$CONFIG_FILE" | cut -d'=' -f2
    fi
}

# Get JAR file for the requested batch
JAR_FILE=$(get_jar_file "$BATCH_TYPE")

if [ -n "$JAR_FILE" ] && [ -f "/app/$JAR_FILE" ]; then
    echo "Starting $BATCH_TYPE Application..."
    exec java -jar "$JAR_FILE"
else
    echo "Error: Unknown or missing application: $BATCH_TYPE"
    echo "Available applications:"
    if [ -f "$CONFIG_FILE" ]; then
        grep "^batch\." "$CONFIG_FILE" | cut -d'.' -f2 | cut -d'.' -f1
    fi
    exit 1
fi