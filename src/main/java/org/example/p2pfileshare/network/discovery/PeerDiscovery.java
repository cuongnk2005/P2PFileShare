package org.example.p2pfileshare.network.discovery;

import org.example.p2pfileshare.model.PeerInfo;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

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
    private static volatile boolean responderRunning = false;
    private static volatile DatagramSocket responderSocket = null;
    private static Thread responderThread = null;
    // chế độ lắng nghe, sử dụng thread
    public static synchronized void startResponder(
            String myPeerId,
            java.util.function.Supplier<String> displayNameSupplier,
            int filePort,
            int controlPort
    ) {
        if (responderRunning) {
            System.out.println("[Discovery] Responder already running");
            return;
        }

        responderRunning = true;

        responderThread = new Thread(() -> {
            DatagramSocket socket = null;
            try {
                // Thử bind các port
                for (int p : DISCOVERY_PORTS) {
                    try {
                        socket = new DatagramSocket(p);
                        boundPort = p;
                        responderSocket = socket;
                        System.out.println("[Discovery] Responder on UDP port: " + p);
                        break;
                    } catch (BindException ignored) {}
                }

                if (socket == null) {
                    System.err.println("[Discovery] Cannot bind any discovery port");
                    responderRunning = false;
                    return;
                }

                byte[] buf = new byte[1024];

                while (responderRunning) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        socket.receive(packet); // ⛔ block ở đây

                        String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                        if (!DISCOVER_MSG.equals(msg)) continue;

                        String response = RESPONSE_PREFIX + "|" +
                                myPeerId + "|" +
                                displayNameSupplier.get() + "|" +
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

                    } catch (SocketException se) {
                        // xảy ra khi socket.close() lúc stop
                        if (responderRunning) {
                            System.err.println("[Discovery] Socket error: " + se.getMessage());
                        }
                        break;
                    }
                }

            } catch (IOException e) {
                if (responderRunning) {
                    e.printStackTrace();
                }
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                responderSocket = null;
                responderRunning = false;
                System.out.println("[Discovery] Responder stopped");
            }

        }, "discovery-responder");

        responderThread.setDaemon(true);
        responderThread.start();
    }

    // Quét tìm peer trong LAN
    public static List<PeerInfo> discoverPeers(String myPeerId, int timeoutMillis) {

        List<PeerInfo> result = new ArrayList<>();

        try (DatagramSocket socket = new DatagramSocket()) {

            socket.setBroadcast(true);  // cho phép gửi broadcast
            socket.setSoTimeout(timeoutMillis); // timeout chỉ chờ trong 3s, quá thì thôi

            byte[] data = DISCOVER_MSG.getBytes();

            // Gửi broadcast đến tất cả DISCOVERY_PORTS
            for (int port : DISCOVERY_PORTS) {
                DatagramPacket packet = new DatagramPacket(
                        data,
                        data.length,
                        InetAddress.getByName("255.255.255.255"), // gửi cho tất cả các mạng LAN
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
                        // Format mới: P2P_DISCOVER_RESPONSE|peerId|displayName|filePort|controlPort
                        String[] parts = msg.split("\\|");
                        if (parts.length >= 5) {
                            String peerId         = parts[1];
                            String displayName    = parts[2];
                            int peerFilePort      = Integer.parseInt(parts[3]);
                            int peerControlPort   = Integer.parseInt(parts[4]);
                            String ip             = resp.getAddress().getHostAddress();

                            // Bỏ qua chính mình dựa trên peerId
                            if (peerId.equals(myPeerId)) {
                                continue;
                            }

                            PeerInfo peer = new PeerInfo(
                                    peerId,
                                    displayName,
                                    ip,
                                    peerFilePort,
                                    peerControlPort,
                                    "Online"
                            );
                            result.add(peer);

                            System.out.println("[Discovery] Found: " + displayName +
                                    " ip: " + ip +
                                    " filePort:" + peerFilePort +
                                    " ctrlPort:" + peerControlPort +
                                    " id:" + peerId);

                        } else if (parts.length >= 4) {
                            // Khoản hỗ trợ format cũ: P2P_DISCOVER_RESPONSE|name|filePort|controlPort
                            String peerName      = parts[1];
                            int peerFilePort     = Integer.parseInt(parts[2]);
                            int peerControlPort  = Integer.parseInt(parts[3]);
                            String ip            = resp.getAddress().getHostAddress();

                            // Bỏ qua chính mình nếu myPeerId equals peerName fallback
                            if (peerName.equals(myPeerId)) {
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
                                    " ip:" + ip +
                                    " filePort:" + peerFilePort +
                                    " ctrlPort:" + peerControlPort);
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
    public static synchronized void stopResponder() {
        if (!responderRunning) {
            System.out.println("[Discovery] Responder already stopped");
            return;
        }

        System.out.println("[Discovery] Stopping responder...");
        responderRunning = false;

        // Đóng socket để unblock receive()
        if (responderSocket != null && !responderSocket.isClosed()) {
            responderSocket.close();
        }

        // Đợi thread kết thúc (optional nhưng debug rất sướng)
        if (responderThread != null && responderThread.isAlive()) {
            try {
                responderThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        responderThread = null;
        responderSocket = null;
    }

}
