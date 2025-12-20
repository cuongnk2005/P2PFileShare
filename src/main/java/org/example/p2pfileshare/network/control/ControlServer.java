package org.example.p2pfileshare.network.control;

import org.example.p2pfileshare.model.PeerInfo;
import org.example.p2pfileshare.service.FileShareService;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Lắng nghe các yêu cầu điều khiển:
 * - CONNECT_REQUEST|fromPeer|toPeer
 * - LIST_FILES|fromPeer|toPeer
 *
 * Sau khi nhận:
 *   1) CONNECT_REQUEST -> hỏi onIncomingConnect(fromPeer)
 *   2) LIST_FILES -> trả tên file được chia sẻ (top-level) dạng TSV trong note
 */
public class ControlServer {

    private final int port;
    private volatile boolean running = false;

    // Đã chấp nhận kết nối từ peerId nào
    private final Set<String> acceptedPeers = ConcurrentHashMap.newKeySet();
    /** callback: nhận peerId gửi yêu cầu, trả true nếu chấp nhận, false nếu từ chối */
    private final Function<String, Boolean> onIncomingConnect;

    // Để trả danh sách file local
    private FileShareService fileShareService;
    private Runnable onPeerAccepted;
    public ControlServer(int port, Function<String, Boolean> onIncomingConnect) {
        this.port = port;
        this.onIncomingConnect = onIncomingConnect;

    }

    // Cho phép inject FileShareService để phục vụ LIST_FILES
    public void setFileShareService(FileShareService fileShareService) {
        this.fileShareService = fileShareService;
    }

    public void start() {
        if (running) return;
        running = true;

        Thread t = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[ControlServer] Listening on port " + port);

                while (running) {
                    Socket client = serverSocket.accept();
                    handleClient(client);
                }

            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                } else {
                    System.out.println("[ControlServer] Stopped");
                }
            }
        }, "control-server");

        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
        // Nếu cần, có thể mở 1 socket ảo vào chính mình để giải phóng accept()
    }

    private void handleClient(Socket socket) {
        Thread t = new Thread(() -> {
            try (Socket s = socket;
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(s.getInputStream()))
                 ;     PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(s.getOutputStream()), true)) {

                String raw = reader.readLine();
                if (raw == null || raw.isEmpty()) return;

                ControlProtocol.ParsedMessage msg = ControlProtocol.parse(raw);
                if (msg == null) return;

                System.out.println("[ControlServer] Received: " + raw);

                if (ControlProtocol.CONNECT_REQUEST.equals(msg.command)) {
                    String fromPeer = msg.fromPeer; // người yêu cầu
                    String toPeer   = msg.toPeer;   // mình

                    boolean accept = true;
                    if (onIncomingConnect != null) {
                        try {
                            accept = Boolean.TRUE.equals(onIncomingConnect.apply(fromPeer));
                            if (accept) {
                                acceptedPeers.add(fromPeer);
                                if (onPeerAccepted != null) {
                                    onPeerAccepted.run();
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            accept = false;
                        }
                    }

                    String respCmd  = accept ? ControlProtocol.CONNECT_ACCEPT
                            : ControlProtocol.CONNECT_REJECT;
                    String respNote = accept ? "Accepted" : "Rejected";

                    // Lưu ý: từ phía server, fromPeer = "mình", toPeer = "người gửi request"
                    String resp = ControlProtocol.build(
                            respCmd,
                            toPeer,     // from = mình
                            fromPeer,   // to   = người yêu cầu
                            respNote
                    );

                    writer.println(resp);
                    System.out.println("[ControlServer] Sent: " + resp);
                }
                else if (ControlProtocol.LIST_FILES.equals(msg.command)) {
                    // Trả về danh sách file chia sẻ (tên/relative path)
                    String toPeer   = msg.toPeer;   // mình
                    String fromPeer = msg.fromPeer; // client
                    // CHẶN NẾU CHƯA ĐƯỢC ACCEPT
                    if (!acceptedPeers.contains(fromPeer)) {
                        System.out.println("[ControlServer] DENY LIST_FILES from " + fromPeer);
                        String denyResp = ControlProtocol.build(
                                ControlProtocol.LIST_FILES_RESPONSE,
                                msg.toPeer,
                                fromPeer,
                                ""   // payload rỗng
                        );
                        writer.println(denyResp);
                        return;
                    }
                    // Build payload TSV: fileName\trelativePath\tsize
                    String payload = buildFileListPayload();
                    System.out.println("[DEBUG] Payload gửi đi:\n" + payload);
                    String resp = ControlProtocol.build(
                            ControlProtocol.LIST_FILES_RESPONSE,
                            toPeer,     // from = mình
                            fromPeer,   // to   = requester
                            payload
                    );

                    writer.println(resp);
                    System.out.println("[ControlServer] Sent LIST_FILES_RESPONSE (" + payload.length() + " bytes)");
                }else if (ControlProtocol.DISCONNECT_REQUEST.equals(msg.command)) {
                    String fromPeer = msg.fromPeer; // người yêu cầu ngắt (mình) hoặc client
                    String toPeer   = msg.toPeer;   // người bị ngắt
                    acceptedPeers.remove(fromPeer); //remove(toPeer)
                    writer.println(ControlProtocol.build(ControlProtocol.DISCONNECT_NOTIFY,
                            msg.toPeer, msg.fromPeer, "Disconnected"));
                    System.out.println("[ControlServer] Disconnected: " + fromPeer);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "control-handler");

        t.setDaemon(true);
        t.start();
    }

    private String buildFileListPayload() {
        if (fileShareService == null) return "";
        List<org.example.p2pfileshare.model.SharedFileLocal> list = fileShareService.listSharedFiles();
        StringBuilder sb = new StringBuilder();
        for (var f : list) {
            // Encode tab-safe by replacing tabs/newlines
            String name = safe(f.getFileName());
            String rel  = safe(f.getRelativePath());
            long size   = f.getSize();
            if (sb.length() > 0) sb.append("<NL>");
            sb.append(name).append('\t').append(rel).append('\t').append(size);
        }
        return sb.toString();
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\t", " ").replace("\n", " ");
    }
    public List<PeerInfo> getAcceptedPeers(List<PeerInfo> allPeers) {
        // Trả về danh sách peerId đã được chấp nhận kết nối
        return allPeers.stream()
                .filter(p -> acceptedPeers.contains(p.getPeerId()))
                .toList();
    }
    public void setOnPeerAccepted(Runnable callback) {
        this.onPeerAccepted = callback;
    }
    public boolean disconnectPeer(PeerInfo peer) {
        if (peer == null) return false;

        // 1) Xoá khỏi accepted set để chặn LIST_FILES ngay lập tức
        acceptedPeers.remove(peer.getPeerId());

        // 2) Gửi notify sang peer đó qua TCP controlPort
        try (Socket s = new Socket(peer.getIp(), peer.getControlPort());
             PrintWriter w = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true)) {

            String msg = ControlProtocol.build(
                    ControlProtocol.DISCONNECT_NOTIFY,
                    "SERVER",                 // from (mình).
                    peer.getPeerId(),         // to
                    "Bạn đã bị ngắt kết nối"
            );

            w.println(msg);
            System.out.println("[ControlServer] Sent DISCONNECT_NOTIFY to " + peer.getIp());
            return true;

        } catch (IOException e) {
            // Dù gửi fail thì cũng đã remove accept -> vẫn chặn được
            System.out.println("[ControlServer] Notify failed: " + e.getMessage());
            return false;
        }
    }

}