#!/bin/bash

echo "Waiting for Kafka to become available..."

MAX_ATTEMPTS=30
ATTEMPT=0

# Цикл ожидания доступности Kafka
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  # Проверяем доступность Kafka, пытаясь получить список топиков
  if kafka-topics --bootstrap-server kafka:9092 --list > /dev/null 2>&1; then
    echo "Kafka is available!"
    break
  fi

  echo "Waiting for Kafka... (attempt $((ATTEMPT+1))/$MAX_ATTEMPTS)"
  sleep 5
  ATTEMPT=$((ATTEMPT+1))
done

# Если после всех попыток Kafka недоступна, выходим с ошибкой
if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
  echo "Error: Kafka not available after $MAX_ATTEMPTS attempts"
  exit 1
fi

echo "Creating Kafka topics..."

# Создаем топики с проверкой ошибок
# Формат: "имя_топика:количество_партиций:фактор_репликации"
topics=(
  "notifications.registration.events:3:1"       # Топик для событий регистрации
  "notifications.reset.password.events:3:1"     # Топик для событий сброса пароля
  "notifications.transaction.events:3:1"        # Топик для событий транзакций
)

# Создание каждого топика из списка
for topic in "${topics[@]}"; do
  # Разбиваем строку на компоненты (имя, партиции, репликация)
  IFS=':' read -r name partitions replication <<< "$topic"

  # Создаем топик, если он еще не существует
  kafka-topics --create --if-not-exists \
    --topic "$name" \
    --bootstrap-server kafka:9092 \
    --partitions "$partitions" \
    --replication-factor "$replication"

  # Проверяем успешность создания топика
  if [ $? -eq 0 ]; then
    echo "Topic $name created successfully"
  else
    echo "Failed to create topic $name"
  fi
done

echo "Kafka topics creation completed"

# Выводим список всех топиков для подтверждения
echo "Listing all topics:"
kafka-topics --list --bootstrap-server kafka:9092