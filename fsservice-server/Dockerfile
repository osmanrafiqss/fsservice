FROM openjdk:8-jre-slim

# configure default port for fsservice
ENV port 8080

# Add Maven dependencies
ADD target/lib /usr/share/fsservice/lib

# Add the service jar
ADD target/${project.build.finalName}.jar /usr/share/fsservice/fsservice.jar

# cmd to execute
CMD /usr/bin/java -jar /usr/share/fsservice/fsservice.jar -p $port