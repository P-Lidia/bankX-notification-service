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

# Инициализируем коллекции
mongosh --host mongodb --port 27017 -u appuser -p apppassword --authenticationDatabase bankx-notification <<EOF
use bankx-notification;

// Создаем коллекцию шаблонов писем (если не существует)
db.createCollection('email_templates');

// Создаем коллекцию для логов уведомлений (если не существует)
db.createCollection('notification_logs');

// Создаем индексы для логов
db.notification_logs.createIndex({ "email": 1 });
db.notification_logs.createIndex({ "activation_key": 1 });
db.notification_logs.createIndex({ "reset_token": 1 });
db.notification_logs.createIndex({ "created_at": 1 });
db.notification_logs.createIndex({ "status": 1 });

// Функция для вставки шаблона, если его нет
function insertTemplateIfNotExists(template) {
  const exists = db.email_templates.findOne({ templateType: template.templateType });
  if (!exists) {
    db.email_templates.insertOne(template);
    print("Inserted template: " + template.templateType);
  } else {
    print("Template already exists: " + template.templateType);
  }
}

const now = new Date();

// Вставляем шаблон для регистрации
insertTemplateIfNotExists({
  templateType: "registration",
  subject: "🌸 Активация аккаунта в BankX",
  body:
    "<div style='background-color: #FFC0CB; padding: 20px;'>" +
    "<p>Уважаемый(ая) <strong>\${firstName} \${lastName}</strong>,</p>" +
    "<p>Для активации вашего аккаунта перейдите по ссылке:</p>" +
    "<p><a href='\${activationLink}'>\${activationLink}</a></p>" +
    "<p>Если вы не регистрировались, просто проигнорируйте это сообщение.</p>" +
    "<br>" +
    "<p>С уважением,<br>Команда BankX</p>" +
    "</div>",
  variables: ["firstName", "lastName", "activationLink"],
  isActive: true,
  isHtml: true,
  createdAt: now,
  updatedAt: now
});

// Вставляем шаблон для запроса сброса пароля
insertTemplateIfNotExists({
  templateType: "password_reset_request",
  subject: "❤️‍🩹 Запрос на сброс пароля BankX",
  body:
    "<div style='background-color: #e0f8e0; padding: 20px;'>" +
    "<p>Уважаемый(ая) <strong>\${firstName} \${lastName}</strong>,</p>" +
    "<p>Для восстановления пароля перейдите по ссылке:</p>" +
    "<p><a href='\${resetLink}'>\${resetLink}</a></p>" +
    "<p>Если вы не запрашивали сброс пароля, просто проигнорируйте это сообщение.</p>" +
    "<br>" +
    "<p>С уважением,<br>Команда BankX</p>" +
    "</div>",
  variables: ["firstName", "lastName", "resetLink"],
  isActive: true,
  isHtml: true,
  createdAt: now,
  updatedAt: now
});

// Вставляем шаблон для уведомления об успешном сбросе пароля
insertTemplateIfNotExists({
  templateType: "password_reset_success",
  subject: "Пароль BankX изменен",
  body: "Уважаемый(ая) \${firstName} \${lastName}, ваш пароль был изменен",
  variables: ["firstName", "lastName"],
  isActive: true,
  isHtml: false,
  createdAt: now,
  updatedAt: now
});

// Вставляем шаблон для уведомления об успешной активации аккаунта
insertTemplateIfNotExists({
  templateType: "account_activated",
  subject: "Успешная регистрация в BankX",
  body: "Уважаемый(ая) \${firstName} \${lastName}, поздравляем вас с успешной регистрацией в BankX!",
  variables: ["firstName", "lastName"],
  isActive: true,
  isHtml: false,
  createdAt: now,
  updatedAt: now
});

print("MongoDB initialization completed successfully!");
EOF

echo "MongoDB database initialized!"