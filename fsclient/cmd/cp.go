// Copyright © 2019 NAME HERE <EMAIL ADDRESS>
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package cmd

import (
	"context"
	"fmt"
	"io"
	"os"
	"time"

	api "github.com/osmanrafiqss/fsservice/fsclient/api"
	"github.com/spf13/cobra"
	"google.golang.org/grpc"
)

// cpCmd represents the cp command
var cpCmd = &cobra.Command{
	Use:   "cp --service <service addr> [local path] [path]",
	Short: "Copies a local file to the remote filesystem service",
	Long: `Copies a local file to the remote filesystem service.
	The command takes two arguments, the [local path] to the local file to copy and the remote [path] to copy the file to.

	For example:
	cp -s 127.0.0.1:8080 /test.txt /usr/test.txt`,
	Args: cobra.ExactArgs(2),
	RunE: func(cmd *cobra.Command, args []string) error {
		conn, connErr := grpc.Dial(serviceAddr, grpc.WithInsecure())
		if connErr != nil {
			return fmt.Errorf("could not connect to filesystem service: %v", connErr)
		}
		defer conn.Close()

		// create client using connection
		client := api.NewFsServiceClient(conn)

		// copy file
		cpErr := copy(client, args[0], args[1])
		if cpErr != nil {
			return fmt.Errorf("could not copy file: %v", cpErr)
		}

		return nil
	},
}

func init() {
	rootCmd.AddCommand(cpCmd)
}

func copy(client api.FsServiceClient, localFilePath string, remoteFilePath string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	cp, err := client.Cp(ctx)
	if err != nil {
		return fmt.Errorf("Error invoking cp command: %v", err)
	}
	defer cp.CloseSend()

	file, err := os.Open(localFilePath)
	if err != nil {
		return fmt.Errorf("Cannot read local file: %v", err)
	}
	defer file.Close()

	// read chunks of file and send to cp
	fileBuffer := make([]byte, 1400)
	for {
		numBytes, err := file.Read(fileBuffer)
		if err != nil && err != io.EOF {
			return fmt.Errorf("Local file read failed: %v", err)
		}

		content := api.FileContent{Chunk: fileBuffer[:numBytes], PathName: remoteFilePath};
		cpErr := cp.Send(&content)

		if cpErr != nil {
			return fmt.Errorf("Copy failed: %v", cpErr)
		}

		if err == io.EOF {
			break
		}
	}

	// Signal transfer complete
	_, closingErr := cp.CloseAndRecv()
	if closingErr != nil {
		return fmt.Errorf("Copy failed: %v", closingErr)
	}

	return nil
}
