package com.soprasteria.fsservice.server;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class FSServer {
	private static final Logger logger = Logger.getLogger(FSServer.class.getName());

	public static void main(String[] args) throws ParseException, InterruptedException, IOException {
		Options options = new Options();
		Option portOption = Option.builder("p").longOpt("port").desc("The port on which to listen for RPCs").type(Number.class).required().hasArg().build();
		options.addOption(portOption);

		try {
			DefaultParser parser = new DefaultParser();
			CommandLine commandLine = parser.parse(options, args);
			int port = ((Number) commandLine.getParsedOptionValue(portOption.getOpt())).intValue();

			logger.info("Starting File System Server on port: " + port);
			
			FsServiceImpl fsService = new FsServiceImpl();
			Server fsServiceServer = ServerBuilder.forPort(port).addService(fsService).build();
			fsServiceServer.start().awaitTermination();

			logger.info("Shutting down File System Server");
			
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(FSServer.class.getSimpleName(), options);
		}		
	}
}
