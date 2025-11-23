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

    private final int fileServerPort;
    private File shareFolder;
    private FileServer fileServer;

    public FileShareService(int fileServerPort) {
        this.fileServerPort = fileServerPort;
    }

    public void setShareFolder(File folder) {
        this.shareFolder = folder;
        // Start/Restart file server
        if (fileServer != null) {
            // hiện tại FileServer không có stop(), nên bỏ qua
        }
        if (folder != null && folder.exists() && folder.isDirectory()) {
            fileServer = new FileServer(fileServerPort, folder.toPath());
            fileServer.start();
        }
    }

    public File getShareFolder() {
        return shareFolder;
    }

    /** tải file từ peer */
    public boolean download(PeerInfo peer, String fileName, Path saveTo) {
        return FileClient.downloadFile(
                peer.getIp(),
                peer.getFileServerPort(),
                fileName,
                saveTo
        );
    }

    /** trả về danh sách file trong thư mục shareFolder */
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
}
