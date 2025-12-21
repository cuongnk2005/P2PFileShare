package org.example.p2pfileshare.network.control;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.example.p2pfileshare.model.PeerInfo;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Client cho kênh điều khiển:
 * - Gửi CONNECT_REQUEST tới peer khác.
 * - Đọc CONNECT_ACCEPT / CONNECT_REJECT trả về.
 * - LIST_FILES để lấy danh sách file chia sẻ (tối giản: tên/relative/size)
 */
public class ControlClient {

    private final String myPeerId;
    private String myDisplayName;

    public ControlClient(String myPeerId, String myDisplayName) {
        this.myPeerId = myPeerId;
        this.myDisplayName = myDisplayName;
    }

    public void setMyDisplayName(String myDisplayName) {
        this.myDisplayName = myDisplayName;
    }

    /**
     * Gửi yêu cầu kết nối tới 1 peer, dùng info từ PeerInfo.
     * RETURN:
     *   true  -> peer kia ACCEPT
     *   false -> peer kia REJECT hoặc lỗi
     */
    public boolean sendConnectRequest(PeerInfo peer) {
        return sendConnectRequest(peer.getIp(), peer.getControlPort(), peer.getPeerId());
    }

    /**
     * Gửi CONNECT_REQUEST tới host:controlPort.
     * toPeer ở đây là peerId của peer đích.
     */
    public boolean sendConnectRequest(String host, int controlPort, String toPeer) {
        System.out.println("[ControlClient] Connecting to " + host + ":" + controlPort +
                " to request connect → " + toPeer);

        try (Socket socket = new Socket(host, controlPort);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream()), true)) {

            // 1) Gửi CONNECT_REQUEST (gửi peerId làm from)
            String msg = ControlProtocol.build(
                    ControlProtocol.CONNECT_REQUEST,
                    myPeerId,   // from (peerId)
                    toPeer,
                    myDisplayName// to (peerId)
            );

            writer.println(msg);
            System.out.println("[ControlClient] Sent: " + msg);

            // 2) Đọc 1 dòng response
            String respRaw = reader.readLine();
            System.out.println("[ControlClient] Received: " + respRaw);

            if (respRaw == null || respRaw.isEmpty()) {
                return false;
            }

            ControlProtocol.ParsedMessage parsed = ControlProtocol.parse(respRaw);
            if (parsed == null) {
                return false;
            }

            // EXPECT:
            //   fromPeer = bên kia (peerId)
            //   toPeer   = myPeerId
            if (!myPeerId.equals(parsed.toPeer)) {
                System.out.println("[ControlClient] Response not for me → " + parsed.toPeer);
                return false;
            }

            if (ControlProtocol.CONNECT_ACCEPT.equals(parsed.command)) {
                System.out.println("[ControlClient] CONNECT_ACCEPT from " + parsed.fromPeer);
                return true;

            } else if (ControlProtocol.CONNECT_REJECT.equals(parsed.command)) {
                System.out.println("[ControlClient] CONNECT_REJECT from " + parsed.fromPeer +
                        " reason=" + parsed.note);
                return false;
            } else {
                System.out.println("[ControlClient] Unknown command: " + parsed.command);
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy danh sách file chia sẻ từ peer đích qua kênh điều khiển
     */
    public List<RemoteFile> listFiles(PeerInfo peer) {
        return listFiles(peer.getIp(), peer.getControlPort(), peer.getPeerId());
    }

    public List<RemoteFile> listFiles(String host, int controlPort, String toPeer) {
        System.out.println("[ControlClient] Request LIST_FILES → " + toPeer);
        List<RemoteFile> files = new ArrayList<>();

        try (Socket socket = new Socket(host, controlPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            String msg = ControlProtocol.build(ControlProtocol.LIST_FILES, myPeerId, toPeer);
            writer.println(msg);

            String respRaw = reader.readLine();
            if (respRaw == null) return files;

            ControlProtocol.ParsedMessage parsed = ControlProtocol.parse(respRaw);
            if (parsed == null) return files;

            if (!myPeerId.equals(parsed.toPeer)) return files;

            if (ControlProtocol.LIST_FILES_RESPONSE.equals(parsed.command)) {
                String payload = parsed.note != null ? parsed.note : "";
                System.out.println("[DEBUG] Payload nhận được:\n" + payload);
                // Parse TSV lines: name\trelative\tsize
                String[] lines = payload.split("<NL>");
                System.out.println("Số dòng: " + lines.length);
                for (String line : lines) {
                    if (line.isBlank()) continue;
                    String[] cols = line.split("\t");
                    String name = cols.length > 0 ? cols[0] : "";
                    String rel  = cols.length > 1 ? cols[1] : name;
                    long size   = 0L;
                    try {
                        size = cols.length > 2 ? Long.parseLong(cols[2]) : 0L;
                    } catch (NumberFormatException ignored) {}
                    files.add(new RemoteFile(name, rel, size));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return files;
    }

    /**
     * Gửi yêu cầu ngắt kết nối tới peer (yêu cầu peer server xoá quyền truy cập của mình).
     * Trả về true nếu peer phản hồi DISCONNECT_NOTIFY (thành công), false nếu lỗi.
     */
    public boolean sendDisconnectRequest(PeerInfo peer) {
        if (peer == null) return false;
        return sendDisconnectRequest(peer.getIp(), peer.getControlPort(), peer.getPeerId());
    }

    public boolean sendDisconnectRequest(String host, int controlPort, String toPeer) {
        System.out.println("[ControlClient] Sending DISCONNECT_REQUEST to " + host + ":" + controlPort + " -> " + toPeer);
        try (Socket socket = new Socket(host, controlPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            String msg = ControlProtocol.build(
                    ControlProtocol.DISCONNECT_REQUEST,
                    myPeerId,   // from = mình
                    toPeer,     // to = peer đích
                    "Request disconnect"
            );

            writer.println(msg);
            System.out.println("[ControlClient] Sent: " + msg);

            // Đọc phản hồi 1 dòng (ControlServer hiện gửi DISCONNECT_NOTIFY)
            String respRaw = reader.readLine();
            System.out.println("[ControlClient] Received: " + respRaw);
            if (respRaw == null || respRaw.isBlank()) {
                return false;
            }

            ControlProtocol.ParsedMessage parsed = ControlProtocol.parse(respRaw);
            if (parsed == null) return false;

            // Nếu là DISCONNECT_NOTIFY cho mình -> gọi handler hiển thị alert và trả về true
            if (ControlProtocol.DISCONNECT_NOTIFY.equals(parsed.command) && myPeerId.equals(parsed.toPeer)) {
                handleDisconnectNotifyClient(parsed);
                return true;
            }

            return false;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // DTO đơn giản cho UI
    public static class RemoteFile {
        public final String name;
        public final String relativePath;
        public final long size;
        public RemoteFile(String name, String relativePath, long size) {
            this.name = name;
            this.relativePath = relativePath;
            this.size = size;
        }
    }

    // Hai hàm dưới này là "notify 1 chiều" – hiện tại chưa dùng,
    // nhưng để sẵn nếu sau này muốn gửi ACCEPT/REJECT trên connection khác.

    public void sendConnectAccept(String host, int controlPort, String toPeer) {
        sendOneWay(host, controlPort,
                ControlProtocol.build(ControlProtocol.CONNECT_ACCEPT, myPeerId, toPeer, "Accepted"));
    }

    public void sendConnectReject(String host, int controlPort, String toPeer, String reason) {
        sendOneWay(host, controlPort,
                ControlProtocol.build(ControlProtocol.CONNECT_REJECT, myPeerId, toPeer, reason));
    }

    private void sendOneWay(String host, int port, String msg) {
        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream()), true)) {

            writer.println(msg);
            System.out.println("[ControlClient] One-way send: " + msg);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDisconnectNotify(ControlProtocol.ParsedMessage msg) {
        String disconnectorName = msg.note != null ? msg.note : "Unknown";
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Ngắt kết nối");
            alert.setHeaderText("Bạn đã bị ngắt kết nối");
            alert.setContentText(disconnectorName);
            alert.showAndWait();
        });
    }
    private void handleDisconnectNotifyClient(ControlProtocol.ParsedMessage msg) {
        String disconnectorName = msg.note != null ? msg.note : "Unknown";
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Ngắt kết nối");
            alert.setHeaderText("ngắt kết nối thành công");
            alert.setContentText(disconnectorName);
            alert.showAndWait();
        });
    }
    public void broadcastUpdateName(List<PeerInfo> connectedPeers, String newName) {
        if (connectedPeers == null || connectedPeers.isEmpty()) {
            System.out.println("[ControlClient] broadcastUpdateName: no connected peers");
            return;
        }

        if (newName == null) newName = "";
        newName = newName.trim();
        if (newName.isEmpty()) {
            System.out.println("[ControlClient] broadcastUpdateName: newName is empty");
            return;
        }

        // Nếu bạn có ControlProtocol.UPDATE_NAME thì nên dùng build() cho đồng bộ format
        // Ví dụ: ControlProtocol.build(ControlProtocol.UPDATE_NAME, myPeerId, "*", newName);
        String msg = "UPDATE_NAME|" + myPeerId + "|" + newName;

        System.out.println("[ControlClient] broadcastUpdateName -> peers=" + connectedPeers.size()
                + " msg=" + msg);

        int ok = 0, fail = 0;

        for (PeerInfo p : connectedPeers) {
            if (p == null) continue;

            // tránh gửi cho chính mình nếu list có chứa mình
            if (myPeerId.equals(p.getPeerId())) continue;

            try {
                sendOneWay(p.getIp(), p.getControlPort(), msg);
                System.out.println("[ControlClient] UPDATE_NAME sent -> " + p.getPeerId()
                        + " (" + p.getIp() + ":" + p.getControlPort() + ")");
                ok++;
            } catch (Exception e) {
                System.out.println("[ControlClient] UPDATE_NAME FAIL -> " + p.getPeerId()
                        + " (" + p.getIp() + ":" + p.getControlPort() + ") err=" + e.getMessage());
                fail++;
            }
        }

        System.out.println("[ControlClient] broadcastUpdateName done. ok=" + ok + ", fail=" + fail);
    }

}
