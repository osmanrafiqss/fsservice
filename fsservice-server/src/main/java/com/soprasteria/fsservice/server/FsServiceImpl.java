package com.soprasteria.fsservice.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

import com.google.protobuf.Timestamp;
import com.soprasteria.fsservice.FsServiceGrpc.FsServiceImplBase;
import com.soprasteria.fsservice.Fsservice.File;
import com.soprasteria.fsservice.Fsservice.ListFilesRequest;

import io.grpc.stub.StreamObserver;

public class FsServiceImpl extends FsServiceImplBase {

	@Override
	public void list(ListFilesRequest request, StreamObserver<File> responseObserver) {
		String pathName = request.getPathName() != null ? request.getPathName() : "/";

		try {
			Path path = Paths.get(pathName);
			if(Files.isDirectory(path)) {
				Files.list(path).map(FsServiceImpl::convert).forEach(f -> responseObserver.onNext(f));
			}
			responseObserver.onCompleted();
		} catch (Throwable t) {
			responseObserver.onError(t);
		}
	}

	private static File convert(Path path) {
		try {
			BasicFileAttributes basicAttributes = Files.getFileAttributeView(path, BasicFileAttributeView.class)
					.readAttributes();
			long lastAccessTimeMs = basicAttributes.lastAccessTime().toMillis();
			Timestamp lastModifiedTimestamp = Timestamp.newBuilder().setSeconds(lastAccessTimeMs / 1000)
					.setNanos((int) ((lastAccessTimeMs % 1000) * 1000000)).build();
			boolean isDirectory = basicAttributes.isDirectory();
			String fileName = isDirectory ? path.toAbsolutePath().toString() : path.getFileName().toString();

			return File.newBuilder().setFileName(fileName).setIsDirectory(isDirectory)
					.setLastAccessTime(lastModifiedTimestamp).build();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
