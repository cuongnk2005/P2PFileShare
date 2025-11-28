package org.example.p2pfileshare.network.transfer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;

public class FileServer {

    private final int port;

    // Folder chia sẻ có thể thay đổi trong runtime
    // Sử dụng AtomicReference để đảm bảo thread-safe - thay doi bien an toan cho nhieu thread
    private final AtomicReference<Path> shareFolder = new AtomicReference<>();

    public FileServer(int port, Path initialFolder) {
        this.port = port;
        this.shareFolder.set(initialFolder);
    }

    // ==============================
    //        HOT-SWAP FOLDER
    // ==============================
    public void changeFolder(Path newFolder) {
        if (newFolder != null && Files.isDirectory(newFolder)) {
            System.out.println("[FileServer] Folder changed to: " + newFolder);
            shareFolder.set(newFolder);
        } else {
            System.out.println("[FileServer] Invalid folder: " + newFolder);
        }
    }

    // ==============================
    //            START
    // ==============================
    public void start() {
        Thread t = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[FileServer] Listening on port " + port);

                while (true) {
                    Socket client = serverSocket.accept();
                    System.out.println("[FileServer] New connection from " + client.getInetAddress());
                    handleClient(client);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        t.setDaemon(true); // server tắt theo app
        t.start();
    }

    // ==============================
    //         HANDLE CLIENT
    // ==============================
    private void handleClient(Socket client) {
        Thread t = new Thread(() -> {
            try (Socket s = client;
                 DataInputStream in = new DataInputStream(s.getInputStream());
                 DataOutputStream out = new DataOutputStream(s.getOutputStream())) {

                // Client gửi tên file
                String fileName = in.readUTF();

                // Luôn lấy folder mới nhất tại thời điểm client kết nối
                Path root = shareFolder.get();
                Path filePath = root.resolve(fileName).normalize();

                System.out.println("[FileServer] Client requested: " + filePath);

                // Kiểm tra không cho vượt ra ngoài thư mục (bảo mật)
                if (!filePath.startsWith(root)) {
                    System.out.println("[FileServer] BLOCKED: Outside folder");
                    out.writeLong(-1);
                    return;
                }

                // File không tồn tại
                if (!Files.exists(filePath)) {
                    out.writeLong(-1);
                    System.out.println("[FileServer] File not found");
                    return;
                }

                // Gửi kích thước file
                long size = Files.size(filePath);
                out.writeLong(size);

                // Gửi nội dung file
                try (InputStream fileIn = Files.newInputStream(filePath)) {
                    byte[] buffer = new byte[8192];
                    long remaining = size;
                    int read;

                    while (remaining > 0 &&
                            (read = fileIn.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                        out.write(buffer, 0, read);
                        remaining -= read;
                    }

                    System.out.println("[FileServer] Done sending " + fileName);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        t.setDaemon(true);
        t.start();
    }
}
