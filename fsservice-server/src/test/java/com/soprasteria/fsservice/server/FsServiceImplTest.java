package com.soprasteria.fsservice.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.soprasteria.fsservice.FsServiceGrpc;
import com.soprasteria.fsservice.Fsservice.CatFileRequest;
import com.soprasteria.fsservice.Fsservice.File;
import com.soprasteria.fsservice.Fsservice.FileContent;
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
	
	private FsServiceGrpc.FsServiceBlockingStub blockingStub;
	
	@Before
	public void beforeTest() throws IOException {
		String serverName = InProcessServerBuilder.generateName();
		Server server = InProcessServerBuilder.forName(serverName).addService(new FsServiceImpl()).build().start();
		grpcCleanup.register(server);
		
		ManagedChannel channel = InProcessChannelBuilder.forName(serverName).build();
		grpcCleanup.register(channel);
		blockingStub = FsServiceGrpc.newBlockingStub(channel);
	}

	@Test
	public void testListing() throws Exception {
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
	
	@Test
	public void testCat() throws Exception {
		final String fileAbsolutePath = folder.newFile("fileA.txt").getAbsolutePath();
		final String testString = generateRandomString(4096);
		
		Path filePath = Paths.get(fileAbsolutePath);
		try (OutputStream os = Files.newOutputStream(filePath) ) {
			os.write(testString.getBytes(StandardCharsets.UTF_8));
		}
		
		CatFileRequest request = CatFileRequest.newBuilder().setPathName(fileAbsolutePath).build();
		Iterator<FileContent> fileContents = blockingStub.cat(request);

		StringBuilder builder = new StringBuilder(testString.length());
		while (fileContents.hasNext()) {
			FileContent fileContent = fileContents.next();
			builder.append(fileContent.getChunk().toString(StandardCharsets.UTF_8));
		}

		Assert.assertEquals(testString, builder.toString());
	}

	private String generateRandomString(int size) {
		char[] characters = "abcdefghijklmnopqrstuvwxyz".toCharArray();
		
		char[] randomChars = new char[size];
		for (int i = 0; i < randomChars.length; i++) {
			randomChars[i] = characters[(int) (Math.random() * characters.length)];
		}
		
		return new String(randomChars);
	}
}
