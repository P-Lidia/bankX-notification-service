#!/bin/bash

echo "Waiting for Kafka to become available..."

if command -v cub &> /dev/null; then
    cub kafka-ready -b kafka:9092 1 20
else
    while ! nc -z kafka 9092; do
        sleep 1
    done
    sleep 10
fi

echo "Creating Kafka topics..."

kafka-topics --create --if-not-exists \
  --topic notifications.registration.events \
  --bootstrap-server kafka:9092 \
  --partitions 3 \
  --replication-factor 1

kafka-topics --create --if-not-exists \
  --topic notifications.reset.password.events \
  --bootstrap-server kafka:9092 \
  --partitions 3 \
  --replication-factor 1

kafka-topics --create --if-not-exists \
  --topic notifications.transaction.events \
  --bootstrap-server kafka:9092 \
  --partitions 3 \
  --replication-factor 1

echo "Kafka topics created successfully"

echo "Listing all topics:"
kafka-topics --list --bootstrap-server kafka:9092