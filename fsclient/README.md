### Introduction

The fsclient is a CLI to utilize the gRPC based filesystem service defined in ../fsservice

The client is implemented in the go language using the Cobra framework for building CLI's.
https://github.com/spf13/cobra


### Getting Started

Prerequisites:

1. Ensure that you have at least golang v1.11 installed: https://golang.org/dl/
   1. Ensure that you have a %GOPATH defined.
   2. Ensure that %GOPATH%\bin is part of your %PATH%.
2. Ensure that you have protoc installed: https://github.com/protocolbuffers/protobuf/releases
   1. Ensure that protoc binary is part of you %PATH%

Building:

It seems the go community is moving towards a more flexible form of modularization in which modules are not tied to the folder structure of the %GOPATH. The fsclient is implemented using go modules and under the perception that the %GOPATH in the go modules paradigm functions like a local maven repository.

Build the fsclient by doin the following:
1. in the fsclient source folder perform: go mod download
2. execute the build.bat or perform the steps listed in the script

Note step 1) is required to download the necessary dependencies prior to executing the protoc-gen-go generator.

### Notes on adding new commands

As the Cobra framework does not currently support go modules (https://github.com/spf13/cobra/pull/817) adding new commands without using a copy-paste approach will require you to copy the folder back and forth between your $GOPATH.