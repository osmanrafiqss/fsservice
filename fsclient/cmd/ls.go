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
	"time"

	api "github.com/osmanrafiqss/fsservice/fsclient/api"
	"github.com/spf13/cobra"
	"google.golang.org/grpc"
)

// lsCmd represents the ls command
var lsCmd = &cobra.Command{
	Use:   "ls [server] [path]",
	Short: "Lists the content of a filesystem service directory",
	Long: `Performs the ls command against a filesystem service, listing its directory contents.
	The command takes two arguments, the server on which to access the filesystem service and the path to list.

	For example:
	ls 127.0.0.1:8080 /`,
	Args: cobra.ExactArgs(2),
	RunE: func(cmd *cobra.Command, args []string) error {
		conn, connErr := grpc.Dial(args[0], grpc.WithInsecure())
		if connErr != nil {
			return fmt.Errorf("could not connect to filesystem service: %v", connErr)
		}
		defer conn.Close()

		// create client using connection
		client := api.NewFsServiceClient(conn)

		// list root
		filesRequest := api.ListFilesRequest{PathName: args[1]}
		lsErr := printLs(client, filesRequest)
		if lsErr != nil {
			return fmt.Errorf("could not perform list against filesystem service: %v", lsErr)
		}

		return nil
	},
}

func init() {
	rootCmd.AddCommand(lsCmd)
}

func printLs(client api.FsServiceClient, filesRequest api.ListFilesRequest) error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	list, err := client.List(ctx, &filesRequest)
	if err != nil {
		return fmt.Errorf("%v.List(_) = _, %v", client, err)
	}

	for {
		file, err := list.Recv()
		if err == io.EOF {
			break
		}
		if err != nil {
			return fmt.Errorf("%v.List(_) = _, %v", client, err)
		}
		fmt.Println(file)
	}

	return nil
}
