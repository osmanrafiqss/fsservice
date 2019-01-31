package com.soprasteria.fsservice.server;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.soprasteria.fsservice.FsServiceGrpc;
import com.soprasteria.fsservice.Fsservice.File;
import com.soprasteria.fsservice.Fsservice.ListFilesRequest;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;

public class FsServiceImplTest {

	@Rule
	public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
	
	@Rule
	public TemporaryFolder folder= new TemporaryFolder();

	@Test
	public void testListing() throws Exception {
		String serverName = InProcessServerBuilder.generateName();
		Server server = InProcessServerBuilder.forName(serverName).addService(new FsServiceImpl()).build().start();
		grpcCleanup.register(server);
		
		ManagedChannel channel = InProcessChannelBuilder.forName(serverName).build();
		grpcCleanup.register(channel);
		FsServiceGrpc.FsServiceBlockingStub blockingStub = FsServiceGrpc.newBlockingStub(channel);
		
		final String fileName = folder.newFile("fileA.txt").getName();
		final String folderPath = folder.newFolder("subdir").getAbsolutePath();
		
		ListFilesRequest request = ListFilesRequest.newBuilder().setPathName(folder.getRoot().getAbsolutePath()).build();
		Iterator<File> files = blockingStub.list(request);
		Assert.assertTrue(files.hasNext());

		ArrayList<String> fileNames = new ArrayList<>();
		fileNames.add(fileName);
		fileNames.add(folderPath);

		while (files.hasNext()) {
			File file = files.next();
			Assert.assertTrue(fileNames.remove(file.getFileName()));
		}

		Assert.assertTrue(fileNames.isEmpty());
		
	}
}
