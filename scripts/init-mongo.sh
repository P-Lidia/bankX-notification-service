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

# Инициализируем коллекции и данные с проверкой существования шаблонов
mongosh --host mongodb --port 27017 -u appuser -p apppassword --authenticationDatabase bankx-notification <<EOF
use bankx-notification;

// Создаем коллекцию шаблонов писем (если не существует)
db.createCollection('email_templates');

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

// Шаблон регистрации
insertTemplateIfNotExists({
  templateType: "registration",
  subject: "🌸 Активация аккаунта в BankX",
  body:
    "<div style='background-color: #FFC0CB; padding: 20px;'>" +
    "<p>Уважаемый(ая) <strong>\\\${firstName} \\\${lastName}</strong>,</p>" +
    "<p>Для активации вашего аккаунта перейдите по ссылке:</p>" +
    "<p><a href='\\\${activationLink}'>\\\${activationLink}</a></p>" +
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

insertTemplateIfNotExists({
  templateType: "password_reset_request",
  subject: "❤️‍🩹 Запрос на сброс пароля BankX",
  body:
    "<div style='background-color: #e0f8e0; padding: 20px;'>" +
    "<p>Уважаемый(ая) <strong>\\\${firstName} \\\${lastName}</strong>,</p>" +
    "<p>Для восстановления пароля перейдите по ссылке:</p>" +
    "<p><a href='\\\${resetLink}'>\\\${resetLink}</a></p>" +
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

// Шаблон успешного сброса пароля
insertTemplateIfNotExists({
  templateType: "password_reset_success",
  subject: "Пароль BankX изменен",
  body: "<p>Уважаемый(ая) <strong>\\\${firstName} \\\${lastName}</strong>,</p>" +
        "<p>Ваш пароль был успешно изменен.</p>" +
        "<br>" +
        "<p>Если это были не вы, немедленно свяжитесь с нашей поддержкой.</p>" +
        "<br>" +
        "<p>С уважением,<br>Команда BankX</p>",
  variables: ["firstName", "lastName"],
  isActive: true,
  isHtml: true,
  createdAt: now,
  updatedAt: now
});

// Шаблон успешной активации аккаунта
insertTemplateIfNotExists({
  templateType: "account_activated",
  subject: "Успешная регистрация в BankX",
  body: "<p>Уважаемый(ая) <strong>\\\${firstName} \\\${lastName}</strong>,</p>" +
        "<p>Поздравляем вас с успешной регистрацией в BankX!</p>" +
        "<br>" +
        "<p>Теперь вы можете войти в свой аккаунт и использовать все возможности нашего сервиса.</p>" +
        "<br>" +
        "<p>С уважением,<br>Команда BankX</p>",
  variables: ["firstName", "lastName"],
  isActive: true,
  isHtml: true,
  createdAt: now,
  updatedAt: now
});

print("MongoDB initialization completed successfully!");
EOF

echo "MongoDB database initialized!"