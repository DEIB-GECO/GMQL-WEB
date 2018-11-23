# JVM docker image
FROM openjdk:8-jre

ENV GMQL_WEB_NAME gmql_web
ENV GMQL_EXECUTABLE ${GMQL_WEB_NAME}/bin/gmql-web

# Set the working directory to /app
WORKDIR /app

# Add the gmql_web.zip to the image
ADD ./${GMQL_WEB_NAME}.zip .

# Create directory for the volume
RUN mkdir ./volume/

# Make port 8000 available to the world outside this container
EXPOSE 8000

# Install
RUN unzip /app/${GMQL_WEB_NAME}.zip
RUN rm /app/${GMQL_WEB_NAME}.zip
RUN mv gmql-web*/ ${GMQL_WEB_NAME}/
RUN ls -a /app

# move configuration files
RUN rm ./${GMQL_WEB_NAME}/conf/gmql_conf/repository.xml
ADD ./docker_conf/repository.xml /app/${GMQL_WEB_NAME}/conf/gmql_conf/
RUN rm ./${GMQL_WEB_NAME}/conf/gmql_conf/executor.xml
ADD ./docker_conf/executor.xml /app/${GMQL_WEB_NAME}/conf/gmql_conf/
RUN rm ./${GMQL_WEB_NAME}/conf/application.conf
ADD ./docker_conf/application.conf /app/${GMQL_WEB_NAME}/conf/



CMD ["sh", "-c", "./${GMQL_WEB_NAME}/bin/gmql-web"]

