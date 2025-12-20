package org.example.p2pfileshare.service;

import org.example.p2pfileshare.model.DownloadHistory;
import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.model.SharedFileLocal;
import org.example.p2pfileshare.network.transfer.FileClient;
import org.example.p2pfileshare.network.transfer.FileServer;
import org.example.p2pfileshare.util.DownloadHistoryManager;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileShareService {

    private final int fileServerPort;    // cổng cho FileServer
    private File shareFolder;            // thư mục đang chia sẻ, client gửi den
    private FileServer fileServer;       // server gửi file cho peer khác
    private HistoryService historyService;
    private String myDisplayName; // Tên hiển thị của peer

    public FileShareService(int fileServerPort,HistoryService historyService ) {
        this.fileServerPort = fileServerPort;
        this.historyService = historyService;
    }

    // ======================
    //    KHỞI TẠO SERVER
    // ======================
    public synchronized void startServer() {
        if (fileServer == null && shareFolder != null) {
            fileServer = new FileServer(fileServerPort, shareFolder.toPath());
            fileServer.start();
        }
    }

    // =================================
    //      ĐỔI FOLDER CHIA SẺ RUNTIME
    // =================================
    public synchronized void setShareFolder(File folder) {

        // folder null → không chia sẻ gì
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            this.shareFolder = null;
            if (fileServer != null) {
                fileServer.changeFolder(null);
            }
            return;
        }

        // set thư mục mới
        this.shareFolder = folder;

        // nếu server chưa chạy → khởi động
        if (fileServer == null) {
            fileServer = new FileServer(fileServerPort, folder.toPath());
            fileServer.start();
        }
        // nếu server đang chạy → đổi folder ngay lập tức (không restart)
        else {
            fileServer.changeFolder(folder.toPath());
        }
    }

    public File getShareFolder() {
        return shareFolder;
    }

    // ==============================
    //        TẢI FILE TỪ PEER
    // ==============================
    public boolean download(PeerInfo peer, String relativePath, Path saveTo) {
        boolean success = FileClient.downloadFile(
                peer.getIp(),
                peer.getFileServerPort(),
                relativePath,
                saveTo
        );
        System.out.println("trạng thái success: " + success);

        if (success) {
            try {
                // Lưu lịch sử tải xuống
                DownloadHistory history = new DownloadHistory(
                        saveTo.getFileName().toString(),
                        saveTo.toAbsolutePath().toString(),
                        peer.getName(),
                        peer.getIp(),
                        LocalDateTime.now()
                );
                historyService.addHistory(history);
                System.out.println("✓ Đã lưu lịch sử tải xuống: " + saveTo.getFileName());
            } catch (Exception e) {
                System.err.println("✗ Lỗi khi lưu lịch sử tải xuống:");
                e.printStackTrace();
                // Không ảnh hưởng đến kết quả download
            }
        }

        return success;
    }

    // ======= NEW: expose download history to UI =========
    public List<DownloadHistory> listDownloadHistory() {
        return historyService.loadHistory();
    }

    // ==============================
    //    LIỆT KÊ FILE LOCAL SHARE
    // ==============================
    public List<SharedFileLocal> listSharedFiles() {
        List<SharedFileLocal> result = new ArrayList<>();

        if (shareFolder == null) return result;

        File[] files = shareFolder.listFiles();
        if (files == null) return result;

        for (File f : files) {
            if (f.isFile()) {
                result.add(buildMetadata(f));
            }
        }

        return result;
    }

    // ==============================
    //        BUILD METADATA
    // ==============================
    private SharedFileLocal buildMetadata(File f) {

        String fileName = f.getName();
        long size = f.length();

        // relative path để phía Client dùng khi tải file
        String relativePath = shareFolder.toPath()
                .relativize(f.toPath())
                .toString();

        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            extension = fileName.substring(dot + 1).toLowerCase();
        }

        String subject = detectSubject(folderNameOrFileName(f));
        String tags = "Toán";

        return new SharedFileLocal(
                fileName,
                relativePath,
                extension,
                size,
                subject,
                tags,
                true
        );
    }

    private String detectSubject(String name) {
        name = name.toLowerCase();

        if (name.contains("java") || name.contains("oop")) return "Java";
        if (name.contains("network")) return "Network";
        if (name.contains("os") || name.contains("process") || name.contains("thread")) return "Operating System";
        if (name.contains("ai") || name.contains("machine")) return "AI";
        if (name.contains("math")) return "Math";

        return "Khác";
    }

    private String folderNameOrFileName(File f) {
        File parent = f.getParentFile();
        if (parent != null) return parent.getName() + " " + f.getName();
        return f.getName();
    }

    public void setMyDisplayName(String myDisplayName) {
        this.myDisplayName = myDisplayName;
    }

    public String getMyDisplayName() {
        return myDisplayName;
    }
}
