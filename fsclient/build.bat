:: install protoc-gen-go into %GOPATH%\bin using get while we wait for 1.12 (https://github.com/golang/go/issues/24250)
go get -u github.com/golang/protobuf/protoc-gen-go

:: use protoc-gen-go to generate go-stubs of fsservice and place them in api folder
mkdir api
protoc --go_out=plugins=grpc:./api --proto_path=..\fsservice\src\main\proto ..\fsservice\src\main\proto\fsservice.proto

:: build the fsclient
go build github.com/osmanrafiqss/fsservice/fsclient