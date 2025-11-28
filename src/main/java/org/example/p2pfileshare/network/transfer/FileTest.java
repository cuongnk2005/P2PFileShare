package org.example.p2pfileshare.network.transfer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTest {

    private final int port;
    private final Path shareFolder;

    public FileTest(int port, Path shareFolder) {
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
        t.setDaemon(true);
        t.start();
    }

    private void handleClient(Socket client) {
        Thread t = new Thread(() -> {
            try (
                    Socket s = client;
                    DataInputStream in = new DataInputStream(s.getInputStream());
                    DataOutputStream out = new DataOutputStream(s.getOutputStream())
            ) {
                String fileName = in.readUTF();
                Path filePath = shareFolder.resolve(fileName);

                System.out.println("[FileServer] Client requested file: " + filePath);

                if (!Files.exists(filePath)) {
                    out.writeLong(-1);
                    System.out.println("[FileServer] File not found");
                    return;
                }

                long size = Files.size(filePath);
                out.writeLong(size);

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
