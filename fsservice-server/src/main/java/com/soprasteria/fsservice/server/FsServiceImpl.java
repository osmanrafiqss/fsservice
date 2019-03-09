package com.soprasteria.fsservice.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Logger;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.soprasteria.fsservice.FsServiceGrpc.FsServiceImplBase;
import com.soprasteria.fsservice.Fsservice.CatFileRequest;
import com.soprasteria.fsservice.Fsservice.File;
import com.soprasteria.fsservice.Fsservice.FileContent;
import com.soprasteria.fsservice.Fsservice.ListFilesRequest;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class FsServiceImpl extends FsServiceImplBase {
	private static final Logger logger = Logger.getLogger(FSServer.class.getName());

	/*
	 * The optimal buffer size for network transfer (i.e. equivalent to the calculated MTU)
	 * 
	 * Calculated as (with 10 bytes slack just to ensure we stay within MTU):
	 * size = ethernet frame size [1518 BYTES] - ethernet header [18 BYTES] - 
	 *        max tcp header size [60 BYTES] - http2 fragment header [9 BYTES] - 
	 *        protobuf field encoding [1 BYTE]
	 */
	private static final int BUFFER_SIZE = 1420;
	
	@Override
	public void list(ListFilesRequest request, StreamObserver<File> responseObserver) {
		try {
			Path path = resolve(request.getPathName());
			if(Files.isDirectory(path)) {
				Files.list(path).map(FsServiceImpl::convert).forEach(f -> responseObserver.onNext(f));
			}
			responseObserver.onCompleted();
		} catch (Throwable t) {
			logger.warning("ls command for path: " + request.getPathName() + " failed with: " + t.getMessage());
			responseObserver.onError(Status.INTERNAL.withDescription(t.getMessage()).asException());
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
	
	
	@Override
	public void cat(CatFileRequest request, StreamObserver<FileContent> responseObserver) {
		try {
			Path path = resolve(request.getPathName());
			if (Files.isRegularFile(path)) {
				try (ByteChannel byteChannel = Files.newByteChannel(path, StandardOpenOption.READ)) {
					ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
					
					// Read chunks of data from channel while not end-of-stream
					while (byteChannel.read(buffer) != -1) {
						buffer.flip();

						// Write chunk of data to stream
						FileContent content = FileContent.newBuilder().setChunk(ByteString.copyFrom(buffer)).build();
						responseObserver.onNext(content);
						
						// Prepare to read another chunk of data
						buffer.clear();
					}

				}
			}
			responseObserver.onCompleted();
		} catch (Exception e) {
			logger.warning("cat command for path: " + request.getPathName() + " failed with: " + e.getMessage());
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
		}
	}
	
	/**
	 * Resolves the specified path to an existing filesystem structure
	 * @param pathName the path to resolve
	 * @return the resolved path  
	 * @throws NonResolveablePathException thrown if the specified path could not be resolved to a filesystem structure
	 */
	private Path resolve(String pathName) throws NonResolveablePathException {
		Path resolved = null;

		try {
			if (pathName == null || pathName.trim().isEmpty()) {
				throw new NonResolveablePathException("no valid path specified");
			}

			resolved = Paths.get(pathName);
			if (!Files.exists(resolved)) {
				throw new NonResolveablePathException("not able to resolve path to filesystem structure: ");
			}
		} catch (Exception e) {
			logger.warning("Unable to access specified path: " + pathName + ", error: " + e.getMessage());
			throw new NonResolveablePathException("unable to resolve path: " + pathName);
		}

		return resolved;
	}
	
	/**
	 * The {@link NonResolveablePathException} indicates an error in resolving a path to an structure (i.e. file or folder) existing on the filesystem.
	 */
	private static class NonResolveablePathException extends Exception {
		private static final long serialVersionUID = 1L;

		public NonResolveablePathException(String msg) {
			super(msg);
		}
	}
}
