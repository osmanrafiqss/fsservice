### Introduction

The Filesystem Service exposes access to the filesystem of the host executing the service using gRPC. The purpose of this project is to trial gRPC based microservices on Kubernetes.

TODO

1. ~~Create go client to utilize fsservice~~
2. Extend fsservice with cat command
3. Convince Thomas to install fsservice on his computer and cat all his files (hopefully finding holiday pictures to share on the intranet)
4. Install onto minikube (kubernetes)
5. Look into envoy / istio for load balancing with services

### Getting Started

Build the project using maven:

mvn clean install

The build results in a docker image - soprasteria/fsservice:latest - being created. 

Create and run the fsservice image using docker:

docker run -p 8080:8080 --name fsservice soprasteria/fsservice:latest 

For running fsservice on another port use the following docker command:

docker run -p <port>:<port> --e port=<port> --name fsservice soprasteria/fsservice:latest

### Private Registry

Publish the docker image to a private docker registry using maven:

mvn clean deploy -Ddocker.username="username" .Ddocker.password="password"

The above command attempts to publish the Docker image to the private registry hosted on Azure, however this may be overriden by providing the registry as a parameter:

mvn clean deploy -Ddocker.push.registry="url" -Ddocker.username="username" .Ddocker.password="password"

### Docker

Originally the intent was to let the Docker image build uses the openjdk:12 image as its base as this is the first openjdk image to truly support the Docker multiarch manifest. This enables the image to be built on both Linux and Windows hosts without change.

https://github.com/docker-library/openjdk/pull/273



However, this poses the issues of how to resolve where to put the jar files as the paths still differ between the two platforms.

So instead the Docker image is built using the 8-jre-slim image and creating a container in Windows will require you to switch to Linux containers.