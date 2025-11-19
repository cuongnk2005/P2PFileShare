package org.example.p2pfileshare.network.transfer;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileClient {

    /**
     * Tải 1 file từ peer khác.
     *
     * @param host       IP của peer
     * @param port       TCP port (file server port)
     * @param fileName   tên file cần tải (phải trùng tên bên shareFolder của server)
     * @param saveToPath nơi lưu file tải về
     * @return true nếu tải thành công, false nếu lỗi
     */
    public static boolean downloadFile(String host, int port, String fileName, Path saveToPath) {
        try (Socket socket = new Socket(host, port);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            // 1) Gửi tên file
            out.writeUTF(fileName);
            out.flush();

            // 2) Nhận kích thước
            long size = in.readLong();
            if (size < 0) {
                System.out.println("[FileClient] File not found on server");
                return false;
            }

            // 3) Nhận dữ liệu file
            Files.createDirectories(saveToPath.getParent()); // tạo thư mục chứa file
            try (OutputStream fileOut = Files.newOutputStream(saveToPath)) {
                byte[] buffer = new byte[8192];
                long remaining = size;
                int read;
                while (remaining > 0 &&
                        (read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    fileOut.write(buffer, 0, read);
                    remaining -= read;
                }
            }

            System.out.println("[FileClient] Downloaded " + fileName + " to " + saveToPath);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}