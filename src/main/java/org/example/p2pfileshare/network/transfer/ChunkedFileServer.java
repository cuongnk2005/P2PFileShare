package org.example.p2pfileshare.network.transfer;

import org.example.p2pfileshare.network.protocol.FileTransferProtocol;
import org.example.p2pfileshare.util.FileHashUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ChunkedFileServer {

    private final int port;
    private final AtomicReference<Path> shareFolder = new AtomicReference<>();
    private static final int DEFAULT_CHUNK_SIZE = 1024 * 1024; // 1 MB

    public ChunkedFileServer(int port, Path initialFolder) {
        this.port = port;
        this.shareFolder.set(initialFolder);
    }

    public void changeFolder(Path newFolder) {
        if (newFolder != null && Files.isDirectory(newFolder)) {
            shareFolder.set(newFolder);
            System.out.println("[ChunkedFileServer] Folder changed to: " + newFolder);
        }
    }

    public void start() {
        Thread t = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[ChunkedFileServer] Listening on port " + port);
                while (true) {
                    Socket client = serverSocket.accept();
                    handleClient(client);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "chunked-file-server");
        t.setDaemon(true);
        t.start();
    }

    private void handleClient(Socket socket) {
        new Thread(() -> {
            try (Socket s = socket;
                 DataInputStream in = new DataInputStream(s.getInputStream());
                 DataOutputStream out = new DataOutputStream(s.getOutputStream())) {

                String line = in.readUTF();
                if (line == null) return;

                FileTransferProtocol.ParsedCommand cmd = FileTransferProtocol.parse(line);
                if (cmd == null) return;

                Path root = shareFolder.get();
                if (root == null) {
                    out.writeUTF("ERROR|No share folder set");
                    return;
                }

                if (FileTransferProtocol.FILE_META_REQUEST.equals(cmd.command)) {
                    handleMetaRequest(cmd, root, out);
                } else if (FileTransferProtocol.GET_CHUNK.equals(cmd.command)) {
                    handleChunkRequest(cmd, root, out);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "chunked-handler").start();
    }

    private void handleMetaRequest(FileTransferProtocol.ParsedCommand cmd, Path root, DataOutputStream out) throws IOException {
        String fileName = cmd.get(1);
        if (fileName == null) {
            out.writeUTF("ERROR");
            out.writeUTF("Missing filename");
            out.flush();
            return;
        }

        Path safeRoot = root.toAbsolutePath().normalize();
        Path filePath = safeRoot.resolve(fileName).normalize();

        if (!filePath.startsWith(safeRoot) || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            out.writeUTF("ERROR");
            out.writeUTF("File not found");
            out.flush();
            return;
        }

        long fileSize = Files.size(filePath);
        int totalChunks = (int) Math.ceil((double) fileSize / DEFAULT_CHUNK_SIZE);

        String fileSha256 = FileHashUtil.sha256(filePath);

        List<String> chunkHashes = new ArrayList<>(Math.max(totalChunks, 0));
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
            for (int i = 0; i < totalChunks; i++) {
                long offset = (long) i * DEFAULT_CHUNK_SIZE;
                raf.seek(offset);

                int toRead = (int) Math.min(DEFAULT_CHUNK_SIZE, fileSize - offset);
                int read = raf.read(buffer, 0, toRead);

                if (read <= 0) {
                    out.writeUTF("ERROR");
                    out.writeUTF("Failed to read chunk " + i);
                    out.flush();
                    return;
                }

                byte[] chunkData = new byte[read];
                System.arraycopy(buffer, 0, chunkData, 0, read);

                chunkHashes.add(FileHashUtil.sha256(chunkData));
            }
        }

        out.writeUTF(FileTransferProtocol.FILE_META_RESPONSE);
        out.writeUTF(fileName);
        out.writeLong(fileSize);
        out.writeInt(DEFAULT_CHUNK_SIZE);
        out.writeInt(totalChunks);
        out.writeUTF(fileSha256);

        for (int i = 0; i < totalChunks; i++) {
            out.writeUTF(chunkHashes.get(i));
        }

        out.flush();
        System.out.println("[ChunkedFileServer] Sent metadata for " + fileName);
    }

    private void handleChunkRequest(FileTransferProtocol.ParsedCommand cmd, Path root, DataOutputStream out) throws IOException {
        String fileName = cmd.get(1);
        String indexStr = cmd.get(2);

        if (fileName == null || indexStr == null) {
            out.writeUTF("ERROR|Missing parameters");
            return;
        }

        int chunkIndex;
        try {
            chunkIndex = Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            out.writeUTF("ERROR|Invalid chunk index");
            return;
        }

        Path filePath = root.resolve(fileName).normalize();
        if (!filePath.startsWith(root) || !Files.exists(filePath)) {
            out.writeUTF("ERROR|File not found");
            return;
        }

        long fileSize = Files.size(filePath);
        long offset = (long) chunkIndex * DEFAULT_CHUNK_SIZE;

        if (offset >= fileSize) {
            out.writeUTF("ERROR|Chunk index out of range");
            return;
        }

        int chunkSize = (int) Math.min(DEFAULT_CHUNK_SIZE, fileSize - offset);

        // Đọc chunk
        byte[] chunkData = new byte[chunkSize];
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            raf.seek(offset);
            raf.readFully(chunkData);
        }

        String chunkHash = FileHashUtil.sha256(chunkData);

        // Gửi: CHUNK_DATA|chunkIndex|chunkSize|hash
        out.writeUTF(FileTransferProtocol.CHUNK_DATA + "|" + chunkIndex + "|" + chunkSize + "|" + chunkHash);
        out.write(chunkData);
        out.flush();

        System.out.println("[ChunkedFileServer] Sent chunk " + chunkIndex + " (" + chunkSize + " bytes)");
    }
}

