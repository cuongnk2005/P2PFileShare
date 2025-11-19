package org.example.p2pfileshare.service;

import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.network.transfer.FileClient;
import org.example.p2pfileshare.network.transfer.FileServer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileShareService {

    private File shareFolder;
    private FileServer fileServer;
    private final int fileServerPort;

    public FileShareService(int fileServerPort) {
        this.fileServerPort = fileServerPort;
    }

    public void setShareFolder(File folder) {
        this.shareFolder = folder;
    }

    public File getShareFolder() {
        return shareFolder;
    }

    /** Tải file từ peer */
    public boolean download(PeerInfo peer, String fileName, Path saveTo) {
        return FileClient.downloadFile(
                peer.getIp(),
                peer.getPort(),
                fileName,
                saveTo
        );
    }

    /** Trả về danh sách file trong thư mục chia sẻ */
    public List<File> listSharedFiles() {
        List<File> result = new ArrayList<>();
        if (shareFolder == null || !shareFolder.exists()) return result;

        File[] files = shareFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) result.add(f);
            }
        }
        return result;
    }

    public void startSharing(File folder) {
        this.shareFolder = folder;
        // start TCP file server
        if (fileServer != null) {
            // nếu muốn, có thể stop server cũ
        }
        fileServer = new FileServer(fileServerPort, folder.toPath());
        fileServer.start();
        System.out.println("[FileShareService] Start FileServer at port "
                + fileServerPort + " folder=" + folder.getAbsolutePath());
    }


    /** Xóa file trong thư mục chia sẻ */
    public boolean removeSharedFile(String fileName) {
        if (shareFolder == null) return false;
        File f = new File(shareFolder, fileName);
        return f.delete();
    }
}
