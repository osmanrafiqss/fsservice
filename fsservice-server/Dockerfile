FROM openjdk:8-jre-slim

# configure default port for fsservice
ENV port 8080

# Add Maven dependencies
ADD lib /usr/share/fsservice/lib

# Add the service jar
ADD ${project.build.finalName}.jar /usr/share/fsservice/fsservice.jar

# Create application log directory
RUN mkdir /var/log/fsservice

# Add the logging properties
ADD logging.properties /usr/share/fsservice/logging.properties

# cmd to execute
CMD java -Djava.util.logging.config.file=/usr/share/fsservice/logging.properties -jar /usr/share/fsservice/fsservice.jar -p $port