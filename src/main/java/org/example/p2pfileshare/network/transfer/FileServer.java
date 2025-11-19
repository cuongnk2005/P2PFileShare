package org.example.p2pfileshare.network.transfer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileServer {

    private final int port;
    private final Path shareFolder;

    public FileServer(int port, Path shareFolder) {
        this.port = port;
        this.shareFolder = shareFolder;
    }

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
        t.setDaemon(true); // server tự tắt khi ứng dụng đóng
        t.start();
    }

    private void handleClient(Socket client) {
        Thread t = new Thread(() -> {
            try (Socket s = client;
                 DataInputStream in = new DataInputStream(s.getInputStream()); // nhận dữ liệu
                 DataOutputStream out = new DataOutputStream(s.getOutputStream())) { // gửi dữ liệu

                // 1) Client gửi tên file (UTF)
                String fileName = in.readUTF();
                Path filePath = shareFolder.resolve(fileName);

                System.out.println("[FileServer] Client requested file: " + filePath);

                if (!Files.exists(filePath)) {
                    out.writeLong(-1); // báo không có file
                    System.out.println("[FileServer] File not found");
                    return;
                }

                // 2) Gửi kích thước
                long size = Files.size(filePath);
                out.writeLong(size);

                // 3) Gửi nội dung file
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
