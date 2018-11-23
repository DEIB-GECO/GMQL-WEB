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

# importing example datasets using repository
ADD ./example_datasets/ /app/example_datasets/
# registering public user
RUN java -jar ./${GMQL_WEB_NAME}/conf/gmql_lib/GMQL-Repository.jar registeruser public
# adding datasets
RUN java -jar ./${GMQL_WEB_NAME}/conf/gmql_lib/GMQL-Repository.jar createds \
                           Example_Dataset_1 \
                           /app/example_datasets/Example_Dataset_1/files/schema.xml \
                           /app/example_datasets/Example_Dataset_1/files/ \
                           public
RUN java -jar ./${GMQL_WEB_NAME}/conf/gmql_lib/GMQL-Repository.jar createds \
                           Example_Dataset_2 \
                           /app/example_datasets/Example_Dataset_2/files/schema.xml \
                           /app/example_datasets/Example_Dataset_2/files/ \
                           public



CMD ["sh", "-c", "./${GMQL_WEB_NAME}/bin/gmql-web"]

