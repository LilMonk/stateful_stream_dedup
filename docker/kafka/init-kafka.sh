#!/bin/bash

# Wait for Kafka and Zookeeper to be ready
sleep 10

# Add users to Kafka with SCRAM-SHA-256 authentication
kafka-configs --zookeeper zookeeper:2181 \
    --alter --add-config 'SCRAM-SHA-256=[password=client-secret]' \
    --entity-type users --entity-name client

kafka-configs --zookeeper zookeeper:2181 \
    --alter --add-config 'SCRAM-SHA-256=[password=admin-secret]' \
    --entity-type users --entity-name admin

# Confirm the users are added
echo "Users added:"
kafka-configs --zookeeper zookeeper:2181 --describe --entity-type users

# Create topics
# retention time 1 week
kafka-topics --bootstrap-server kafka:9093 \
    --create --topic iot_events \
    --partitions 10 \
    --replication-factor 1 \
    --config retention.ms=604800000

kafka-topics --bootstrap-server kafka:9093 \
    --create --topic iot_dedup_events \
    --partitions 10 \
    --replication-factor 1 \
    --config retention.ms=604800000

sleep 10

echo "Topics created:"