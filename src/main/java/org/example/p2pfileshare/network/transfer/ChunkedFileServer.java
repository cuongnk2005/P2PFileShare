package org.example.p2pfileshare.network.transfer;

import org.example.p2pfileshare.network.protocol.FileTransferProtocol;
import org.example.p2pfileshare.util.FileHashUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class ChunkedFileServer {

    private final int port;
    private final AtomicReference<Path> shareFolder = new AtomicReference<>();
    private static final int DEFAULT_CHUNK_SIZE = 1024 * 1024; // 1 MB

    // Đỡ tạo vô hạn thread
    private final ExecutorService pool = Executors.newFixedThreadPool(32);

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
                    pool.submit(() -> handleClient(client));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "chunked-file-server");
        t.setDaemon(true);
        t.start();
    }

    private void handleClient(Socket socket) {
        try (Socket s = socket;
             DataInputStream in = new DataInputStream(s.getInputStream());
             DataOutputStream out = new DataOutputStream(s.getOutputStream())) {

            // ✅ Client gửi writeUTF -> server phải readUTF
            String request = in.readUTF();
            if (request == null || request.isBlank()) return;

            FileTransferProtocol.ParsedCommand cmd = FileTransferProtocol.parse(request);
            if (cmd == null) {
                sendError(out, "Invalid command");
                return;
            }

            Path root = shareFolder.get();
            if (root == null) {
                sendError(out, "No share folder set");
                return;
            }

            if (FileTransferProtocol.FILE_META_REQUEST.equals(cmd.command)) {
                handleMetaRequest(cmd, root, out);
            } else if (FileTransferProtocol.GET_CHUNK.equals(cmd.command)) {
                handleChunkRequest(cmd, root, out);
            } else {
                sendError(out, "Unknown command: " + cmd.command);
            }

        } catch (EOFException eof) {
            // client đóng sớm
        } catch (IOException e) {
            System.err.println("[ChunkedFileServer] Client error: " + e.getMessage());
        }
    }

    private void handleMetaRequest(FileTransferProtocol.ParsedCommand cmd, Path root, DataOutputStream out) throws IOException {
        String fileName = cmd.get(1);
        if (fileName == null) {
            sendError(out, "Missing filename");
            return;
        }

        Path filePath = root.resolve(fileName).normalize();
        if (!filePath.startsWith(root) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendError(out, "File not found");
            return;
        }

        long fileSize = Files.size(filePath);
        int totalChunks = (int) Math.ceil((double) fileSize / DEFAULT_CHUNK_SIZE);

        // hash toàn file
        String fileSha256 = FileHashUtil.sha256(filePath);

        // hash từng chunk
        List<String> chunkHashes = new ArrayList<>(totalChunks);
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
            for (int i = 0; i < totalChunks; i++) {
                long offset = (long) i * DEFAULT_CHUNK_SIZE;
                raf.seek(offset);

                int toRead = (int) Math.min(DEFAULT_CHUNK_SIZE, fileSize - offset);
                int read = raf.read(buffer, 0, toRead);

                if (read <= 0) {
                    chunkHashes.add(FileHashUtil.sha256(new byte[0]));
                } else {
                    byte[] chunkData = new byte[read];
                    System.arraycopy(buffer, 0, chunkData, 0, read);
                    chunkHashes.add(FileHashUtil.sha256(chunkData));
                }
            }
        }

        // ✅ Binary response đúng với client requestMetadata():
        // type, name, fileSize, chunkSize, totalChunks, fileSha256, hash[i]...
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

        System.out.println("[ChunkedFileServer] Sent metadata for " + fileName + " chunks=" + totalChunks);
    }

    private void handleChunkRequest(FileTransferProtocol.ParsedCommand cmd, Path root, DataOutputStream out) throws IOException {
        String fileName = cmd.get(1);
        String indexStr = cmd.get(2);

        if (fileName == null || indexStr == null) {
            sendError(out, "Missing parameters");
            return;
        }

        int chunkIndex;
        try {
            chunkIndex = Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            sendError(out, "Invalid chunk index");
            return;
        }

        Path filePath = root.resolve(fileName).normalize();
        if (!filePath.startsWith(root) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendError(out, "File not found");
            return;
        }

        long fileSize = Files.size(filePath);
        long offset = (long) chunkIndex * DEFAULT_CHUNK_SIZE;
        if (offset >= fileSize || chunkIndex < 0) {
            sendError(out, "Chunk index out of range");
            return;
        }

        int dataLen = (int) Math.min(DEFAULT_CHUNK_SIZE, fileSize - offset);

        byte[] chunkData = new byte[dataLen];
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            raf.seek(offset);
            raf.readFully(chunkData);
        }

        String chunkHash = FileHashUtil.sha256(chunkData);

        // ✅ Binary response đúng với client downloadChunk():
        // type, index(int), len(int), hash(UTF), bytes...
        out.writeUTF(FileTransferProtocol.CHUNK_DATA);
        out.writeInt(chunkIndex);
        out.writeInt(dataLen);
        out.writeUTF(chunkHash);
        out.write(chunkData);
        out.flush();

        System.out.println("[ChunkedFileServer] Sent chunk " + chunkIndex + " len=" + dataLen);
    }

    private void sendError(DataOutputStream out, String reason) throws IOException {
        out.writeUTF("ERROR");
        out.writeUTF(reason);
        out.flush();
    }
}
