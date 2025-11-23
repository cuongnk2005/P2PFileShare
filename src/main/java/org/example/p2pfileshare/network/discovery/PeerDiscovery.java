package org.example.p2pfileshare.network.discovery;

import org.example.p2pfileshare.model.PeerInfo;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PeerDiscovery V2
 * - Cho phép nhiều peer chạy trên cùng 1 máy bằng cách mỗi peer bind 1 port khác nhau.
 * - Broadcast tìm peer qua một tập port cố định.
 */
public class PeerDiscovery {

    // Các port có thể dùng cho discovery (UDP)
    private static final int[] DISCOVERY_PORTS = {
            50000, 50001, 50002, 50003, 50004
    };

    private static final String DISCOVER_MSG    = "P2P_DISCOVER_REQUEST";
    private static final String RESPONSE_PREFIX = "P2P_DISCOVER_RESPONSE";

    // Lưu port mà responder của tiến trình này đã bind (chỉ để log/debug)
    private static volatile int boundPort = -1;
    private static volatile boolean responderStarted = false;

    /**
     * Khởi động 1 thread UDP responder:
     * - Lắng nghe DISCOVER_REQUEST trên một trong các DISCOVERY_PORTS
     * - Trả về: P2P_DISCOVER_RESPONSE|peerName|filePort|controlPort
     */
    public static void startResponder(String myName, int filePort, int controlPort) {
        if (responderStarted) return;
        responderStarted = true;

        Thread t = new Thread(() -> {
            DatagramSocket socket = null;
            try {
                // Thử bind lần lượt các port trong DISCOVERY_PORTS
                for (int p : DISCOVERY_PORTS) {
                    try {
                        socket = new DatagramSocket(p);
                        boundPort = p;
                        System.out.println("[Discovery] Responder bound on UDP port " + p);
                        break;
                    } catch (BindException ex) {
                        // Port đang dùng, thử port khác
                    }
                }

                if (socket == null) {
                    System.err.println("[Discovery] Không thể bind bất kỳ discovery port nào!");
                    return;
                }

                byte[] buf = new byte[1024];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                    if (!DISCOVER_MSG.equals(msg)) {
                        continue;
                    }

                    String ip = packet.getAddress().getHostAddress();

                    // Chuẩn format: P2P_DISCOVER_RESPONSE|Tên|filePort|controlPort
                    String response = RESPONSE_PREFIX + "|" +
                            myName + "|" +
                            filePort + "|" +
                            controlPort;

                    byte[] respData = response.getBytes();
                    DatagramPacket resp = new DatagramPacket(
                            respData,
                            respData.length,
                            packet.getAddress(),
                            packet.getPort()
                    );
                    socket.send(resp);

                    System.out.println("[Discovery] Reply to " + ip + ":" + packet.getPort() +
                            " -> " + response);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                responderStarted = false;
            }
        }, "discovery-responder");

        t.setDaemon(true);
        t.start();
    }

    /**
     * Gửi broadcast DISCOVER_REQUEST, đợi phản hồi trong timeoutMillis,
     * trả về danh sách PeerInfo tìm được.
     */
    public static List<PeerInfo> discoverPeers(String myName, int timeoutMillis) {

        List<PeerInfo> result = new ArrayList<>();

        try (DatagramSocket socket = new DatagramSocket()) {

            socket.setBroadcast(true);
            socket.setSoTimeout(timeoutMillis);

            byte[] data = DISCOVER_MSG.getBytes();

            // Gửi broadcast đến tất cả DISCOVERY_PORTS
            for (int port : DISCOVERY_PORTS) {
                DatagramPacket packet = new DatagramPacket(
                        data,
                        data.length,
                        InetAddress.getByName("255.255.255.255"),
                        port
                );
                socket.send(packet);
            }
            System.out.println("[Discovery] Sent broadcast to ports range");

            byte[] buf = new byte[1024];
            long start = System.currentTimeMillis();

            while (System.currentTimeMillis() - start < timeoutMillis) {
                try {
                    DatagramPacket resp = new DatagramPacket(buf, buf.length);
                    socket.receive(resp);

                    String msg = new String(resp.getData(), 0, resp.getLength()).trim();

                    if (msg.startsWith(RESPONSE_PREFIX)) {
                        // Format: P2P_DISCOVER_RESPONSE|Tên|filePort|controlPort
                        String[] parts = msg.split("\\|");
                        if (parts.length >= 4) {
                            String peerName      = parts[1];
                            int peerFilePort     = Integer.parseInt(parts[2]);
                            int peerControlPort  = Integer.parseInt(parts[3]);
                            String ip            = resp.getAddress().getHostAddress();

                            // Bỏ qua chính mình
                            if (peerName.equalsIgnoreCase(myName)) {
                                continue;
                            }

                            PeerInfo peer = new PeerInfo(
                                    peerName,
                                    ip,
                                    peerFilePort,
                                    peerControlPort,
                                    "Online"
                            );
                            result.add(peer);

                            System.out.println("[Discovery] Found: " + peerName +
                                    " @ " + ip +
                                    " filePort=" + peerFilePort +
                                    " ctrlPort=" + peerControlPort);
                        }
                    }

                } catch (SocketTimeoutException e) {
                    // hết thời gian đợi -> thoát vòng while
                    break;

                } catch (IOException ex) {
                    ex.printStackTrace();
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
