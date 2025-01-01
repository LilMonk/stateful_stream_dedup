#!/bin/bash

# List of required environment variables
required_vars=(
  "KAFKA_SOURCE_TOPIC"
  "KAFKA_BROKER"
  "KAFKA_SASL_USERNAME"
  "KAFKA_SASL_PASSWORD"
  "KAFKA_SECURITY_PROTOCOL"
  "KAFKA_SASL_MECHANISM"
)

# Check if each required environment variable is set
for var in "${required_vars[@]}"; do
  if [ -z "${!var}" ]; then
    echo "Error: Environment variable $var is not set."
    exit 1
  else
    echo "$var=${!var}"
  fi
done

echo "All required environment variables are set."

# Run the Kafka samples ingestion script
python3 kafka_samples_ingestion.py