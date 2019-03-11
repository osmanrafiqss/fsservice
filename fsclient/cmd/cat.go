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

// catCmd represents the cat command
var catCmd = &cobra.Command{
	Use:   "cat [server] [path]",
	Short: "Print a file on the standard output",
	Long: `Performs the cat command against a filesystem service, printing the file content.
	The command takes two arguments, the server on which to access the filesystem service and the path to a file.

	For example:
	cat 127.0.0.1:8080 /test.txt`,
	Args: cobra.ExactArgs(2),
	RunE: func(cmd *cobra.Command, args []string) error {
		conn, connErr := grpc.Dial(args[0], grpc.WithInsecure())
		if connErr != nil {
			return fmt.Errorf("could not connect to filesystem service: %v", connErr)
		}
		defer conn.Close()

		// create client using connection
		client := api.NewFsServiceClient(conn)

		// cat file
		catFileRequest := api.CatFileRequest{PathName: args[1]}
		catErr := printCat(client, catFileRequest)
		if catErr != nil {
			return fmt.Errorf("could not perform cat against filesystem service: %v", catErr)
		}

		return nil
	},
}

func init() {
	rootCmd.AddCommand(catCmd)
}

func printCat(client api.FsServiceClient, catFileRequest api.CatFileRequest) error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	contents, err := client.Cat(ctx, &catFileRequest)
	if err != nil {
		return fmt.Errorf("%v", err)
	}

	for {
		content, err := contents.Recv()
		if err == io.EOF {
			break
		}
		if err != nil {
			return fmt.Errorf("%v", err)
		}
		fmt.Printf("%s", content.Chunk)
	}

	return nil
}
