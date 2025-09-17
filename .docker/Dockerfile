# Используем официальный образ WildFly с JDK 17
FROM quay.io/wildfly/wildfly:27.0.1.Final-jdk17

# Добавляем пользователя admin в WildFly для доступа к административной консоли
RUN /opt/jboss/wildfly/bin/add-user.sh admin admin --silent

# Копируем собранный WAR-файл приложения в директорию деплоя WildFly
COPY target/bankx-notification.war /opt/jboss/wildfly/standalone/deployments/

# Создаем директорию для логов (если не существует)
RUN mkdir -p /opt/jboss/wildfly/standalone/log/

# Открываем порты для приложения (8082) и административной консоли (9990)
EXPOSE 8082 9990

# Запускаем WildFly в standalone режиме с привязкой ко всем сетевым интерфейсам
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]