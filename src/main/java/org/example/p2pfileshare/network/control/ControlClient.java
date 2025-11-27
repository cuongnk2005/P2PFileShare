package org.example.p2pfileshare.network.control;

import org.example.p2pfileshare.model.PeerInfo;

import java.io.*;
import java.net.Socket;

/**
 * Client cho kênh điều khiển:
 * - Gửi CONNECT_REQUEST tới peer khác.
 * - Đọc CONNECT_ACCEPT / CONNECT_REJECT trả về.
 */
public class ControlClient {

    private final String myPeerId;
    private final String myDisplayName;

    public ControlClient(String myPeerId, String myDisplayName) {
        this.myPeerId = myPeerId;
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
                    toPeer      // to (peerId)
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
}
