package org.example.p2pfileshare.network.transfer;

import org.example.p2pfileshare.model.DownloadProgress;
import org.example.p2pfileshare.model.FileMetadata;
import org.example.p2pfileshare.network.protocol.FileTransferProtocol;
import org.example.p2pfileshare.util.FileHashUtil;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ChunkedFileClient {

    /**
     * Bước 1: Request metadata từ server
     */
    public static FileMetadata requestMetadata(String host, int port, String fileName) throws IOException {
        try (Socket socket = new Socket(host, port);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            // 1) Gửi request
            String request = FileTransferProtocol.buildMetaRequest(fileName);
            out.writeUTF(request);
            out.flush();

            // 2) Đọc response type trước
            String type = in.readUTF();
            if (type == null) {
                throw new IOException("No response from server");
            }

            // 3) Nếu ERROR thì đọc reason (2-field)
            if ("ERROR".equals(type)) {
                String reason = in.readUTF(); // server gửi thêm 1 field reason
                throw new IOException("Server error: " + reason);
            }

            // 4) Kiểm tra đúng loại response
            if (!FileTransferProtocol.FILE_META_RESPONSE.equals(type)) {
                throw new IOException("Unexpected response type: " + type);
            }

            // 5) Đọc lần lượt các field theo đúng thứ tự server gửi
            String name = in.readUTF();
            long fileSize = in.readLong();
            int chunkSize = in.readInt();
            int totalChunks = in.readInt();
            String fileSha256 = in.readUTF();

            // 6) Đọc từng chunk hash
            List<String> chunkHashes = new ArrayList<>(totalChunks);
            for (int i = 0; i < totalChunks; i++) {
                chunkHashes.add(in.readUTF());
            }

            return new FileMetadata(name, fileSize, chunkSize, totalChunks, fileSha256, chunkHashes);
        }
    }


    /**
     * Bước 2: Tải file với chunk + resume + integrity
     * @param progressCallback callback nhận progress (0.0 - 1.0)
     */
    public static boolean downloadFile(String host, int port, String fileName, Path saveTo,
                                      Consumer<Double> progressCallback) throws IOException {

        // 1) Request metadata
        FileMetadata meta = requestMetadata(host, port, fileName);
        System.out.println("[ChunkedFileClient] Metadata: " + meta.getTotalChunks() + " chunks, size=" + meta.getFileSize());

        // 2) Tạo file tạm + progress
        Path partFile = Path.of(saveTo.toString() + ".part");
        DownloadProgress progress = new DownloadProgress(
            fileName, meta.getFileSize(), meta.getChunkSize(), meta.getTotalChunks(), meta.getFileSha256()
        ); if (!Files.exists(partFile)) {
            try (RandomAccessFile raf = new RandomAccessFile(partFile.toFile(), "rw")) {
                raf.setLength(meta.getFileSize());
            }
        }

        // Tạo file tạm với đúng size (pre-allocate)


        // 3) Tải từng chunk còn thiếu
        for (int i = 0; i < meta.getTotalChunks(); i++) {
            if (progress.isChunkComplete(i)) {
                continue; // đã tải rồi (resume)
            }

            boolean success = downloadChunk(host, port, fileName, i, meta, partFile, progress);
            if (!success) {
                System.err.println("[ChunkedFileClient] Failed to download chunk " + i);
                return false;
            }

            // Callback progress
            if (progressCallback != null) {
                progressCallback.accept(progress.getProgressPercent() / 100.0);
            }
        }

        // 4) Verify toàn file (optional nhưng xịn)
        String actualHash = FileHashUtil.sha256(partFile);
        if (!actualHash.equals(meta.getFileSha256())) {
            System.err.println("[ChunkedFileClient] File hash mismatch!");
            return false;
        }

        // 5) Đổi tên .part → file thật
        Files.move(partFile, saveTo, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[ChunkedFileClient] Download complete: " + saveTo);

        return true;
    }

    /**
     * Tải 1 chunk và verify hash
     */
    private static boolean downloadChunk(String host, int port, String fileName, int chunkIndex,
                                        FileMetadata meta, Path partFile, DownloadProgress progress) {

        final int MAX_RETRIES = 3;

        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try (Socket socket = new Socket(host, port);
                 DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                // Request chunk
                String request = FileTransferProtocol.buildChunkRequest(fileName, chunkIndex);
                out.writeUTF(request);
                out.flush();

                // Read response header: CHUNK_DATA|chunkIndex|chunkSize|hash
                String header = in.readUTF();
                if (header == null || header.startsWith("ERROR")) {
                    System.err.println("[ChunkedFileClient] Chunk " + chunkIndex + " error: " + header);
                    continue;
                }

                String[] parts = header.split("\\|");
                if (parts.length < 4) continue;

                int receivedIndex = Integer.parseInt(parts[1]);
                int chunkSize = Integer.parseInt(parts[2]);
                String expectedHash = parts[3];

                if (receivedIndex != chunkIndex) {
                    System.err.println("[ChunkedFileClient] Chunk index mismatch");
                    continue;
                }

                // Read chunk data
                byte[] chunkData = new byte[chunkSize];
                in.readFully(chunkData);

                // Verify hash
                String actualHash = FileHashUtil.sha256(chunkData);
                if (!actualHash.equals(expectedHash)) {
                    System.err.println("[ChunkedFileClient] Chunk " + chunkIndex + " hash mismatch (retry " + (retry + 1) + ")");
                    continue;
                }

                // Ghi vào file tạm đúng offset
                long offset = (long) chunkIndex * meta.getChunkSize();
                try (RandomAccessFile raf = new RandomAccessFile(partFile.toFile(), "rw")) {
                    raf.seek(offset);
                    raf.write(chunkData);
                }

                // Đánh dấu hoàn thành
                progress.markChunkComplete(chunkIndex);
                System.out.println("[ChunkedFileClient] Chunk " + chunkIndex + " OK (" + String.format("%.1f", progress.getProgressPercent()) + "%)");

                return true;

            } catch (IOException e) {
                System.err.println("[ChunkedFileClient] Chunk " + chunkIndex + " error (retry " + (retry + 1) + "): " + e.getMessage());
            }
        }

        return false; // hết retry
    }
}

