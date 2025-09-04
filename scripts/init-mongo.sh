#!/bin/bash

echo "Waiting for MongoDB to become available..."

MAX_ATTEMPTS=30
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  if mongosh --host mongodb --port 27017 -u admin -p password --authenticationDatabase admin --eval "db.adminCommand('ping')" > /dev/null 2>&1; then
    echo "MongoDB is available!"
    break
  fi

  echo "Waiting for MongoDB... (attempt $((ATTEMPT+1))/$MAX_ATTEMPTS)"
  sleep 2
  ATTEMPT=$((ATTEMPT+1))
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
  echo "Error: MongoDB not available after $MAX_ATTEMPTS attempts"
  exit 1
fi

echo "Initializing MongoDB database..."

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

mongosh --host mongodb --port 27017 -u appuser -p apppassword --authenticationDatabase bankx-notification <<EOF
use bankx-notification;

db.createCollection('email_templates');

db.email_templates.insertOne({
  templateType: "registration",
  subject: "Активация аккаунта в BankX",
  body: "Уважаемый(ая) {{firstName}} {{lastName}}, для активации вашего аккаунта перейдите по ссылке: {{activationLink}}",
  variables: ["firstName", "lastName", "activationLink"],
  isActive: true,
  createdAt: new Date(),
  updatedAt: new Date()
});

db.email_templates.insertOne({
  templateType: "password_reset",
  subject: "Восстановление пароля в BankX",
  body: "Уважаемый(ая) {{firstName}} {{lastName}}! Для восстановления пароля перейдите по ссылке: {{resetLink}}",
  variables: ["firstName", "lastName", "resetLink"],
  isActive: true,
  createdAt: new Date(),
  updatedAt: new Date()
});

db.createCollection('notifications');

db.notifications.createIndex({ email: 1 });
db.notifications.createIndex({ type: 1 });
db.notifications.createIndex({ createdAt: -1 });
db.notifications.createIndex({ status: 1 });

print("MongoDB initialization completed successfully!");
EOF

echo "MongoDB database initialized!"