#!/bin/bash

echo "Waiting for MongoDB to become available..."

MAX_ATTEMPTS=30
ATTEMPT=0

# Цикл ожидания доступности MongoDB
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  # Проверяем доступность MongoDB с помощью команды ping
  if mongosh --host mongodb --port 27017 -u admin -p password --authenticationDatabase admin --eval "db.adminCommand('ping')" > /dev/null 2>&1; then
    echo "MongoDB is available!"
    break
  fi

  echo "Waiting for MongoDB... (attempt $((ATTEMPT+1))/$MAX_ATTEMPTS)"
  sleep 2
  ATTEMPT=$((ATTEMPT+1))
done

# Если после всех попыток MongoDB недоступна, выходим с ошибкой
if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
  echo "Error: MongoDB not available after $MAX_ATTEMPTS attempts"
  exit 1
fi

echo "Initializing MongoDB database..."

# Создаем пользователя приложения с правами на чтение/запись
mongosh --host mongodb --port 27017 -u admin -p password --authenticationDatabase admin <<EOF
use bankx-notification;

db.createUser({
  user: "appuser",
  pwd: "apppassword",
  roles: [
    {
      role: "readWrite",
      db: "bankx-notification"
    }
  ]
});
EOF

# Инициализируем коллекции и данные с использованием учетных данных приложения
mongosh --host mongodb --port 27017 -u appuser -p apppassword --authenticationDatabase bankx-notification <<EOF
use bankx-notification;

// Создаем коллекцию шаблонов писем (если не существует)
db.createCollection('email_templates');

// Вставляем шаблон для регистрации
db.email_templates.insertOne({
  templateType: "registration",
  subject: "Активация аккаунта в BankX",
  body: "Уважаемый(ая) {{firstName}} {{lastName}}, для активации вашего аккаунта перейдите по ссылке: {{activationLink}}",
  variables: ["firstName", "lastName", "activationLink"], // Переменные для подстановки в шаблон
  isActive: true,    // Шаблон активен и будет использоваться
  isHtml: false,     // Шаблон в текстовом формате (не HTML)
  createdAt: new Date(),
  updatedAt: new Date()
});

// Вставляем шаблон для запроса сброса пароля
db.email_templates.insertOne({
  templateType: "password_reset_request",
  subject: "Запрос на сброс пароля BankX",
  body: "Уважаемый(ая) {{firstName}} {{lastName}}! Для восстановления пароля перейдите по ссылке: {{resetLink}}",
  variables: ["firstName", "lastName", "resetLink"],
  isActive: true,
  isHtml: false,
  createdAt: new Date(),
  updatedAt: new Date()
});

// Вставляем шаблон для уведомления об успешном сбросе пароля
db.email_templates.insertOne({
  templateType: "password_reset_success",
  subject: "Пароль BankX изменен",
  body: "Уважаемый(ая) {{firstName}} {{lastName}}, ваш пароль был изменен",
  variables: ["firstName", "lastName"],
  isActive: true,
  isHtml: false,
  createdAt: new Date(),
  updatedAt: new Date()
});

// Вставляем шаблон для уведомления об успешной активации аккаунта
db.email_templates.insertOne({
  templateType: "account_activated",
  subject: "Успешная регистрация в BankX",
  body: "Уважаемый(ая) {{firstName}} {{lastName}}, поздравляем вас с успешной регистрацией в BankX!",
  variables: ["firstName", "lastName"],
  isActive: true,
  isHtml: false,
  createdAt: new Date(),
  updatedAt: new Date()
});

print("MongoDB initialization completed successfully!");
EOF

echo "MongoDB database initialized!"