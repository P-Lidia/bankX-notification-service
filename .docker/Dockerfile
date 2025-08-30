FROM quay.io/wildfly/wildfly:27.0.1.Final-jdk17
RUN /opt/jboss/wildfly/bin/add-user.sh admin admin --silent
COPY target/bankx-notification.war /opt/jboss/wildfly/standalone/deployments/
RUN mkdir -p /opt/jboss/wildfly/standalone/log/
EXPOSE 8080 9990
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]