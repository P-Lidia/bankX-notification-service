#!/bin/bash

echo "Waiting for Kafka to become available..."

MAX_ATTEMPTS=30
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  if kafka-topics --bootstrap-server kafka:9092 --list > /dev/null 2>&1; then
    echo "Kafka is available!"
    break
  fi

  echo "Waiting for Kafka... (attempt $((ATTEMPT+1))/$MAX_ATTEMPTS)"
  sleep 5
  ATTEMPT=$((ATTEMPT+1))
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
  echo "Error: Kafka not available after $MAX_ATTEMPTS attempts"
  exit 1
fi

echo "Creating Kafka topics..."

# Создаем топики с проверкой ошибок
topics=(
  "notifications.registration.events:3:1"
  "notifications.reset.password.events:3:1"
  "notifications.transaction.events:3:1"
)

for topic in "${topics[@]}"; do
  IFS=':' read -r name partitions replication <<< "$topic"

  kafka-topics --create --if-not-exists \
    --topic "$name" \
    --bootstrap-server kafka:9092 \
    --partitions "$partitions" \
    --replication-factor "$replication"

  if [ $? -eq 0 ]; then
    echo "Topic $name created successfully"
  else
    echo "Failed to create topic $name"
  fi
done

echo "Kafka topics creation completed"

echo "Listing all topics:"
kafka-topics --list --bootstrap-server kafka:9092