FROM quay.io/wildfly/wildfly:27.0.1.Final-jdk17

# Добавляем администратора
RUN /opt/jboss/wildfly/bin/add-user.sh admin admin --silent

# Копируем WAR файл
COPY target/bankx-notification.war /opt/jboss/wildfly/standalone/deployments/

# Создаем директорию для логов
RUN mkdir -p /opt/jboss/wildfly/standalone/log/

# Открываем порты
EXPOSE 8080 9990

# Запускаем WildFly
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]